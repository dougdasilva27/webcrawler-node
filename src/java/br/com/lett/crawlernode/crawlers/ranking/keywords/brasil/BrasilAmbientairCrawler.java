package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilAmbientairCrawler extends CrawlerRankingKeywords {

	public BrasilAmbientairCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		// número de produtos por página do market
		this.pageSize = 8;

		this.log("Página " + this.currentPage);

		String keyword = this.location.replaceAll(" ", "%20");

		// monta a url com a keyword e a página
		String url = "http://www.ambientair.com.br/busca.html?loja=110&acao=BU&busca=" + keyword
				+ "&passo=exibeTodos&ordem=PROD_NOME&pagina=" + this.currentPage;
		this.log("Link onde são feitos os crawlers: " + url);

		// chama função de pegar o html
		this.currentDoc = fetchDocument(url);

		Elements products = this.currentDoc.select("div.divProdutos ul > li");

		// se obter 1 ou mais links de produtos e essa página tiver resultado
		// faça:
		if (products.size() >= 1) {
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
				if (this.arrayProducts.size() == productsLimit) {
					break;
				}

			}
		} else {
			setTotalBusca();
			this.result = false;
			this.log("Keyword sem resultado!");
		}

		if (!hasNextPage()) {
			setTotalBusca();
		}

		this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
				+ this.arrayProducts.size() + " produtos crawleados");
	}

	@Override
	protected boolean hasNextPage() {
		Elements products = this.currentDoc.select("div.divProdutos ul > li");

		if (products.size() >= 8) {
			return true;
		}

		return false;
	}

	private String crawlInternalId(Element e) {
		String internalId = null;
		
		Element id = e.select("input.compare").first();
		
		if (id != null) {
			internalId = id.val();
		}

		return internalId;
	}

	private String crawlInternalPid(Element e) {
		return null;
	}

	private String crawlProductUrl(Element e) {
		String productUrl = null;
		Element eUrl = e.select("a.produto").first();
		
		if(eUrl != null) {
			productUrl = eUrl.attr("href");
			if (!productUrl.contains("ambientair")) {
				productUrl = "http://www.ambientair.com.br/" + productUrl;
			}
		}

		return productUrl;
	}
}
