package br.com.lett.crawlernode.crawlers.ranking.keywords.portoalegre;

import java.util.Arrays;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class PortoalegreBichopetstoreCrawler extends CrawlerRankingKeywords {
  
  private static final String HOME_PAGE = "https://www.bichopetstore.com.br/";

  public PortoalegreBichopetstoreCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 15;
    this.log("Página " + this.currentPage);

    String url = HOME_PAGE + "index.php?route=product/search&search=" 
        + this.keywordEncoded + "&limit=100&page=" + this.currentPage;
    
    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".product-layout");

    if (!products.isEmpty()) {

      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalPid = crawlInternalPid(e);
        String productUrl = CrawlerUtils.scrapUrl(e, ".image > a", Arrays.asList("href"), "https:", HOME_PAGE);

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


  private String crawlInternalPid(Element e) {
    String internalPid = null;
    Element internalPidElement = e.selectFirst(".btn");
    
    if(internalPidElement != null && internalPidElement.hasAttr("onclick")) {
      String onclick = internalPidElement.attr("onclick");
      
      internalPid = onclick.replace("cart.add('", "").replace("');", "");
      if(internalPid.isEmpty()) internalPid = null;
    }

    return internalPid;
  }

  @Override
  protected void setTotalProducts() {
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(
        this.currentDoc, 
        "#content > div > div.col-sm-6.text-right", 
        "total", 
        "(", 
        true, true, 0);

    this.log("Total de produtos: " + this.totalProducts);
  }
}
