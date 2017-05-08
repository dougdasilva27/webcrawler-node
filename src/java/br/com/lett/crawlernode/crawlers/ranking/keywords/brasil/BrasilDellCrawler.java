package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONObject;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilDellCrawler extends CrawlerRankingKeywords{

	public BrasilDellCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 10;
	
		this.log("Página "+ this.currentPage);
		
		String keyword = this.keywordWithoutAccents.replaceAll(" ", "%20");
		
		//monta a url com a keyword e a página
		String url = "http://pilot.search.dell.com/queryunderstandingapi/4/br/pt/19/search-v/products?term=" + keyword;
		String payload = createRequestPayload(url, this.currentPage, pageSize);
		
		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar o html
		JSONObject products = new JSONObject(fetchStringPOST(url, payload, headers, null));
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.length() >= 1) {			
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalBusca == 0){
				setTotalBusca(products);
			}
			
			Map<String, String> productsMap = crawlSkuInformations(products);
			
			if(productsMap.size() >= 1) {
			
				for(Entry<String, String> product : productsMap.entrySet()) {
					
					// InternalPid
					String internalPid = null;
					
					// InternalId
					String internalId = product.getKey();
					
					// Url do produto
					String productUrl = product.getValue();
					
					saveDataProduct(internalId, internalPid, productUrl);
					
					this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
					
					if(this.arrayProducts.size() == productsLimit){
						break;
					}
				}
			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado na página atual!");
		}
	
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
	}

	@Override
	protected boolean hasNextPage() {
		//se  elemeno page obtiver algum resultado
		if(this.arrayProducts.size() < this.totalBusca){
			//tem próxima página
			return true;
		} 
			
		return false;
	}
	
	protected void setTotalBusca(JSONObject products) {		
		if(products.has("AdditionalInformation")) {
			JSONObject info = products.getJSONObject("AdditionalInformation");
			
			if(info.has("ResultCount-Products")) {
				try	{				
					this.totalBusca = Integer.parseInt(info.getString("ResultCount-Products").trim());
				} catch(Exception e) {
					this.logError(CommonMethods.getStackTraceString(e));
				}
				
				this.log("Total da busca: "+this.totalBusca);
			}
		}
	}
	
	/**
	 * Mapa de id e url
	 * @param products
	 * @return
	 */
	private Map<String,String> crawlSkuInformations(JSONObject products) {
		Map<String,String> productMap = new HashMap<>();
		
		if(products.has("Results")) {
			JSONArray arrayProducts = products.getJSONArray("Results");
			
			for(int i = 0; i < arrayProducts.length(); i++) {
				JSONObject product = arrayProducts.getJSONObject(i);
				
				if(product.has("Data")) {
					JSONObject data = product.getJSONObject("Data");
					
					if(data.has("ProductId") && data.has("MoreDetailsLink") && !data.getString("ProductId").trim().isEmpty()) {
						productMap.put(data.getString("ProductId"), data.getString("MoreDetailsLink"));
					}
				}
			}
		}
		
		return productMap;
	}
	
	private String createRequestPayload(String location, int page, int pageSize) {
		JSONObject jsonPayload = new JSONObject();
		
		jsonPayload.put("IncludeRefiners", true);
		jsonPayload.put("IncludeCategoryTree", false);
		jsonPayload.put("FiltersUpdatedByUser", false);
		jsonPayload.put("PreviousTerm", location);
		jsonPayload.put("VirtualAssistantData", new JSONObject());
		jsonPayload.put("Categories", new JSONArray());
		
		JSONObject options = new JSONObject();
		
		options.put("withqueryunderstandingenabled", true);
		options.put("WithNoTrackingEnabled", false);
		options.put("IncludeGraph", false);
		options.put("IncludeSignals", false);
		options.put("UrlReferrer", "http://www1.la.dell.com/content/default.aspx?c=br&l=pt&s=&s=gen&~ck=cr");
		
		JSONArray resultOptions = new JSONArray();
		JSONObject result = new JSONObject();
		
		result.put("Name", "include-fast-query");
		result.put("Value", false);
		resultOptions.put(result);
		
		options.put("ResultOptions", resultOptions);
		
		jsonPayload.put("Options", options);
		
		JSONObject profile = new JSONObject();
		
		profile.put("Segment", "gen");
		profile.put("CustomerSet", "");
		profile.put("Language", "pt");
		profile.put("Country", "br");
		profile.put("STPShopEnabled", false);
		
		jsonPayload.put("Profile", profile);
		
		jsonPayload.put("Products", new JSONArray()
									.put(new JSONObject()
										.put("Code", "")));
		
		jsonPayload.put("PagingOptions", new JSONObject()
										.put("Skip", this.currentPage * pageSize)
										.put("Take", pageSize)
										.put("Strategy", "ProductStrategy"));
		
		return jsonPayload.toString();
	}
}
