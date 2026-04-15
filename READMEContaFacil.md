# 💳 ContaFácil

Sistema bancário completo com back-end em Java Spring Boot, front-end em Angular e deploy em VPS.

---

## 📌 Sobre o Projeto

O **ContaFácil** é uma aplicação bancária full-stack que simula operações reais de um banco digital. O sistema permite abertura de contas, movimentações financeiras, controle de cartão de crédito e geração de extrato, com autenticação segura via JWT.

> Projeto desenvolvido com foco em boas práticas de arquitetura, segurança e regras de negócio reais do setor financeiro.

---

## 🚀 Funcionalidades

### 👤 Autenticação
- Cadastro de usuário com e-mail, username e senha
- Login com e-mail **ou** username
- Autenticação stateless via JWT
- Expiração e validação de token

### 🏦 Conta Bancária
- Abertura de conta corrente
- Consulta de saldo
- Depósito e saque
- Transferência entre contas (TED)
- Encerramento de conta

### 💸 Transações
- Registro de todas as movimentações
- Validação de saldo insuficiente
- Rollback automático em caso de falha (`@Transactional`)
- Histórico completo por conta

### 📄 Extrato
- Extrato por período (data inicial e final)
- Filtro por tipo de transação (depósito, saque, transferência)
- Saldo anterior e saldo final no período

### 💳 Cartão de Crédito
- Emissão de cartão vinculado à conta
- Limite disponível e utilizado
- Lançamento de compras
- Geração de fatura mensal
- Pagamento de fatura
- Bloqueio e desbloqueio de cartão

---

## 🛠️ Stack Tecnológica

### Back-end
| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 21 | Linguagem principal |
| Spring Boot | 4.x | Framework base |
| Spring Security | 7.x | Autenticação e autorização |
| Spring Data JPA | - | Persistência de dados |
| Hibernate | 7.x | ORM |
| MySQL | 8.x | Banco de dados |
| JWT (Auth0) | - | Tokens de autenticação |
| Lombok | - | Redução de boilerplate |
| Maven | - | Gerenciamento de dependências |
| JUnit 5 + Mockito | - | Testes unitários |
| Swagger/OpenAPI | - | Documentação da API |

### Front-end
| Tecnologia | Uso |
|---|---|
| Angular 17+ | Framework principal |
| TypeScript | Linguagem |
| Angular Material | Componentes UI |
| RxJS | Requisições reativas |

### Infraestrutura
| Tecnologia | Uso |
|---|---|
| VPS (Ubuntu) | Hospedagem |
| Nginx | Reverse proxy |
| Docker | Containerização |

---

## 🏗️ Arquitetura do Back-end

```
src/
└── main/java/dev/teamwin/contafacil/
    ├── config/
    │   ├── SecurityConfig.java
    │   └── SwaggerConfig.java
    ├── controller/
    │   ├── AuthController.java
    │   ├── ContaController.java
    │   ├── TransacaoController.java
    │   ├── ExtratoController.java
    │   └── CartaoController.java
    ├── domain/
    │   ├── UserDomain.java
    │   ├── ContaDomain.java
    │   ├── TransacaoDomain.java
    │   └── CartaoDomain.java
    ├── dto/
    │   ├── RegisterDTO.java
    │   ├── LoginDTO.java
    │   ├── ContaDTO.java
    │   ├── TransacaoDTO.java
    │   ├── ExtratoDTO.java
    │   └── CartaoDTO.java
    ├── enums/
    │   ├── TipoTransacaoEnum.java
    │   └── StatusCartaoEnum.java
    ├── mapper/
    │   ├── UserMapper.java
    │   ├── ContaMapper.java
    │   └── TransacaoMapper.java
    ├── repository/
    │   ├── UserRepository.java
    │   ├── ContaRepository.java
    │   ├── TransacaoRepository.java
    │   └── CartaoRepository.java
    ├── security/
    │   ├── SecurityFilter.java
    │   └── TokenService.java
    └── service/
        ├── AuthService.java
        ├── ContaService.java
        ├── TransacaoService.java
        ├── ExtratoService.java
        └── CartaoService.java
```

---

## 📡 Endpoints da API

### Autenticação
```
POST /auth/register     → Cadastro de usuário
POST /auth/login        → Login (retorna JWT)
```

### Conta
```
POST   /contas                  → Abrir conta
GET    /contas/{id}             → Consultar conta
GET    /contas/{id}/saldo       → Consultar saldo
DELETE /contas/{id}             → Encerrar conta
```

### Transações
```
POST /transacoes/deposito       → Realizar depósito
POST /transacoes/saque          → Realizar saque
POST /transacoes/transferencia  → Realizar transferência
```

### Extrato
```
GET /extrato/{contaId}          → Extrato completo
GET /extrato/{contaId}?dataInicio=&dataFim=   → Extrato por período
```

### Cartão
```
POST   /cartoes/{contaId}       → Emitir cartão
GET    /cartoes/{id}            → Consultar cartão
POST   /cartoes/{id}/compra     → Lançar compra
GET    /cartoes/{id}/fatura     → Consultar fatura
POST   /cartoes/{id}/pagamento  → Pagar fatura
PATCH  /cartoes/{id}/bloquear   → Bloquear cartão
PATCH  /cartoes/{id}/desbloquear → Desbloquear cartão
```

---

## ⚙️ Como Executar Localmente

### Pré-requisitos
- Java 21+
- MySQL 8+
- Node.js 18+ (para o front-end)
- Maven

### Back-end

```bash
# Clone o repositório
git clone https://github.com/1real-sys/ContaFacil.git

# Configure o banco no application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/contafacil
spring.datasource.username=seu_usuario
spring.datasource.password=sua_senha

# Execute
./mvnw spring-boot:run
```

### Front-end

```bash
cd frontend

npm install

ng serve
```

Acesse: `http://localhost:4200`

### Documentação da API (Swagger)
Após subir o back-end, acesse:
```
http://localhost:8080/swagger-ui.html
```

---

## 🧪 Testes

```bash
./mvnw test
```

Cobertura de testes nas regras de negócio:
- ✅ Saldo insuficiente para saque
- ✅ Transferência entre contas inexistentes
- ✅ Limite de cartão excedido
- ✅ Operação em conta encerrada
- ✅ Geração correta de fatura

---

## 🔐 Segurança

- Senhas armazenadas com **BCrypt**
- Autenticação via **JWT stateless**
- Endpoints protegidos por token
- CORS configurado
- CSRF desabilitado (padrão para APIs REST)

---

## 🌐 Deploy

- Back-end: VPS Ubuntu + Docker + Nginx
- Front-end: VPS Ubuntu + Nginx
- Banco de dados: MySQL na VPS
- Domínio: [contafacil.com.br](https://contafacil.com.br) ← *atualizar após deploy*

---

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.

---

<p align="center">Desenvolvido por <a href="https://linkedin.com/in/seu-perfil">seu nome</a></p>
