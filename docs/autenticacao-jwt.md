# Autenticação com JWT — Grossi Sports

Documento de apoio da Aula 13. Descreve como obter e usar o token
Bearer, quais claims são emitidos, as limitações de validade e
revogação e as recomendações de uso seguro.

## 1. Visão geral

A partir da Aula 13 a API deixou de usar **HTTP Basic** (credencial
reenviada a cada requisição) e passou a usar **OAuth2 Resource Server
com JWT Bearer**. O login e a senha são enviados apenas ao endpoint de
autenticação; as chamadas seguintes apresentam somente o token.

O fluxo é *stateless*: o servidor não mantém sessão. A autenticação
das requisições protegidas é decidida pela validação local do token.

```
POST /api/autenticacao/login  ──►  accessToken (JWT, 30 min)
                                        │
                                        ▼
Authorization: Bearer <accessToken>  ──►  rotas protegidas
```

## 2. Como obter o token

Requisição pública (não exige token):

```
POST /api/autenticacao/login
Content-Type: application/json

{
  "login": "operador.postman",
  "senha": "uma-frase-senha-2026"
}
```

Resposta `200 OK`:

```json
{
  "accessToken": "eyJraWQiOi...",
  "tokenType": "Bearer",
  "expiresIn": 1800,
  "expiresAt": "2026-10-29T21:30:00Z"
}
```

- `accessToken`: JWT assinado.
- `tokenType`: sempre `Bearer`.
- `expiresIn`: validade em segundos (1800 = 30 minutos).
- `expiresAt`: instante absoluto de expiração (UTC).

## 3. Como usar o token

Envie o token no cabeçalho `Authorization`:

```
GET /api/contas-acesso/me
Authorization: Bearer eyJraWQiOi...
```

Sem token, com token malformado, com assinatura inválida ou expirado,
a resposta é `401 Unauthorized`. O HTTP Basic deixou de funcionar: mesmo
com login e senha corretos, uma requisição Basic recebe `401`.

## 4. Claims emitidos

O token contém:

| Claim | Significado |
|---|---|
| `iss` | Emissor (`https://grossi-sports.local`) |
| `sub` | Login da conta autenticada |
| `jti` | Identificador único do token |
| `iat` | Instante de emissão |
| `exp` | Instante de expiração (emissão + 30 min) |
| `perfis` | Perfis derivados das authorities (ex.: `["OPERADOR"]`) |

Os `perfis` vêm exclusivamente das authorities do usuário autenticado,
nunca do corpo da requisição, o que impede que o cliente conceda a si
mesmo um perfil.

## 5. Assinatura e chave

- Algoritmo fixo: **HS256** (HMAC-SHA256) na emissão e na validação.
- Chave simétrica lida de `JWT_SECRET`, no arquivo `.env` (ignorado
  pelo Git). A chave **não** é versionada nem aparece na documentação.
- A chave deve ter **pelo menos 32 bytes** (256 bits), codificada em
  Base64. A aplicação falha na inicialização se a variável estiver
  ausente, não for Base64 válido ou tiver menos de 32 bytes.
- O profile de testes usa uma chave **exclusiva de teste**, que não é
  segredo de produção.

Gerar uma chave (fora do código):

```
# PowerShell
$b = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b)
[Convert]::ToBase64String($b)

# macOS/Linux
openssl rand -base64 32
```

## 6. Validação do token

O Resource Server valida:

1. **Assinatura** — garante que o token foi emitido por quem conhece a
   chave e que não foi alterado.
2. **Emissor** (`iss`) — precisa ser exatamente o emissor configurado.
3. **Expiração** (`exp`) — token vencido é rejeitado.

Um JWT é **assinado**, não criptografado: o conteúdo é legível por
qualquer pessoa que possua o token. Por isso, o token nunca deve
carregar dados sensíveis — neste projeto ele contém apenas `iss`,
`sub`, `jti`, `iat`, `exp` e `perfis`.

## 7. Credenciais inválidas

Login inexistente, senha incorreta e conta bloqueada produzem a mesma
resposta `401` com mensagem genérica (`Login ou senha inválidos`). Isso
evita que um atacante descubra, pelas respostas, quais logins existem.

## 8. Limitações (escopo da aula)

- **Validade fixa de 30 minutos.** Não há renovação automática de token
  nesta aula; ao expirar, é necessário autenticar novamente. *Refresh
  token está fora do escopo.*
- **Sem revogação.** Como a validação é local e *stateless*, um token já
  emitido permanece válido até `exp`, mesmo que a conta seja bloqueada
  ou a senha seja alterada nesse intervalo. *Logout com revogação de
  token está fora do escopo.*
- **Sem autorização por perfil.** Os perfis são emitidos no token, mas
  ainda não restringem rotas específicas nesta aula.
- **Chave simétrica (HS256).** Emissor e validador compartilham a mesma
  chave. *RSA/JWK Set está fora do escopo.*

## 9. Recomendações de segurança

- **Use HTTPS/TLS.** O token concede acesso enquanto estiver válido;
  transmiti-lo em texto claro permite que seja interceptado e reusado.
- **Não registre tokens em logs** nem os inclua em relatórios.
- **Mantenha `JWT_SECRET` fora do Git** (apenas no `.env` local ou em
  um cofre de segredos da plataforma).
- **Não fixe credenciais** em coleções Postman ou arquivos versionados;
  use variáveis preenchidas localmente.

## 10. Referências no projeto

- Configuração de segurança: `src/main/java/com/curso/grossisports/SecurityConfig.java`
- Infraestrutura JWT: `src/main/java/com/curso/grossisports/security/JwtConfig.java`
- Emissão de claims: `src/main/java/com/curso/grossisports/security/TokenService.java`
- Endpoint de login: `src/main/java/com/curso/grossisports/api/AutenticacaoController.java`
- Testes: `src/test/java/com/curso/grossisports/ContaAcessoSecurityTest.java`
- Contrato OpenAPI: `docs/api/api-docs.yaml`
- Documentação OpenAPI: `docs/api/documentacao-openapi.md`
