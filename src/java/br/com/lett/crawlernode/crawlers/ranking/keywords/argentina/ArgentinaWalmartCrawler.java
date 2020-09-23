package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class ArgentinaWalmartCrawler extends CrawlerRankingKeywords{

   private static final String HOME_PAGE ="www.walmart.com.ar";

	public ArgentinaWalmartCrawler(Session session) {
		super(session);
	}

	private JSONObject fetchProducts(){
      String apiProducts = "https://ucustom.walmart.com.ar/docs/search.json?bucket=walmart_search_stage&family=product&view=default&text="
      + this.keywordEncoded
      + "&window=50&sort=$_substance_value&direction=-1&levels=1&attributes[sales_channel][]=15&page="
      + this.currentPage;

      this.log("Link onde são feitos os crawlers: "+apiProducts);

	   Request request = Request.RequestBuilder.create().setUrl(apiProducts).build();

      JSONObject response = JSONUtils.stringToJson(dataFetcher.get(session, request).getBody());

      return response.optJSONObject("data");
   }

	@Override
	protected void extractProductsFromCurrentPage() {
		this.log("Página "+ this.currentPage);
		
		//número de produtos por página do market
		this.pageSize = 48;
		
		// Take all data from the json API.
		JSONObject data = fetchProducts();
		JSONArray products = data.optJSONArray("views");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.length() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) setTotalProducts(data);
			
			for(Object product: products) {
				// InternalPid
				String internalPid 	= crawlInternalPid(product);
				
				// InternalId
				String internalId 	= crawlInternalId(product);
				
				// Url do produto
				String productUrl = crawlProductUrl(product);
				
				saveDataProduct(internalId, internalPid, productUrl);
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
				if(this.arrayProducts.size() == productsLimit) break;
				
			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
		}
	
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
	}

	@Override
	protected boolean hasNextPage() {
		return arrayProducts.size() < this.totalProducts;
	}

	private void setTotalProducts(Object e) {
	   this.totalProducts = ((JSONObject) e).optInt("count");

		this.log("Total da busca: "+this.totalProducts);
	}
	
	private String crawlInternalId(Object e){
		return null;
	}
	
	private String crawlInternalPid(Object e){
		return ((JSONObject) e).optString("product_id");
	}
	
	private String crawlProductUrl(Object e){
	   String link = ((JSONObject) e).optString("permalink") + "/p";

	   return CrawlerUtils.completeUrl(link, "https", HOME_PAGE);
	}
}
