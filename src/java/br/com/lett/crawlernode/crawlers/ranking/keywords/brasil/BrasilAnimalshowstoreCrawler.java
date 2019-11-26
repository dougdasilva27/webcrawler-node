package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilAnimalshowstoreCrawler extends CrawlerRankingKeywords {
  
  private static final String HOME_PAGE = "www.animalshowstore.com.br";

  public BrasilAnimalshowstoreCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 21;
    this.log("Página " + this.currentPage);

    String url = "https://busca.animalshowstore.com.br/" + this.keywordEncoded + "?pagina=" + this.currentPage;
    
    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select("#listProduct > li");

    if (!products.isEmpty()) {
      if(this.totalProducts == 0) {
        setTotalProducts();
      }
      
      for (Element e : products) {
        String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "[data-item-id]", "data-item-id");
        String productUrl = CrawlerUtils.scrapUrl(e, "[itemprop=url]", "href", "http", HOME_PAGE);

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
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".list-results", "de", "", true, false, 0);
    
    this.log("Total products: " + this.totalProducts);
  }
}
