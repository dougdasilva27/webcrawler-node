package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.ArrayList;
import java.util.List;
import org.apache.http.cookie.Cookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilEpocacosmeticosCrawler extends CrawlerRankingKeywords {
  
  public BrasilEpocacosmeticosCrawler(Session session) {
    super(session);
  }
  
  private List<Cookie> cookies = new ArrayList<>();
  
  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 24;
    this.log("Página " + this.currentPage);
    
    String url = "https://busca.epocacosmeticos.com.br/busca?q=" + this.keywordEncoded + "&page=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);
    
    this.currentDoc = fetchDocument(url, cookies);
    
    Elements products = this.currentDoc.select(".nm-product-item");
    
    if (!products.isEmpty()) {
      
      if (this.totalProducts == 0) {
        setTotalProducts();
      }
      
      for (Element e : products) {
        // InternalPid
        String internalPid = crawlInternalPid(e);
        
        // InternalId
        String internalId = crawlInternalId(e);
        
        // Url do produto
        String productUrl = crawlProductUrl(e);
        
        saveDataProduct(internalId, internalPid, productUrl);
        
        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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
  protected boolean hasNextPage() {
    if (this.arrayProducts.size() < this.totalProducts) {
      return true;
    }
    
    return false;
  }
  
  @Override
  protected void setTotalProducts() {
    
    Element totalProducts = this.currentDoc.selectFirst(".neemu-total-products-container");
    
    if (totalProducts != null) {
      String text = totalProducts.text().replaceAll("[^0-9]", "").trim();
      
      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
      }
      
      this.log("Total da busca: " + this.totalProducts);
    }
  }
  
  private String crawlInternalId(Element e) {
    return null;
  }
  
  private String crawlInternalPid(Element e) {
    String internalPid = null;
    Element pid = e.selectFirst(".nm-product-item");
    
    if (pid != null) {
      String[] tokens = pid.attr("id").split("nm-product-");
      internalPid = tokens[tokens.length - 1].trim().split(" ")[0].trim();
    }
    
    return internalPid;
  }
  
  
  private String crawlProductUrl(Element e) {
    String productUrl = null;
    Element eUrl = e.select(".nm-product-name a").first();
    
    if (eUrl != null) {
      productUrl = eUrl.attr("href").trim();
      
      if (!productUrl.startsWith("http")) {
        productUrl = "https:" + productUrl;
      }
    }
    
    return productUrl;
  }
  
  
}
