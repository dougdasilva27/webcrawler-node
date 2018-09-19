package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilParceiropgCrawler extends CrawlerRankingKeywords {
  
  public BrasilParceiropgCrawler(Session session) {
    super(session);
  }
  
  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 12;
    
    this.log("Página " + this.currentPage);
    
    String url = "https://www.parceiropg.com.br/catalogsearch/result/index/?p=" + this.currentPage + "&q=" + this.keywordEncoded;
    this.log("Link onde são feitos os crawlers: " + url);
    
    this.currentDoc = fetchDocument(url);
    
    Elements products = this.currentDoc.select(".item.product.product-item");
    
    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }
      
      for (Element e : products) {
        String internalPid = null;
        
        String internalId = crawlInternalId(e);
        
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
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.selectFirst("#toolbar-amount > span:eq(2)");
    
    if (totalElement != null) {
      String text = totalElement.text();
      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
      }
      
      this.log("Total da busca: " + this.totalProducts);
    }
  }
  
  private String crawlInternalId(Element e) {
    String internalId = null;
    Element idElement = e.selectFirst(".product-item-info > a");
    
    if (idElement != null) {
      internalId = CommonMethods.getLast(idElement.attr("href").split("-"));
      if (internalId.contains(".")) {
        internalId = internalId.split("\\.")[0];
      }
    }
    
    return internalId;
  }
  
  private String crawlProductUrl(Element e) {
    String productUrl = null;
    Element url = e.selectFirst(".product-item-info > a");
    
    if (url != null) {
      productUrl = url.attr("href");
      
      if (!productUrl.startsWith("http")) {
        productUrl = "https://www.parceiropg.com.br" + productUrl;
      }
    }
    
    return productUrl;
  }
}
