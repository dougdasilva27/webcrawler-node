package br.com.lett.crawlernode.crawlers.ranking.keywords.belohorizonte;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BelohorizonteSupernossoCrawler extends CrawlerRankingKeywords{

	public BelohorizonteSupernossoCrawler(Session session) {
		super(session);
	}

	private static final String HOME_PAGE = "https://www.supernossoemcasa.com.br/e-commerce/";
	
	@Override
	protected void extractProductsFromCurrentPage() {
		this.pageSize = 60;
	
		this.log("Página "+ this.currentPage);
		
		JSONObject productsInfo = crawlProductInfo();
		JSONArray products = productsInfo.has("elements") ? productsInfo.getJSONArray("elements") : new JSONArray();
		
		if(products.length() > 0) {
			if(totalProducts == 0) {
				this.totalProducts = productsInfo.has("elementsCount") ? productsInfo.getInt("elementsCount") : 0;
				this.log("Total: " + this.totalProducts);
			}
			
			for(int i = 0; i < products.length(); i++) {
				JSONObject product = products.getJSONObject(i);
				
				String internalPid = crawlInternalPid(product);
				String internalId = crawlInternalId(product);
				String productUrl = crawlProductUrl(product, internalPid);
				
				saveDataProduct(internalId, internalPid, productUrl);
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
				
				if(this.arrayProducts.size() == productsLimit) {
					break;
				}
			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
		}
		
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
	}

	@Override
	protected boolean hasNextPage() {
		if(this.arrayProducts.size() < this.totalProducts) {
			return true;
		}
			
		return false;
	}	
	
	private String crawlInternalId(JSONObject product) {
		String internalId = null;
		
		if(product.has("sku")) {
			internalId = product.getString("sku");
		}
		
		return internalId;
	}
	
	private String crawlInternalPid(JSONObject product) {
		String internalPid = null;
		
		if(product.has("id")) {
			internalPid = product.getString("id");
		}
		
		return internalPid;
	}
	
	private String crawlProductUrl(JSONObject product, String id){
		String productUrl = null;
		
		if(product.has("name")) {
			productUrl = HOME_PAGE + "p/" + id + "/" + 
					CommonMethods.removeAccents(product.getString("name")).replace(" ", "-")
						.toLowerCase().replaceAll("[^a-z0-9]+", "-");
		}
		
		return productUrl;
	}
	
	private JSONObject crawlProductInfo() {
		JSONObject products = new JSONObject();
		String payload = "{\"pageNumber\": " + (this.currentPage-1) + ", \"pageSize\": 60, \"query\": \"" + this.location+ "\","
				+ " \"filters\": [], \"order\": \"relevancia\"}";
		
		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		headers.put("referer", "https://www.supernossoemcasa.com.br/e-commerce/search?query=nestle");

		String page = fetchPostFetcher("https://www.supernossoemcasa.com.br/e-commerce/api/products/autocomplete", payload, headers, null).trim();
		
		if(page != null && page.startsWith("{") && page.endsWith("}")) {
			try {
				products = new JSONObject(page);
			} catch (JSONException e) {
				logError(CommonMethods.getStackTrace(e));
			}
		}
		
		return products;
	}
}
