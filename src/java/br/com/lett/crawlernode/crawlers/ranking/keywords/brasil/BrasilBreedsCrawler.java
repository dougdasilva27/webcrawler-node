package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.Arrays;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilBreedsCrawler extends CrawlerRankingKeywords {
  
  private static final String HOME_PAGE = "https://www.breeds.com.br/";

  public BrasilBreedsCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 21;
    this.log("Página " + this.currentPage);

    String url = "https://busca.breeds.com.br/" 
        + this.keywordEncoded + "?pagina=" 
        + this.currentPage;
    
    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select("#listProduct > li > div");

    if (!products.isEmpty()) {

      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, null, "data-item-id");
        String productUrl = CrawlerUtils.scrapUrl(e, ".product-image > a", Arrays.asList("href"), "https:", HOME_PAGE);

        saveDataProduct(null, internalPid, productUrl);

        this.log(
            "Position: " + this.position + 
            " - InternalId: " + null +
            " - InternalPid: " + internalPid + 
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
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(
        this.currentDoc, 
        ".filter-details .list-results", 
        "de", 
        "", 
        true, false, 0);

    this.log("Total de produtos: " + this.totalProducts);
  }
}
