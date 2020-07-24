package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilAbaraujoCrawler extends CrawlerRankingKeywords {
	  
public BrasilAbaraujoCrawler(Session session) {
		super(session);
	}

private static final String HOME_PAGE = "www.abaraujo.com.br";

@Override
protected void extractProductsFromCurrentPage() {
  this.pageSize = 12;
  this.log("Página " + this.currentPage);

  String url = "https://www.abaraujo.com/catalogsearch/result/index/?p=" + this.currentPage + "&q=" + this.keywordEncoded;
  
  this.log("Link onde são feitos os crawlers: " + url);
  this.currentDoc = fetchDocument(url);
  Elements products = this.currentDoc.select(".products .products .product.item");

  if (!products.isEmpty()) {
    if (this.totalProducts == 0) {
      setTotalProducts();
    }
  	
    for (Element e : products) {
      String internalPid = scrapInternalPid(e);
      String productUrl = CrawlerUtils.scrapUrl(e, ".product-item-photo", "href", "http:", HOME_PAGE);

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


private String scrapInternalPid(Element e) {
  String internalPid = null;
  Element pidElement = e.selectFirst("[id*=product-price-]");
  
  if(pidElement != null) {
    String pid = pidElement.id();
    
    if(pid.startsWith("product-price-")) {
      internalPid = pid.substring("product-price-".length()).trim();
    }
  }

  return internalPid;
}

@Override
protected void setTotalProducts() {
  Elements amounts = this.currentDoc.select("#toolbar-amount .toolbar-number");
  this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(CommonMethods.getLast(amounts), null, null, null, true, true, 0);

  this.log("Total de produtos: " + this.totalProducts);
}
}
