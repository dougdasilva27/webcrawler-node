package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class SaopauloDrogasilCrawler extends CrawlerRankingKeywords {

	public SaopauloDrogasilCrawler(Session session) {
		super(session);
	}

	private String redirectUrl;

	private String crawlInternalId(List<String> internalIds, int index) {
		String internalId = null;

		if (internalIds.size() > index) {
			internalId = internalIds.get(index);
		}

		return internalId;
	}

	private List<String> crawlInternalIds() {
		List<String> internalIds = new ArrayList<>();
		Elements productList = this.currentDoc.select("script[type=text/javascript]");

		JSONArray jsonProducts = new JSONArray();

		for (Element e : productList) {
			String script = e.outerHtml().toLowerCase();

			if (script.contains("['product_list']")) {
				int x = script.indexOf("'] =") + 4;
				int y = script.indexOf(";", x);

				String json = script.substring(x, y).trim();

				if (json.startsWith("[") && json.endsWith("]")) {
					jsonProducts = new JSONArray(json);
				}

				break;

			} else if (script.contains("spark.addsearch")) {
				String text = script.replaceAll("\"", "");

				String[] ids = text.split(";");

				for (int i = 0; i < ids.length; i++) {
					String textScript = ids[i];

					if (textScript.contains("addpagesku")) {
						JSONObject json = new JSONObject();

						int x = textScript.indexOf("(") + 1;
						int y = textScript.indexOf(")", x);

						json.put("id", textScript.substring(x, y));

						jsonProducts.put(json);
					}
				}

				break;
			}
		}

		for (int i = 0; i < jsonProducts.length(); i++) {
			JSONObject product = jsonProducts.getJSONObject(i);

			if (product.has("id")) {
				internalIds.add(i, product.getString("id"));
			} else {
				internalIds.add(i, null);
			}
		}

		return internalIds;
	}

	private String crawlInternalPid(Element e) {
		String internalPid = null;
		Element pidElement = e.select("> a img").first();

		if (pidElement != null) {
			String[] tokens = pidElement.attr("id").split("-");
			internalPid = tokens[tokens.length - 1];
		}

		return internalPid;
	}

	private String crawlProductUrl(Element e) {
		String urlProduct = null;
		Element urlElement = e.select(".product-name > a[title]").first();

		if (urlElement != null) {
			urlProduct = urlElement.attr("title");
		}

		return urlProduct;
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		String keyword = this.location.replaceAll(" ", "%20");

		this.log("Página " + this.currentPage);

		// monta a url com a keyword e a página
		String searchUrl = "http://busca.drogasil.com.br/search?w=" + keyword + "&cnt=36&srt="
				+ this.arrayProducts.size();

		if (this.currentPage == 1) {
			this.currentDoc = fetchDocument(searchUrl);
			this.log("Link onde são feitos os crawlers: " + searchUrl);

			this.redirectUrl = this.currentDoc.baseUri();

		} else {
			// Caso de busca
			if (this.currentDoc.select(".category-title").first() == null) {
				this.currentDoc = fetchDocument(searchUrl);
				this.log("Link onde são feitos os crawlers: " + searchUrl);

				// número de produtos por página do market
				this.pageSize = 12;

				// Caso de categoria
			} else {
				String urlCategory = this.redirectUrl + "?p=" + this.currentPage;

				this.currentDoc = fetchDocument(urlCategory);
				this.log("Link onde são feitos os crawlers: " + urlCategory);

				// número de produtos por página do market
				this.pageSize = 24;
			}
		}

		// os internalIds estão em um json
		List<String> productsList = crawlInternalIds();
		Elements products = this.currentDoc.select("li.item div.container:not(.limit)");

		int index = 0;

		// se obter 1 ou mais links de produtos e essa página tiver resultado
		// faça:
		if (products.size() >= 1) {
			// total de produtos na busca
			if (this.totalBusca == 0) {
				setTotalBusca();
			}

			for (Element e : products) {
				// InternalPid
				String internalPid = crawlInternalPid(e);

				// InternalId
				String internalId = crawlInternalId(productsList, index);

				// Url do produto
				String productUrl = crawlProductUrl(e);

				saveDataProduct(internalId, internalPid, productUrl);

				index++;

				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: "
						+ internalPid + " - Url: " + productUrl);
				if (this.arrayProducts.size() == productsLimit)
					break;
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
		// se não peguei todos os produtos tem próxima página
		if (this.arrayProducts.size() < this.totalBusca) {
			return true;
		}

		return false;
	}

	@Override
	protected void setTotalBusca() {
		Element totalElement = this.currentDoc.select("p.amount").first();

		if (totalElement != null) {
			try {
				this.totalBusca = Integer.parseInt(totalElement.text().replaceAll("[^0-9]", "").trim());
			} catch (Exception e) {
				this.logError(e.getMessage());
			}
		}

		this.log("Total da busca: " + this.totalBusca);
	}
}
