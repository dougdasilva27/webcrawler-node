package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.MathUtils;

public class SaopauloDicicoCrawler extends CrawlerRankingKeywords {
  
  public SaopauloDicicoCrawler(Session session) {
    super(session);
  }
  
  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 20;
    
    this.log("Página " + this.currentPage);
    String url = "http://www.dicico.com.br/dicico-br/search/?No=" + this.arrayProducts.size() + "&Nrpp=" + this.pageSize + "&Ntt="
        + this.keywordWithoutAccents.replace(" ", "%20");
    this.log("Link onde são feitos os crawlers: " + url);
    
    this.currentDoc = fetchDocument(url);
    
    Elements products = this.currentDoc.select("div.info-box");
    
    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }
      for (Element e : products) {
        
        String internalPid = crawlInternalPid(e);
        String productUrl = crawlProductUrl(e);
        
        saveDataProduct(null, internalPid, productUrl);
        
        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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
    Element totalElement = this.currentDoc.selectFirst(".active > a > span.pl-5");
    
    if (totalElement != null) {
      String total = totalElement.ownText().replaceAll("[^0-9]", "").trim();
      
      if (!total.isEmpty()) {
        this.totalProducts = Integer.parseInt(total);
      }
    }
    
    this.log("Total da busca: " + this.totalProducts);
  }
  
  private String crawlInternalPid(Element e) {
    String internalPid = null;
    
    Element text = e.selectFirst(".sku");
    if (text != null) {
      internalPid = MathUtils.parseInt((text.ownText())).toString();
    }
    return internalPid;
  }
  
  private String crawlProductUrl(Element e) {
    String productUrl = null;
    Element urlElement = e.select(".jq-name > a").first();
    
    if (urlElement != null) {
      productUrl = urlElement.attr("href");
      
      if (!productUrl.contains("dicico.com.br")) {
        productUrl = "https://www.dicico.com.br" + productUrl.replace(".com//", ".com/");
      }
    }
    return productUrl;
  }
  
}
