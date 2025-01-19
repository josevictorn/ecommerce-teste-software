package ecommerce.external.fake;

import java.util.ArrayList;
import java.util.List;

import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;
import ecommerce.external.IEstoqueExternal;

public class EstoqueSimulado implements IEstoqueExternal {
  private final List<Long> produtosIndisponiveis;

  public EstoqueSimulado(List<Long> produtosIndisponiveis) {
    this.produtosIndisponiveis = produtosIndisponiveis != null ? produtosIndisponiveis : new ArrayList<>();
  }

  @Override
  public EstoqueBaixaDTO darBaixa(List<Long> produtosIds, List<Long> produtosQuantidades) {
    // Simula uma operação bem-sucedida de baixa no estoque
    return new EstoqueBaixaDTO(true);
  }

  @Override
  public DisponibilidadeDTO verificarDisponibilidade(List<Long> produtosIds, List<Long> produtosQuantidades) {
    // Identifica quais produtos da solicitação estão indisponíveis
    List<Long> idsIndisponiveis = new ArrayList<>();
    for (Long id : produtosIds) {
      if (produtosIndisponiveis.contains(id)) {
        idsIndisponiveis.add(id);
      }
    }

    boolean disponivel = idsIndisponiveis.isEmpty();
    return new DisponibilidadeDTO(disponivel, idsIndisponiveis);
  }
}
