package ecommerce.external.fake;

import ecommerce.dto.PagamentoDTO;
import ecommerce.external.IPagamentoExternal;

public class PagamentoSimulado implements IPagamentoExternal {
  private final boolean pagamentoAutorizado;
  private final Long transacaoIdSimulada;

  public PagamentoSimulado(boolean pagamentoAutorizado, Long transacaoIdSimulada) {
    this.pagamentoAutorizado = pagamentoAutorizado;
    this.transacaoIdSimulada = transacaoIdSimulada;
  }

  @Override
  public PagamentoDTO autorizarPagamento(Long clienteId, Double custoTotal) {
    if (pagamentoAutorizado) {
      // Retorna pagamento autorizado com o ID de transação simulado
      return new PagamentoDTO(true, transacaoIdSimulada);
    } else {
      // Retorna pagamento não autorizado
      return new PagamentoDTO(false, null);
    }
  }

  @Override
  public void cancelarPagamento(Long clienteId, Long pagamentoTransacaoId) {
    // Simula o cancelamento do pagamento
    System.out.println("Pagamento cancelado para o cliente: " + clienteId + ", Transação: " + pagamentoTransacaoId);
  }
}
