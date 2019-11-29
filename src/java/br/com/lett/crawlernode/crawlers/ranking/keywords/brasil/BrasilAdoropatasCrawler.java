package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilAdoropatasCrawler extends CrawlerRankingKeywords {
	  
  private static final String HOME_PAGE = "www.adoropatas.com.br";

  public BrasilAdoropatasCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 12;
    this.log("Página " + this.currentPage);

    String url = "https://www.adoropatas.com.br/catalogsearch/result/index/?p=" + this.currentPage + "&q=" + this.keywordEncoded;
    
    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".products .products .product.item");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }
    	
      for (Element e : products) {
        String internalId = scrapInternalId(e);
        String productUrl = CrawlerUtils.scrapUrl(e, "a.product-image", "href", "http:", HOME_PAGE);

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


  private String scrapInternalId(Element e) {
    String internalId = null;
    Element pidElement = e.selectFirst("[id*=product-price-]");
    
    if(pidElement != null) {
      String pid = pidElement.id();
      
      if(pid.startsWith("product-price-")) {
        internalId = pid.substring("product-price-".length()).trim();
      }
    }

    return internalId;
  }
  
  @Override
  protected void setTotalProducts() {
    Elements amounts = this.currentDoc.select("#toolbar-amount .toolbar-number");
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(CommonMethods.getLast(amounts), null, null, null, true, true, 0);

    this.log("Total de produtos: " + this.totalProducts);
  }
}
