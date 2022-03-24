Contexto
================================================================================
<!--
Explique o porque este pull-request foi aberto, qual problema ele resolve.
Esta explicação deve incluir informações suficientes para uma pessoa sem contexto poder entender.
-->

Informações
================================================================================

- MarketId:

- Urls
    - Produto Disponivel:
    - Produto Indisponivel:
    - Produto Promoção:

- Keywords
    - keywords simples:
    - keywords com caracteres especiais:
    - keywords com espaços:
    - keywords de categorias:

Qual foi a alteração?
================================================================================
<!-- Explique como e qual correção foi aplicada -->



Testes
================================================================================

### O que foi testado?

#### Core

- [ ] Está capturando todas as informações:
    - [ ] Imagem primária e secundárias
    - [ ] Descrição
    - [ ] Rating
    - [ ] Produto com desconto (sales)
    - [ ] Categoria
- [ ] Rodou com um produto Disponivel
- [ ] Rodou com um produto Indisponível
- [ ] Rodou com um produto Promoção
- [ ] Preço Formatado (vírgula, kg)
--------------------------------------------------------------------------------
#### Ranking
- [ ] Teste de keywords
  - [ ] Testado com keywords que contém espaços
  - [ ] Testado com keywords que contém caracteres especiais 
  - [ ] Testado com keywords de categoria 
- [ ] Está capturando paginação
- [ ] Ordem dos produtos (primeira e segunda página)
- [ ] Está capturando preço e disponibilidade corretamente (indisponível preço é `null`) 

--------------------------------------------------------------------------------
### DISCOVERY

- [ ]  Não tem mismatching (Ids correspondente nos modos Ranking e Core)



--------------------------------------------------------------------------------

Mergiu? Builda ai !!
================================================================================
## [jenkins](https://jenkins.lett.global/job/webcrawler-node/)


--------------------------------------------------------------------------------
> **Revisor**
> - Não entendeu? Pergunte!
> - Enxergou uma solução alternativa, comente! (_Pode ser uma boa oportunidade de compartilhar aprendizados_)
> - Encontrou problemas de código? Tente usar a sugestão via github.
