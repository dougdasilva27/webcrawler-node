package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import java.util.ArrayList;
import java.util.List;
import org.apache.http.cookie.Cookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class ArgentinaFravegaCrawler extends CrawlerRankingKeywords {
  
  public ArgentinaFravegaCrawler(Session session) {
    super(session);
  }
  
  private List<Cookie> cookies = new ArrayList<>();
  
  @Override
  protected void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);
    
    this.pageSize = 24;
    
    String keyword = this.keywordWithoutAccents.replaceAll(" ", "%20");
    String url = "https://www.fravega.com/busca?ft=" + keyword + "#" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);
    
    this.currentDoc = fetchDocument(url, cookies);
    
    Elements products = this.currentDoc.select(".shelf-resultado > div > ul");
    
    if (products.size() >= 1) {
      if (this.totalProducts == 0)
        setTotalProducts();
      
      for (Element e : products) {
        String internalPid = crawlInternalPid(e);
        
        // String internalId = crawlInternalId(e);
        
        // String productUrl = crawlProductUrl(e);
        
        // saveDataProduct(internalId, internalPid, productUrl);
        
        // this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " +
        // internalPid + " - Url: " + productUrl);
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
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.selectFirst(".resultado-busca-numero > span.value");
    
    if (totalElement != null) {
      try {
        this.totalProducts = Integer.parseInt(totalElement.text().replaceAll("[^0-9]", "").trim());
      } catch (Exception e) {
        this.logError(CommonMethods.getStackTrace(e));
      }
      
      this.log("Total da busca: " + this.totalProducts);
    }
  }
  
  private String crawlInternalPid(Element e) {
    String internalPid = null;
    
    Element getInfo = e.selectFirst(".image > a[href]");
    
    if (getInfo != null) {
      // image has sku ID- Think about a better logic to get it
    }
    return internalPid;
  }
  
}
