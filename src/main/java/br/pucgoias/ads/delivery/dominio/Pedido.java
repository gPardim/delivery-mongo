package br.pucgoias.ads.delivery.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class Pedido {

    private String id;

    private String restauranteId;

    private Cliente cliente;

    private List<ItemPedido> itens;

    private StatusPedido status;

    private BigDecimal total;

    private Instant criadoEm;

    private Long versao;

    protected Pedido() {
    }

    public Pedido(String restauranteId, Cliente cliente, List<ItemPedido> itens) {
        this.restauranteId = restauranteId;
        this.cliente = cliente;
        this.itens = List.copyOf(itens);
        this.criadoEm = Instant.now();
    }

    public void avancarStatus() {
        throw new UnsupportedOperationException("TODO: implementar Pedido.avancarStatus()");
    }

    public void cancelar() {
        throw new UnsupportedOperationException("TODO: implementar Pedido.cancelar()");
    }

    public String getId() { return id; }
    public String getRestauranteId() { return restauranteId; }
    public Cliente getCliente() { return cliente; }
    public List<ItemPedido> getItens() { return itens; }
    public StatusPedido getStatus() { return status; }
    public BigDecimal getTotal() { return total; }
    public Instant getCriadoEm() { return criadoEm; }
    public Long getVersao() { return versao; }
}
