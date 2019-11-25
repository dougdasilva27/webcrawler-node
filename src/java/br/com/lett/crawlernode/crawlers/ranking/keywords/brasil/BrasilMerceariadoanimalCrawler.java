package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.Arrays;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilMerceariadoanimalCrawler extends CrawlerRankingKeywords {
  
  private static final String HOME_PAGE = "merceariadoanimal.com.br";

  public BrasilMerceariadoanimalCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 18;
    this.log("Página " + this.currentPage);

    String url = "https://www.merceariadoanimal.com.br/" 
        + this.keywordEncoded + "/?page=" + this.currentPage;
    
    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".ProductList  > li > .ProductImage");

    if (!products.isEmpty()) {
      for (Element e : products) {
        String internalPid = e.hasAttr("data-product") ? e.attr("data-product") : null;
        String productUrl = CrawlerUtils.scrapUrl(e, "a", Arrays.asList("href"), "https:", HOME_PAGE);

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
  protected boolean hasNextPage() {
    return this.currentDoc.selectFirst(".CategoryPagination .FloatRight > a") != null;
  }
}
