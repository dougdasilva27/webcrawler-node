package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class SaopauloLenovoCrawler extends CrawlerRankingKeywords {

	public SaopauloLenovoCrawler(Session session) {
		super(session);
	}

	private String crawlInternalId(Element e) {
		String internalId = null;
		Element id = e.select(".media .quick-links span > a").first();

		if (id != null) {
			internalId = id.attr("data-src");
		}

		return internalId;
	}

	private String crawlInternalPid() {
		return null;
	}

	private String crawlProductUrl(Element e) {
		String urlProduct = null;
		Element urlElement = e.select(".media > a").first();

		if (urlElement != null) {
			urlProduct = urlElement.attr("href");

			if (urlProduct.contains("?")) {
				urlProduct = urlProduct.split("\\?")[0];
			}

			if (!urlProduct.startsWith("http://shop.lenovo.com/")) {
				urlProduct = "http://shop.lenovo.com/" + urlProduct;
			}
		}

		return urlProduct;
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		// número de produtos por página do market
		this.pageSize = 12;

		this.log("Página " + this.currentPage);

		// monta a url com a keyword e a página
		String url = "http://shop.lenovo.com/SEUILibrary/controller/e/brweb/LenovoPortal/pt_BR/site.workflow:"
				+ "SimpleSiteSearch?cc=br&lang=pt&q=" + this.keywordEncoded + "&o=" + this.arrayProducts.size();
		this.log("Link onde são feitos os crawlers: " + url);

		// chama função de pegar a url
		this.currentDoc = fetchDocumentWithWebDriver(url);

		Elements products = this.currentDoc.select("#resultList li.listview");

		// se obter 1 ou mais links de produtos e essa página tiver resultado
		// faça:
		if (products.size() >= 1) {
			// se o total de busca não foi setado ainda, chama a função para
			// setar
			if (this.totalProducts == 0) {
				setTotalProducts();
			}

			for (Element e : products) {
				// Url do produto
				String productUrl = crawlProductUrl(e);

				// InternalPid
				String internalPid = crawlInternalPid();

				// InternalId
				String internalId = crawlInternalId(e);

				if (internalId == null) {
					this.position++;
					continue;
				}

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
		// se tiver menos que 12 posições na página, não há proxima página
		if (this.currentDoc.select("#resultList li.listview").size() < 12) {
			// não tem próxima página
			return false;
		}

		return true;
	}

	@Override
	protected void setTotalProducts() {
		Element totalElement = this.currentDoc.select("#numresultform label").first();

		if (totalElement != null) {
			try {

				String[] tokens = totalElement.ownText().split("de");

				this.totalProducts = Integer.parseInt(tokens[tokens.length - 1].replaceAll("[^0-9]", "").trim());
			} catch (Exception e) {
				this.logError(CommonMethods.getStackTraceString(e));
			}

			this.log("Total da busca: " + this.totalProducts);
		}
	}

}
