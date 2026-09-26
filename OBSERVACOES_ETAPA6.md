# Etapa 6 — Verificação no banco (delivery_test)

## 1. `db.pedidos.findOne()`

```json
{
  _id: ObjectId('6ab822627b0d4f0c2cdf6410'),
  restauranteId: '6ab822617b0d4f0c2cdf640f',
  cliente: { nome: 'Ana Souza', telefone: '62999990001' },
  itens: [
    { codigo: 'PAMONHA', nome: 'Pamonha de sal', precoUnitario: Decimal128('12.00'), quantidade: 2 },
    { codigo: 'EMPADAO', nome: 'Empadao goiano', precoUnitario: Decimal128('25.50'), quantidade: 1 }
  ],
  status: 'RECEBIDO',
  total: Decimal128('49.50'),
  criadoEm: ISODate('2026-09-26T19:52:02.022Z'),
  versao: Long('0'),
  _class: 'br.pucgoias.ads.delivery.dominio.Pedido'
}
```

**Observação:** `total` e `precoUnitario` foram gravados como `Decimal128`, não como texto — resultado de anotar os dois campos com `@Field(targetType = FieldType.DECIMAL128)`. Sem essa anotação, o Spring Data MongoDB grava `BigDecimal` como string, o que impediria o `$sum` do pipeline de agregação de funcionar corretamente (o faturamento sairia zerado).

O documento também evidencia as duas decisões de modelagem do agregado `Pedido`:
- **Incorporação**: `cliente` e `itens` fazem parte do mesmo documento, lidos e gravados como uma unidade só.
- **Referência**: `restauranteId` é apenas uma string apontando para outro agregado (`Restaurante`), que existe de forma independente.
- **Referência estendida**: `nome` e `precoUnitario` de cada item foram copiados do cardápio no momento da criação do pedido — uma alteração posterior de preço no restaurante não afeta pedidos já existentes (regra R4).

## 2. `db.pedidos.countDocuments({ total: { $type: "decimal" } })`

Resultado: `1`

Confirma, de forma programática, que o campo `total` do pedido está gravado com o tipo BSON `decimal` (Decimal128), e não como string.

## 3. `db.restaurantes.find({ "cardapio.codigo": "PAMONHA" }, { nome: 1, "cardapio.$": 1 })`

```json
[
  {
    _id: ObjectId('6ab822617b0d4f0c2cdf640f'),
    nome: 'Sabor do Cerrado',
    cardapio: [
      { codigo: 'PAMONHA', nome: 'Pamonha de sal', preco: Decimal128('12.00'), disponivel: true }
    ]
  }
]
```

**Observação:** o operador posicional `$` na projeção retorna apenas o elemento do array `cardapio` que casou com o filtro da consulta (`cardapio.codigo: "PAMONHA"`), em vez do array inteiro. É a mesma lógica usada na atualização condicional de `DeliveryService.alterarPreco`, que usa `"cardapio.$.preco"` para alterar apenas o item correspondente — aqui é a versão de leitura do mesmo mecanismo.
