package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilAmazonCrawler extends CrawlerRankingKeywords {

	public BrasilAmazonCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		// número de produtos por página do market
		this.pageSize = 20;

		this.log("Página " + this.currentPage);

		// monta a url com a keyword e a página
		String url = "https://www.amazon.com.br/s/?page=" + this.currentPage + "&keywords="
				+ this.keywordEncoded + "&ie=UTF8";
		this.log("Link onde são feitos os crawlers: " + url);

		// chama função de pegar o html
		this.currentDoc = fetchDocument(url);

		Elements products = this.currentDoc.select(".s-result-list li.s-result-item");
		Element result = this.currentDoc.select("#noResultsTitle").first();

		// se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if (!products.isEmpty() && result == null) {
			// se o total de busca não foi setado ainda, chama a função para setar
			if (this.totalProducts == 0) {
				setTotalProducts();
			}

			for (Element e : products) {

				// InternalPid
				String internalPid = crawlInternalPid(e);

				// InternalId
				String internalId = internalPid;

				// Url do produto
				String productUrl = crawlProductUrl(e);

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
		return this.totalProducts > this.arrayProducts.size();
	}

	@Override
	protected void setTotalProducts() {
		Element totalElement = this.currentDoc.select("#s-result-count").first();

		if (totalElement != null) {
			String text = totalElement.ownText().trim();

			if (text.contains("de")) {
				String total = text.split("de")[1].replaceAll("[^0-9]", "").trim();

				if (!total.isEmpty()) {
					this.totalProducts = Integer.parseInt(total);
				}
			}

			this.log("Total da busca: " + this.totalProducts);
		}
	}

	private String crawlInternalPid(Element e) {
		return e.attr("data-asin");
	}

	private String crawlProductUrl(Element e) {
		String productUrl = null;

		Element url = e.select(".a-link-normal").first();

		if (url != null) {
			productUrl = url.attr("href").split("\\?")[0];

			if (!productUrl.contains("amazon.com.br")) {
				productUrl = "https://www.amazon.com.br" + e.attr("href");
			}
		}

		return productUrl;
	}

}
