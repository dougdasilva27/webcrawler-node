package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilPontoanimalCrawler extends CrawlerRankingKeywords {
  
  private static final String HOME_PAGE = "www.pontoanimalpetshop.com.br";

  public BrasilPontoanimalCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 28;
    this.log("Página " + this.currentPage);

    String url = "https://www.pontoanimalpetshop.com.br/Busca/" + this.keywordEncoded + "/1/0/" + (this.currentPage-1) + ".html";
    
    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".row .col-lg-3 > a[href]:first-child");

    if (!products.isEmpty()) {      
      for (Element e : products) {
        String productUrl = CrawlerUtils.scrapUrl(e, null, "href", "https", HOME_PAGE);
        String internalId = scrapInternalId(productUrl);

        saveDataProduct(internalId, null, productUrl);

        this.log(
            "Position: " + this.position + 
            " - InternalId: " + internalId +
            " - InternalPid: " + null + 
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
  
  private String scrapInternalId(String url) {
    String internalId = null;  
    String[] urlSplit = url.split("/");
    
    for(int i = 0; i < urlSplit.length; i++) {
      if(urlSplit[i].equals("produto") && i+1 < urlSplit.length) {
        internalId = urlSplit[i+1];
        break;
      }
    }
    
    return internalId;
  }
  
  @Override
  protected boolean hasNextPage() {
    Elements pagination = this.currentDoc.select(".pagination > li");
    Element page = CommonMethods.getLast(pagination);
    
    return page != null && !page.hasClass("disabled");
  }
}
