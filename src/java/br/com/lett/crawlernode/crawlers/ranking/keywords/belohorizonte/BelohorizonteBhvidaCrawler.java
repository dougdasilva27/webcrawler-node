package br.com.lett.crawlernode.crawlers.ranking.keywords.belohorizonte;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BelohorizonteBhvidaCrawler  extends CrawlerRankingKeywords {

  public BelohorizonteBhvidaCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 36;
    
    this.log("Página "+ this.currentPage);
    
    String url = "https://www.bhvida.com/produto.php?LISTA=procurar&PROCURAR="+ this.keywordEncoded +"&pg=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: "+url); 
    
    this.currentDoc = fetchDocument(url);      

    Elements products =  this.currentDoc.select("#produtos li");
    if(products.size() >= 1) {          
      if(this.totalProducts == 0) setTotalProducts();
      for (Element e : products) {
        String productUrl  = crawlUrl(e);
        String internalPid = crawlInternalPid(productUrl);
        saveDataProduct(null, internalPid, productUrl);
        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
        if(this.arrayProducts.size() == productsLimit) break;
        
      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }
    
  }
 
  private String crawlUrl(Element e) {
    String productUrl = CrawlerUtils.completeUrl(e.attr("rel"), "https:", "www.bhvida.com/");    
    String newPid = CommonMethods.getLast(e.selectFirst(".moldura-p img").attr("src").split("="));
    String oldPid = getIdFromUrl(productUrl);
  
    productUrl = productUrl.replace(oldPid, newPid);
        
    return productUrl;
  }
  
  
  private String crawlInternalPid(String url) {
    return getIdFromUrl(url);
  }

  private String getIdFromUrl (String url) {
    String id = null;    
    String lastPart = CommonMethods.getLast(url.split("\\?")[0].split("-"));    
    
    if(lastPart.contains(".")) {
      id = lastPart.split("\\.")[0];
    } else {
      id = lastPart;
    }
    return id;
  }
  
}
