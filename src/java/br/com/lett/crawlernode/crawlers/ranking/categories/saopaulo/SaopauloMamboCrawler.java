package br.com.lett.crawlernode.crawlers.ranking.categories.saopaulo;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingCategories;

public class SaopauloMamboCrawler extends CrawlerRankingCategories {

	public SaopauloMamboCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		this.log("Página " + this.currentPage);

		// número de produtos por página do market
		this.pageSize = 30;

		// monta a url com a keyword e a página
		String url;
		
		if(this.categoryUrl.contains("?")) {
			url = this.categoryUrl + "&PageNumber=" + this.currentPage;
		} else {
			url = this.categoryUrl + "?PageNumber=" + this.currentPage;
		}
				
		this.log("Link onde são feitos os crawlers: " + url);

		// chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products = this.currentDoc.select(".prateleira.principal > ul > li[layout]");

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
			setTotalProducts();
			this.result = false;
			this.log("Keyword sem resultado!");
		}

		this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
				+ this.arrayProducts.size() + " produtos crawleados");
		
		if(!hasNextPage()) {
			setTotalProducts();
		}
	}

	@Override
	protected boolean hasNextPage() {
		if (this.pageSize >= this.currentDoc.select(".prateleira.principal > ul > li[layout]").size()) {
			return true;
		}

		return false;
	}


	private String crawlInternalId(Element e) {
		String internalId = null;

		Element inidElement = e.select(".collection-content > div[data-trustvox-product-code]").first();

		if (inidElement != null) {
			internalId = inidElement.attr("data-trustvox-product-code");
		}

		return internalId;
	}

	private String crawlInternalPid(Element e) {
		return null;
	}

	private String crawlProductUrl(Element e) {
		String urlProduct = null;
		Element urlElement = e.select(".collection-link > a").first();

		if (urlElement != null) {
			urlProduct = urlElement.attr("href");
		}

		return urlProduct;
	}
}
