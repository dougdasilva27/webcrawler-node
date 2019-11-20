package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.Arrays;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilNossosbichospetCrawler extends CrawlerRankingKeywords {
  
  private static final String HOME_PAGE = "https://www.nossosbichospet.com.br";

  public BrasilNossosbichospetCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 48;
    this.log("Página " + this.currentPage);

    String url = HOME_PAGE + "/buscapagina?ft=" + this.keywordEncoded + 
        "&PS=48&sl=ef3fcb99-de72-4251-aa57-71fe5b6e149f&cc=4&sm=0&PageNumber=" + this.currentPage;
    
    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    
    Elements products = this.currentDoc.select("ul > li:not(.helperComplement)");
    Elements helper = this.currentDoc.select("ul > li.helperComplement");

    if (!products.isEmpty()) {
      for(int i = 0; i < products.size() && i < helper.size(); i++) {
        Element prod = products.get(i);
        Element help = helper.get(i);
        
        String internalPid = scrapInternalPid(help);
        String productUrl = CrawlerUtils.scrapUrl(prod, "a.productImage", Arrays.asList("href"), "https", HOME_PAGE);

        saveDataProduct(null, internalPid, productUrl);
        
        this.log(
            "Position: " + this.position + 
            " - InternalId: " + null +
            " - InternalPid: " + internalPid + 
            " - Url: " + productUrl);
        
        if (this.arrayProducts.size() == productsLimit)
          break;
      }
      
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
        + this.arrayProducts.size() + " produtos crawleados");
  }
  
  private String scrapInternalPid(Element doc) {
    String internalPid = null;
    
    if(doc != null && doc.id().contains("helperComplement_")) {
      internalPid = doc.id().replace("helperComplement_", "").trim();
    }
    
    return internalPid;        
  }

  @Override
  protected boolean hasNextPage() {
    Integer productCount = this.currentDoc.select("ul > li:not(.helperComplement)").size();
    
    return productCount >= this.pageSize;
  }
}
