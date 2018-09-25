package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilAmoedoCrawler extends CrawlerRankingKeywords {
  
  public BrasilAmoedoCrawler(Session session) {
    super(session);
  }
  
  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 32;
    
    this.log("Página " + this.currentPage);
    
    String keyword = this.keywordWithoutAccents.replaceAll(" ", "%20");
    
    // monta a url com a keyword e a página
    String url = "https://www.amoedo.com.br/catalogsearch/result/?q=" + keyword + "&p=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);
    
    // chama função de pegar a url
    this.currentDoc = fetchDocument(url);
    
    Elements products = this.currentDoc.select(".products-grid .item");
    // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
    if (!products.isEmpty()) {
      // se o total de busca não foi setado ainda, chama a função para setar
      if (this.totalProducts == 0) {
        setTotalProducts();
      }
      
      for (Element e : products) {
        // InternalPid
        String internalPid = crawlInternalPid(e);
        
        // Url do produto
        String urlProduct = crawlProductUrl(e);
        
        saveDataProduct(null, internalPid, urlProduct);
        
        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
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
  protected boolean hasNextPage() {
    return this.currentDoc.select(".next.i-next").first() != null;
  }
  
  @Override
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.select(".amount").first();
    
    if (totalElement != null) {
      String token = totalElement.ownText().replaceAll("[^0-9]", "").trim();
      
      if (!token.isEmpty()) {
        this.totalProducts = Integer.parseInt(token);
        this.log("Total da busca: " + this.totalProducts);
      }
    }
  }
  
  private String crawlInternalPid(Element e) {
    String internalPid = null;
    Element pid = e.select(".codSku").first();
    
    if (pid != null) {
      String pidCandidate = pid.ownText().trim();
      
      if (pidCandidate.contains(":")) {
        internalPid = pidCandidate.split(":")[1].trim();
      } else {
        internalPid = pidCandidate;
      }
    }
    
    return internalPid;
  }
  
  private String crawlProductUrl(Element e) {
    String urlProduct = null;
    Element urlElement = e.select("> a").first();
    
    if (urlElement != null) {
      urlProduct = urlElement.attr("href");
    }
    
    return urlProduct;
  }
}
