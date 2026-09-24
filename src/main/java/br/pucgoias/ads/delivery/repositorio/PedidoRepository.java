package br.pucgoias.ads.delivery.repositorio;

import org.springframework.data.mongodb.repository.MongoRepository;

import br.pucgoias.ads.delivery.dominio.Pedido;

public interface PedidoRepository extends MongoRepository<Pedido, String> {
}
