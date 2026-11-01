CREATE TABLE ajustes_compra (
    id BIGINT NOT NULL AUTO_INCREMENT,
    compra_id BIGINT NOT NULL,
    valor_creditado DECIMAL(19, 2) NOT NULL,
    valor_abatido DECIMAL(19, 2) NOT NULL,
    data_ajuste DATETIME(6) NOT NULL,

    CONSTRAINT pk_ajustes_compra PRIMARY KEY (id),
    CONSTRAINT uk_ajustes_compra_compra UNIQUE (compra_id),
    CONSTRAINT fk_ajustes_compra_compra
                           FOREIGN KEY (compra_id) REFERENCES compras_cartao (id),
    CONSTRAINT ck_ajustes_compra_creditado CHECK ( valor_creditado >= 0 ),
    CONSTRAINT ck_ajustes_compra_abatido CHECK ( valor_abatido >= 0 )
);