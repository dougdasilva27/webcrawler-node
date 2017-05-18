package br.com.lett.crawlernode.crawlers.ranking.categories.saopaulo;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingCategories;

public class SaopauloNetfarmaCrawler extends CrawlerRankingCategories {

	public SaopauloNetfarmaCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		// número de produtos por página do market
		this.pageSize = 24;

		this.log("Página " + this.currentPage);

		// monta a url com a keyword e a página
		String url = this.location + "?tamanho=24&ordenar=popular&pagina=" + this.currentPage;
		
		this.log("Link onde são feitos os crawlers: " + url);

		// chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products = this.currentDoc.select("#lista-produtos .product-item");

		// se obter 1 ou mais links de produtos e essa página tiver resultado
		// faça:
		if (products.size() >= 1) {
			for (Element e : products) {
				// Url do produto
				String urlProduct = crawlProductUrl(e);

				// InternalPid
				String internalPid = crawlInternalPid(e);

				// InternalId
				String internalId = crawlInternalId();

				saveDataProduct(internalId, internalPid, urlProduct);

				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: "
						+ internalPid + " - Url: " + urlProduct);
				if (this.arrayProducts.size() == productsLimit)
					break;
			}
		} else {
			setTotalProducts();
			this.result = false;
			this.log("Keyword sem resultado!");
		}

		this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
				+ this.arrayProducts.size() + " produtos crawleados");
		
		if (!(hasNextPage())) {
			setTotalProducts();
		}
	}

	@Override
	protected boolean hasNextPage() {
		Element pageInactive = this.currentDoc.select("li.neemu-pagination-last.neemu-pagination-inactive").first();
		Element page = this.currentDoc.select("li.neemu-pagination-last").first();;

		// se elemeno page obtiver algum resultado
		if (pageInactive != null || page == null){
			return false;
		}

		return true;
	}

	private String crawlInternalId() {
		String internalId = null;

		return internalId;
	}

	private String crawlInternalPid(Element e) {
		return e.attr("id").split("-")[1];
	}

	private String crawlProductUrl(Element e) {
		String urlProduct = null;
		
		Element url = e.select(" > a").first();
		
		if(url != null) {
			urlProduct = url.attr("href");
			
			if(!urlProduct.startsWith("http") && urlProduct.contains("netfarma")) {
				urlProduct = "https:" + urlProduct;
			} else if(!urlProduct.contains("netfarma")) {
				urlProduct = "https://www.netfarma.com.br" + urlProduct;
			}
		}
		
		return urlProduct;
	}
}
