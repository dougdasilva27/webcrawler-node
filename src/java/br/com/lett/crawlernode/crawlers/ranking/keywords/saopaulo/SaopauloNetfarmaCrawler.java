package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class SaopauloNetfarmaCrawler extends CrawlerRankingKeywords {

	public SaopauloNetfarmaCrawler(Session session) {
		super(session);
	}

	private String crawlInternalId(String url) {
		String internalId = null;

		String[] tokens = url.split("/");
		internalId = tokens[tokens.length - 2];

		return internalId;
	}

	private String crawlInternalPid(Element e) {
		String internalPid = null;

		return internalPid;
	}

	private String crawlProductUrl(Element e) {
		String urlProduct = e.attr("href");

		return urlProduct;
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		// número de produtos por página do market
		this.pageSize = 20;

		this.log("Página " + this.currentPage);

		// monta a url com a keyword e a página
		String url = "http://busca.netfarma.com.br/busca?q=" + this.keywordEncoded + "&page=" + this.currentPage
				+ "&results_per_page=80";
		this.log("Link onde são feitos os crawlers: " + url);

		// chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products = this.currentDoc.select("div.produtos > ul .nome a");

		// se obter 1 ou mais links de produtos e essa página tiver resultado
		// faça:
		if (products.size() >= 1) {
			for (Element e : products) {
				// Url do produto
				String urlProduct = crawlProductUrl(e);

				// InternalPid
				String internalPid = crawlInternalPid(e);

				// InternalId
				String internalId = crawlInternalId(urlProduct);

				saveDataProduct(internalId, internalPid, urlProduct);

				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: "
						+ internalPid + " - Url: " + urlProduct);
				if (this.arrayProducts.size() == productsLimit)
					break;
			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
		}

		this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
				+ this.arrayProducts.size() + " produtos crawleados");
		if (!(hasNextPage()))
			setTotalBusca();
	}

	@Override
	protected boolean hasNextPage() {
		Element page = this.currentDoc.select("li.neemu-pagination-last.neemu-pagination-inactive").first();

		// se elemeno page obtiver algum resultado
		if (page != null)
			return false;

		return true;
	}

}
