package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilVianimalCrawler extends CrawlerRankingKeywords {
   
 private static final String HOME_PAGE = "www.vianimal.com.br";

 public BrasilVianimalCrawler(Session session) {
   super(session);
 }

 @Override
 protected void extractProductsFromCurrentPage() {
   this.pageSize = 120;
   this.log("Página " + this.currentPage);

   String url = "http://www.vianimal.com.br/catalogsearch/result/index/?p=2" + this.currentPage + "&q=" + this.keywordEncoded;
   
   this.log("Link onde são feitos os crawlers: " + url);
   this.currentDoc = fetchDocument(url);
   Elements products = this.currentDoc.select(".category-products ul > li.item");

   if (!products.isEmpty()) {
     if (this.totalProducts == 0) {
       setTotalProducts();
     }
     
     for (Element e : products) {
       String internalPid = scrapInternalPid(e);
       String productUrl = CrawlerUtils.scrapUrl(e, ".product-name > a", "href", "https", HOME_PAGE);

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
   Element pidElement = e.selectFirst(".add-to-links > li > a");
   
   if(pidElement != null && pidElement.hasAttr("href")) {
     String[] addToCartPidUrlPaths = pidElement.attr("href").split("/");
     
     for(int i = 0; i < addToCartPidUrlPaths.length; i++) {
        String path = addToCartPidUrlPaths[i];
        
        if(path.equals("product") && (i+1) < addToCartPidUrlPaths.length) {
           internalPid = addToCartPidUrlPaths[i+1];
           break;
        }
     }
   }

   return internalPid;
 }
 
 @Override
 protected void setTotalProducts() {
   this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, 
         ".toolbar .pager .amount", null, null, false, false, 0);

   this.log("Total de produtos: " + this.totalProducts);
 }
}
