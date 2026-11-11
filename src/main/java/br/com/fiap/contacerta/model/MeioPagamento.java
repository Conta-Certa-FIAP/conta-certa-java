package br.com.fiap.contacerta.model;

/** Valores aceitos pelo CHECK de tb_transacao.tp_meio. */
public enum MeioPagamento {
    PIX,
    TRANSFERENCIA,
    CARTAO_DEBITO,
    CARTAO_CREDITO,
    BOLETO,
    OUTRO;

    public boolean usaCartao() {
        return this == CARTAO_DEBITO || this == CARTAO_CREDITO;
    }
}
