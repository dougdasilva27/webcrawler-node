package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.Arrays;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilCatdogshopCrawler extends CrawlerRankingKeywords {
  
  private static final String HOME_PAGE = "catdogshop.com.br/";

  public BrasilCatdogshopCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 12;
    this.log("Página " + this.currentPage);

    String url = "https://" + HOME_PAGE + 
        "search/page/" + this.currentPage + 
        "/?q=" + this.keywordEncoded;
    
    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".product-table .item");

    if (!products.isEmpty()) {

      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String productUrl = CrawlerUtils.scrapUrl(e, "a", Arrays.asList("href"), "https:", HOME_PAGE);

        saveDataProduct(null, null, productUrl);

        this.log(
            "Position: " + this.position + 
            " - InternalId: " + null +
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
  protected boolean hasNextPage() {
    return this.currentDoc.selectFirst(".pagination-arrow-link .pagination-arrow-next") != null;
  }
}
