-- =====================================================================
-- CONTA CERTA - Global Solution FIAP 2026/2 (CitizenTech)
-- Script de criação do banco usado pela API Java
-- Banco: Oracle (FIAP)
--
-- IMPORTANTE: a API Java usa EXATAMENTE estes nomes de tabelas,
-- colunas e sequences. Não renomeie nada sem avisar o time do Java.
-- Pode ADICIONAR colunas (desde que aceitem nulo ou tenham DEFAULT)
-- e pode ADICIONAR tabelas novas sem problema.
--
-- Ordem: DROPs (opcional) -> sequences -> tabelas -> comentários
--        -> dados iniciais obrigatórios (bancos e categorias)
-- =====================================================================


-- ---------------------------------------------------------------------
-- 0) LIMPEZA (opcional) - descomente para recriar tudo do zero
-- ---------------------------------------------------------------------
-- DROP TABLE tb_movimento_cofre CASCADE CONSTRAINTS;
-- DROP TABLE tb_cofre CASCADE CONSTRAINTS;
-- DROP TABLE tb_pagamento CASCADE CONSTRAINTS;
-- DROP TABLE tb_divida CASCADE CONSTRAINTS;
-- DROP TABLE tb_transacao CASCADE CONSTRAINTS;
-- DROP TABLE tb_cartao CASCADE CONSTRAINTS;
-- DROP TABLE tb_conta_bancaria CASCADE CONSTRAINTS;
-- DROP TABLE tb_indicador_economico CASCADE CONSTRAINTS;
-- DROP TABLE tb_categoria CASCADE CONSTRAINTS;
-- DROP TABLE tb_banco CASCADE CONSTRAINTS;
-- DROP TABLE tb_usuario CASCADE CONSTRAINTS;
-- DROP SEQUENCE sq_tb_usuario;
-- DROP SEQUENCE sq_tb_banco;
-- DROP SEQUENCE sq_tb_categoria;
-- DROP SEQUENCE sq_tb_indicador_economico;
-- DROP SEQUENCE sq_tb_conta_bancaria;
-- DROP SEQUENCE sq_tb_cartao;
-- DROP SEQUENCE sq_tb_transacao;
-- DROP SEQUENCE sq_tb_divida;
-- DROP SEQUENCE sq_tb_pagamento;
-- DROP SEQUENCE sq_tb_cofre;
-- DROP SEQUENCE sq_tb_movimento_cofre;


-- ---------------------------------------------------------------------
-- 1) SEQUENCES (uma por tabela, padrão sq_tb_nome)
-- ---------------------------------------------------------------------
CREATE SEQUENCE sq_tb_usuario             START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE sq_tb_banco               START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE sq_tb_categoria           START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE sq_tb_indicador_economico START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE sq_tb_conta_bancaria      START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE sq_tb_cartao              START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE sq_tb_transacao           START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE sq_tb_divida              START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE sq_tb_pagamento           START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE sq_tb_cofre               START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE sq_tb_movimento_cofre     START WITH 1 INCREMENT BY 1 NOCACHE;


-- ---------------------------------------------------------------------
-- 2) TABELAS SEM CHAVE ESTRANGEIRA
-- ---------------------------------------------------------------------

CREATE TABLE tb_usuario (
    cd_usuario             NUMBER(10)     CONSTRAINT pk_tb_usuario PRIMARY KEY,
    nm_usuario             VARCHAR2(100)  NOT NULL,
    ds_email               VARCHAR2(150)  NOT NULL,
    nr_cpf                 CHAR(11)       NOT NULL,
    ds_senha_hash          VARCHAR2(100)  NOT NULL,
    vl_renda_declarada     NUMBER(12,2),
    qt_horas_trabalho_mes  NUMBER(3)      DEFAULT 220 NOT NULL,
    dt_cadastro            DATE           DEFAULT SYSDATE NOT NULL,
    CONSTRAINT uk_tb_usuario_email  UNIQUE (ds_email),
    CONSTRAINT uk_tb_usuario_cpf    UNIQUE (nr_cpf),
    CONSTRAINT ck_tb_usuario_cpf    CHECK (REGEXP_LIKE(nr_cpf, '^[0-9]{11}$')),
    CONSTRAINT ck_tb_usuario_renda  CHECK (vl_renda_declarada >= 0),
    CONSTRAINT ck_tb_usuario_horas  CHECK (qt_horas_trabalho_mes BETWEEN 1 AND 744)
);

CREATE TABLE tb_banco (
    cd_banco          NUMBER(10)    CONSTRAINT pk_tb_banco PRIMARY KEY,
    nm_banco          VARCHAR2(60)  NOT NULL,
    nr_codigo_compe   CHAR(3)       NOT NULL,
    CONSTRAINT uk_tb_banco_nome   UNIQUE (nm_banco),
    CONSTRAINT uk_tb_banco_compe  UNIQUE (nr_codigo_compe)
);

CREATE TABLE tb_categoria (
    cd_categoria   NUMBER(10)    CONSTRAINT pk_tb_categoria PRIMARY KEY,
    nm_categoria   VARCHAR2(50)  NOT NULL,
    tp_movimento   VARCHAR2(7)   NOT NULL,
    st_essencial   CHAR(1)       DEFAULT 'N' NOT NULL,
    CONSTRAINT uk_tb_categoria_nome   UNIQUE (nm_categoria),
    CONSTRAINT ck_tb_categoria_tipo   CHECK (tp_movimento IN ('ENTRADA', 'SAIDA')),
    CONSTRAINT ck_tb_categoria_essen  CHECK (st_essencial IN ('S', 'N'))
);

CREATE TABLE tb_indicador_economico (
    cd_indicador     NUMBER(10)    CONSTRAINT pk_tb_indicador PRIMARY KEY,
    cd_serie_bcb     NUMBER(6)     NOT NULL,
    nm_indicador     VARCHAR2(40)  NOT NULL,
    dt_referencia    DATE          NOT NULL,
    vl_indicador     NUMBER(14,6)  NOT NULL,
    dt_atualizacao   DATE          DEFAULT SYSDATE NOT NULL,
    CONSTRAINT uk_tb_indicador_serie_dt UNIQUE (cd_serie_bcb, dt_referencia)
);


-- ---------------------------------------------------------------------
-- 3) TABELAS COM CHAVE ESTRANGEIRA (na ordem das dependências)
-- ---------------------------------------------------------------------

-- Tabela associativa do N:N  usuario <-> banco
CREATE TABLE tb_conta_bancaria (
    cd_conta             NUMBER(10)     CONSTRAINT pk_tb_conta_bancaria PRIMARY KEY,
    cd_usuario           NUMBER(10)     NOT NULL,
    cd_banco             NUMBER(10)     NOT NULL,
    nr_agencia           VARCHAR2(4)    NOT NULL,
    nr_conta             VARCHAR2(12)   NOT NULL,
    vl_saldo             NUMBER(12,2)   DEFAULT 0 NOT NULL,
    st_dados_simulados   CHAR(1)        DEFAULT 'N' NOT NULL,
    dt_vinculo           DATE           DEFAULT SYSDATE NOT NULL,
    CONSTRAINT fk_tb_conta_usuario  FOREIGN KEY (cd_usuario) REFERENCES tb_usuario (cd_usuario),
    CONSTRAINT fk_tb_conta_banco    FOREIGN KEY (cd_banco)   REFERENCES tb_banco (cd_banco),
    CONSTRAINT uk_tb_conta_numero   UNIQUE (cd_banco, nr_agencia, nr_conta),
    CONSTRAINT ck_tb_conta_agencia  CHECK (REGEXP_LIKE(nr_agencia, '^[0-9]{4}$')),
    CONSTRAINT ck_tb_conta_simul    CHECK (st_dados_simulados IN ('S', 'N'))
);

CREATE TABLE tb_cartao (
    cd_cartao           NUMBER(10)     CONSTRAINT pk_tb_cartao PRIMARY KEY,
    cd_conta            NUMBER(10)     NOT NULL,
    tp_cartao           VARCHAR2(7)    NOT NULL,
    nr_final            CHAR(4)        NOT NULL,
    vl_limite           NUMBER(12,2),
    vl_fatura_atual     NUMBER(12,2)   DEFAULT 0 NOT NULL,
    nr_dia_vencimento   NUMBER(2),
    dt_cadastro         DATE           DEFAULT SYSDATE NOT NULL,
    CONSTRAINT fk_tb_cartao_conta    FOREIGN KEY (cd_conta) REFERENCES tb_conta_bancaria (cd_conta),
    CONSTRAINT ck_tb_cartao_tipo     CHECK (tp_cartao IN ('CREDITO', 'DEBITO')),
    CONSTRAINT ck_tb_cartao_final    CHECK (REGEXP_LIKE(nr_final, '^[0-9]{4}$')),
    CONSTRAINT ck_tb_cartao_limite   CHECK (vl_limite >= 0),
    CONSTRAINT ck_tb_cartao_fatura   CHECK (vl_fatura_atual >= 0),
    CONSTRAINT ck_tb_cartao_venc     CHECK (nr_dia_vencimento BETWEEN 1 AND 28),
    CONSTRAINT ck_tb_cartao_credito  CHECK (tp_cartao = 'DEBITO' OR vl_limite IS NOT NULL)
);

CREATE TABLE tb_transacao (
    cd_transacao    NUMBER(10)     CONSTRAINT pk_tb_transacao PRIMARY KEY,
    cd_conta        NUMBER(10)     NOT NULL,
    cd_categoria    NUMBER(10)     NOT NULL,
    cd_cartao       NUMBER(10),
    ds_transacao    VARCHAR2(120)  NOT NULL,
    vl_transacao    NUMBER(12,2)   NOT NULL,
    tp_movimento    VARCHAR2(7)    NOT NULL,
    tp_meio         VARCHAR2(20)   NOT NULL,
    dt_transacao    DATE           DEFAULT SYSDATE NOT NULL,
    st_simulada     CHAR(1)        DEFAULT 'N' NOT NULL,
    CONSTRAINT fk_tb_transacao_conta  FOREIGN KEY (cd_conta)     REFERENCES tb_conta_bancaria (cd_conta),
    CONSTRAINT fk_tb_transacao_categ  FOREIGN KEY (cd_categoria) REFERENCES tb_categoria (cd_categoria),
    CONSTRAINT fk_tb_transacao_cartao FOREIGN KEY (cd_cartao)    REFERENCES tb_cartao (cd_cartao),
    CONSTRAINT ck_tb_transacao_valor  CHECK (vl_transacao > 0),
    CONSTRAINT ck_tb_transacao_tipo   CHECK (tp_movimento IN ('ENTRADA', 'SAIDA')),
    CONSTRAINT ck_tb_transacao_meio   CHECK (tp_meio IN ('PIX', 'TRANSFERENCIA', 'CARTAO_DEBITO',
                                                         'CARTAO_CREDITO', 'BOLETO', 'OUTRO')),
    CONSTRAINT ck_tb_transacao_simul  CHECK (st_simulada IN ('S', 'N'))
);

CREATE TABLE tb_divida (
    cd_divida               NUMBER(10)     CONSTRAINT pk_tb_divida PRIMARY KEY,
    cd_usuario              NUMBER(10)     NOT NULL,
    cd_conta                NUMBER(10),
    nm_credor               VARCHAR2(80)   NOT NULL,
    tp_divida               VARCHAR2(25)   NOT NULL,
    vl_original             NUMBER(12,2)   NOT NULL,
    vl_saldo_devedor        NUMBER(12,2)   NOT NULL,
    tx_juros_mensal         NUMBER(6,3)    NOT NULL,
    qt_parcelas_total       NUMBER(4)      NOT NULL,
    qt_parcelas_pagas       NUMBER(4)      DEFAULT 0 NOT NULL,
    vl_parcela              NUMBER(12,2)   NOT NULL,
    dt_contratacao          DATE           NOT NULL,
    dt_proximo_vencimento   DATE,
    nr_dias_atraso          NUMBER(5)      DEFAULT 0 NOT NULL,
    st_divida               VARCHAR2(12)   DEFAULT 'EM_DIA' NOT NULL,
    st_simulada             CHAR(1)        DEFAULT 'N' NOT NULL,
    CONSTRAINT fk_tb_divida_usuario  FOREIGN KEY (cd_usuario) REFERENCES tb_usuario (cd_usuario),
    CONSTRAINT fk_tb_divida_conta    FOREIGN KEY (cd_conta)   REFERENCES tb_conta_bancaria (cd_conta),
    CONSTRAINT ck_tb_divida_tipo     CHECK (tp_divida IN ('CARTAO_ROTATIVO', 'CARTAO_PARCELADO',
                                                          'CHEQUE_ESPECIAL', 'EMPRESTIMO_PESSOAL',
                                                          'CONSIGNADO', 'FINANCIAMENTO_VEICULO',
                                                          'FINANCIAMENTO_IMOVEL', 'CREDIARIO',
                                                          'CONTA_ATRASADA', 'OUTRO')),
    CONSTRAINT ck_tb_divida_original CHECK (vl_original > 0),
    CONSTRAINT ck_tb_divida_saldo    CHECK (vl_saldo_devedor >= 0),
    CONSTRAINT ck_tb_divida_juros    CHECK (tx_juros_mensal BETWEEN 0 AND 100),
    CONSTRAINT ck_tb_divida_parc_tot CHECK (qt_parcelas_total > 0),
    CONSTRAINT ck_tb_divida_parc_pg  CHECK (qt_parcelas_pagas BETWEEN 0 AND qt_parcelas_total),
    CONSTRAINT ck_tb_divida_vl_parc  CHECK (vl_parcela > 0),
    CONSTRAINT ck_tb_divida_atraso   CHECK (nr_dias_atraso >= 0),
    CONSTRAINT ck_tb_divida_status   CHECK (st_divida IN ('EM_DIA', 'ATRASADA', 'QUITADA')),
    CONSTRAINT ck_tb_divida_simul    CHECK (st_simulada IN ('S', 'N'))
);

CREATE TABLE tb_pagamento (
    cd_pagamento   NUMBER(10)     CONSTRAINT pk_tb_pagamento PRIMARY KEY,
    cd_divida      NUMBER(10)     NOT NULL,
    cd_conta       NUMBER(10),
    vl_pagamento   NUMBER(12,2)   NOT NULL,
    dt_pagamento   DATE           DEFAULT SYSDATE NOT NULL,
    CONSTRAINT fk_tb_pagamento_divida FOREIGN KEY (cd_divida) REFERENCES tb_divida (cd_divida),
    CONSTRAINT fk_tb_pagamento_conta  FOREIGN KEY (cd_conta)  REFERENCES tb_conta_bancaria (cd_conta),
    CONSTRAINT ck_tb_pagamento_valor  CHECK (vl_pagamento > 0)
);

CREATE TABLE tb_cofre (
    cd_cofre       NUMBER(10)     CONSTRAINT pk_tb_cofre PRIMARY KEY,
    cd_usuario     NUMBER(10)     NOT NULL,
    nm_cofre       VARCHAR2(60)   NOT NULL,
    vl_meta        NUMBER(12,2),
    dt_limite      DATE,
    vl_guardado    NUMBER(12,2)   DEFAULT 0 NOT NULL,
    dt_criacao     DATE           DEFAULT SYSDATE NOT NULL,
    CONSTRAINT fk_tb_cofre_usuario  FOREIGN KEY (cd_usuario) REFERENCES tb_usuario (cd_usuario),
    CONSTRAINT ck_tb_cofre_meta     CHECK (vl_meta > 0),
    CONSTRAINT ck_tb_cofre_guardado CHECK (vl_guardado >= 0)
);

CREATE TABLE tb_movimento_cofre (
    cd_movimento   NUMBER(10)     CONSTRAINT pk_tb_movimento_cofre PRIMARY KEY,
    cd_cofre       NUMBER(10)     NOT NULL,
    cd_conta       NUMBER(10)     NOT NULL,
    tp_movimento   VARCHAR2(8)    NOT NULL,
    vl_movimento   NUMBER(12,2)   NOT NULL,
    dt_movimento   DATE           DEFAULT SYSDATE NOT NULL,
    CONSTRAINT fk_tb_mov_cofre_cofre  FOREIGN KEY (cd_cofre) REFERENCES tb_cofre (cd_cofre),
    CONSTRAINT fk_tb_mov_cofre_conta  FOREIGN KEY (cd_conta) REFERENCES tb_conta_bancaria (cd_conta),
    CONSTRAINT ck_tb_mov_cofre_tipo   CHECK (tp_movimento IN ('DEPOSITO', 'RESGATE')),
    CONSTRAINT ck_tb_mov_cofre_valor  CHECK (vl_movimento > 0)
);


-- ---------------------------------------------------------------------
-- 4) COMENTÁRIOS DAS TABELAS
-- ---------------------------------------------------------------------
COMMENT ON TABLE tb_usuario             IS 'Pessoa que usa o Conta Certa. Senha guardada criptografada (BCrypt).';
COMMENT ON TABLE tb_banco               IS 'Catálogo de bancos que podem ser conectados (nomes reais, integração simulada).';
COMMENT ON TABLE tb_categoria           IS 'Categorias de entradas e saídas; st_essencial indica gasto essencial.';
COMMENT ON TABLE tb_indicador_economico IS 'Cache dos indicadores do Banco Central (Selic, IPCA, dólar).';
COMMENT ON TABLE tb_conta_bancaria      IS 'Associativa N:N entre usuário e banco: conta do usuário em um banco.';
COMMENT ON TABLE tb_cartao              IS 'Cartões de crédito e débito ligados a uma conta bancária.';
COMMENT ON TABLE tb_transacao           IS 'Entradas e saídas de dinheiro de uma conta (extrato).';
COMMENT ON TABLE tb_divida              IS 'Dívidas do usuário, com tipo, juros, parcelas e situação.';
COMMENT ON TABLE tb_pagamento           IS 'Pagamentos feitos para abater uma dívida.';
COMMENT ON TABLE tb_cofre               IS 'Cofres/metas de economia do usuário.';
COMMENT ON TABLE tb_movimento_cofre     IS 'Depósitos e resgates de um cofre.';


-- ---------------------------------------------------------------------
-- 5) DADOS INICIAIS OBRIGATÓRIOS (a API depende deles)
-- ---------------------------------------------------------------------

-- Bancos (código COMPE oficial)
INSERT INTO tb_banco (cd_banco, nm_banco, nr_codigo_compe) VALUES (sq_tb_banco.NEXTVAL, 'Banco do Brasil', '001');
INSERT INTO tb_banco (cd_banco, nm_banco, nr_codigo_compe) VALUES (sq_tb_banco.NEXTVAL, 'Santander', '033');
INSERT INTO tb_banco (cd_banco, nm_banco, nr_codigo_compe) VALUES (sq_tb_banco.NEXTVAL, 'Banco Inter', '077');
INSERT INTO tb_banco (cd_banco, nm_banco, nr_codigo_compe) VALUES (sq_tb_banco.NEXTVAL, 'Caixa Econômica Federal', '104');
INSERT INTO tb_banco (cd_banco, nm_banco, nr_codigo_compe) VALUES (sq_tb_banco.NEXTVAL, 'Bradesco', '237');
INSERT INTO tb_banco (cd_banco, nm_banco, nr_codigo_compe) VALUES (sq_tb_banco.NEXTVAL, 'Nubank', '260');
INSERT INTO tb_banco (cd_banco, nm_banco, nr_codigo_compe) VALUES (sq_tb_banco.NEXTVAL, 'C6 Bank', '336');
INSERT INTO tb_banco (cd_banco, nm_banco, nr_codigo_compe) VALUES (sq_tb_banco.NEXTVAL, 'Itaú Unibanco', '341');

-- Categorias de ENTRADA
INSERT INTO tb_categoria (cd_categoria, nm_categoria, tp_movimento, st_essencial) VALUES (sq_tb_categoria.NEXTVAL, 'Salário', 'ENTRADA', 'N');
INSERT INTO tb_categoria (cd_categoria, nm_categoria, tp_movimento, st_essencial) VALUES (sq_tb_categoria.NEXTVAL, 'Renda extra', 'ENTRADA', 'N');
INSERT INTO tb_categoria (cd_categoria, nm_categoria, tp_movimento, st_essencial) VALUES (sq_tb_categoria.NEXTVAL, 'Transferência recebida', 'ENTRADA', 'N');
INSERT INTO tb_categoria (cd_categoria, nm_categoria, tp_movimento, st_essencial) VALUES (sq_tb_categoria.NEXTVAL, 'Resgate do cofre', 'ENTRADA', 'N');

-- Categorias de SAÍDA (S = essencial, N = não essencial)
INSERT INTO tb_categoria (cd_categoria, nm_categoria, tp_movimento, st_essencial) VALUES (sq_tb_categoria.NEXTVAL, 'Mercado', 'SAIDA', 'S');
INSERT INTO tb_categoria (cd_categoria, nm_categoria, tp_movimento, st_essencial) VALUES (sq_tb_categoria.NEXTVAL, 'Moradia e contas', 'SAIDA', 'S');
INSERT INTO tb_categoria (cd_categoria, nm_categoria, tp_movimento, st_essencial) VALUES (sq_tb_categoria.NEXTVAL, 'Transporte e veículo', 'SAIDA', 'S');
INSERT INTO tb_categoria (cd_categoria, nm_categoria, tp_movimento, st_essencial) VALUES (sq_tb_categoria.NEXTVAL, 'Saúde', 'SAIDA', 'S');
INSERT INTO tb_categoria (cd_categoria, nm_categoria, tp_movimento, st_essencial) VALUES (sq_tb_categoria.NEXTVAL, 'Educação', 'SAIDA', 'S');
INSERT INTO tb_categoria (cd_categoria, nm_categoria, tp_movimento, st_essencial) VALUES (sq_tb_categoria.NEXTVAL, 'Pagamento de dívidas', 'SAIDA', 'S');
INSERT INTO tb_categoria (cd_categoria, nm_categoria, tp_movimento, st_essencial) VALUES (sq_tb_categoria.NEXTVAL, 'Fast food e delivery', 'SAIDA', 'N');
INSERT INTO tb_categoria (cd_categoria, nm_categoria, tp_movimento, st_essencial) VALUES (sq_tb_categoria.NEXTVAL, 'Lazer e viagem', 'SAIDA', 'N');
INSERT INTO tb_categoria (cd_categoria, nm_categoria, tp_movimento, st_essencial) VALUES (sq_tb_categoria.NEXTVAL, 'Compras pessoais', 'SAIDA', 'N');
INSERT INTO tb_categoria (cd_categoria, nm_categoria, tp_movimento, st_essencial) VALUES (sq_tb_categoria.NEXTVAL, 'Assinaturas', 'SAIDA', 'N');
INSERT INTO tb_categoria (cd_categoria, nm_categoria, tp_movimento, st_essencial) VALUES (sq_tb_categoria.NEXTVAL, 'Depósito no cofre', 'SAIDA', 'N');

COMMIT;

-- Conferência rápida: deve listar 8 bancos e 15 categorias
SELECT COUNT(*) AS qt_bancos FROM tb_banco;
SELECT COUNT(*) AS qt_categorias FROM tb_categoria;
