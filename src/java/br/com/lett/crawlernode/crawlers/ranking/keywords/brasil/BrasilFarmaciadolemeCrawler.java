package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import br.com.lett.crawlernode.core.fetcher.methods.POSTFetcher;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilFarmaciadolemeCrawler extends CrawlerRankingKeywords{

	public BrasilFarmaciadolemeCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 12;
	
		this.log("Página "+ this.currentPage);
		
		String postUrl = "https://www.farmaciadoleme.com.br/loja/search_autocomplete/get_products";
		this.log("Link onde são feitos os crawlers: " + postUrl);	
		
		String payload = "term=" + this.keywordEncoded;
		
		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/x-www-form-urlencoded");
		
		String page = POSTFetcher.fetchPagePOSTWithHeaders(postUrl, session, payload, null, 1, headers);
		JSONObject productsInfo = new JSONObject();
		
		try {
			productsInfo = new JSONObject(page);
		} catch (JSONException e1) {
			this.logError(CommonMethods.getStackTrace(e1), e1);
		}
		
		if(productsInfo.has("message")) {
			JSONArray products = productsInfo.getJSONArray("message");
			this.totalProducts = products.length();
			this.log("Total da busca: " + this.totalProducts);
			
			if(this.totalProducts > 0) {
				for(int i = 0; i < products.length(); i++) {
					JSONObject product = products.getJSONObject(i);
					
					String internalPid = null;
					String productUrl = crawlProductUrl(product);
					String internalId = crawlInternalId(productUrl);
					
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
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
		}
		
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
	}

	@Override
	protected boolean hasNextPage() {
		return false;
	}
	
	private String crawlInternalId(String url) {
		String internalId = null;
		String token = url.split("produto/")[1].split("/")[0].replaceAll("[^0-9]", "").trim();
		
		if(!token.isEmpty()) {
			internalId = token;
		}
		
		return internalId;
	}
	
	private String crawlProductUrl(JSONObject product){
		String productUrl = null;
		
		if(product.has("url")) {
			productUrl = product.getString("url");
			
			if(!productUrl.startsWith("https://www.farmaciadoleme.com.br/")) {
				productUrl = ("https://www.farmaciadoleme.com.br/" + productUrl).replace("br//", "br/");
			}
		}
		
		return productUrl;
	}
}
