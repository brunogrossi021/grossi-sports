# Testes e Cobertura — Grossi Sports

Documento de apoio da Aula 15. Descreve a **estratégia de testes** do
Grossi Sports, a medição de cobertura com **JaCoCo** e o **portão de
qualidade** aplicado na fase `verify` do Maven: no mínimo **70 % de
linhas** e **40 % de branches** no bundle do projeto.

## 1. Visão geral

Até a Aula 14 a suíte já cobria domínio, serviços e API, mas a cobertura
não era medida nem imposta: era possível reduzir testes sem que o build
reclamasse. A Aula 15 torna a cobertura **mensurável e verificável**:

- **JaCoCo** instrumenta os testes e gera o relatório em
  `target/site/jacoco/` (`index.html` e `jacoco.csv`).
- Um **portão** (`check`) falha o build quando os limites mínimos não são
  atingidos, evitando regressões silenciosas de cobertura.

A cobertura é um **meio**, não um fim: a meta da aula é ampliar a
confiança sobre as regras de negócio, não apenas "pintar linhas".

## 2. Pirâmide de testes

A suíte segue a divisão clássica, do mais rápido e isolado para o mais
integrado:

| Camada | Tipo | Exemplo | Spring / banco |
|---|---|---|---|
| Domínio | Unitário puro | `domain/ContaAcessoTest` | Não |
| Serviço | Unitário com mocks | `service/PessoaServiceUnitTest` | Não (Mockito) |
| API | Integração `MockMvc` | `PessoaApiTest` | Sim |

- **Domínio.** Instancia entidades diretamente e valida invariantes sem
  Spring nem banco. É a camada mais rápida.
- **Serviço.** Usa `@ExtendWith(MockitoExtension.class)` com
  `@Mock`/`@InjectMocks`: testa regras de negócio, decisões e exceções
  sem subir contexto Spring.
- **API.** Usa `@SpringBootTest` + `@AutoConfigureMockMvc(addFilters =
  false)` + `@ActiveProfiles("test")` + `@Transactional`. Valida status
  HTTP, corpo JSON, cabeçalhos e o mapeamento de erros.

> **Sobre `addFilters = false`.** Os testes de API desativam a cadeia de
> filtros de segurança para focar no comportamento HTTP dos controllers.
> A autorização por perfis é coberta em testes dedicados
> (`AutorizacaoApiTest`, `ContaAcessoSecurityTest`).

## 3. Testes adicionados na Aula 15

| Arquivo | Camada | Casos |
|---|---|---|
| `domain/ContaAcessoTest.java` | Domínio | 8 |
| `service/PessoaServiceUnitTest.java` | Serviço | 14 |
| `service/ClienteServiceUnitTest.java` | Serviço | 10 |
| `service/ColaboradorServiceUnitTest.java` | Serviço | 12 |
| `PessoaApiTest.java` | API | 6 |
| `ClienteApiTest.java` | API | 7 |
| `ColaboradorApiTest.java` | API | 6 |
| **Total novo** | | **63** |

Os CPFs usados nos testes de API são **gerados em tempo de execução**
(dígitos verificadores calculados), evitando colisões com dados de outras
execuções e com o *dataset* de teste.

## 4. Defeito revelado pelo teste de domínio

O `ContaAcessoTest` expôs um defeito real em
`ContaAcesso.substituirPerfis(...)`: a checagem de perfil nulo usava
`novosPerfis.contains(null)`. Em conjuntos imutáveis como
`Set.of(ADMINISTRADOR)`, `contains(null)` lança `NullPointerException`,
pois a implementação chama `null.equals(...)`.

O guard foi reescrito de forma *null-safe*, preservando a intenção
original (conta nunca sem perfil):

```java
if (novosPerfis.stream().anyMatch(Objects::isNull)) {
    throw new IllegalArgumentException("Perfil é obrigatório");
}
```

O comportamento observável (rejeitar conjunto sem perfil) permanece o
mesmo; apenas o caminho com `Set.of(...)` deixou de lançar NPE.

## 5. Medição com JaCoCo

### 5.1 Como medir

A instrumentação e o relatório são produzidos automaticamente pelo
plugin. Para gerar somente o relatório durante o desenvolvimento:

```powershell
.\mvnw.cmd clean verify
```

O relatório fica em `target/site/jacoco/index.html` (visual) e
`target/site/jacoco/jacoco.csv` (dados). Nada disso é versionado:
`target/` está no `.gitignore`.

### 5.2 Números da aula

| Momento | Linhas | Branches |
|---|---|---|
| Baseline (pré-Aula 15) | 59,99 % | 44,40 % |
| Após `ContaAcessoTest` | 60,37 % | 45,20 % |
| Após testes de serviço | 69,68 % | 63,80 % |
| **Final (após testes de API)** | **75,57 %** | **63,80 %** |

Totais finais medidos pelo JaCoCo (contadores do bundle):

| Contador | Cobertos / Total | Percentual |
|---|---|---|
| Linhas (LINE) | 1169 / 1547 | **75,57 %** |
| Branches (BRANCH) | 319 / 500 | **63,80 %** |
| Instruções (INSTRUCTION) | 4467 / 6107 | 73,15 % |
| Métodos (METHOD) | 321 / 427 | 75,18 % |

### 5.3 Cobertura por pacote

| Pacote | Linhas | Branches |
|---|---|---|
| `com.curso.grossisports` | 95,7 % | 0,0 % |
| `com.curso.grossisports.api` | 68,1 % | 0,0 % |
| `com.curso.grossisports.api.dto` | 61,3 % | 0,0 % |
| `com.curso.grossisports.api.mapper` | 61,5 % | 100,0 % |
| `com.curso.grossisports.domain` | 82,7 % | 64,8 % |
| `com.curso.grossisports.exception` | 100,0 % | 0,0 % |
| `com.curso.grossisports.repository` | 58,1 % | 37,5 % |
| `com.curso.grossisports.security` | 82,9 % | 80,0 % |
| `com.curso.grossisports.service` | 76,1 % | 65,2 % |
| `com.curso.grossisports.validation` | 33,3 % | 25,0 % |

## 6. Portão de qualidade

No `pom.xml`, o `jacoco-maven-plugin` está configurado com
`prepare-agent` (instrumentação) e, na fase `verify`, com `report` +
`check`:

```xml
<properties>
  <jacoco.version>0.8.16</jacoco.version>
  <jacoco.minimum.line.coverage>0.70</jacoco.minimum.line.coverage>
  <jacoco.minimum.branch.coverage>0.40</jacoco.minimum.branch.coverage>
</properties>
```

```xml
<execution>
  <id>report-and-check-coverage</id>
  <phase>verify</phase>
  <goals>
    <goal>report</goal>
    <goal>check</goal>
  </goals>
  <configuration>
    <rules>
      <rule>
        <element>BUNDLE</element>
        <limits>
          <limit>
            <counter>LINE</counter>
            <value>COVEREDRATIO</value>
            <minimum>${jacoco.minimum.line.coverage}</minimum>
          </limit>
          <limit>
            <counter>BRANCH</counter>
            <value>COVEREDRATIO</value>
            <minimum>${jacoco.minimum.branch.coverage}</minimum>
          </limit>
        </limits>
      </rule>
    </rules>
  </configuration>
</execution>
```

### 6.1 Por que 70 % e 40 %?

Os limites são um **piso**, não o teto alcançado. A suíte atual está em
**75,57 %** de linhas e **63,80 %** de branches; o portão foi fixado
abaixo disso para:

- **Não quebrar o build** por oscilações naturais (uma refatoração que
  adiciona alguns *getters* ou ramos defensivos).
- **Bloquear regressões graves** — derrubar a cobertura de 75 % para
  abaixo de 70 % exige remover ou desabilitar testes relevantes.
- **Reconhecer o custo marginal** de perseguir 100 %: *branches* em
  classes de infraestrutura (repositórios, DTOs, validações) têm baixo
  retorno.

A proporção entre os limites (linhas mais exigentes que branches) reflete
que, em código de aplicação, é mais fácil e mais útil cobrir linhas de
fluxo do que todas as combinações de ramos.

### 6.2 Falha controlada

Para verificar que o portão realmente **barra** o build, foi feito um
teste controlado: elevando temporariamente
`jacoco.minimum.branch.coverage` de `0.40` para `0.90`, o `verify` falhou:

```
[WARNING] Rule violated for bundle grossi-sports: branches covered ratio
is 0.63, but expected minimum is 0.90
[INFO] BUILD FAILURE
[ERROR] Failed to execute goal org.jacoco:jacoco-maven-plugin:0.8.16:check
(report-and-check-coverage) ... Coverage checks have not been met.
```

Os 157 testes passaram; a falha veio **exclusivamente** do goal `check`,
comprovando que o limite é efetivo. O valor foi então restaurado para
`0.40`.

## 7. Boas práticas adotadas

- **Sem alterar regra de negócio para inflar cobertura.** Quando o teste
  revelou um defeito real, o código de produção foi corrigido — não o
  teste, e não o limite.
- **Testes determinísticos.** Dados únicos gerados em tempo de execução
  (CPF, matrícula, e-mail) evitam dependência de ordem ou de estado.
- **Isolamento transacional.** Testes de API são `@Transactional`, com
  *rollback* ao final de cada caso.
- **Nada de relatórios versionados.** `target/` permanece fora do Git.

## 8. Limitações

- **Cobertura ≠ qualidade.** Linhas cobertas não garantem asserções
  significativas; um teste sem `assert` também "cobre".
- **Branches de infraestrutura.** Repositórios Spring Data e DTOs
  mantêm cobertura menor por não concentrarem regra de negócio.
- **Sem teste de mutação.** Não há verificação da força das asserções
  (ex.: PIT) nesta aula.

## 9. Referências no projeto

- Configuração do JaCoCo e limites: `pom.xml`
- Teste de domínio: `src/test/java/com/curso/grossisports/domain/ContaAcessoTest.java`
- Testes de serviço:
  `src/test/java/com/curso/grossisports/service/PessoaServiceUnitTest.java`,
  `ClienteServiceUnitTest.java`, `ColaboradorServiceUnitTest.java`
- Testes de API:
  `src/test/java/com/curso/grossisports/PessoaApiTest.java`,
  `ClienteApiTest.java`, `ColaboradorApiTest.java`
- Relatório gerado (não versionado): `target/site/jacoco/index.html`
- Autenticação JWT: [`autenticacao-jwt.md`](autenticacao-jwt.md)
- Autorização por perfis: [`autorizacao-perfis.md`](autorizacao-perfis.md)
