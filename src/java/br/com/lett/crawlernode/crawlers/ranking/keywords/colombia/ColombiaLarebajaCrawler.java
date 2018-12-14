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
    
    //monta a url com a keyword e a página
    String url = "https://www.larebajavirtual.com/catalogo/buscar?busqueda=" + this.keywordEncoded;
    this.log("Link onde são feitos os crawlers: "+url); 
    
    this.currentDoc = fetchDocument(url);      
        
    Elements products =  this.currentDoc.select(".listaProductos li");
    
    //se obter 1 ou mais links de produtos e essa página tiver resultado faça:
    if(products.size() >= 1) {          
        //se o total de busca não foi setado ainda, chama a função para setar
        if(this.totalProducts == 0) setTotalProducts();
        for(Element e : products) {     
            // InternalId
            String internalId = crawlInternalId(e);
            
            // Url do produto
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

    if(e != null && e.selectFirst("input[data-producto]") != null) {
      internalId = e.selectFirst("input[data-producto]").attr("data-producto").trim();      
    }
    
    return internalId;
  }
  
}
