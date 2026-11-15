# Auditoria e proteção da autenticação — Grossi Sports

Documento de apoio da Aula 17. Descreve o **modelo de ameaças**, a
**política de proteção contra tentativas automatizadas de login**, a
**trilha de auditoria de segurança**, o tratamento de **concorrência e
transações**, a **autorização** da consulta administrativa e os
**riscos residuais e limitações** da solução.

Complementa [`docs/autenticacao-jwt.md`](autenticacao-jwt.md),
[`docs/ciclo-tokens.md`](ciclo-tokens.md) e
[`docs/autorizacao-perfis.md`](autorizacao-perfis.md).

## 1. Objetivo e escopo

Até a Aula 16 a API autenticava corretamente, mas **não registrava o que
acontecia** nem **desacelerava quem tentava repetidamente**. A Aula 17
acrescenta duas capacidades:

1. **Proteção contra tentativas automatizadas de login** — um contador
   persistente por identificador que ativa um bloqueio temporário após
   um número configurável de falhas dentro de uma janela.
2. **Trilha de auditoria de segurança** — eventos estruturados e somente
   leitura, com consulta administrativa paginada.

O escopo é deliberadamente restrito. **Não** entram nesta aula: WAF,
CAPTCHA, MFA, SIEM, detecção comportamental, reputação de IP, *rate
limiter* de borda nem introspecção de token. Esses controles pertencem a
camadas externas e são discutidos como riscos residuais na seção 11.

## 2. Modelo de ameaças

O ativo a proteger é o **acesso autenticado** à API. As ameaças tratadas
e a postura adotada:

| Ameaça | Descrição | Resposta desta aula |
|---|---|---|
| Força bruta online | Muitas tentativas de senha contra um login | Contador de falhas + bloqueio temporário |
| *Password spraying* | Poucas senhas por vez contra muitos logins | Limitação documentada (seção 11) |
| Enumeração de contas | Descobrir quais logins existem | Resposta `401` genérica e idêntica |
| *Log injection* | Injetar texto/segredos na trilha | Tipo enumerado, campos validados, sem texto livre |
| Vazamento por auditoria | Gravar senha ou token na trilha | Minimização e validação (seção 6) |
| Falha de auditoria silenciosa | Ação sensível sem registro | Evento na mesma transação da ação (seção 7) |
| Consulta indevida da trilha | Operador lendo eventos | Autorização em duas camadas (seção 8) |

Premissas de fronteira: a API roda atrás de TLS; o banco é confiável; a
resposta de login não revela o motivo exato da recusa. A solução
**não** afirma não repúdio criptográfico: a trilha é um registro de
aplicação, não uma evidência assinada.

## 3. Política de proteção contra tentativas automatizadas

A política vem de `app.security.authentication-protection` e é validada
na **inicialização** — a aplicação falha ao subir se algum valor for
nulo, zero ou negativo:

| Propriedade | Variável de ambiente | Padrão | Significado |
|---|---|---|---|
| `max-failed-attempts` | `AUTH_PROTECTION_MAX_ATTEMPTS` | `5` | Falhas consecutivas que ativam o bloqueio |
| `failure-window` | `AUTH_PROTECTION_FAILURE_WINDOW` | `PT15M` | Intervalo em que as falhas são agrupadas |
| `lock-duration` | `AUTH_PROTECTION_LOCK_DURATION` | `PT15M` | Tempo em que novas tentativas são recusadas |

Os valores padrão (5 falhas / 15 min / 15 min) são **didáticos e
ajustáveis**, não uma recomendação universal.

Regras aplicadas pelo contador:

- O login é **normalizado** com `trim()` e minúsculas
  (`Locale.ROOT`) antes de qualquer decisão. Variações de caixa e
  espaços contam para o **mesmo** identificador.
- A falha fora da janela **reinicia** a contagem e reabre a janela no
  instante atual.
- O bloqueio é ativado **exatamente** quando a contagem alcança o
  máximo (na 5ª falha, no padrão).
- A liberação ocorre no **instante exato** de `bloqueadaAte`: o bloqueio
  vale enquanto `agora` for estritamente anterior ao fim.
- Uma autenticação válida **limpa** falhas, janela e bloqueio.

O bloqueio temporário **não** é o `StatusContaAcesso.BLOQUEADA`:
aquele é uma decisão administrativa persistente; este é uma contenção
automática que se dissolve sozinha.

A recusa por bloqueio acontece **antes** de qualquer comparação BCrypt:
uma senha correta durante o bloqueio é recusada sem custo de hash. A
resposta é sempre o mesmo `401` genérico (`Login ou senha inválidos`),
para login inexistente, senha incorreta, conta bloqueada e bloqueio
temporário ativo.

## 4. Contador de falhas e concorrência

O contador vive na tabela `protecao_autenticacao` e guarda **apenas o
hash SHA-256** do login normalizado — o login original **nunca** é
persistido:

| Coluna | Descrição |
|---|---|
| `chave_login` | SHA-256 hexadecimal do login normalizado; único |
| `falhas_consecutivas` | Contagem na janela corrente |
| `janela_iniciada_em` | Início da janela de observação |
| `bloqueada_ate` | Fim do bloqueio temporário, quando ativo |

**Não há `Map` em memória.** O estado é persistente para sobreviver a
reinícios e para valer entre instâncias. Duas falhas simultâneas não
podem perder atualização:

1. A primeira linha é criada com *upsert* nativo do PostgreSQL
   (`INSERT ... ON CONFLICT (chave_login) DO NOTHING`), que tolera a
   corrida de duas transações tentando inserir a mesma chave.
2. A releitura para escrita usa `SELECT ... FOR UPDATE`
   (`PESSIMISTIC_WRITE`): a linha permanece bloqueada até o fim da
   transação, de modo que a segunda falha concorrente só lê o valor já
   atualizado.

O teste de concorrência exercita exatamente esse cenário: três falhas
sequenciais mais duas simultâneas resultam em **5 falhas** e **um único**
evento `BLOQUEIO_TEMPORARIO_ATIVADO`.

## 5. Trilha de auditoria: eventos

Cada evento é uma linha de `evento_seguranca` com um vocabulário
**fechado** de 12 tipos (`TipoEventoSeguranca`), nunca texto livre:

| Grupo | Tipos |
|---|---|
| Login | `LOGIN_SUCESSO`, `LOGIN_FALHA`, `LOGIN_RECUSADO_BLOQUEIO` |
| Bloqueio | `BLOQUEIO_TEMPORARIO_ATIVADO` |
| Conta | `CONTA_CRIADA`, `SENHA_ALTERADA`, `PERFIS_ALTERADOS`, `STATUS_ALTERADO` |
| Sessão | `REFRESH_SUCESSO`, `REFRESH_FALHA`, `LOGOUT`, `LOGOUT_GLOBAL` |

Cada linha responde a quatro perguntas de investigação:

| Campo | Pergunta | Observação |
|---|---|---|
| `tipo` | O que ocorreu? | Enumerado fechado |
| `ocorridoEm` | Quando? | Instante do `Clock` controlável |
| `atorContaId` | Quem iniciou? | ID histórico, quando conhecido |
| `alvoContaId` | Quem foi afetado? | ID histórico, quando conhecido |
| `identificadorHash` | Correlação | SHA-256 do login, quando aplicável |
| `enderecoOrigem` | De onde? | `getRemoteAddr()` da requisição |

Os identificadores de conta são **valores históricos**, não associações
JPA: a trilha não depende do ciclo de vida posterior da conta e não é
apagada nem alterada se a conta for removida.

A origem é capturada no login e nas operações administrativas pelo
`HttpServletRequest.getRemoteAddr()`. O projeto **não confia** em
`X-Forwarded-For` para esta decisão: o cabeçalho é controlado pelo
cliente e, sem um *proxy* confiável configurado, forjaria a origem. No
refresh e no logout, que não recebem `HttpServletRequest` no serviço, a
origem é registrada como nula — evitando alterar assinaturas já
existentes.

## 6. Minimização de dados

A trilha **nunca** grava:

- senha em texto aberto;
- hash BCrypt da senha;
- access token ou refresh token;
- hash do refresh token;
- cabeçalho `Authorization`;
- corpo da requisição;
- o login original em falhas de autenticação.

Em falhas de login aparece apenas o `identificadorHash` de correlação.
A estrutura é fechada para reduzir *log injection*:

- `identificadorHash`, quando presente, precisa casar com
  `[0-9a-f]{64}`;
- `enderecoOrigem` tem no máximo **45** caracteres (tamanho de um IPv6
  textual) e rejeita `\r` e `\n`;
- não existe campo de mensagem livre.

> **Minimização, não anonimização.** O hash reduz a exposição do login,
> mas **não** o torna irreversível: logins costumam ser previsíveis e
> podem ser confirmados por força bruta offline. Por isso o hash é
> tratado como **minimização de dados**, não como anonimização.

## 7. Transações

A fronteira transacional depende do tipo de operação:

- **Falhas de autenticação.** `AutenticacaoService.autenticar` **não** é
  transacional. `ProtecaoAutenticacaoService.registrarFalha` abre sua
  própria transação e **confirma antes** de a exceção `401` propagar.
  Sem isso, a exceção desfaria o contador e a evidência de segurança.
- **Ações administrativas.** `AuditoriaSegurancaService.registrar` é
  `@Transactional` (`REQUIRED`) e participa da transação corrente. O
  evento e a mudança de negócio (criação, senha, perfis, status)
  confirmam ou revertem **juntos**: se a gravação obrigatória do evento
  falhar, a ação sensível também não é confirmada — a implementação
  **falha fechada**.
- **Ciclo de tokens.** `SessaoAutenticacaoService.renovar` usa
  `@Transactional(noRollbackFor = RefreshTokenInvalidoException.class)`
  para que a revogação gravada nos caminhos de reutilização, expiração e
  conta inativa sobreviva à exceção; o evento `REFRESH_FALHA` é gravado
  na fronteira transacional, antes de a exceção propagar.

## 8. Autorização da consulta

A consulta da trilha exige `ADMINISTRADOR` em **duas camadas** (defesa
em profundidade):

1. A política HTTP em `SecurityConfig` aplica
   `hasRole("ADMINISTRADOR")` a `GET /api/auditoria-seguranca`.
2. O método `AuditoriaSegurancaService.consultar` reafirma a regra com
   `@PreAuthorize("hasRole('ADMINISTRADOR')")`.

Consequências: um `OPERADOR` autenticado recebe `403 Forbidden`; um
`ADMINISTRADOR` recebe `200 OK`; sem token, a resposta é `401`. Não
existe endpoint de escrita — a trilha é **somente leitura** pela API.

## 9. Endpoint de consulta

| Endpoint | Método | Autenticação | Sucesso | Erros documentados |
|---|---|---|---|---|
| `/api/auditoria-seguranca` | GET | Bearer (ADMINISTRADOR) | 200 OK | 400, 401, 403 |

Parâmetros de consulta:

| Parâmetro | Padrão | Descrição |
|---|---|---|
| `tipo` | (ausente) | Filtro opcional por tipo de evento |
| `page` | `0` | Número da página (base zero) |
| `size` | `20` | Tamanho da página (1 a 100) |

A ordenação é **cronológica decrescente** por `ocorridoEm`, com `id`
como desempate — coerente com a leitura de um incidente (o mais recente
primeiro). O DTO `EventoSegurancaResponse` expõe `id`, `tipo`,
`ocorridoEm`, `atorContaId`, `alvoContaId` e `enderecoOrigem`, mas
**não** o `identificadorHash` de correlação. `tipo` inválido e `size`
fora de `1..100` produzem `400`.

## 10. Matriz ameaça–controle–evidência–risco

| Ameaça | Controle | Evidência no projeto | Risco residual |
|---|---|---|---|
| Força bruta contra um login | Contador + bloqueio temporário | `ProtecaoAutenticacao`, `ProtecaoAutenticacaoService`, `ProtecaoAutenticacaoApiTest` | Bloqueio temporário, não definitivo |
| Corrida de falhas simultâneas | Upsert + `PESSIMISTIC_WRITE` | `ProtecaoAutenticacaoServiceConcorrenciaTest` | Contenção em picos no mesmo login |
| Enumeração de contas | `401` genérico idêntico | `ProtecaoAutenticacaoApiTest` (genérica) | Tempo de resposta pode variar |
| *Log injection* | Tipo enumerado + validação de campos | `EventoSeguranca`, `EventoSegurancaTest` | — (estrutura fechada) |
| Vazamento por auditoria | Minimização e validação | `EventoSeguranca`, testes de segredos | Hash ainda é dado pessoal pseudônimo |
| Falha de auditoria silenciosa | Evento na mesma transação | `AuditoriaFalhaReverteAcaoTest` | Indisponibilidade do banco bloqueia a ação |
| Consulta indevida da trilha | Autorização em duas camadas | `AuditoriaSegurancaApiTest`, `SecurityConfig` | Administrador comprometido lê a trilha |
| *Password spraying* distribuído | (parcial) contador por login | Documentado na seção 11 | **Não mitigado** nesta aula |

## 11. Riscos residuais e limitações

- **Spraying distribuído.** O contador é por **login**, não por origem.
  Um atacante com muitos IPs que use poucas senhas por login contra
  muitas contas não é desacelerado por esta política. Mitigação exigiria
  reputação de IP, limites por origem ou detecção comportamental —
  **fora do escopo**.
- **Nenhum controle de borda.** Não há WAF, CAPTCHA, MFA, SIEM, *rate
  limiter* de borda nem introspecção de token. Esses controles
  complementares não substituem esta proteção nem são substituídos por
  ela.
- **DoS por bloqueio.** Um atacante pode bloquear deliberadamente o
  login de um usuário legítimo ao errar a senha até o limite. É o
  *trade-off* clássico do *lockout*: a proteção também é um vetor de
  negação de serviço.
- **Crescimento de tabelas.** `evento_seguranca` e
  `protecao_autenticacao` crescem continuamente. Não há expurgo nem
  particionamento nesta aula; retenção precisa de política operacional
  (seção 12).
- **Sem não repúdio criptográfico.** A trilha não é assinada nem
  encadeada. Um administrador com acesso ao banco poderia, em tese,
  alterá-la. Não se afirma não repúdio.
- **Bloqueio temporário, não definitivo.** Ao fim de `lock-duration` as
  tentativas voltam a ser aceitas; a contenção é uma desaceleração, não
  uma proibição.

## 12. Retenção e privacidade

A trilha contém **dados pessoais pseudonimizados**: `enderecoOrigem`
(IP) e `identificadorHash` (derivado do login). Recomendações:

- Definir uma **retenção** alinhada à necessidade de investigação (por
  exemplo, alguns meses) e um processo de expurgo periódico — **não**
  implementado nesta aula.
- Restringir a consulta a `ADMINISTRADOR` (já feito) e tratar os dados
  como sensíveis em *backups* e exportações.
- Tratar o hash como **minimização**, nunca como anonimização (seção 6).

## 13. Configuração

```
# Aula 17 - Proteção contra tentativas automatizadas de login
app.security.authentication-protection.max-failed-attempts=${AUTH_PROTECTION_MAX_ATTEMPTS:5}
app.security.authentication-protection.failure-window=${AUTH_PROTECTION_FAILURE_WINDOW:PT15M}
app.security.authentication-protection.lock-duration=${AUTH_PROTECTION_LOCK_DURATION:PT15M}
```

As três propriedades são validadas na inicialização. A migração
`011-auditoria-protecao-autenticacao` cria `protecao_autenticacao`,
`evento_seguranca` e os índices de leitura cronológica, filtro por tipo e
correlação por identificador.

## 14. Referências no projeto

- Propriedades: `src/main/java/com/curso/grossisports/security/ProtecaoAutenticacaoProperties.java`
- Contador: `src/main/java/com/curso/grossisports/domain/ProtecaoAutenticacao.java`
- Repositório do contador: `src/main/java/com/curso/grossisports/repository/ProtecaoAutenticacaoRepository.java`
- Serviço de proteção: `src/main/java/com/curso/grossisports/service/ProtecaoAutenticacaoService.java`
- Evento e tipos: `src/main/java/com/curso/grossisports/domain/EventoSeguranca.java` e `TipoEventoSeguranca.java`
- Serviço de auditoria: `src/main/java/com/curso/grossisports/service/AuditoriaSegurancaService.java`
- Consulta: `src/main/java/com/curso/grossisports/api/AuditoriaSegurancaController.java`
- DTO e *mapper*: `src/main/java/com/curso/grossisports/api/dto/EventoSegurancaResponse.java` e `api/mapper/EventoSegurancaMapper.java`
- Migração: `src/main/resources/db/changelog/changes/011-auditoria-protecao-autenticacao.yaml`
- Testes de domínio: `src/test/java/com/curso/grossisports/domain/ProtecaoAutenticacaoTest.java` e `EventoSegurancaTest.java`
- Testes de propriedades: `src/test/java/com/curso/grossisports/security/ProtecaoAutenticacaoPropertiesTest.java` e `ProtecaoAutenticacaoPropertiesBindingTest.java`
- Teste de concorrência: `src/test/java/com/curso/grossisports/service/ProtecaoAutenticacaoServiceConcorrenciaTest.java`
- Testes HTTP: `src/test/java/com/curso/grossisports/ProtecaoAutenticacaoApiTest.java` e `AuditoriaSegurancaApiTest.java`
- Teste de rollback: `src/test/java/com/curso/grossisports/AuditoriaFalhaReverteAcaoTest.java`
- JWT de acesso: [`docs/autenticacao-jwt.md`](autenticacao-jwt.md)
- Ciclo de tokens: [`docs/ciclo-tokens.md`](ciclo-tokens.md)
- Autorização por perfis: [`docs/autorizacao-perfis.md`](autorizacao-perfis.md)
