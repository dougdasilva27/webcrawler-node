package br.com.lett.crawlernode.crawlers.ranking.keywords.curitiba;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class CuritibaLefarmaCrawler extends CrawlerRankingKeywords {

  public CuritibaLefarmaCrawler(Session session) {
    super(session);
    // TODO Auto-generated constructor stub
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 24;  

    this.log("Página "+ this.currentPage);
    
    String url = "https://www.lefarma.com.br/resultadopesquisa?pag="+ this.currentPage + "&departamento=&buscarpor=" + this.keywordEncoded + "&smart=0";
    
    this.log("Link onde são feitos os crawlers: " + url); 
    
    this.currentDoc = fetchDocument(url);      
    Elements elements = this.currentDoc.select(".vitrine_geral #scroller li");
    if(elements.size() > 0) {    
      
      if(totalProducts == 0) {
        this.totalProducts = elements.size() > 0 ? elements.size(): 0;
        this.log("Total: " + this.totalProducts);
      }
      
      if(elements != null) {
        for (Element e: elements) {
          
          String internalPid = crawlInternalPid(e);
          String productUrl = crawlProductUrl(e);
          
          saveDataProduct(null, internalPid, productUrl);
          
          this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
        }
      }
    }
  }

  private String crawlProductUrl(Element e) {
    String url = null;
    String text = e.selectFirst(".titulo a").attr("href").trim();
    
    if(text != null) {
      text = text.substring(text.indexOf("=") + 2, text.lastIndexOf("'"));
      url = text;
    }
    
    return url;
  }


  private String crawlInternalPid(Element e) {
    String internalPid = null;
    String text = e.selectFirst(".collection-product-code a").text().trim();
 
    if(text != null) {
      internalPid = text;
    }

    return internalPid;
  }
  
  @Override
  protected boolean hasNextPage() {
    return this.currentDoc.select(".prox-page") != null;
  }
}
