package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilMartinsCrawler extends CrawlerRankingKeywords {

	public BrasilMartinsCrawler(Session session) {
		super(session);
	}

	@Override
	public void extractProductsFromCurrentPage() {
		// número de produtos por página do market
		this.pageSize = 12;

		this.log("Página " + this.currentPage);

		// monta a url com a keyword e a página
		String url = "https://b.martins.com.br/busca.aspx?q=" + this.keywordEncoded
				+ "&pagesize=150&page=" + this.currentPage;
		this.log("Link onde são feitos os crawlers: " + url);

		// chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products = this.currentDoc.select(".product-search > .quick-view");

		// se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if (products.size() >= 1) {
			// se o total de busca não foi setado ainda, chama a função para setar
			if (this.totalProducts == 0) {
				setTotalProducts();
			}

			for (Element e : products) {
				String internalPid = null;
				String internalId = crawlInternalId(e);
				String urlProduct = crawlProductUrl(e);

				saveDataProduct(internalId, internalPid, urlProduct);

				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: "
						+ internalPid + " - Url: " + urlProduct);
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
		Elements page = this.currentDoc.select("a[title='Próxima página']");

		// se elemeno page obtiver algum resultado
		if (page.size() > 0) {
			// tem próxima página
			return true;
		}

		return false;

	}

	@Override
	protected void setTotalProducts() {
		Element totalElement =
				this.currentDoc.select("div#ctnTituloQtdItensEncontrados strong").first();

		try {
			if (totalElement != null) {
				this.totalProducts = Integer.parseInt(totalElement.text());
			}
		} catch (Exception e) {
			this.logError(CommonMethods.getStackTrace(e));
		}

		this.log("Total da busca: " + this.totalProducts);
	}

	private String crawlInternalId(Element e) {
		String internalId = e.attr("data-codigo");

		if (internalId.isEmpty()) {
			return null;
		}

		return internalId;
	}

	private String crawlProductUrl(Element e) {
		String productUrl = e.attr("data-url");

		if (!productUrl.startsWith("https://b.martins.com.br/")) {
			productUrl = ("https://b.martins.com.br/" + productUrl).replace("br//", "br/");
		}

		return productUrl;
	}
}
