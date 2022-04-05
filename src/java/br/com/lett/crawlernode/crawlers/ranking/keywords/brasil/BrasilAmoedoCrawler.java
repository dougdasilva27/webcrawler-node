package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilAmoedoCrawler extends CrawlerRankingKeywords {
  
  public BrasilAmoedoCrawler(Session session) {
    super(session);
  }
  
  @Override
  protected void extractProductsFromCurrentPage() throws MalformedProductException {
    this.pageSize = 24;
    
    this.log("Página " + this.currentPage);
    
    String keyword = this.keywordWithoutAccents.replaceAll(" ", "%20");
    
    String url = "https://www.amoedo.com.br/catalogsearch/result/?q=" + keyword + "&p=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);
    
    this.currentDoc = fetchDocument(url);
    
    Elements products = this.currentDoc.select(".products.list.items .product-item");

    // Se existir uma mensagem de "Não foram encontrados resultados exatos para a busca", devemos desconsiderar a lista de produtos retornada.
    boolean noExactResultsMessage = this.currentDoc.selectFirst(".search.results > .message.notice") != null;
    
    if (!products.isEmpty() && !noExactResultsMessage) {

      if (this.totalProducts == 0) {
        setTotalProducts();
      }
      
      for (Element e : products) {
        String internalId = crawlInternalId(e);
        
        String urlProduct = crawlProductUrl(e);
        String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product-item-link", true);
        Integer price = crawlPrice(e, 0);
        String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".product-image-photo", Arrays.asList("src"), "https", "www.amoedo.com.br");
        boolean isAvailable = price != 0;

        RankingProduct productRanking = RankingProductBuilder.create()
            .setUrl(urlProduct)
            .setInternalId(internalId)
            .setInternalPid(null)
            .setImageUrl(imageUrl)
            .setName(name)
            .setPriceInCents(price)
            .setAvailability(isAvailable)
            .build();

         saveDataProduct(productRanking);
        
        if (this.arrayProducts.size() == productsLimit)
          break;
      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }
    
    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }
  
  @Override
  protected boolean hasNextPage() {
    return this.currentDoc.selectFirst(".pages-item-next") != null;
  }
  
  @Override
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.selectFirst(".toolbar-amount > span:last-child");
    
    if (totalElement != null) {
      String token = totalElement.ownText().replaceAll("[^0-9]", "").trim();
      
      if (!token.isEmpty()) {
        this.totalProducts = Integer.parseInt(token);
        this.log("Total da busca: " + this.totalProducts);
      }
    }
  }
  
  private String crawlInternalId(Element e) {
    String internalId = null;
  
    Element idElement = e.selectFirst(".price-box");
    
    if (idElement != null) {
      internalId = idElement.attr("data-product-id").trim();
    }
    
    return internalId;
  }
  
  private String crawlProductUrl(Element e) {
    String urlProduct = null;
    Element urlElement = e.selectFirst(".product-item-link");
    
    if (urlElement != null) {
      urlProduct = urlElement.attr("href");
    }
    
    return urlProduct;
  }

  private Integer crawlPrice(Element e, Integer value){
     String priceStr = CrawlerUtils.scrapStringSimpleInfo(e, "span.price", true);
     Integer priceInCents = value;

     if (priceStr != null) {
        final String regex = "[0-9]+\\,[0-9]+";
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(priceStr);

        if (matcher.find()) {
           priceStr = matcher.group(0);
           priceInCents = CommonMethods.stringPriceToIntegerPrice(priceStr, ',', value);

        }
     }
     return priceInCents;
  }
  
}
