CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE contas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conta_corrente VARCHAR(20) NOT NULL,
    agencia VARCHAR(20) NOT NULL,
    saldo DECIMAL(19, 2) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT pk_contas PRIMARY KEY (id),
    CONSTRAINT uk_contas_conta_corrente UNIQUE (conta_corrente),
    CONSTRAINT fk_contas_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_contas_saldo_non_negative CHECK (saldo >= 0)
);

CREATE INDEX idx_contas_user_id ON contas (user_id);

CREATE TABLE cartoes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    limite_total DECIMAL(19, 2) NOT NULL,
    limite_utilizado DECIMAL(19, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    numero_cartao VARCHAR(19) NOT NULL,
    cvv VARCHAR(4) NOT NULL,
    data_validade DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    conta_id BIGINT NOT NULL,
    CONSTRAINT pk_cartoes PRIMARY KEY (id),
    CONSTRAINT uk_cartoes_numero_cartao UNIQUE (numero_cartao),
    CONSTRAINT fk_cartoes_conta FOREIGN KEY (conta_id) REFERENCES contas (id),
    CONSTRAINT ck_cartoes_limite_total_non_negative CHECK (limite_total >= 0),
    CONSTRAINT ck_cartoes_limite_utilizado_non_negative CHECK (limite_utilizado >= 0)
);

CREATE INDEX idx_cartoes_conta_id ON cartoes (conta_id);

CREATE TABLE faturas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ano INT NOT NULL,
    mes INT NOT NULL,
    data_fechamento DATE NOT NULL,
    data_vencimento DATE NOT NULL,
    valor_total DECIMAL(19, 2) NOT NULL,
    valor_pago DECIMAL(19, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    cartao_id BIGINT NOT NULL,
    CONSTRAINT pk_faturas PRIMARY KEY (id),
    CONSTRAINT uk_fatura_cartao_ano_mes UNIQUE (cartao_id, ano, mes),
    CONSTRAINT fk_faturas_cartao FOREIGN KEY (cartao_id) REFERENCES cartoes (id),
    CONSTRAINT ck_faturas_mes_valid CHECK (mes >= 1 AND mes <= 12),
    CONSTRAINT ck_faturas_valor_total_non_negative CHECK (valor_total >= 0),
    CONSTRAINT ck_faturas_valor_pago_non_negative CHECK (valor_pago >= 0),
    CONSTRAINT ck_faturas_valor_pago_lte_total CHECK (valor_pago <= valor_total)
);

CREATE INDEX idx_faturas_cartao_id ON faturas (cartao_id);

CREATE TABLE compras_cartao (
    id BIGINT NOT NULL AUTO_INCREMENT,
    valor DECIMAL(19, 2) NOT NULL,
    data_compra DATETIME(6) NOT NULL,
    estabelecimento VARCHAR(120) NOT NULL,
    ultimos4_digitos VARCHAR(4) NOT NULL,
    categoria VARCHAR(40) NOT NULL,
    fatura_id BIGINT NOT NULL,
    CONSTRAINT pk_compras_cartao PRIMARY KEY (id),
    CONSTRAINT fk_compras_cartao_fatura FOREIGN KEY (fatura_id) REFERENCES faturas (id),
    CONSTRAINT ck_compras_cartao_valor_positive CHECK (valor > 0)
);

CREATE INDEX idx_compras_cartao_fatura_id ON compras_cartao (fatura_id);

CREATE TABLE transacoes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    descricao VARCHAR(255) NOT NULL,
    valor DECIMAL(19, 2) NOT NULL,
    saldo_antes DECIMAL(19, 2) NOT NULL,
    saldo_depois DECIMAL(19, 2) NOT NULL,
    observacao VARCHAR(255),
    conta_id BIGINT NOT NULL,
    conta_destino_id BIGINT,
    data_transacao DATETIME(6) NOT NULL,
    CONSTRAINT pk_transacoes PRIMARY KEY (id),
    CONSTRAINT fk_transacoes_conta FOREIGN KEY (conta_id) REFERENCES contas (id),
    CONSTRAINT fk_transacoes_conta_destino FOREIGN KEY (conta_destino_id) REFERENCES contas (id),
    CONSTRAINT ck_transacoes_valor_positive CHECK (valor > 0)
);

CREATE INDEX idx_transacoes_conta_data ON transacoes (conta_id, data_transacao);
CREATE INDEX idx_transacoes_conta_destino_id ON transacoes (conta_destino_id);
