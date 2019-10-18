package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.net.URI;
import java.util.Arrays;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilCasadofazendeiroCrawler extends CrawlerRankingKeywords {
  
  private static final String HOME_PAGE = "casafazendeiro.com.br";

  public BrasilCasadofazendeiroCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 15;
    this.log("Página " + this.currentPage);

    String url = "https://" + HOME_PAGE + "/index.php?route=product/search&search=" + 
        this.keywordEncoded + "&page=" +  this.currentPage;
    
    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".product-layout");

    if (!products.isEmpty()) {

      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String productUrl = CrawlerUtils.scrapUrl(e, ".product-info-title a", Arrays.asList("href"), "https:", HOME_PAGE);
        String internalId = crawlInternalId(productUrl);
            
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


  private String crawlInternalId(String url) {
    String internalPid = null;

    URI uri = URI.create(url);
    String urlPath = uri.getPath();
    internalPid = CommonMethods.getLast(urlPath.split("-"));

    return internalPid;
  }

  @Override
  protected void setTotalProducts() {
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(
        this.currentDoc, 
        "#content .row .text-right", 
        "total de", 
        "(", 
        true, true, 0);

    this.log("Total de produtos: " + this.totalProducts);
  }
}
