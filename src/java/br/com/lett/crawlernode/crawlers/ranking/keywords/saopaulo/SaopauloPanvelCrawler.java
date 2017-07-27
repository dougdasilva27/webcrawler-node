package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class SaopauloPanvelCrawler extends CrawlerRankingKeywords {

	public SaopauloPanvelCrawler(Session session) {
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

		if (!urlProduct.startsWith("https://www.panvel.com")) {
			urlProduct = "https://www.panvel.com" + urlProduct;
		}

		return urlProduct;
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		// número de produtos por página do market
		this.pageSize = 15;

		this.log("Página " + this.currentPage);

		// monta a url com a keyword e a página
		String url = "http://www.panvel.com/panvel/buscarProduto.do?paginaAtual=" + this.currentPage
				+ "&tipo=bar&termoPesquisa=" + this.keywordWithoutAccents.replace(" ", "+");
		this.log("Link onde são feitos os crawlers: " + url);

		// chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products = this.currentDoc.select("a.lnk_mais_detalhes.gsaLink");

		// se obter 1 ou mais links de produtos e essa página tiver resultado
		// faça:
		if (products.size() >= 1) {
			// se o total de busca não foi setado ainda, chama a função para
			// setar
			if (this.totalProducts == 0)
				setTotalProducts();

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
	}

	@Override
	protected boolean hasNextPage() {
		if ((this.totalProducts > this.arrayProducts.size()) && this.currentDoc.select("a.lnk_mais_detalhes.gsaLink").size() > 0) {
			return true;
		}

		return false;
	}

	@Override
	protected void setTotalProducts() {
		Element totalElement = this.currentDoc.select("div.pag p").first();

		if (totalElement != null) {
			try {
				int x = totalElement.text().indexOf("de");

				String token = totalElement.text().substring(x + 2).trim();

				this.totalProducts = Integer.parseInt(token);
			} catch (Exception e) {
				this.logError(CommonMethods.getStackTrace(e));
			}

			this.log("Total da busca: " + this.totalProducts);
		}
	}
}
