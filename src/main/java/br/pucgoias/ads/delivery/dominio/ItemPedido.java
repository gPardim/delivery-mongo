package br.pucgoias.ads.delivery.dominio;

import java.math.BigDecimal;

public record ItemPedido(
        String codigo,
        String nome,
        BigDecimal precoUnitario,
        int quantidade) {

    public BigDecimal subtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
