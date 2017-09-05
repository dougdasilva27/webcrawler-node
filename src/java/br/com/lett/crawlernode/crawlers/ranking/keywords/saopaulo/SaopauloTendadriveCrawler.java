package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class SaopauloTendadriveCrawler extends CrawlerRankingKeywords {

	public SaopauloTendadriveCrawler(Session session) {
		super(session);
	}

	private List<Cookie> cookies = new ArrayList<>();
	
	@Override 
	public void processBeforeFetch() {
		this.log("Adding cookie...");
		BasicClientCookie cookie = new BasicClientCookie("VTEXSC", "sc=10");
		cookie.setDomain("busca.tendadrive.com.br");
		cookie.setPath("/");
		this.cookies.add(cookie);
		
		String url = "http://busca.tendadrive.com.br/Site/Track.aspx?q=sadia&page=1&results_per_page=300&sc=10&referrer=";
		
		Map<String, String> cookiesMap = fetchCookies(url, cookies);
		
		for(Entry<String, String> entry : cookiesMap.entrySet()) {
			BasicClientCookie cookie2 = new BasicClientCookie(entry.getKey(), entry.getValue());
			cookie2.setDomain("busca.tendadrive.com.br");
			cookie2.setPath("/");
			this.cookies.add(cookie2);
		}
	}
	
	@Override
	protected void extractProductsFromCurrentPage() {
		this.log("Página " + this.currentPage);
		this.pageSize = 24;

		String keyword = this.keywordWithoutAccents.replaceAll(" ", "%20");
		String url = "http://busca.tendadrive.com.br/busca?q=" + keyword + "&page=" + this.currentPage;
		takeAScreenshot(url);

		String apiUrl = "http://busca.tendadrive.com.br/busca?q=" + keyword + "&page=" + this.currentPage + "&ajaxSearch=1&sc=10";
		this.log("Link onde são feitos os crawlers: " + apiUrl);	

		JSONObject search = fetchJSONObject(apiUrl, cookies);
		JSONArray products = crawlProducts(search);

		if (products.length() > 0) {
			if (this.totalProducts == 0) {
				setTotalProducts(search);
			}

			for (int i = 0; i < products.length(); i++) {
				JSONObject product = products.getJSONObject(i);
				
				String internalPid = crawlInternalPid(product);
				String internalId = null;
				String productUrl = crawlProductUrl(product);

				saveDataProduct(internalId, internalPid, productUrl);

				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: "
						+ internalPid + " - Url: " + productUrl);
				if (this.arrayProducts.size() == productsLimit) {
					break;
				}
			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
		}

		this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
				+ this.arrayProducts.size() + " produtos crawleados");
	}

	@Override
	protected boolean hasNextPage() {
		return arrayProducts.size() < this.totalProducts;
	}

	protected void setTotalProducts(JSONObject search) {
		if(search.has("totalProducts")) {
			JSONObject info = search.getJSONObject("totalProducts");
			
			if(info.has("totalResults")) {
				this.totalProducts = info.getInt("totalResults");
				
				this.log("Total: " + this.totalProducts);
			}
		}
	}
	
	private String crawlInternalPid(JSONObject product) {
		String internalPid = null;

		if(product.has("originalId")) {
			internalPid = product.get("originalId").toString();
		}
		
		return internalPid;
	}

	private String crawlProductUrl(JSONObject product) {
		String urlProduct = null;

		if (product.has("productUrl")) {
			urlProduct = product.getString("productUrl");
			
			if(!urlProduct.startsWith("http")) {
				urlProduct = "https:" + urlProduct;
			}
		}

		return urlProduct;
	}

	
	private JSONArray crawlProducts(JSONObject json) {
		JSONArray products = new JSONArray();
		
		if(json.has("productsInfo")) {
			JSONObject info = json.getJSONObject("productsInfo");
			
			if(info.has("products")) {
				products = info.getJSONArray("products");
			}
		}
		
		return products;
	}

}
