package br.com.lett.crawlernode.crawlers.ranking.categories.saopaulo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingCategories;

public class SaopauloPaodeacucarCrawler extends CrawlerRankingCategories {

	public SaopauloPaodeacucarCrawler(Session session) {
		super(session);
	}

	private List<Cookie> cookies = new ArrayList<>();

	private static final String HOME_PAGE = "https://www.paodeacucar.com/";
	private static final String STORE_ID = "501";
	
	@Override
	public void extractProductsFromCurrentPage() {
		// número de produtos por página do market
		this.pageSize = 0;

		this.log("Página " + this.currentPage);

		// monta a url com a keyword e a página
		String url = this.categoryUrl;
		
		if(url.contains("?")) {
			url = url.replace(HOME_PAGE, "https://api.gpa.digital/pa/products/list/").replace("%2520", "%20") 
					+ "&storeId="+ STORE_ID +"&qt=36&p=" + this.currentPage;
		} else {
			url = url.replace(HOME_PAGE, "https://api.gpa.digital/pa/products/list/") + "?storeId="+ STORE_ID +"&qt=36&p=" + this.currentPage;
		}
		
		this.log("Link onde são feitos os crawlers: " + this.categoryUrl + "?p=" + this.currentPage);

		// chama função de pegar a url
		JSONObject search = fetchJSONObject(url, cookies);
		
		if(search.has("content")) {
			search = search.getJSONObject("content");
		}
				
		// se obter 1 ou mais links de produtos e essa página tiver resultado
		if (search.has("products") && search.getJSONArray("products").length() > 0) {
			JSONArray products = search.getJSONArray("products");
			
			// se o total de busca não foi setado ainda, chama a função para
			if (this.totalProducts == 0) {
				setTotalProducts(search);
			}

			for (int i = 0; i < products.length(); i++) {
				JSONObject product = products.getJSONObject(i);
				
				// Url do produto
				String productUrl = crawlProductUrl(product);

				// InternalPid
				String internalPid = crawlInternalPid(product);

				// InternalId
				String internalId = crawlInternalId(product);

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
		if (this.arrayProducts.size() < this.totalProducts) {
			return true;
		}

		return false;
	}

	@Override
	protected void processBeforeFetch() {
		if (this.cookies.isEmpty()) {
			BasicClientCookie cookie = new BasicClientCookie("ep.selected_store", "501");
			cookie.setDomain(".paodeacucar.com");
			cookie.setPath("/");
			cookie.setExpiryDate(new Date(System.currentTimeMillis() + 604800000L + 604800000L));

			this.cookies.add(cookie);
		}
	}

	protected void setTotalProducts(JSONObject search) {
		if(search.has("totalElements")) {		
			this.totalProducts = search.getInt("totalElements");
			this.log("Total da busca: " + this.totalProducts);				
		}
	}
	
	private String crawlInternalId(	JSONObject product) {
		String internalId = null;
		
		if(product.has("id")) {
			internalId = product.get("id").toString();
		}

		return internalId;
	}

	private String crawlInternalPid(JSONObject product) {
		String internalPid = null;
		
		if(product.has("sku")) {
			internalPid = product.getString("sku");
		}

		return internalPid;
	}

	private String crawlProductUrl(JSONObject product) {
		String urlProduct = null;

		if(product.has("urlDetails")) {
			urlProduct = HOME_PAGE + product.getString("urlDetails");
		}
		
		return urlProduct;
	}
}
