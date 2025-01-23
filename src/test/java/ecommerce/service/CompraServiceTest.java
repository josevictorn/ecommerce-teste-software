package ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ecommerce.dto.CompraDTO;
import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;
import ecommerce.dto.PagamentoDTO;
import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Cliente;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.TipoCliente;
import ecommerce.external.IEstoqueExternal;
import ecommerce.external.IPagamentoExternal;

public class CompraServiceTest {

  // private CompraService compraService;

  // @BeforeEach
  // void setUp() {
  //     compraService = new CompraService(null, null, null, null); // Dependências são irrelevantes para este teste.
  // }
//gustavo
   @Mock
    private CarrinhoDeComprasService carrinhoService;

    @Mock
    private ClienteService clienteService;

    @Mock
    private IEstoqueExternal estoqueExternal;

    @Mock
    private IPagamentoExternal pagamentoExternal;

    @InjectMocks
    private CompraService compraService;

    @BeforeEach
    void setUp() {
        // Inicializa os mocks do Mockito
        MockitoAnnotations.openMocks(this);
    }

 @Test
    void finalizarCompra_comCarrinhoValido() {
        // Configurar cliente
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setTipo(TipoCliente.BRONZE); // Define o tipo do cliente

        // Configurar produto e itens no carrinho
        Produto produto = new Produto();
        produto.setId(1L);
        produto.setPreco(BigDecimal.valueOf(100));
        produto.setPeso(5);

        ItemCompra item = new ItemCompra();
        item.setProduto(produto);
        item.setQuantidade(2L);

        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1L, cliente, Arrays.asList(item), LocalDate.now());

        // Criar uma instância de DisponibilidadeDTO válida
        DisponibilidadeDTO disponibilidade = new DisponibilidadeDTO(true, Collections.emptyList());

        // Criar uma instância de PagamentoDTO válida
        PagamentoDTO pagamento = new PagamentoDTO(true, 12345L);

        // Criar uma instância de EstoqueBaixaDTO válida
        EstoqueBaixaDTO baixaDTO = new EstoqueBaixaDTO(true);

        // Configurar comportamento dos mocks
        when(clienteService.buscarPorId(1L)).thenReturn(cliente); // Cliente encontrado
        when(carrinhoService.buscarPorCarrinhoIdEClienteId(eq(1L), eq(cliente))).thenReturn(carrinho); // Carrinho encontrado
        when(estoqueExternal.verificarDisponibilidade(anyList(), anyList())).thenReturn(disponibilidade); // Estoque disponível
        when(pagamentoExternal.autorizarPagamento(anyLong(), anyDouble())).thenReturn(pagamento); // Pagamento autorizado
        when(estoqueExternal.darBaixa(anyList(), anyList())).thenReturn(baixaDTO); // Estoque atualizado com sucesso

        // Executar o método
        CompraDTO resultado = compraService.finalizarCompra(1L, 1L);

        // Verificar interações e resultado
        verify(clienteService, times(1)).buscarPorId(1L);
        verify(carrinhoService, times(1)).buscarPorCarrinhoIdEClienteId(eq(1L), eq(cliente));
        verify(estoqueExternal, times(1)).verificarDisponibilidade(anyList(), anyList());
        verify(pagamentoExternal, times(1)).autorizarPagamento(anyLong(), anyDouble());
        verify(estoqueExternal, times(1)).darBaixa(anyList(), anyList());
        assertTrue(resultado.sucesso());
        assertEquals("Compra finalizada com sucesso.", resultado.mensagem());
    }
//jose
  @Test
  void calcularCustoTotal_clienteOuro_comFreteGratis_semDescontoItens_pesoAte5kg() {
    Cliente cliente = new Cliente();
    cliente.setTipo(TipoCliente.OURO);

    Produto produto = new Produto();
    produto.setPeso(3); // Peso unitário do produto
    produto.setPreco(BigDecimal.valueOf(150));

    ItemCompra item = new ItemCompra();
    item.setProduto(produto);
    item.setQuantidade(1L); // Quantidade de itens

    List<ItemCompra> itens = Arrays.asList(item);

    // Criar o carrinho de compras com cliente e itens
    CarrinhoDeCompras carrinho = new CarrinhoDeCompras(
      1L, 
      cliente, 
      itens, 
      LocalDate.now()
    );

    // Cálculo do custo total, sem frete (cliente Ouro)
    BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho);

    // Cálculo esperado: 150,00 (sem desconto nos itens)
    // Peso total: 3 * 1 = 3kg
    // Frete: 3kg * 0,00 = R$ 0,00 (o frete seria grátis de qualquer forma, pois o cliente é OURO)
    BigDecimal esperado = BigDecimal.valueOf(150);

    assertEquals(esperado, custoTotal);
  }

   @Test
  void calcularCustoTotal_clientePrata_comDescontoFrete_semDescontoItens_pesoEntre1e10kg() {
      Cliente cliente = new Cliente();
      cliente.setTipo(TipoCliente.PRATA);

      Produto produto = new Produto();
      produto.setPeso(5); // Peso unitário do produto
      produto.setPreco(BigDecimal.valueOf(100));

      ItemCompra item = new ItemCompra();
      item.setProduto(produto);
      item.setQuantidade(2L); // Quantidade de itens

      List<ItemCompra> itens = Arrays.asList(item);

      CarrinhoDeCompras carrinho = new CarrinhoDeCompras(
        1L, 
        cliente, 
        itens, 
        LocalDate.now()
      );

      // Cálculo do custo total com desconto de 50% no frete (cliente Prata)
      BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho).setScale(1, RoundingMode.HALF_UP);

      // Cálculo esperado: 100 * 2 = 200,00 (sem desconto nos itens)
      // Peso total: 5 * 2 = 10kg
      // Frete: 10kg * 2,00 (R$ 20,00) com desconto de 50%, ou seja, R$ 10,00
      BigDecimal esperado = BigDecimal.valueOf(100 * 2).add(BigDecimal.valueOf(10)).setScale(1, RoundingMode.HALF_UP);

      assertEquals(esperado, custoTotal);
  }

  @Test
  void calcularCustoTotal_clientePrata_comDescontoFrete_semDescontoItens_pesoEntre5e10kg() {
    Cliente cliente = new Cliente();
    cliente.setTipo(TipoCliente.PRATA);

    Produto produto = new Produto();
    produto.setPeso(4); // Peso unitário do produto
    produto.setPreco(BigDecimal.valueOf(100));

    ItemCompra item = new ItemCompra();
    item.setProduto(produto);
    item.setQuantidade(2L); // Quantidade de itens

    List<ItemCompra> itens = Arrays.asList(item);

    // Criar o carrinho de compras com cliente e itens
    CarrinhoDeCompras carrinho = new CarrinhoDeCompras(
      1L, 
      cliente, 
      itens, 
      LocalDate.now()
    );

    // Cálculo do custo total com desconto de 50% no frete (cliente Prata)
    BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho).setScale(1, RoundingMode.HALF_UP);

    // Cálculo esperado: 100 * 2 = 200,00 (sem desconto nos itens)
    // Peso total: 4 * 2 = 8kg
    // Frete: 8kg * 2,00 (R$ 16,00) com desconto de 50%, ou seja, R$ 8,00
    BigDecimal esperado = BigDecimal.valueOf(100 * 2).add(BigDecimal.valueOf(8)).setScale(1, RoundingMode.HALF_UP);

    assertEquals(esperado, custoTotal);
  }

  @Test
  void calcularCustoTotal_clienteBronze_comFreteintegral_semDescontoItens_pesoEntre10e50kg() {
    Cliente cliente = new Cliente();
    cliente.setTipo(TipoCliente.BRONZE);

    Produto produto = new Produto();
    produto.setPeso(12); // Peso unitário do produto
    produto.setPreco(BigDecimal.valueOf(50));

    ItemCompra item = new ItemCompra();
    item.setProduto(produto);
    item.setQuantidade(3L); // Quantidade de itens

    List<ItemCompra> itens = Arrays.asList(item);

    // Criar o carrinho de compras com cliente e itens
    CarrinhoDeCompras carrinho = new CarrinhoDeCompras(
      1L, 
      cliente, 
      itens, 
      LocalDate.now()
    );

    // Cálculo do custo total com frete integral (cliente Bronze)
    BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho).setScale(1, RoundingMode.HALF_UP);

    // Cálculo esperado: 50 * 3 = 150,00 (sem desconto nos itens)
    // Peso total: 12 * 3 = 36kg
    // Frete: 36kg * 4,00 (R$ 144,00)
    BigDecimal esperado = BigDecimal.valueOf(50 * 3).add(BigDecimal.valueOf(144)).setScale(1, RoundingMode.HALF_UP);

    assertEquals(esperado, custoTotal);
  }
  
  @Test
  void calcularCustoTotal_clientePrata_comDescontoFrete_semDescontoItens_pesoAcima50kg() {
    Cliente cliente = new Cliente();
    cliente.setTipo(TipoCliente.PRATA);

    Produto produto = new Produto();
    produto.setPeso(20); // Peso unitário do produto
    produto.setPreco(BigDecimal.valueOf(50));

    ItemCompra item = new ItemCompra();
    item.setProduto(produto);
    item.setQuantidade(3L); // Quantidade de itens

    List<ItemCompra> itens = Arrays.asList(item);

    // Criar o carrinho de compras com cliente e itens
    CarrinhoDeCompras carrinho = new CarrinhoDeCompras(
      1L, 
      cliente, 
      itens, 
      LocalDate.now()
    );

    // Cálculo do custo total com desconto de 50% no frete (cliente Prata)
    BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho).setScale(1, RoundingMode.HALF_UP);

    // Cálculo esperado: 50 * 3 = 150,00 (sem desconto nos itens)
    // Peso total: 20 * 3 = 60kg
    // Frete: 60kg * 7,00 (R$ 420,00) com desconto de 50%, ou seja, R$ 210,00
    BigDecimal esperado = BigDecimal.valueOf(50 * 3).add(BigDecimal.valueOf(210)).setScale(1, RoundingMode.HALF_UP);

    assertEquals(esperado, custoTotal);
  }

  @Test
  void calcularCustoTotal_clienteBronze_semDescontoFrete_comDesconto10EmItens_pesoAcima50kg() {
    Cliente cliente = new Cliente();
    cliente.setTipo(TipoCliente.BRONZE);

    Produto produto = new Produto();
    produto.setPeso(5); // Peso unitário do produto
    produto.setPreco(BigDecimal.valueOf(200)); // Preço unitário do produto

    ItemCompra item = new ItemCompra();
    item.setProduto(produto);
    item.setQuantidade(3L); // Quantidade de itens

    List<ItemCompra> itens = Arrays.asList(item);

    // Criar o carrinho de compras com cliente e itens
    CarrinhoDeCompras carrinho = new CarrinhoDeCompras(
      1L, 
      cliente, 
      itens, 
      LocalDate.now()
    );

    // Cálculo do custo total com desconto de 10% no valor dos itens (cliente Bronze)
    BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho).setScale(1, RoundingMode.HALF_UP);

    // Cálculo esperado:
    // Preço dos itens: 200 * 3 = 600,00
    // Desconto de 10%: 600,00 * 0.9 = 540,00
    // Peso total: 5 * 3 = 15kg
    // Frete: 15kg * 4,00 = 60,00 (frete de R$ 4,00 por kg)
    BigDecimal esperado = BigDecimal.valueOf(540).add(BigDecimal.valueOf(60)).setScale(1, RoundingMode.HALF_UP);

    assertEquals(esperado, custoTotal);
  }

  @Test
  void calcularCustoTotal_clienteOuro_comFreteGratis_comDesconto20EmItens_pesoEntre5e10kg() {
    Cliente cliente = new Cliente();
    cliente.setTipo(TipoCliente.OURO);

    Produto produto = new Produto();
    produto.setPeso(2); // Peso unitário do produto
    produto.setPreco(BigDecimal.valueOf(300)); // Preço unitário do produto

    ItemCompra item = new ItemCompra();
    item.setProduto(produto);
    item.setQuantidade(4L); // Quantidade de itens

    List<ItemCompra> itens = Arrays.asList(item);

    // Criar o carrinho de compras com cliente e itens
    CarrinhoDeCompras carrinho = new CarrinhoDeCompras(
      1L, 
      cliente, 
      itens, 
      LocalDate.now()
    );

    // Cálculo do custo total com desconto de 20% no valor dos itens (cliente Prata)
    BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho).setScale(1, RoundingMode.HALF_UP);

    // Cálculo esperado:
    // Preço dos itens: 300 * 4 = 1200,00
    // Desconto de 20%: 1200,00 * 0.8 = 960,00
    // Peso total: 2kg * 4 = 8kg
    // Frete: 8kg * 2,00 = 16,00 (frete seria de R$ 2,00 por kg, porém, o frete é grátis, pois o cliente é OURO).
    BigDecimal esperado = BigDecimal.valueOf(960).add(BigDecimal.valueOf(0)).setScale(1, RoundingMode.HALF_UP);

    assertEquals(esperado, custoTotal);
  }

  @Test
  void calcularCustoTotal_clientePrata_comDescontoFrete_semDescontoItens_PesoLimite50kg() {
     
      Cliente cliente = new Cliente();
      cliente.setTipo(TipoCliente.PRATA);

     
      Produto produto = new Produto();
      produto.setPeso(50); // Peso unitário do produto
      produto.setPreco(BigDecimal.valueOf(100));

     
      ItemCompra item = new ItemCompra();
      item.setProduto(produto);
      item.setQuantidade(1L); // Quantidade de itens

    
      List<ItemCompra> itens = Arrays.asList(item);

      CarrinhoDeCompras carrinho = new CarrinhoDeCompras(
        1L, 
        cliente, 
        itens, 
        LocalDate.now()
      );

      // Cálculo do custo total com desconto de 50% no frete (cliente Prata)
      BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho).setScale(1, RoundingMode.HALF_UP);
      // Cálculo esperado: 100 * 1 = 100,00 (sem desconto nos itens)
      // Peso total: 50 * 1 = 50kg
      // Frete: 50kg * 4,00 (R$ 200,00) com desconto de 50%, ou seja, R$ 100,00
      BigDecimal esperado = BigDecimal.valueOf(100 * 1).add(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
      // Verificando se o custo total calculado é igual ao esperado
      assertEquals(esperado, custoTotal);
  }

}
//teste
