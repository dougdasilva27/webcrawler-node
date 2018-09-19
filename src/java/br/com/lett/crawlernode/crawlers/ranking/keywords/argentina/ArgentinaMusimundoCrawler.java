package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import java.util.ArrayList;
import java.util.List;
import org.apache.http.cookie.Cookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.MathUtils;

public class ArgentinaMusimundoCrawler extends CrawlerRankingKeywords {
  
  public ArgentinaMusimundoCrawler(Session session) {
    super(session);
  }
  
  private List<Cookie> cookies = new ArrayList<>();
  
  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 16;
    
    this.log("Página " + this.currentPage);
    String url = "https://www.musimundo.com/Busqueda?&search=" + this.keywordWithoutAccents.replace(" ", "%20") + "&page=" + this.currentPage
        + "&limitRows=16&typeGrid=grid";
    this.log("Link onde são feitos os crawlers: " + url);
    
    this.currentDoc = fetchDocument(url, cookies);
    Elements products = this.currentDoc.select(".products.grid.view > .product");
    
    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }
      
      for (Element e : products) {
        String productUrl = crawlProductUrl(e);
        String internalId = crawlInternalId(e);
        
        saveDataProduct(internalId, null, productUrl);
        
        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit) {
          break;
        }
        
      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }
    
    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }
  
  @Override
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.selectFirst(".results > h2");
    
    if (totalElement != null) {
      this.totalProducts = MathUtils.parseInt(totalElement.text());
      this.log("Total da busca: " + this.totalProducts);
    }
  }
  
  private String crawlInternalId(Element e) {
    String internalId = null;
    Element idElement = e.selectFirst(".actions > span > button.submit");
    
    if (idElement != null) {
      internalId = idElement.attr("data-product-id").trim();
    }
    
    return internalId;
  }
  
  private String crawlProductUrl(Element e) {
    String productUrl = null;
    
    Element urlElement = e.selectFirst("a");
    
    if (urlElement != null) {
      productUrl = urlElement.attr("href");
      
      if (!productUrl.contains("musimundo")) {
        productUrl = ("https://www.musimundo.com/" + urlElement.attr("href"));
      }
    }
    
    return productUrl;
  }
  
}
