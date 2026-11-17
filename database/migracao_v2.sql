-- =====================================================================
-- CONTA CERTA - Migração v2 (integração com o front)
--
-- Rode UMA VEZ no Oracle que já tem as tabelas criadas pelo script.sql antigo,
-- ANTES de publicar a versão nova da API (ela já lê as colunas novas: sem a migração,
-- as rotas de dívidas e transações respondem erro 500).
-- Rodar de novo dá erro nos ALTER/CREATE ("coluna/objeto já existe"); isso não estraga nada.
-- Banco criado do zero com o script.sql atual NÃO precisa deste arquivo
-- (o script.sql já contém tudo o que está aqui).
--
-- O que muda:
--   1) tb_divida ganha nm_divida (apelido da dívida, ex.: "Cartão Banco A")
--   2) tb_transacao ganha st_recorrente ("repetir todo mês")
--   3) tabelas novas: tb_plano_quitacao, tb_licao_concluida e tb_contato
--   4) categoria nova: "Contas de consumo" (luz, água, internet)
-- Nada é apagado nem renomeado: os dados que já existem continuam valendo.
-- =====================================================================

-- 1) Apelido da dívida (opcional: as dívidas antigas ficam com nulo)
ALTER TABLE tb_divida ADD nm_divida VARCHAR2(60 CHAR);

-- 2) Transação recorrente (as transações antigas ficam com 'N')
ALTER TABLE tb_transacao ADD st_recorrente CHAR(1) DEFAULT 'N' NOT NULL;
ALTER TABLE tb_transacao ADD CONSTRAINT ck_tb_transacao_recor CHECK (st_recorrente IN ('S', 'N'));

-- 3) Tabelas novas
CREATE SEQUENCE sq_tb_contato START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE tb_plano_quitacao (
    cd_usuario       NUMBER(10)     CONSTRAINT pk_tb_plano_quitacao PRIMARY KEY,
    vl_mensal        NUMBER(12,2)   NOT NULL,
    tp_metodo        VARCHAR2(12)   NOT NULL,
    dt_atualizacao   DATE           DEFAULT SYSDATE NOT NULL,
    CONSTRAINT fk_tb_plano_usuario  FOREIGN KEY (cd_usuario) REFERENCES tb_usuario (cd_usuario),
    CONSTRAINT ck_tb_plano_valor    CHECK (vl_mensal > 0),
    CONSTRAINT ck_tb_plano_metodo   CHECK (tp_metodo IN ('AVALANCHE', 'BOLA_DE_NEVE'))
);

CREATE TABLE tb_licao_concluida (
    cd_usuario     NUMBER(10)   NOT NULL,
    cd_licao       NUMBER(10)   NOT NULL,
    dt_conclusao   DATE         DEFAULT SYSDATE NOT NULL,
    CONSTRAINT pk_tb_licao_concluida PRIMARY KEY (cd_usuario, cd_licao),
    CONSTRAINT fk_tb_licao_usuario   FOREIGN KEY (cd_usuario) REFERENCES tb_usuario (cd_usuario)
);

CREATE TABLE tb_contato (
    cd_contato     NUMBER(10)      CONSTRAINT pk_tb_contato PRIMARY KEY,
    nm_remetente   VARCHAR2(80 CHAR)    NOT NULL,
    ds_email       VARCHAR2(150 CHAR)   NOT NULL,
    ds_mensagem    VARCHAR2(1000 CHAR)  NOT NULL,
    dt_envio       DATE            DEFAULT SYSDATE NOT NULL
);

COMMENT ON TABLE tb_plano_quitacao  IS 'Valor mensal e método (avalanche ou bola de neve) escolhidos pelo usuário.';
COMMENT ON TABLE tb_licao_concluida IS 'Lições das trilhas de aprendizado que o usuário já concluiu.';
COMMENT ON TABLE tb_contato         IS 'Mensagens enviadas pelo formulário de contato do site.';

-- 4) Categoria nova (essencial). O NOT EXISTS evita duplicar se este bloco for rodado de novo.
INSERT INTO tb_categoria (cd_categoria, nm_categoria, tp_movimento, st_essencial)
SELECT sq_tb_categoria.NEXTVAL, 'Contas de consumo', 'SAIDA', 'S' FROM dual
 WHERE NOT EXISTS (SELECT 1 FROM tb_categoria WHERE nm_categoria = 'Contas de consumo');

COMMIT;

-- Conferência: deve mostrar 16 categorias e as 3 tabelas novas
SELECT COUNT(*) AS qt_categorias FROM tb_categoria;
SELECT table_name FROM user_tables
 WHERE table_name IN ('TB_PLANO_QUITACAO', 'TB_LICAO_CONCLUIDA', 'TB_CONTATO');
