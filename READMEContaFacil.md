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
- Login com e-mail
- Autenticação stateless via JWT
- Expiração e validação de token

### 🏦 Conta Bancária
- Abertura de conta corrente com agência gerada automaticamente
- Consulta de saldo
- Depósito e saque
- Transferência entre contas (TED)
- Encerramento de conta

### 💸 Transações
- Registro de todas as movimentações
- Validação de saldo insuficiente
- Rollback automático em caso de falha (`@Transactional`)
- Extrato por período com filtro por tipo de transação

### 💳 Cartão de Crédito
- Emissão de cartão vinculado à conta (VISA ou MASTERCARD)
- Solicitação de limite (entre R$ 1.000 e R$ 2.500)
- Lançamento e cancelamento de compras
- Geração automática de fatura mensal
- Pagamento de fatura com débito em conta
- Bloqueio, desbloqueio e cancelamento de cartão

---

## 🛠️ Stack Tecnológica

### Back-end
| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 21 | Linguagem principal |
| Spring Boot | 3.x | Framework base |
| Spring Security | 7.x | Autenticação e autorização |
| Spring Data JPA | - | Persistência de dados |
| Hibernate | 7.x | ORM |
| MySQL | 8.x | Banco de dados |
| Flyway | - | Versionamento do banco |
| JWT (Auth0) | - | Tokens de autenticação |
| Lombok | - | Redução de boilerplate |
| Maven | - | Gerenciamento de dependências |
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
    ├── auth/
    │   ├── AuthController.java
    │   ├── AuthService.java
    │   ├── LoginRequestDTO.java
    │   ├── RegisterRequestDTO.java
    │   └── ResponseDTO.java
    ├── cartao/
    │   ├── CartaoController.java
    │   ├── CartaoDomain.java
    │   ├── CartaoMapper.java
    │   ├── CartaoRepository.java
    │   ├── CartaoService.java
    │   ├── BandeiraCartao.java
    │   └── StatusCartao.java
    ├── comprasCartao/
    │   ├── ComprasController.java
    │   ├── ComprasCartaoDomain.java
    │   ├── CompraCartaoMapper.java
    │   ├── ComprasCartaoRepository.java
    │   ├── ComprasService.java
    │   ├── CategoriaEstabelecimento.java
    │   └── StatusCompra.java
    ├── conta/
    │   ├── ContaController.java
    │   ├── ContaDomain.java
    │   ├── ContaMapper.java
    │   ├── ContaRepository.java
    │   └── ContaService.java
    ├── extrato/
    │   ├── ExtratoController.java
    │   ├── ExtratoMapper.java
    │   └── ExtratoService.java
    ├── fatura/
    │   ├── FaturaController.java
    │   ├── FaturaDomain.java
    │   ├── FaturaMapper.java
    │   ├── FaturaRepository.java
    │   ├── FaturaService.java
    │   └── StatusFatura.java
    ├── transacao/
    │   ├── TransacaoController.java
    │   ├── TransacaoDomain.java
    │   ├── TransacaoMapper.java
    │   ├── TransacaoRepository.java
    │   └── TransacaoService.java
    ├── user/
    │   ├── UserDomain.java
    │   ├── UserMapper.java
    │   ├── UserRepository.java
    │   └── UserService.java
    └── infra/
        ├── exception/
        │   └── GlobalExceptionHandler.java
        └── security/
            ├── CustomUserDetailsService.java
            ├── SecurityConfig.java
            ├── SecurityFilter.java
            └── TokenService.java
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
POST   /conta/abrirConta    → Abrir conta corrente
GET    /conta/minhaConta    → Consultar dados da conta
GET    /conta/saldo         → Consultar saldo
DELETE /conta/encerrar      → Encerrar conta
```

### Transações
```
POST /transacao/depositar   → Realizar depósito
POST /transacao/saque       → Realizar saque
POST /transacao/ted         → Realizar transferência (TED)
```

### Extrato
```
GET /extrato?dataInicio=&dataFim=        → Extrato por período
GET /extrato?dataInicio=&dataFim=&tipo=  → Extrato por período e tipo
```

### Cartão
```
POST   /cartao/emitirCartao                   → Emitir cartão
GET    /cartao/meusCartoes                    → Listar cartões
POST   /cartao/{cartaoId}/solicitarLimite     → Solicitar limite
POST   /cartao/{cartaoId}/desbloquearCartao   → Ativar cartão
POST   /cartao/{cartaoId}/bloquearCartao      → Bloquear cartão
PATCH  /cartao/{cartaoId}/cancelarCartao      → Cancelar cartão
```

### Compras
```
POST  /compras/{cartaoId}/lancar    → Lançar compra
PATCH /compras/{compraId}/cancelar  → Cancelar compra
```

### Fatura
```
GET  /faturas/{cartaoId}/atual      → Consultar fatura atual
GET  /faturas/{cartaoId}/historico  → Histórico de faturas
POST /faturas/{cartaoId}/pagar      → Pagar fatura
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
http://localhost:8080/swagger-ui/index.html
```

---

## 🔐 Segurança

- Senhas armazenadas com **BCrypt**
- Autenticação via **JWT stateless**
- Endpoints protegidos por token
- CORS configurado
- CSRF desabilitado (padrão para APIs REST)

---

## 🌐 Deploy 

**Em andamento

- Back-end: VPS Ubuntu + Docker + Nginx
- Front-end: VPS Ubuntu + Nginx
- Banco de dados: MySQL na VPS
- Domínio: [contafacil.com.br](https://contafacil.com.br) ← *atualizar após deploy*

---

## 📄 Licença

--

---

<p align="center">Em Desenvolvimento por: Juliano =) <a href="https://www.linkedin.com/in/julianojlm/">Meu Linkedin</a></p>