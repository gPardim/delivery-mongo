package br.pucgoias.ads.delivery.dominio;

import br.pucgoias.ads.delivery.excecao.TransicaoInvalidaException;

public enum StatusPedido {
    RECEBIDO,
    EM_PREPARO,
    SAIU_PARA_ENTREGA,
    ENTREGUE,
    CANCELADO;

    public StatusPedido proximo() {
        throw new UnsupportedOperationException("TODO: implementar StatusPedido.proximo()");
    }

    public boolean permiteCancelamento() {
        return this == RECEBIDO;
    }
}
