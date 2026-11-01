# ContaFácil

Aplicação full stack que simula operações de conta corrente e cartão de crédito. Foi criada para praticar desenvolvimento de software no domínio financeiro: regras de saldo e limite, persistência transacional, autenticação e histórico de movimentações.

> Este é um projeto de estudo e portfólio. Os valores e cartões são simulados; não há integração com instituições financeiras ou redes de pagamento.

## O que a aplicação faz

- Cadastro e login com autenticação JWT.
- Abertura de conta, consulta de saldo, depósito, saque e transferência entre contas.
- Extrato com filtro por período e tipo de transação.
- Emissão e gerenciamento de cartão, solicitação de limite e lançamento de compras.
- Faturas mensais, pagamento total ou parcial e histórico de compras.
- Cancelamento de compra com registro de ajuste. Quando a fatura já recebeu pagamento, a parte ainda pendente é abatida e a parte paga gera um **crédito de confiança** na conta corrente e no extrato.
- Interface Angular responsiva com temas.

### Exemplo da regra de cancelamento

Duas compras de R$ 100 formam uma fatura de R$ 200. Depois de pagar R$ 150, restam R$ 50 pendentes. Ao cancelar uma das compras de R$ 100, o sistema abate R$ 50 da fatura e credita R$ 50 na conta corrente. O valor pago da fatura continua em R$ 150, e o ajuste guarda os valores abatido e creditado.

Essa regra é coberta pelos [testes de cancelamento](src/test/java/dev/teamwin/contafacil/comprasCartao/ComprasCancelamentoIntegrationTest.java).

## Tecnologias

| Camada | Tecnologias |
| --- | --- |
| API | Java 21, Spring Boot 4, Spring Security, Spring Data JPA, Maven |
| Dados | MySQL, Flyway, Caffeine |
| Interface | Angular 21, TypeScript, SCSS |
| Testes | JUnit, Spring Boot Test, Testcontainers com MySQL |
| Documentação da API | Springdoc OpenAPI / Swagger UI |

O backend organiza as regras por domínio em `src/main/java/dev/teamwin/contafacil/`. As migrações do banco ficam em `src/main/resources/db/migration/`; a interface está em `front/`.

## Executar localmente

### Pré-requisitos

- JDK 21.
- MySQL disponível localmente.
- Node.js e npm compatíveis com o Angular 21.
- Docker ativo para executar os testes de integração com Testcontainers.

Crie um banco vazio, por exemplo:

```sql
CREATE DATABASE conta_facil;
```

Configure as variáveis de ambiente no terminal que iniciará a API:

```bash
export DATABASE_URL='jdbc:mysql://localhost:3306/conta_facil'
export DATABASE_USERNAME='seu_usuario'
export DATABASE_PASSWORD='sua_senha'
export JWT_SECRET='uma_chave_aleatoria_longa_para_desenvolvimento'
```

Para gerar uma chave local, você pode usar `openssl rand -hex 32`. O Spring lê essas variáveis do ambiente; criar um arquivo `.env` sozinho não as carrega automaticamente.

Inicie a API na raiz do projeto:

```bash
./mvnw spring-boot:run
```

O Flyway executa as migrações ao iniciar, e a API fica em `http://localhost:8080`. Em outro terminal, inicie o Angular:

```bash
cd front
npm ci
npm start
```

A interface fica em `http://localhost:4200`. O servidor de desenvolvimento encaminha as chamadas `/api` para a API local por meio de `front/proxy.conf.json`.

Com a API em execução, a documentação interativa está em `http://localhost:8080/swagger-ui/index.html` quando `SWAGGER_ENABLED` estiver habilitado, que é o valor padrão na configuração atual.

## Testes e build

Com o Docker ativo, execute os testes Java na raiz:

```bash
./mvnw test
```

Para verificar o build da interface:

```bash
cd front
npm run build
```

Os testes de integração cobrem cenários de cancelamento com fatura sem pagamento, parcialmente paga e quitada, incluindo o crédito na conta e a prevenção de crédito duplicado. Há também testes de serviços de cartão e transações.

## Roteiro para explorar

1. Cadastre um usuário, entre na aplicação e abra uma conta.
2. Deposite um valor, emita um cartão e solicite limite.
3. Faça duas compras de R$ 100 e pague R$ 150 da fatura.
4. Cancele uma das compras na tela de faturas.
5. Confira o abatimento na fatura, o crédito de confiança na conta e o lançamento no extrato.

## Escopo

A aplicação demonstra regras de negócio e fluxos de uma API financeira simulada. Não processa dinheiro real, não emite cartões reais e não se conecta a sistemas bancários externos.
