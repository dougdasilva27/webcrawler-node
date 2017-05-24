package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class SaopauloPaodeacucarCrawler extends CrawlerRankingKeywords {

	public SaopauloPaodeacucarCrawler(Session session) {
		super(session);
	}

	private List<Cookie> cookies = new ArrayList<>();

	@Override
	public void extractProductsFromCurrentPage() {
		// número de produtos por página do market
		this.pageSize = 36;

		this.log("Página " + this.currentPage);

		// monta a url com a keyword e a página
		String url = "https://paodeacucar.resultspage.com/search?&cnt=36&ep.selected_store=501&lot=json&p=Q"
				+ "&srt="+ this.arrayProducts.size() +"&ts=json-full&w=" + this.keywordEncoded;
		
		this.log("Link onde são feitos os crawlers: " + url);

		// chama função de pegar a url
		JSONObject search = fetchJSONObject(url, cookies); 
				
		// se obter 1 ou mais links de produtos e essa página tiver resultado
		if (search.has("results") && search.getJSONArray("results").length() > 0) {
			JSONArray products = search.getJSONArray("results");
			
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
				String internalId = crawlInternalId(productUrl);

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
		if(search.has("result_meta")) {
			JSONObject resultMeta = search.getJSONObject("result_meta");
			
			if(resultMeta.has("total")) {		
				this.totalProducts = resultMeta.getInt("total");
				this.log("Total da busca: " + this.totalProducts);				
			}
		}
	}
	
	private String crawlInternalId(String url) {
		String[] tokens = url.split("/");
		return tokens[tokens.length - 1];
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

		if(product.has("url")) {
			urlProduct = product.getString("url");
		}
		
		return urlProduct;
	}
}
