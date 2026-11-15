# Ciclo de vida dos tokens — Grossi Sports

Documento de apoio da Aula 16. Descreve como o par **access token +
refresh token** é emitido, renovado, rotacionado e revogado, os limites
de inatividade e de validade absoluta, e a principal limitação do
*logout* em uma arquitetura *stateless*.

Complementa [`docs/autenticacao-jwt.md`](autenticacao-jwt.md), que
descreve o JWT de acesso em si.

## 1. Visão geral

Até a Aula 15 o login devolvia apenas um **access token** JWT de 30
minutos. Ao expirar, o usuário precisava autenticar novamente. A partir
da Aula 16 o login passa a devolver também um **refresh token opaco**,
de vida mais longa, usado exclusivamente para obter novos access tokens
sem reenviar a senha.

```
POST /api/autenticacao/login
        │
        ├── accessToken  (JWT, 30 min) ──► rotas protegidas
        └── refreshToken (opaco)       ──► POST /api/autenticacao/refresh
                                                │
                                                ├── novo accessToken
                                                └── novo refreshToken (rotação)
```

| Token | Formato | Validade | Onde é usado |
|---|---|---|---|
| Access token | JWT assinado (HS256) | 30 minutos | Cabeçalho `Authorization: Bearer` |
| Refresh token | Opaco (32 bytes aleatórios) | Inatividade 30 min / absoluta 8 h | Corpo de `/refresh` e `/logout` |

O access token continua **autocontido** e validado localmente, sem
consulta ao banco. O refresh token, por outro lado, é **estado no
servidor**: o banco guarda apenas o hash SHA-256, e cada uso é
verificado e contabilizado.

## 2. Refresh token opaco

- Gerado com `SecureRandom` (32 bytes), codificado em **Base64 URL sem
  padding**.
- O valor em claro só existe na resposta do login/renovação e no cliente.
  O servidor persiste **apenas o hash SHA-256 em hexadecimal** — nunca o
  token em claro, que não aparece em logs.
- Como não carrega informação, não pode ser "decodificado" por um
  atacante: só é útil para quem o possui por inteiro.

## 3. Sessões e famílias

Cada login cria uma **família** de refresh tokens (`familiaId`, um UUID).
A primeira geração é gravada com o hash, o instante de criação, o
instante de última atividade e o **limite absoluto** da família.

Na renovação, o token apresentado é **rotacionado**: ele é marcado como
utilizado e uma **nova geração** é criada com o **mesmo `familiaId`** e o
**mesmo limite absoluto**. A última atividade é atualizada a cada
renovação.

```
família (familiaId = UUID)
 ├── geração 1  (utilizada)
 ├── geração 2  (utilizada)
 └── geração 3  (ativa)  ◄── somente ela renova
```

O access token emitido carrega o claim `sid` com o `familiaId`, o que
correlaciona o acesso à sessão renovável que o originou.

## 4. Limites de validade

As durações vêm de `app.security.refresh-token` e podem ser ajustadas por
variáveis de ambiente (valores padrão entre parênteses):

| Propriedade | Variável | Padrão | Significado |
|---|---|---|---|
| `idle-timeout` | `REFRESH_TOKEN_IDLE_TIMEOUT` | `PT30M` | Tempo máximo sem renovar |
| `absolute-expiration` | `REFRESH_TOKEN_ABSOLUTE_EXPIRATION` | `PT8H` | Tempo máximo da família, independente de atividade |

Regras aplicadas na renovação, nesta ordem:

1. **Reutilização** — o token já foi trocado.
2. **Revogação** — o token ou a família já foi revogada.
3. **Expiração absoluta** — passou do limite da família.
4. **Inatividade** — passou do limite desde a última atividade.
5. **Conta inativa** — a conta não pode mais autenticar.

As fronteiras são **inclusivas**: atingir exatamente o limite já expira.

## 5. Detecção de reutilização

Se um refresh token **já utilizado** for reapresentado, presume-se
vazamento (o token legítimo já foi trocado por um novo). Nesse caso a
**família inteira é revogada** com motivo `REUTILIZACAO`, incluindo o
sucessor legítimo — quem roubou o token não consegue continuar, e o dono
é forçado a autenticar novamente.

A revogação é persistida mesmo quando a renovação termina em erro: a
operação usa `@Transactional(noRollbackFor = RefreshTokenInvalidoException.class)`
para que a evidência de segurança sobreviva à exceção que gera o `401`.

## 6. Endpoints

| Endpoint | Método | Autenticação | Sucesso | Descrição |
|---|---|---|---|---|
| `/api/autenticacao/login` | POST | Pública | 200 | Emite access + refresh |
| `/api/autenticacao/refresh` | POST | Pública | 200 | Rotaciona e emite novo par |
| `/api/autenticacao/logout` | POST | Pública | 204 | Revoga a família do refresh token |
| `/api/autenticacao/logout-todos` | POST | Bearer | 204 | Revoga todas as sessões da conta |

### 6.1 Login

```
POST /api/autenticacao/login
Content-Type: application/json

{ "login": "operador.postman", "senha": "uma-frase-senha-2026" }
```

Resposta `200 OK`:

```json
{
  "accessToken": "eyJraWQiOi...",
  "tokenType": "Bearer",
  "expiresIn": 1800,
  "expiresAt": "2026-11-15T12:30:00Z",
  "refreshToken": "8fJ2s1kQ...",
  "refreshTokenExpiresAt": "2026-11-15T20:00:00Z"
}
```

### 6.2 Renovação

```
POST /api/autenticacao/refresh
Content-Type: application/json

{ "refreshToken": "8fJ2s1kQ..." }
```

Devolve o mesmo formato do login, com **novos** access e refresh tokens.
Rota pública: o refresh token é a credencial da operação.

### 6.3 Logout local

```
POST /api/autenticacao/logout
Content-Type: application/json

{ "refreshToken": "8fJ2s1kQ..." }
```

Revoga a família identificada pelo token e responde `204 No Content`.
É **idempotente e silencioso**: um token desconhecido, já revogado ou
expirado também responde `204`, sem revelar se a sessão existia. O motivo
registrado é `LOGOUT`.

### 6.4 Logout global

```
POST /api/autenticacao/logout-todos
Authorization: Bearer <accessToken>
```

Exige um access token válido e revoga **todas** as sessões da conta
autenticada, respondendo `204 No Content`. É útil quando se suspeita de
comprometimento da conta. O motivo registrado é `LOGOUT_GLOBAL`.

## 7. Limitação importante do logout

O logout revoga os **refresh tokens**, impedindo que a sessão seja
renovada. Ele **não invalida imediatamente os access tokens já
emitidos**: como o JWT é autocontido e validado localmente, um access
token continua válido até o seu `exp` original (no máximo 30 minutos).

Consequências práticas:

- Após o logout, o cliente deve descartar o access token localmente.
- Em caso de comprometimento, o logout global interrompe a renovação, mas
  os access tokens em circulação expiram naturalmente em até 30 minutos.
- Invalidação imediata de access token exigiria estado adicional (por
  exemplo, uma *denylist* de `jti` ou introspecção a cada requisição),
  o que está **fora do escopo** desta aula.

## 8. Modelo de dados

A tabela `sessao_refresh_token` (migração `010-sessoes-refresh-token`)
guarda cada geração:

| Coluna | Descrição |
|---|---|
| `conta_acesso_id` | Conta dona da sessão |
| `familia_id` | Família à qual a geração pertence |
| `token_hash` | SHA-256 (hex) do refresh token; único |
| `criada_em` | Criação da geração |
| `ultima_atividade_em` | Base do limite de inatividade |
| `expira_em` | Limite absoluto da família |
| `utilizada_em` | Quando a geração foi trocada |
| `revogada_em` | Quando a geração foi revogada |
| `motivo_revogacao` | Motivo (`LOGOUT`, `REUTILIZACAO`, ...) |

### Motivos de revogação

`LOGOUT`, `LOGOUT_GLOBAL`, `INATIVIDADE`, `EXPIRACAO_ABSOLUTA`,
`REUTILIZACAO`, `CONTA_INATIVA`.

## 9. Recomendações de segurança

- **Use HTTPS/TLS** para o refresh token e o access token.
- **Não registre tokens nem hashes em logs** (o projeto não o faz).
- **Guarde `JWT_SECRET` fora do Git** e nunca versione credenciais.
- **Rotacione sempre**: um refresh token usado deixa de valer.
- **Prefira o logout global** quando houver suspeita de vazamento.

## 10. Referências no projeto

- Serviço do ciclo de tokens: `src/main/java/com/curso/grossisports/service/SessaoAutenticacaoService.java`
- Entidade e motivo: `src/main/java/com/curso/grossisports/domain/SessaoRefreshToken.java` e `MotivoRevogacaoRefreshToken.java`
- Repositório: `src/main/java/com/curso/grossisports/repository/SessaoRefreshTokenRepository.java`
- Propriedades: `src/main/java/com/curso/grossisports/security/RefreshTokenProperties.java`
- Emissão do JWT com `sid`: `src/main/java/com/curso/grossisports/security/TokenService.java`
- Endpoints: `src/main/java/com/curso/grossisports/api/AutenticacaoController.java`
- Migração: `src/main/resources/db/changelog/changes/010-sessoes-refresh-token.yaml`
- Testes de serviço: `src/test/java/com/curso/grossisports/service/SessaoAutenticacaoServiceTest.java`
- Testes HTTP: `src/test/java/com/curso/grossisports/AutenticacaoSessaoApiTest.java`
- JWT de acesso: [`docs/autenticacao-jwt.md`](autenticacao-jwt.md)
