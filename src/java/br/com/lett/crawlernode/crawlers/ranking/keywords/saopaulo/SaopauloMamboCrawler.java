package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.json.JSONArray;
import org.json.JSONObject;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class SaopauloMamboCrawler extends CrawlerRankingKeywords {

	public SaopauloMamboCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		this.log("Página " + this.currentPage);
		this.pageSize = 24;

		String keyword = this.keywordWithoutAccents.replaceAll(" ", "%20");
		String url = "http://busca.mambo.com.br/busca?q=" + keyword + "&page=" + this.currentPage;
		takeAScreenshot(url);

		String apiUrl = "http://busca.mambo.com.br/busca?q=" + keyword + "&page=" + this.currentPage + "&ajaxSearch=1";
		this.log("Link onde são feitos os crawlers: " + apiUrl);	

		JSONObject search = fetchJSONObject(apiUrl);
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
