package br.pucgoias.ads.delivery.servico;

import java.math.BigDecimal;
import java.util.List;

import org.bson.types.Decimal128;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.mongodb.client.result.UpdateResult;

import br.pucgoias.ads.delivery.dominio.Cliente;
import br.pucgoias.ads.delivery.dominio.FaturamentoRestaurante;
import br.pucgoias.ads.delivery.dominio.ItemCardapio;
import br.pucgoias.ads.delivery.dominio.Pedido;
import br.pucgoias.ads.delivery.dominio.Restaurante;
import br.pucgoias.ads.delivery.excecao.RecursoNaoEncontradoException;
import br.pucgoias.ads.delivery.excecao.RestauranteDuplicadoException;
import br.pucgoias.ads.delivery.repositorio.PedidoRepository;
import br.pucgoias.ads.delivery.repositorio.RestauranteRepository;

@Service
public class DeliveryService {

    private final RestauranteRepository restauranteRepository;
    private final PedidoRepository pedidoRepository;
    private final MongoTemplate mongoTemplate;

    public DeliveryService(RestauranteRepository restauranteRepository,
                           PedidoRepository pedidoRepository,
                           MongoTemplate mongoTemplate) {
        this.restauranteRepository = restauranteRepository;
        this.pedidoRepository = pedidoRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public Restaurante cadastrarRestaurante(Restaurante restaurante) {
        try {
            return restauranteRepository.save(restaurante);
        } catch (DuplicateKeyException e) {
            throw new RestauranteDuplicadoException(restaurante.getNome());
        }
    }

    public Restaurante buscarRestaurante(String id) {
        return restauranteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Restaurante", id));
    }

    public Pedido buscarPedido(String id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", id));
    }

    public void alterarPreco(String restauranteId, String codigo, BigDecimal novoPreco) {
        Query query = Query.query(Criteria.where("id").is(restauranteId)
                .and("cardapio.codigo").is(codigo));
        Update update = new Update().set("cardapio.$.preco", new Decimal128(novoPreco));
        UpdateResult resultado = mongoTemplate.updateFirst(query, update, Restaurante.class);
        if (resultado.getMatchedCount() == 0) {
            throw new RecursoNaoEncontradoException("Item " + codigo + " do restaurante", restauranteId);
        }
    }


    public void adicionarItemCardapio(String restauranteId, ItemCardapio item) {
        Query query = Query.query(Criteria.where("id").is(restauranteId)
                .and("cardapio.codigo").ne(item.codigo()));
        Update update = new Update().push("cardapio", item);
        UpdateResult resultado = mongoTemplate.updateFirst(query, update, Restaurante.class);
        if (resultado.getMatchedCount() == 0) {
            if (!restauranteRepository.existsById(restauranteId)) {
                throw new RecursoNaoEncontradoException("Restaurante", restauranteId);
            }
            throw new br.pucgoias.ads.delivery.excecao.ItemDuplicadoException(item.codigo());
        }
    }

    public Pedido criarPedido(String restauranteId, Cliente cliente, List<ItemSolicitado> solicitados) {
        if (solicitados == null || solicitados.isEmpty()) {
            throw new br.pucgoias.ads.delivery.excecao.PedidoInvalidoException("pedido deve conter ao menos um item");
        }
        Restaurante restaurante = buscarRestaurante(restauranteId);
        List<br.pucgoias.ads.delivery.dominio.ItemPedido> itens = new java.util.ArrayList<>();
        for (ItemSolicitado solicitado : solicitados) {
            if (solicitado.quantidade() <= 0) {
                throw new br.pucgoias.ads.delivery.excecao.PedidoInvalidoException(
                        "quantidade deve ser positiva: " + solicitado.codigo());
            }
            ItemCardapio itemCardapio = restaurante.buscarItem(solicitado.codigo())
                    .filter(ItemCardapio::disponivel)
                    .orElseThrow(() -> new br.pucgoias.ads.delivery.excecao.ItemIndisponivelException(solicitado.codigo()));
            itens.add(new br.pucgoias.ads.delivery.dominio.ItemPedido(
                    itemCardapio.codigo(), itemCardapio.nome(), itemCardapio.preco(), solicitado.quantidade()));
        }
        Pedido pedido = new Pedido(restauranteId, cliente, itens);
        return pedidoRepository.save(pedido);
    }

    public Pedido avancarStatus(String pedidoId) {
        Pedido pedido = buscarPedido(pedidoId);
        pedido.avancarStatus();
        return pedidoRepository.save(pedido);
    }

    public Pedido cancelarPedido(String pedidoId) {
        Pedido pedido = buscarPedido(pedidoId);
        pedido.cancelar();
        return pedidoRepository.save(pedido);
    }

    public List<FaturamentoRestaurante> faturamentoPorRestaurante() {
        org.springframework.data.mongodb.core.aggregation.Aggregation agregacao =
                org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation(
                    org.springframework.data.mongodb.core.aggregation.Aggregation.match(
                            Criteria.where("status").is(br.pucgoias.ads.delivery.dominio.StatusPedido.ENTREGUE)),
                    org.springframework.data.mongodb.core.aggregation.Aggregation.group("restauranteId")
                            .sum("total").as("faturamento")
                            .count().as("quantidadePedidos"),
                    org.springframework.data.mongodb.core.aggregation.Aggregation
                            .project("faturamento", "quantidadePedidos")
                            .and("restauranteId").previousOperation(),
                    org.springframework.data.mongodb.core.aggregation.Aggregation.sort(
                            org.springframework.data.domain.Sort.Direction.DESC, "faturamento")
                );
        return mongoTemplate.aggregate(agregacao, Pedido.class, FaturamentoRestaurante.class)
                .getMappedResults();
    }
}
