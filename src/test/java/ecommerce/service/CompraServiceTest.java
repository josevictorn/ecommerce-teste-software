package ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

  @Test
  void calcularCustoTotal_clientePrata_semFreteZero_pesoAbaixo5kg() {
      Cliente cliente = new Cliente();
      cliente.setTipo(TipoCliente.PRATA);

      Produto produto = new Produto();
      produto.setPeso(1); // Peso unitário do produto (1kg)
      produto.setPreco(BigDecimal.valueOf(100)); // Preço unitário do produto
   
      ItemCompra item = new ItemCompra();
      item.setProduto(produto);
      item.setQuantidade(3L); // Quantidade de itens (3 produtos de 100, total de R$ 300)

    
      List<ItemCompra> itens = Arrays.asList(item);
    
      CarrinhoDeCompras carrinho = new CarrinhoDeCompras(
          1L, 
          cliente, 
          itens, 
          LocalDate.now()
      );
      // Cálculo do custo total com desconto de 50% no frete (cliente Prata)
      BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho).setScale(1, RoundingMode.HALF_UP);
      // Cálculo esperado: 100 * 3 = 300  (sem desconto)
      // Peso total: 3 * 1 = 3kg  (menor que 5kg, então o frete será 0)
      // Frete: 3kg * 0 = 0
      BigDecimal esperado = BigDecimal.valueOf(100 * 3).add(BigDecimal.valueOf(0)).setScale(1, RoundingMode.HALF_UP);
      // Verificando se o custo total calculado é igual ao esperado
      assertEquals(esperado, custoTotal);
  }

  @Test
  void carrinhoCompra_valor1000_comDesconto10() {
      Cliente cliente = new Cliente();
      cliente.setTipo(TipoCliente.PRATA);

      Produto produto = new Produto();
      produto.setPeso(5); // Peso do produto
      produto.setPreco(BigDecimal.valueOf(250)); // Preço da unidade do produto

      ItemCompra item = new ItemCompra();
      item.setProduto(produto);
      item.setQuantidade(4L); // Quantidade de itens (4)

      List<ItemCompra> itens = Arrays.asList(item);
      // Criando o carrinho de compras
      CarrinhoDeCompras carrinho = new CarrinhoDeCompras(
          1L, 
          cliente, 
          itens, 
          LocalDate.now()
      );
      // Cálculo do custo total com desconto de 10% (custoItens = 1000)
      // 4 produtos de 250 = 1000
      // 10% de desconto
      // 1000 - 100 = 900
      BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho).setScale(1, RoundingMode.HALF_UP);
      // Esperado: Peso total = 4 produtos de 5kg = 20kg
      // 20kg * R$ 4,00 = R$ 80,00
      // Frete com desconto de 50% (cliente PRATA): 80 * 0.5 = 40,0
      BigDecimal esperado = BigDecimal.valueOf(250 * 4).subtract(BigDecimal.valueOf(250 * 4).multiply(BigDecimal.valueOf(0.1))) 
              .add(BigDecimal.valueOf(20 * 4).multiply(BigDecimal.valueOf(0.5)));
      // Verificando se o custo total calculado é igual ao esperado  
      assertEquals(esperado, custoTotal);
      
  }
  


  @Test
  void carrinhoCompra_valor999_comDesconto10() {
      Cliente cliente = new Cliente();
      cliente.setTipo(TipoCliente.PRATA);

      Produto produto = new Produto();
      produto.setPeso(5); // Peso do produto
      produto.setPreco(BigDecimal.valueOf(249.75)); // Preço do produto (249.75 * 4 = 999)
    
      ItemCompra item = new ItemCompra();
      item.setProduto(produto);
      item.setQuantidade(4L); // Quantidade de itens (249.75 * 4 = 999)
      // Criando a lista de itens no carrinho
      List<ItemCompra> itens = Arrays.asList(item);
      // Criando o carrinho de compras
      CarrinhoDeCompras carrinho = new CarrinhoDeCompras(
          1L, 
          cliente, 
          itens, 
          LocalDate.now()
      );
      // Cálculo do custo total com desconto de 10% (custoItens = 999)
      // 4 produtos de 249.75 = 999
      // 10% de desconto
      // 999 - 99.9 = 899.1
      BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho).setScale(1, RoundingMode.HALF_UP);
      // Peso total = 4 produtos de 5kg = 20kg
      // 20kg * R$ 4,00 = R$ 80,00
      // Frete com desconto de 50% (cliente PRATA): 80 * 0.5 = 40,00
      BigDecimal esperado = BigDecimal.valueOf(249.75 * 4).subtract(BigDecimal.valueOf(249.75 * 4).multiply(BigDecimal.valueOf(0.1))) 
              .add(BigDecimal.valueOf(20 * 4).multiply(BigDecimal.valueOf(0.5))).setScale(1, RoundingMode.HALF_UP);

      assertEquals(esperado, custoTotal);
  }
  
  @Test
  void carrinhoCompra_valor1001_comDesconto20() {
      Cliente cliente = new Cliente();
      cliente.setTipo(TipoCliente.PRATA);

      Produto produto = new Produto();
      produto.setPeso(5); // Peso do produto
      produto.setPreco(BigDecimal.valueOf(250.25)); // Preço do produto (250.25 * 4 = 1001)

      ItemCompra item = new ItemCompra();
      item.setProduto(produto);
      item.setQuantidade(4L); // Quantidade de itens (250.25 * 4 = 1001)

      List<ItemCompra> itens = Arrays.asList(item);

      CarrinhoDeCompras carrinho = new CarrinhoDeCompras(
          1L, 
          cliente, 
          itens, 
          LocalDate.now()
      );
      // Custo total com desconto de 20% (custoItens = 1001)
      // 4 produtos de 250.25 = 1001
      // 20% de desconto
      // 1001 - 200.2 = 800.8
      BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho).setScale(1, RoundingMode.HALF_UP);
      // Peso total = 4 produtos de 5kg = 20kg
      // BigDecimal frete = BigDecimal.valueOf(20 * 4); // 20kg * R$ 4,00 = R$ 80,00
      // Frete com desconto de 50% (cliente PRATA): 80 * 0.5 = 40,00
      BigDecimal esperado = BigDecimal.valueOf(250.25 * 4).subtract(BigDecimal.valueOf(250.25 * 4).multiply(BigDecimal.valueOf(0.2)))
              .add(BigDecimal.valueOf(20 * 4).multiply(BigDecimal.valueOf(0.5))).setScale(1, RoundingMode.HALF_UP);
      // Verificando se o custo total calculado é igual ao esperado
      assertEquals(esperado, custoTotal);
  }
  
  @Test
  void carrinhoCompra_valorAcimaDe500_comDesconto10() {
      Cliente cliente = new Cliente();
      cliente.setTipo(TipoCliente.PRATA);

      Produto produto = new Produto();
      produto.setPeso(5); // Peso do produto
      produto.setPreco(BigDecimal.valueOf(125.25)); // Preço do produto (125.25 * 4 = 501)

      ItemCompra item = new ItemCompra();
      item.setProduto(produto);
      item.setQuantidade(4L); // Quantidade de itens (125.25 * 4 = 501)

      List<ItemCompra> itens = Arrays.asList(item);

      CarrinhoDeCompras carrinho = new CarrinhoDeCompras(
          1L, 
          cliente, 
          itens, 
          LocalDate.now()
      );
      // Cálculo do custo total com desconto de 10% (custoItens = 501)
      // 4 produtos de 125.25 = 501
      // 10% de desconto
      // 501 - 50.1 = 450.90
      BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho).setScale(1, RoundingMode.HALF_UP);
      // Peso total = 4 produtos de 5kg = 20kg
      // 20kg * R$ 4,00 = R$ 80,00
      // Frete com desconto de 50% (cliente PRATA): 80 * 0.5 = 40,00 
      BigDecimal esperado = BigDecimal.valueOf(125.25 * 4).subtract(BigDecimal.valueOf(125.25 * 4).multiply(BigDecimal.valueOf(0.1)))
              .add(BigDecimal.valueOf(20 * 4).multiply(BigDecimal.valueOf(0.5))).setScale(1, RoundingMode.HALF_UP);
      
      // Verificando se o custo total calculado é igual ao esperado
      assertEquals(esperado, custoTotal);
  }
  
  
  @Test
  void carrinhoCompra_valor500_semDesconto10() {
      Cliente cliente = new Cliente();
      cliente.setTipo(TipoCliente.PRATA);

      Produto produto = new Produto();
      produto.setPeso(5); // Peso do produto
      produto.setPreco(BigDecimal.valueOf(125)); // Preço do produto (125 * 4 = 500)

      ItemCompra item = new ItemCompra();
      item.setProduto(produto);
      item.setQuantidade(4L); // Quantidade de itens (125 * 4 = 500)

    
      List<ItemCompra> itens = Arrays.asList(item);

      CarrinhoDeCompras carrinho = new CarrinhoDeCompras(
          1L, 
          cliente, 
          itens, 
          LocalDate.now()
      );
      // Cálculo do custo total sem desconto (custoItens = 500)
      // 4 produtos de 125 = 500
      // Nenhum desconto aplicado
      // 500 - 0 = 500
      BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho).setScale(1, RoundingMode.HALF_UP);
      // Peso total = 4 produtos de 5kg = 20kg
      // 20kg * R$ 4,00 = R$ 80,00
      // Frete com desconto de 50% (cliente PRATA): 80 * 0.5 = 40,00
      BigDecimal esperado = BigDecimal.valueOf(125 * 4).subtract(BigDecimal.valueOf(125 * 4).multiply(BigDecimal.valueOf(0)))
              .add(BigDecimal.valueOf(20 * 4).multiply(BigDecimal.valueOf(0.5))).setScale(1, RoundingMode.HALF_UP);

      // Verificando se o custo total calculado é igual ao esperado
      assertEquals(esperado, custoTotal);


  }


  @Test
  void calcularCustoTotal_clientePrata_comDescontoFrete_semDescontoItens_pesoTotal5kg() {
      Cliente cliente = new Cliente();
      cliente.setTipo(TipoCliente.PRATA);

      Produto produto = new Produto();
      produto.setPeso(5); // Peso unitário do produto (5kg)
      produto.setPreco(BigDecimal.valueOf(100)); // Preço do produto 

      ItemCompra item = new ItemCompra();
      item.setProduto(produto);
      item.setQuantidade(1L); // Quantidade de itens (5kg)

      List<ItemCompra> itens = Arrays.asList(item);
      CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1L, cliente, itens, LocalDate.now());
      // Cálculo do custo total com desconto de 50% no frete 
      BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho).setScale(1, RoundingMode.HALF_UP);
      // Cálculo esperado:
      // Preço dos itens: 100
      // Peso total: 5kg (frete gratuito)
      BigDecimal esperado = BigDecimal.valueOf(100).setScale(1, RoundingMode.HALF_UP);

      assertEquals(esperado, custoTotal);
  }

  @Test
  void finalizarCompra_disponibilidadeItens() {
      // Cliente
      Cliente cliente = new Cliente();
      cliente.setId(1L);
      cliente.setTipo(TipoCliente.BRONZE);

      // Produto e itens no carrinho
      Produto produto = new Produto();
      produto.setId(1L);
      produto.setPreco(BigDecimal.valueOf(50));
      produto.setPeso(2);

      ItemCompra item = new ItemCompra();
      item.setProduto(produto);
      item.setQuantidade(2L);

      CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1L, cliente, Arrays.asList(item), LocalDate.now());

      // Cria uma instância de DisponibilidadeDTO
      DisponibilidadeDTO disponibilidade = new DisponibilidadeDTO(false, Collections.emptyList());

      // Configurar mocks
      when(clienteService.buscarPorId(1L)).thenReturn(cliente);
      when(carrinhoService.buscarPorCarrinhoIdEClienteId(eq(1L), eq(cliente))).thenReturn(carrinho);
      when(estoqueExternal.verificarDisponibilidade(anyList(), anyList())).thenReturn(disponibilidade);

      // Tentar finalizar a compra 
      IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
          compraService.finalizarCompra(1L, 1L);
      });

      // Verificar exceção
      assertEquals("Itens fora de estoque.", exception.getMessage());
  }
  
  
  @Test
  void finalizarCompra_comErro() {
      // Cliente
      Cliente cliente = new Cliente();
      cliente.setId(1L);
      cliente.setTipo(TipoCliente.BRONZE);

      // Configurar produto e itens no carrinho
      Produto produto = new Produto();
      produto.setId(1L);
      produto.setPreco(BigDecimal.valueOf(50));
      produto.setPeso(2);

      ItemCompra item = new ItemCompra();
      item.setProduto(produto);
      item.setQuantidade(2L);

      CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1L, cliente, Arrays.asList(item), LocalDate.now());

      // Criar uma instância de DisponibilidadeDTO
      DisponibilidadeDTO disponibilidade = new DisponibilidadeDTO(true, Collections.emptyList());

      // Criar uma instância de PagamentoDTO 
      PagamentoDTO pagamento = new PagamentoDTO(true, 10L);

      // Criar uma instância de EstoqueBaixaDTO
      EstoqueBaixaDTO baixaDTO = new EstoqueBaixaDTO(false);

      // Configurar mocks
      when(clienteService.buscarPorId(1L)).thenReturn(cliente);
      when(carrinhoService.buscarPorCarrinhoIdEClienteId(eq(1L), eq(cliente))).thenReturn(carrinho);
      when(estoqueExternal.verificarDisponibilidade(anyList(), anyList())).thenReturn(disponibilidade);
      when(pagamentoExternal.autorizarPagamento(anyLong(), anyDouble())).thenReturn(pagamento);
      when(estoqueExternal.darBaixa(anyList(), anyList())).thenReturn(baixaDTO);

      // Tentar finalizar
      IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
          compraService.finalizarCompra(1L, 1L);
      });

      // Verificar a mensagem da exceção
      assertEquals("Erro ao dar baixa no estoque.", exception.getMessage());
  }
  
  @Test
  void finalizarCompra_CancelaPagamento() {
      // Configurar cliente
      Cliente cliente = new Cliente();
      cliente.setId(1L); 
      cliente.setTipo(TipoCliente.BRONZE);

      // Configurar produto e itens no carrinho
      Produto produto = new Produto();
      produto.setId(1L);
      produto.setPreco(BigDecimal.valueOf(50));
      produto.setPeso(2);

      ItemCompra item = new ItemCompra();
      item.setProduto(produto);
      item.setQuantidade(2L);

      CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1L, cliente, Arrays.asList(item), LocalDate.now());

      // Criar uma instância de DisponibilidadeDTO
      DisponibilidadeDTO disponibilidade = new DisponibilidadeDTO(true, Collections.emptyList());

      // Criar uma instância de PagamentoDTO 
      PagamentoDTO pagamento = new PagamentoDTO(true, 10L);

      // Criar uma instância que simula erro ao dar baixa
      EstoqueBaixaDTO baixaDTO = new EstoqueBaixaDTO(false);

      // Configurar mocks
      when(clienteService.buscarPorId(1L)).thenReturn(cliente);
      when(carrinhoService.buscarPorCarrinhoIdEClienteId(eq(1L), eq(cliente))).thenReturn(carrinho);
      when(estoqueExternal.verificarDisponibilidade(anyList(), anyList())).thenReturn(disponibilidade);
      when(pagamentoExternal.autorizarPagamento(anyLong(), anyDouble())).thenReturn(pagamento);
      when(estoqueExternal.darBaixa(anyList(), anyList())).thenReturn(baixaDTO);

      // Tenta Finalizar
      IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
          compraService.finalizarCompra(1L, 1L);
      });

      // Verificar  exceção
      assertEquals("Erro ao dar baixa no estoque.", exception.getMessage());

      // Verificar se o método cancelarPagamento foi chamado com o ID do cliente e o ID da transação
      verify(pagamentoExternal, times(1)).cancelarPagamento(eq(cliente.getId()), eq(pagamento.transacaoId()));
  }
  
  
  @Test
  void finalizarCompra_compraFinalizadaSucesso() {
      // Configurar cliente
      Cliente cliente = new Cliente();
      cliente.setId(1L);
      cliente.setTipo(TipoCliente.BRONZE);

      // Configurar produto e itens no carrinho
      Produto produto = new Produto();
      produto.setId(1L);
      produto.setPreco(BigDecimal.valueOf(50));
      produto.setPeso(2);

      ItemCompra item = new ItemCompra();
      item.setProduto(produto);
      item.setQuantidade(2L);

      CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1L, cliente, Arrays.asList(item), LocalDate.now());

      // Criar uma instância de DisponibilidadeDTO 
      DisponibilidadeDTO disponibilidade = new DisponibilidadeDTO(true, Collections.emptyList());

      // Criar uma instância de PagamentoDTO v
      PagamentoDTO pagamento = new PagamentoDTO(true, 10L);

      // Criar uma instância de EstoqueBaixaDTO 
      EstoqueBaixaDTO baixaDTO = new EstoqueBaixaDTO(true);

      // Configurar mocks
      when(clienteService.buscarPorId(1L)).thenReturn(cliente);
      when(carrinhoService.buscarPorCarrinhoIdEClienteId(eq(1L), eq(cliente))).thenReturn(carrinho);
      when(estoqueExternal.verificarDisponibilidade(anyList(), anyList())).thenReturn(disponibilidade);
      when(pagamentoExternal.autorizarPagamento(anyLong(), anyDouble())).thenReturn(pagamento);
      when(estoqueExternal.darBaixa(anyList(), anyList())).thenReturn(baixaDTO);

      // Executar o método
      CompraDTO resultado = compraService.finalizarCompra(1L, 1L);

      // Verificar que o CompraDTO foi retornado 
      assertTrue(resultado.sucesso());
      assertEquals("Compra finalizada com sucesso.", resultado.mensagem());
      assertEquals(10L, resultado.transacaoPagamentoId()); 
  }
  
  
  @Test
  void finalizarCompra_pagamentoNaoAutorizado() {
      // Configurar cliente
      Cliente cliente = new Cliente();
      cliente.setId(1L);
      cliente.setTipo(TipoCliente.BRONZE);

      // Configurar produto e itens no carrinho
      Produto produto = new Produto();
      produto.setId(1L);
      produto.setPreco(BigDecimal.valueOf(50));
      produto.setPeso(2);

      ItemCompra item = new ItemCompra();
      item.setProduto(produto);
      item.setQuantidade(2L);

      CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1L, cliente, Arrays.asList(item), LocalDate.now());

      // Criar uma instância de DisponibilidadeDTO
      DisponibilidadeDTO disponibilidade = new DisponibilidadeDTO(true, Collections.emptyList());

      // Criar uma instância de PagamentoDTO válida, mas com pagamento não autorizado
      PagamentoDTO pagamento = new PagamentoDTO(false, 10L);

      // Criar uma instância de EstoqueBaixaDTO válida
      EstoqueBaixaDTO baixaDTO = new EstoqueBaixaDTO(true);

      // Configurar mocks
      when(clienteService.buscarPorId(1L)).thenReturn(cliente);
      when(carrinhoService.buscarPorCarrinhoIdEClienteId(eq(1L), eq(cliente))).thenReturn(carrinho);
      when(estoqueExternal.verificarDisponibilidade(anyList(), anyList())).thenReturn(disponibilidade);
      when(pagamentoExternal.autorizarPagamento(anyLong(), anyDouble())).thenReturn(pagamento);
      when(estoqueExternal.darBaixa(anyList(), anyList())).thenReturn(baixaDTO);

      // Tentar finalizar
      Exception exception = assertThrows(IllegalStateException.class, () -> {
          compraService.finalizarCompra(1L, 1L);
      });

      // Verificar exceção
      assertEquals("Pagamento não autorizado.", exception.getMessage());
  }
  
  
  @Test
  void finalizarCompra_autorizadoPagamento() {
      // Configurar cliente
      Cliente cliente = new Cliente();
      cliente.setId(1L);
      cliente.setTipo(TipoCliente.BRONZE);

      // Configurar produto e itens no carrinho
      Produto produto = new Produto();
      produto.setId(1L);
      produto.setPreco(BigDecimal.valueOf(100));
      produto.setPeso(5);

      ItemCompra item = new ItemCompra();
      item.setProduto(produto);
      item.setQuantidade(2L);

      CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1L, cliente, Arrays.asList(item), LocalDate.now());

      // Criar uma instância de DisponibilidadeDTO
      DisponibilidadeDTO disponibilidade = new DisponibilidadeDTO(true, Collections.emptyList());

      // Criar uma instância de PagamentoDTO
      PagamentoDTO pagamento = new PagamentoDTO(true, 120L);

      // Criar uma instância de EstoqueBaixaDTO 
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
      verify(pagamentoExternal, times(1)).autorizarPagamento(eq(1L), eq(220.0d)); // Atualize para 220.0d se esse for o valor correto
      verify(estoqueExternal, times(1)).darBaixa(anyList(), anyList());

      assertTrue(resultado.sucesso());
      
      // Verificar exceção
      assertEquals("Compra finalizada com sucesso.", resultado.mensagem());
  }

}
//teste
