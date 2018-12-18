package br.com.lett.crawlernode.crawlers.ranking.keywords.belohorizonte;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BelohorizonteBhvidaCrawler  extends CrawlerRankingKeywords {

  public BelohorizonteBhvidaCrawler(Session session) {
    super(session);
    // TODO Auto-generated constructor stub
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
        String internalId  = crawlInternalId(e);
        String productUrl  = crawlProductUrl(e);
        String internalPid = crawlInternalPid(e);
        saveDataProduct(internalId, internalPid, productUrl);
        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
        if(this.arrayProducts.size() == productsLimit) break;
        
      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }
    
  }

  private String crawlProductUrl(Element e) {
    return e != null ? "https://www.bhvida.com/" + e.selectFirst("li[rel]").attr("rel"): null;
  }

  private String crawlInternalId(Element doc) {
    String internalId = null;

    Element serchedId = doc.selectFirst("#frmcarrinho #codigo");
    if(serchedId != null) {
      internalId = serchedId.val().trim();
    }

    return internalId;
  } 
  
  private String crawlInternalPid(Element e) {
    String internalPid = null;

    Element serchedId = e.selectFirst("#detalhes-mini > ul > li:last-child");
    if(serchedId != null) {
      internalPid = serchedId.ownText();
    }

    return internalPid;
  }
  
  private String getIdFromUrl (Element e) {
    return null;
  }
  
}
