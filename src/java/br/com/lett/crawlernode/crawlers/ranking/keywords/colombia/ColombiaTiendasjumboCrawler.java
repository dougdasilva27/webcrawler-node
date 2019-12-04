package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import java.util.Arrays;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class ColombiaTiendasjumboCrawler extends CrawlerRankingKeywords {
  
  private static final String HOME_PAGE = "https://www.tiendasjumbo.co";

  public ColombiaTiendasjumboCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 24;
    this.log("Página " + this.currentPage);

    String url = HOME_PAGE + "/busca?q=" + this.keywordEncoded + "&page=" + this.currentPage;
    
    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    
    Elements products = this.currentDoc.select("ul > li:not(.helperComplement)");
    Elements helper = this.currentDoc.select("ul > li.helperComplement");

    if (!products.isEmpty()) {
      for(int i = 0; i < products.size() && i < helper.size(); i++) {
        Element prod = products.get(i);
        Element help = helper.get(i);
        
        String internalPid = scrapInternalPid(help);
        String productUrl = CrawlerUtils.scrapUrl(prod, ".nm-product-item .nm-product-img-container a.nm-product-img-link", Arrays.asList("href"), "https", HOME_PAGE);

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
    Integer productCount = this.currentDoc.select(".nm-main-search-container ul.neemu-products-container.nm-view-type-grid > li").size();
    
    return productCount >= this.pageSize;
  }
}
