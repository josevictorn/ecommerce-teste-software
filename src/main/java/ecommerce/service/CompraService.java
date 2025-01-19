package ecommerce.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ecommerce.dto.CompraDTO;
import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;
import ecommerce.dto.PagamentoDTO;
import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Cliente;
import ecommerce.external.IEstoqueExternal;
import ecommerce.external.IPagamentoExternal;
import jakarta.transaction.Transactional;

@Service
public class CompraService {

	private final CarrinhoDeComprasService carrinhoService;
	private final ClienteService clienteService;

	private final IEstoqueExternal estoqueExternal;
	private final IPagamentoExternal pagamentoExternal;

	@Autowired
	public CompraService(CarrinhoDeComprasService carrinhoService, ClienteService clienteService,
			IEstoqueExternal estoqueExternal, IPagamentoExternal pagamentoExternal) {
		this.carrinhoService = carrinhoService;
		this.clienteService = clienteService;

		this.estoqueExternal = estoqueExternal;
		this.pagamentoExternal = pagamentoExternal;
	}

	@Transactional
	public CompraDTO finalizarCompra(Long carrinhoId, Long clienteId) {
		Cliente cliente = clienteService.buscarPorId(clienteId);
		CarrinhoDeCompras carrinho = carrinhoService.buscarPorCarrinhoIdEClienteId(carrinhoId, cliente);

		List<Long> produtosIds = carrinho.getItens().stream().map(i -> i.getProduto().getId())
				.collect(Collectors.toList());
		List<Long> produtosQtds = carrinho.getItens().stream().map(i -> i.getQuantidade()).collect(Collectors.toList());

		DisponibilidadeDTO disponibilidade = estoqueExternal.verificarDisponibilidade(produtosIds, produtosQtds);

		if (!disponibilidade.disponivel()) {
			throw new IllegalStateException("Itens fora de estoque.");
		}

		BigDecimal custoTotal = calcularCustoTotal(carrinho);

		PagamentoDTO pagamento = pagamentoExternal.autorizarPagamento(cliente.getId(), custoTotal.doubleValue());

		if (!pagamento.autorizado()) {
			throw new IllegalStateException("Pagamento não autorizado.");
		}

		EstoqueBaixaDTO baixaDTO = estoqueExternal.darBaixa(produtosIds, produtosQtds);

		if (!baixaDTO.sucesso()) {
			pagamentoExternal.cancelarPagamento(cliente.getId(), pagamento.transacaoId());
			throw new IllegalStateException("Erro ao dar baixa no estoque.");
		}

		CompraDTO compraDTO = new CompraDTO(true, pagamento.transacaoId(), "Compra finalizada com sucesso.");

		return compraDTO;
	}

	public BigDecimal calcularCustoTotal(CarrinhoDeCompras carrinho) {
    BigDecimal custoItens = carrinho.getItens().stream()
			.map(item -> item.getProduto().getPreco().multiply(BigDecimal.valueOf(item.getQuantidade())))
			.reduce(BigDecimal.ZERO, BigDecimal::add);

    double pesoTotal = carrinho.getItens().stream()
			.mapToDouble(item -> item.getProduto().getPeso() * item.getQuantidade())
			.sum();

    // Cálculo do valor do frete com base no peso
    BigDecimal frete = calcularFrete(pesoTotal, carrinho.getCliente());

    BigDecimal desconto = BigDecimal.ZERO;
    if (custoItens.compareTo(BigDecimal.valueOf(1000)) > 0) {
			desconto = custoItens.multiply(BigDecimal.valueOf(0.2));
    } else if (custoItens.compareTo(BigDecimal.valueOf(500)) > 0) {
			desconto = custoItens.multiply(BigDecimal.valueOf(0.1));
    }

    BigDecimal custoItensComDesconto = custoItens.subtract(desconto);

    // Cálculo do custo total (itens com desconto + frete)
    return custoItensComDesconto.add(frete);
	}

	private BigDecimal calcularFrete(double pesoTotal, Cliente cliente) {
		BigDecimal valorFrete;

		if (pesoTotal <= 5) {
			valorFrete = BigDecimal.ZERO;
		} else if (pesoTotal <= 10) {
			valorFrete = BigDecimal.valueOf(pesoTotal * 2);
		} else if (pesoTotal <= 50) {
			valorFrete = BigDecimal.valueOf(pesoTotal * 4);
		} else {
			valorFrete = BigDecimal.valueOf(pesoTotal * 7);
		}

		// Aplicação de descontos no frete com base no tipo de cliente
		switch (cliente.getTipo()) {
			case OURO:
				valorFrete = BigDecimal.ZERO; // Isenção total
				break;
			case PRATA:
				valorFrete = valorFrete.multiply(BigDecimal.valueOf(0.5)); // 50% de desconto
				break;
			case BRONZE:
				break; // Sem desconto adicional
			default:
				break; // Sem desconto adicional
		}

		return valorFrete;
	}

}
