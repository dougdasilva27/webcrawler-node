package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class SaopauloSondaCrawler extends CrawlerRankingKeywords {

	public SaopauloSondaCrawler(Session session) {
		super(session);
	}

	private String crawlInternalId(String url) {
		String internalId = null;

		String[] tokens = url.split("/");
		internalId = tokens[tokens.length - 1];

		return internalId;
	}

	private String crawlInternalPid(Element e) {
		String internalPid = null;

		return internalPid;
	}

	private String crawlProductUrl(Element e) {
		String urlProduct = e.attr("href");

		if (!urlProduct.startsWith("http://www.sondadelivery.com.br")) {
			urlProduct = "http://www.sondadelivery.com.br" + urlProduct;
		}

		return urlProduct;
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		// número de produtos por página do market
		this.pageSize = 10;

		this.log("Página " + this.currentPage);

		String keyword = this.location.replaceAll(" ", "%20");

		// monta a url com a keyword e a página
		String url = "http://www.sondadelivery.com.br/delivery.aspx/busca/0/0/0/" + this.currentPage + "/" + keyword;
		this.log("Link onde são feitos os crawlers: " + url);

		// chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements id = this.currentDoc.select("div.txt-produto > a");

		// se obter 1 ou mais links de produtos e essa página tiver resultado
		// faça:
		if (id.size() >= 1) {
			for (Element e : id) {
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
			setTotalBusca();
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
		Element page = this.currentDoc.select("div.paginacao > a[title=\"Próxima página\"]").first();

		// se elemeno page não obtiver nenhum resultado
		if (page == null)
			return false;

		return true;
	}
}
