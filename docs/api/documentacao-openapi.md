# Documentação da API — Grossi Sports

## 1. Objetivo

A documentação da API Grossi Sports descreve os recursos disponíveis para o gerenciamento de produtos esportivos, grupos de produtos, fornecedores, movimentações de estoque, pessoas, clientes, colaboradores, vendas e contas de acesso. A especificação OpenAPI permite consultar os endpoints, os parâmetros, os dados de entrada e saída, os esquemas de segurança e as respostas HTTP documentadas.

## 2. Operações e respostas HTTP

A tabela apresenta as operações identificadas na especificação OpenAPI do projeto, seus métodos HTTP e os códigos de resposta registrados.

| Endpoint | Método HTTP | Sucesso documentado | Erros documentados |
|---|---|---|---|
| `/api/produtos` | GET | 200 OK | 400 Bad Request |
| `/api/produtos` | POST | 201 Created | 400 Bad Request, 404 Not Found, 409 Conflict |
| `/api/produtos/{id}` | GET | 200 OK | 404 Not Found |
| `/api/produtos/{id}` | PUT | 200 OK | 400 Bad Request, 404 Not Found, 409 Conflict |
| `/api/produtos/{id}/estoque/entradas` | POST | 200 OK | 400 Bad Request, 404 Not Found |
| `/api/produtos/{id}/estoque/saidas` | POST | 200 OK | 400 Bad Request, 404 Not Found |
| `/api/produtos/{id}/ativar` | PATCH | 200 OK | 404 Not Found |
| `/api/produtos/{id}/inativar` | PATCH | 200 OK | 404 Not Found |
| `/api/grupos-produtos` | GET | 200 OK | 400 Bad Request |
| `/api/grupos-produtos` | POST | 201 Created | 400 Bad Request, 409 Conflict |
| `/api/grupos-produtos/{id}` | GET | 200 OK | 404 Not Found |
| `/api/grupos-produtos/{id}` | PUT | 200 OK | 400 Bad Request, 404 Not Found, 409 Conflict |
| `/api/grupos-produtos/{id}` | DELETE | 204 No Content | 404 Not Found, 409 Conflict |
| `/api/grupos-produtos/{id}/ativar` | PATCH | 200 OK | 404 Not Found |
| `/api/grupos-produtos/{id}/inativar` | PATCH | 200 OK | 404 Not Found |
| `/api/fornecedores` | GET | 200 OK | Não especificados na documentação |
| `/api/fornecedores` | POST | 201 Created | 400 Bad Request, 409 Conflict |
| `/api/pessoas` | GET | 200 OK | 400 Bad Request |
| `/api/pessoas` | POST | 201 Created | 400 Bad Request, 409 Conflict |
| `/api/pessoas/{id}` | GET | 200 OK | 404 Not Found |
| `/api/pessoas/{id}` | PUT | 200 OK | 400 Bad Request, 404 Not Found, 409 Conflict |
| `/api/pessoas/{id}/status` | PUT | 200 OK | 400 Bad Request, 404 Not Found |
| `/api/pessoas/{id}/ativar` | PATCH | 200 OK | 404 Not Found |
| `/api/pessoas/{id}/inativar` | PATCH | 200 OK | 404 Not Found |
| `/api/clientes` | GET | 200 OK | 400 Bad Request |
| `/api/clientes` | POST | 201 Created | 400 Bad Request, 404 Not Found, 409 Conflict |
| `/api/clientes/{id}` | GET | 200 OK | 404 Not Found |
| `/api/clientes/{id}` | PUT | 200 OK | 400 Bad Request, 404 Not Found |
| `/api/colaboradores` | GET | 200 OK | 400 Bad Request |
| `/api/colaboradores` | POST | 201 Created | 400 Bad Request, 404 Not Found, 409 Conflict |
| `/api/colaboradores/{id}` | GET | 200 OK | 404 Not Found |
| `/api/colaboradores/{id}` | PUT | 200 OK | 400 Bad Request, 404 Not Found, 409 Conflict |
| `/api/vendas` | GET | 200 OK | 400 Bad Request |
| `/api/vendas` | POST | 201 Created | 400 Bad Request, 404 Not Found |
| `/api/vendas/{id}` | GET | 200 OK | 404 Not Found |
| `/api/vendas/{id}/itens` | POST | 200 OK | 400 Bad Request, 404 Not Found |
| `/api/vendas/{vendaId}/itens/{itemId}` | DELETE | 200 OK | 400 Bad Request, 404 Not Found |
| `/api/vendas/{id}/finalizacao` | POST | 200 OK | 400 Bad Request, 404 Not Found |
| `/api/vendas/{id}/cancelamento` | POST | 200 OK | 400 Bad Request, 404 Not Found |
| `/api/health` | GET | 200 OK | Não especificados na documentação |
| `/api/contas-acesso` | POST | 201 Created | 400 Bad Request, 409 Conflict |
| `/api/contas-acesso/me` | GET | 200 OK | 404 Not Found |
| `/api/contas-acesso/me/senha` | PUT | 204 No Content | 400 Bad Request, 404 Not Found |

**Observação:** os códigos apresentados correspondem às respostas registradas no arquivo OpenAPI. Quando a documentação não apresenta um erro para determinada operação, isso não significa necessariamente que o erro seja impossível; significa apenas que ele não está especificado nesse contrato.

## 3. Limites da documentação automática

A geração automática com OpenAPI facilita a consulta dos endpoints, parâmetros, modelos de dados e respostas HTTP da API. Entretanto, ela não garante, sozinha, que todas as regras de negócio estejam descritas de maneira completa ou que todos os exemplos estejam atualizados. Por isso, a documentação deve ser revisada e complementada conforme as alterações do sistema. Os testes automatizados ajudam a verificar aspectos do contrato, mas não substituem os testes funcionais das regras de negócio, como validações de dados, prevenção de cadastros duplicados, controle de saldo de estoque e baixa transacional de estoque na finalização de vendas.

## 4. Evidências e referências

**Interface Swagger UI:**  
http://localhost:8080/swagger-ui/index.html

**Especificação OpenAPI em JSON:**  
http://localhost:8080/v3/api-docs

**Especificação OpenAPI em YAML:**  
http://localhost:8080/v3/api-docs.yaml

**Arquivo YAML versionado no projeto:**  
`docs/api/api-docs.yaml`

**Teste automatizado do contrato OpenAPI:**  
`src/test/java/com/curso/grossisports/OpenApiTest.java`

**Repositório do projeto:**  
https://github.com/brunogrossi021/grossi-sports

## 5. Divergências identificadas e correções

Durante a revisão do contrato OpenAPI, identificamos que a tabela
de operações desta documentação ainda apresentava os métodos HTTP
dos endpoints de ativação e inativação como "A confirmar".

A inspeção do arquivo `api-docs.yaml` mostrou que as operações
utilizam o método `PATCH`: ativação e inativação de produtos,
grupos de produtos e pessoas. Corrigimos a tabela para refletir os
métodos registrados no contrato gerado.

Na evolução da Aula 10, o contrato passou a incluir os recursos de
clientes, colaboradores e vendas, além da pesquisa paginada de
pessoas e da alteração de status por `PUT /api/pessoas/{id}/status`.
O arquivo `docs/api/api-docs.yaml` foi regenerado a partir do
endpoint `/v3/api-docs.yaml` e a tabela foi atualizada com todas as
operações e respostas documentadas.

Essa revisão demonstra que a documentação auxiliar precisa ser
comparada com o contrato real, pois informações desatualizadas podem
confundir quem consome a API.

## 6. Segurança e autenticação

A partir da Aula 12 a API é protegida com Spring Security no modo
*stateless*, usando **HTTP Basic**. O contrato OpenAPI declara o
esquema de segurança `basicAuth` (`type: http`, `scheme: basic`) e o
aplica globalmente.

- **Rotas públicas:** `GET /api/health`, `POST /api/contas-acesso`,
  `/v3/api-docs/**`, `/v3/api-docs.yaml`, `/swagger-ui.html` e
  `/swagger-ui/**`.
- **Rotas protegidas:** todas as demais exigem credencial válida e
  respondem `401 Unauthorized` sem autenticação.

O `POST /api/contas-acesso` é um **autocadastro didático** que cria a
pessoa e a conta de acesso na mesma transação, sempre com o perfil
`OPERADOR`. O cliente não informa `pessoaId` nem perfil de acesso, o
que impede vincular credenciais a uma pessoa já existente. As
respostas nunca contêm senha ou hash.

> HTTP Basic transmite a credencial codificada em Base64 em cada
> requisição; por isso, em produção, só deve ser usado sobre HTTPS/TLS.
> O cadastro público inicial é uma simplificação didática, não um
> fluxo pronto para produção.
