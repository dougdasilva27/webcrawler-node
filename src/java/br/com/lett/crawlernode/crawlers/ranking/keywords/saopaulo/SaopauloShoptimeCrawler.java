package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class SaopauloShoptimeCrawler extends CrawlerRankingKeywords {

	public SaopauloShoptimeCrawler(Session session) {
		super(session);
	}

	private String crawlInternalId(Element e) {
		String internalId = null;

		return internalId;
	}

	private String crawlInternalPid(JSONObject json) {
		String internalPid = null;

		if (json.has("originalId")) {
			internalPid = json.getString("originalId");
		}

		return internalPid;
	}

	private String crawlProductUrl(JSONObject json) {
		String urlProduct = null;

		if (json.has("productUrlStore")) {
			urlProduct = json.getString("productUrlStore");

		}
		return urlProduct;
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		// número de produtos por página do market
		this.pageSize = 20;

		this.log("Página " + this.currentPage);

		// monta a url com a keyword e a página
		String urlJson = "http://busca.shoptime.com.br/api/v1/search?q=" + this.keywordEncoded + "&page="
				+ this.currentPage + "&results_per_page=24&format=json";
		// String url =
		// "http://busca.shoptime.com.br/busca.php?q="+this.keywordWithParamPlus+"&page="+this.currentPage;
		this.log("Link onde são feitos os crawlers: " + urlJson);

		JSONObject jsonProducts = fetchJSONObject(urlJson);

		if (this.totalBusca == 0) {
			this.totalBusca = this.setTotalBusca(jsonProducts);
		}

		if (jsonProducts.has("productsInfo")) {
			JSONObject products = jsonProducts.getJSONObject("productsInfo");

			if (products.has("products")) {
				JSONArray arrayProducts = products.getJSONArray("products");

				for (int i = 0; i < arrayProducts.length(); i++) {
					JSONObject jsonTemp = arrayProducts.getJSONObject(i);

					String internalPid = crawlInternalPid(jsonTemp);

					// InternalId
					String internalId = crawlInternalId(null);

					// Url do produto
					String urlProduct = crawlProductUrl(jsonTemp);

					saveDataProduct(internalId, internalPid, urlProduct);

					this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: "
							+ internalPid + " - Url: " + urlProduct);
					if (this.arrayProducts.size() == productsLimit)
						break;
				}
			}
		} else {
			this.result = false;
			this.log("Fim das páginas");
		}

		this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
				+ this.arrayProducts.size() + " produtos crawleados");
	}

	@Override
	protected boolean hasNextPage() {
		return true;
	}

	protected int setTotalBusca(JSONObject json) {
		int totalBusca = 0;

		if (json.has("searchInfo")) {
			JSONObject searchInfo = json.getJSONObject("searchInfo");

			if (searchInfo.has("number_of_results")) {
				totalBusca = searchInfo.getInt("number_of_results");
			}
		}

		this.log("Total da busca: " + totalBusca);

		return totalBusca;
	}

}
