package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.Arrays;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilOnagapetshopCrawler  extends CrawlerRankingKeywords {
  
  private static final String HOME_PAGE = "onagapetshop.com.br";

  public BrasilOnagapetshopCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 0;
    this.log("Página " + this.currentPage);

    String url = "https://" + HOME_PAGE + "/search/page/" + this.currentPage + "/?q=" + this.keywordEncoded;    

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".thumbnail .image-wrap > div > a");

    if (!products.isEmpty()) {
      for (Element e : products) {
        String productUrl = CrawlerUtils.scrapUrl(e, null, Arrays.asList("href"), "https:", HOME_PAGE);

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
    return this.currentDoc.selectFirst(".last-page") == null;
  }
}
