# README

## AUTORES
- GUSTAVO SOUSA BERNARDES
- JOSE VICTOR DO NASCIMENTO FERREIRA 
- VLADIMIR VIEIRA DO NASCIMENTO

## INTRODUÇÃO
  Este trabalho é um projeto backend de uma aplicação de e-commerce estruturada como uma API REST, organizada em três camadas: a primeira é denominada de Controller, cujo é responsável por lidar com as requisições HTTP; já a segunda é denominada Service, onde contém a lógica de negócio; enquanto que a terceira, denominada de Repository, é responsável pela interação com o banco de dados. Neste trabalho, o objetivo principal foi o de implementar testes automatizados para a funcionalidade de finalizar compra, que faz parte de um processo de checkout em um e-commerce.

## INSTRUÇÕES DE COMPILAÇÃO E EXECUÇÃO

### Configuração do ambiente
  Utilizando o terminal extraia todos os arquivos relacionados ao projeto que foi desenvolvido, para isso você deve digitar:
  
    unzip 'ecommerce-teste-software-main.zip'

### Certifique-se que o Java 11 ou superior esteja instalado em sua máquina
    
    java -version

### Instalar o Maven
  O Maven, atualmente, é conhecido como uma ferramenta de automação de construção e gerenciamento de dependências.

  Diante disso, vamos verificar se o Maven está instalado no seu sistema. Para isso, abra o terminal ou prompt de comando e digite:

    mvn -v

  Se o Maven não estiver instalado, você pode seguir as instruções de instalação presentes no site oficial do Maven:

    https://maven.apache.org/install.html

### COMO EXECUTAR O PROJETO

  Execute, na raiz do projeto, o seguinte comando para rodar os testes de mutação:

    mvn clean test pitest:mutationCoverage

  Note que:
  
  - Por meio do clean o Maven vai limpar o diretório de build.
    
  - Por utilizar test, os testes serão executados.
    
  - Por meio de pitest:mutationCoverage  mutantes do código devem ser gerados e diante disso vamos poder verificar se eles foram mortos pelos testes.

### Relatório dos mutantes obtidos

  Com a execução da etapa anterior, o PIT constroi um relatório sobre mutantes e os testes. O relatório pode ser encontrado por meio do seguinte caminho target/pit-reports. Dentro desta pasta você encontrará um arquivo denominado de index.html.

  No index.html você poderá verificar:

- Cobertura das linhas de código(Obtivemos 100%), 

- Cobertura de mutantes(Obtivemos 97%)

- Teste de força(Obtivemos 97%)

### Cobertura de mutantes

Note que obtivemos uma cobertura de mutação de 97%, e isto permite sugerir que 97% dos mutantes gerados foram “mortos” pelos testes.

Possíveis Razões:

  Mutantes Específicos Não Mortos: Alguns mutantes sobreviveram devido existirem casos de teste específicos que não cobriram todas as possibilidades ou caminhos do código. Por exemplo, para produtosIds.add(item.getProduto().getId()), a mutação sobreviveu pois a remoção de getId() não foi detectada pelos testes existentes. Assim como, a remoção da chamada de add() que não foi detectada pelos testes existentes; já para produtosQtds.add(item.getQuantidade()), note que a mutação sobreviveu porque os testes não validaram a modificação da lista produtosQtds ou não verificaram se o método add() foi executado. 
