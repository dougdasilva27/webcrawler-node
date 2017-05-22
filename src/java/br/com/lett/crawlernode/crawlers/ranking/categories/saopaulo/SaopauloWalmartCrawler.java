package br.com.lett.crawlernode.crawlers.ranking.categories.saopaulo;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingCategories;
import br.com.lett.crawlernode.util.CommonMethods;

public class SaopauloWalmartCrawler extends CrawlerRankingCategories {

	public SaopauloWalmartCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		// número de produtos por página do market
		this.pageSize = 20;

		this.log("Página " + this.currentPage);

		// monta a url com a keyword e a página
		String url;
		
		if(this.location.contains("?")) {
			url = this.location + "&PageNumber=" + this.currentPage;
		} else {
			url = this.location + "?PageNumber=" + this.currentPage;
		}
		
		this.log("Link onde são feitos os crawlers: " + url);

		// chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products = this.currentDoc.select(".shelf-product-item");

		// se obter 1 ou mais links de produtos e essa página tiver resultado
		// faça:
		if (products.size() >= 1) {
			// se o total de busca não foi setado ainda, chama a função para
			// setar
			if (this.totalProducts == 0)
				setTotalProducts();

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
		Element page = this.currentDoc.select("a.btn.btn-primary.shelf-view-more.next").first();

		// se elemeno page não obtiver nenhum resultado
		if (page == null) {
			// não tem próxima página
			return false;
		}

		return true;

	}

	@Override
	protected void setTotalProducts() {
		Element totalElement = this.currentDoc.select(".result-items").first();

		if (totalElement != null) {
			try {
				this.totalProducts = Integer.parseInt(totalElement.text().replaceAll("[^0-9]", "").trim());
			} catch (Exception e) {
				this.logError(CommonMethods.getStackTrace(e));
			}

			this.log("Total da busca: " + this.totalProducts);
		}
	}
	
	private String crawlInternalId(Element e) {
		String internalId = null;

		return internalId;
	}

	private String crawlInternalPid(Element e) {
		String internalPid = null;

		if (e.hasAttr("data-productid")) {
			internalPid = e.attr("data-productid");
		}

		return internalPid;
	}

	private String crawlProductUrl(Element e) {
		String urlProduct = null;
		Element urlElement = e.select("section > a").first();

		if (urlElement != null) {
			urlProduct = urlElement.attr("href");

			if (!urlProduct.contains("walmart")) {
				urlProduct = "https://www.walmart.com.br/" + urlProduct;
			} else if (!urlProduct.startsWith("https:")) {
				urlProduct = "https:" + urlProduct;
			}
		}

		return urlProduct;
	}

}
