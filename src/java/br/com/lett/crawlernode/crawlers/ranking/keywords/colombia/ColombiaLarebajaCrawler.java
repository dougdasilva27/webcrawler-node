package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class ColombiaLarebajaCrawler extends CrawlerRankingKeywords {

  public ColombiaLarebajaCrawler(Session session) {
    super(session);
    // TODO Auto-generated constructor stub
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 24;  

    this.log("Página "+ this.currentPage);
    
    String url = "https://www.larebajavirtual.com/catalogo/buscar?busqueda=" + this.keywordEncoded;
    this.log("Link onde são feitos os crawlers: "+url); 
    
    this.currentDoc = fetchDocument(url);      
        
    Elements products =  this.currentDoc.select(".listaProductos li");
    
    if(!products.isEmpty()) {          
        if(this.totalProducts == 0) setTotalProducts();
        
        for(Element e : products) {     
            String internalId = crawlInternalId(e);
            String productUrl = crawlProductUrl(e);
            saveDataProduct(internalId, null, productUrl);
            
            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
            if(this.arrayProducts.size() == productsLimit) break;            
        }
    } else {
        this.result = false;
        this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");  
  }
  

  private String crawlProductUrl(Element e) {    
    return e != null ? "https://www.larebajavirtual.com/" + e.selectFirst(".content_product a").attr("href"): null;
  }
  
  
  private String crawlInternalId(Element e) {
    String internalId = null;
    Element dataProducto = e.selectFirst("input[data-producto]");
    
    if(dataProducto != null) {
      internalId = dataProducto.attr("data-producto").trim();      
    }
    
    return internalId;
  }
  
}
