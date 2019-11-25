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

public class BrasilCasadoprodutorCrawler extends CrawlerRankingKeywords {

   public BrasilCasadoprodutorCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.APACHE;
   }

   @Override
   public void extractProductsFromCurrentPage() {
      this.pageSize = 24;
      this.log("Página " + this.currentPage);

      String keyword = this.keywordWithoutAccents.replaceAll(" ", "");

      String url = "https://www.casadoprodutor.com.br/resultadopesquisa?pag=" + this.currentPage + "&departamento=&buscarpor=" + keyword;
      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      
      JSONObject jsonObject = CrawlerUtils.selectJsonFromHtml(this.currentDoc, "script", "dataLayer.push(", ");", false, true); 
      JSONObject ecommerceProducts = jsonObject.getJSONObject("ecommerce");
      JSONArray productsArray = ecommerceProducts != null && ecommerceProducts.has("impressions") ? ecommerceProducts.getJSONArray("impressions") : new JSONArray();

      Elements products = this.currentDoc.select(".loadProducts ul > li");

      if (!products.isEmpty() && products.size() == productsArray.length()) {
         for (int index = 0; index < products.size(); index++) {
            Object obj = productsArray.get(index);
            JSONObject productJSON = obj instanceof JSONObject ? (JSONObject) obj : new JSONObject();
            String internalPid = scrapInternalPid(productJSON);
            String productUrl = scrapUrl(products.get(index), ".foto a", "href");
            saveDataProduct(null, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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
	  
	  //System.err.println(doc);
      Element urlElement = cssSelector != null ? doc.selectFirst(cssSelector) : doc;
      url = urlElement.attr("href").trim();
      if (urlElement != null) {
    
    	  int indexBegin = url.indexOf("window.location='")+ 17;
    	  int indexFinal = url.length()-2;
    	  url = url.substring(indexBegin, indexFinal);
    	  
      }
      
      
      return url;
  }

   private String scrapInternalId(JSONObject productJSON) {
      String internalId = null;

      if (productJSON.has("sku") && !productJSON.isNull("sku")) {
         internalId = productJSON.get("sku").toString();
      }

      return internalId;
   }

   private String scrapInternalPid(JSONObject productJSON) {
      String internalPid = null;

      if (productJSON.has("id") && !productJSON.isNull("id")) {
         internalPid = productJSON.get("id").toString();
         if(internalPid.contains("-")) {
       	  	 int index = internalPid.indexOf("-");
        	 internalPid = internalPid.substring(0, index);
         }
      }

      return internalPid;
   }
}
