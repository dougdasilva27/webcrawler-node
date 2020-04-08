package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilAnimalshowstoreCrawler extends CrawlerRankingKeywords {
  
  private static final String HOME_PAGE = "www.animalshowstore.com.br";

  public BrasilAnimalshowstoreCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 13;
    this.log("Página " + this.currentPage);

    String url = "https://www.animalshowstore.com.br/loja/busca.php?palavra_busca=" + this.keywordEncoded + "&pg=" + this.currentPage;

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".item.flex");

    if (!products.isEmpty()) {
      if(this.totalProducts == 0) {
        setTotalProducts();
      }
      
      for (Element e : products) {
        String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-box", "data-id");
        String productUrl = CrawlerUtils.scrapUrl(e, ".action-product > a", "href", "https", HOME_PAGE);

        saveDataProduct(internalId, null, productUrl);

        this.log(
            "Position: " + this.position + 
            " - InternalId: " + internalId +
            " - InternalPid: " + null + 
            " - Url: " + productUrl);
        
        if (this.arrayProducts.size() == productsLimit)
          break;
      }
      
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
        + this.arrayProducts.size() + " produtos crawleados");

  }
  
  @Override
  protected void setTotalProducts() {    
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".search-counter", "Resultado", "produto", true, false, 0);
    
    this.log("Total products: " + this.totalProducts);
  }
}
