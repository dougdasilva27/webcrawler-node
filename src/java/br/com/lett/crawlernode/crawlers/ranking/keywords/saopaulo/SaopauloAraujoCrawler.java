package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class SaopauloAraujoCrawler extends CrawlerRankingKeywords {

	public SaopauloAraujoCrawler(Session session) {
		super(session);
	}

	private String crawlInternalId(Element e) {
		String internalId = null;

		return internalId;
	}

	private String crawlInternalPid(Element e) {
		String internalPid = null;
		Element pid = e.select("input.qd_cpProdId.qd-productId").first();

		if (pid != null) {
			internalPid = pid.attr("value");
		}

		return internalPid;
	}

	private String crawlProductUrl(Element e) {
		String urlProduct = null;
		Element urlElement = e.select("a.qd_cpProductLink").first();

		if (urlElement != null) {
			urlProduct = urlElement.attr("href");
		}

		return urlProduct;
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		// número de produtos por página do market
		this.pageSize = 32;

		String key = this.location.replaceAll(" ", "%20");
		;

		this.log("Página " + this.currentPage);
		// monta a url com a keyword e a página
		String url = "http://busca.araujo.com.br/busca?q=" + key + "&page=" + this.currentPage;
		this.log("Link onde são feitos os crawlers: " + url);

		// chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products = this.currentDoc.select("div.prateleira ul li[layout]");

		// se obter 1 ou mais links de produtos e essa página tiver resultado
		// faça:
		if (products.size() >= 1) {
			// se o total de busca não foi setado ainda, chama a função para
			// setar
			if (this.totalBusca == 0)
				setTotalBusca();

			for (Element e : products) {
				// InternalPid
				String internalPid = crawlInternalPid(e);

				// InternalId
				String internalId = crawlInternalId(e);

				// Url do produto
				String productUrl = crawlProductUrl(e);

				saveDataProduct(internalId, internalPid, productUrl);

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
		Elements id = this.currentDoc.select("div.prateleira ul li[layout]");

		if (id.size() >= 32)
			return true;
		else
			return false;
	}

	@Override
	protected void setTotalBusca() {
		Element totalElement = this.currentDoc.select("div#neemu-total-products-container span.value").first();

		try {
			if (totalElement != null)
				this.totalBusca = Integer.parseInt(totalElement.text());
		} catch (Exception e) {
			this.logError(e.getMessage());
		}

		this.log("Total da busca: " + this.totalBusca);
	}
}
