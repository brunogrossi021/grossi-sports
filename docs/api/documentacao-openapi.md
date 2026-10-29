# Documentação da API — Grossi Sports

## 1. Objetivo

A documentação da API Grossi Sports descreve os recursos disponíveis para o gerenciamento de produtos esportivos, grupos de produtos, fornecedores e movimentações de estoque. A especificação OpenAPI permite consultar os endpoints, os parâmetros, os dados de entrada e saída e as respostas HTTP documentadas.

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
| `/api/grupos-produtos` | GET | 200 OK | 400 Bad Request |
| `/api/grupos-produtos` | POST | 201 Created | 400 Bad Request, 409 Conflict |
| `/api/grupos-produtos/{id}` | GET | 200 OK | 404 Not Found |
| `/api/grupos-produtos/{id}` | PUT | 200 OK | 400 Bad Request, 404 Not Found, 409 Conflict |
| `/api/grupos-produtos/{id}` | DELETE | 204 No Content | 404 Not Found, 409 Conflict |
| `/api/fornecedores` | GET | 200 OK | Não especificados na documentação |
| `/api/fornecedores` | POST | 201 Created | 400 Bad Request, 409 Conflict |
| `/api/produtos/{id}/ativar` | A confirmar | 200 OK | 404 Not Found |
| `/api/produtos/{id}/inativar` | A confirmar | 200 OK | 404 Not Found |
| `/api/grupos-produtos/{id}/ativar` | A confirmar | 200 OK | 404 Not Found |
| `/api/grupos-produtos/{id}/inativar` | A confirmar | 200 OK | 404 Not Found |
| `/api/health` | GET | 200 OK | Não especificados na documentação |

**Observação:** os códigos apresentados correspondem às respostas registradas no arquivo OpenAPI. Quando a documentação não apresenta um erro para determinada operação, isso não significa necessariamente que o erro seja impossível; significa apenas que ele não está especificado nesse contrato.

Os métodos HTTP das operações de ativação e inativação devem ser confirmados diretamente no arquivo YAML antes da versão final da tabela.

## 3. Limites da documentação automática

A geração automática com OpenAPI facilita a consulta dos endpoints, parâmetros, modelos de dados e respostas HTTP da API. Entretanto, ela não garante, sozinha, que todas as regras de negócio estejam descritas de maneira completa ou que todos os exemplos estejam atualizados. Por isso, a documentação deve ser revisada e complementada conforme as alterações do sistema. Os testes automatizados ajudam a verificar aspectos do contrato, mas não substituem os testes funcionais das regras de negócio, como validações de dados, prevenção de cadastros duplicados e controle de saldo de estoque.

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
