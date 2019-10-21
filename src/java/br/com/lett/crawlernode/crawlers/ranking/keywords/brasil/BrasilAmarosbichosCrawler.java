package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilAmarosbichosCrawler extends CrawlerRankingKeywords {
  
  private static final String HOME_PAGE = "www.petshopamarosbichos.com.br";

  public BrasilAmarosbichosCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 15;
    this.log("Página " + this.currentPage);

    String url = "https://www.petshopamarosbichos.com.br/busca?page=" 
        + this.currentPage + "&q=" 
        + this.keywordEncoded;
    
    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".result-list > div");

    if (!products.isEmpty()) {
      for (Element e : products) {
        String internalPid = scrapInternalPid(e, ".description-product .product-name > a");
        String productUrl = CrawlerUtils.scrapUrl(e, ".description-product .product-name > a", "href", "http:", HOME_PAGE);

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


  private String scrapInternalPid(Element e, String selector) {
    String internalPid = null;
    Element urlElement = e.selectFirst(selector);
    
    if(urlElement != null && urlElement.hasAttr("href")) {
      String url = urlElement.attr("href");
      
      String[] div1 = url.split("/");
      
      if(div1.length > 0) {
        String[] div2 = div1[div1.length-1].split("-");
        internalPid = div2[0];
      }
    }

    return internalPid;
  }
  
  @Override
  protected boolean hasNextPage() {
    return this.currentDoc.selectFirst(".next_page.disabled") == null;
  }
}
