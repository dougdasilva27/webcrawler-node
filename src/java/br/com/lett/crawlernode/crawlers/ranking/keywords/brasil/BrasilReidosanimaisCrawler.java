package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;

public class BrasilReidosanimaisCrawler extends CrawlerRankingKeywords {

   public BrasilReidosanimaisCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.APACHE;
   }

   @Override
   public void extractProductsFromCurrentPage() {
      this.pageSize = 36;
      this.log("Página " + this.currentPage);

      String keyword = this.keywordWithoutAccents.replace(" ", "");

      String url = "https://www.reidosanimais.com.br/catalogsearch/result/index/?__dinTrafficSource=eyJ1cmwiOiJodHRwczovL3d3dy5yZWlkb3NhbmltYWlzLmNvbS5ici8iLCJyZWZlcmVyIjoiIn0%3D&p=" + this.currentPage + "&q=" + keyword;
      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("div .products ul > li .products__case");
      
      if (!products.isEmpty()&& products.size() == pageSize) {
 
    	  for(Element element : products) {
    		  String internalId = scrapInternalId(element, ".product__image.product-image img", "product-id");
    		  String productUrl = scrapUrl(element, ".product__header a", "href");
    		  saveDataProduct(internalId, null, productUrl);

    		  this.log("Position: " + this.position +  " - InternalId: " + internalId + " - Url: " + productUrl);
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
	   boolean hasNext = false;
	   Elements nextPage = this.currentDoc.select(".paginacao a");
	   
	   for(Element e : nextPage){
		   if(e.text() == ">") {
			   hasNext = true;
		   }
	   }
	   return hasNext;
   }
   
  private String scrapUrl(Element doc, String cssSelector,String attributes) {
	  String url = null;
	  
      Element urlElement = cssSelector != null ? doc.selectFirst(cssSelector) : doc;
      url = urlElement.attr(attributes).trim();
      return url;
  }

   private String scrapInternalId(Element doc, String cssSelector,String attributes) {
      String internalId = null;

      Element internalIdElement = cssSelector != null ? doc.selectFirst(cssSelector) : doc;
      internalId = internalIdElement.attr(attributes);
      return internalId;
   }
}
