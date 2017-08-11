package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilBifarmaCrawler extends CrawlerRankingKeywords {

	public BrasilBifarmaCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		this.log("Página " + this.currentPage);

		// monta a url com a keyword e a página
		String url = "https://www.bifarma.com.br/busca_Loja.html?q=" + this.keywordWithoutAccents.replace(" ", "+");
		this.log("Link onde são feitos os crawlers: " + url);

		// chama função de pegar o html
		this.currentDoc = fetchDocument(url);

		Elements products = this.currentDoc.select("#gridProdutos .product");

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
			this.result = false;
			this.log("Keyword sem resultado!");
		}
		
		this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
				+ this.arrayProducts.size() + " produtos crawleados");
	}

	@Override
	protected boolean hasNextPage() {
		return false;
	}


	private String crawlInternalId(Element e) {
		String internalId = null;
		
		return internalId;
	}

	private String crawlInternalPid(Element e) {
		String internalPid = null;
		
		Element id = e.select("#produto_id").first();
		
		if(id != null) {
			internalPid = id.val();
		}
		
		return internalPid;
	}

	private String crawlProductUrl(Element e) {
		String productUrl = null;

		Element url = e.select("meta[itemprop=url]").first();
		
		if(url != null) {
			productUrl = url.attr("content");
	
			if (!productUrl.contains("bifarma")) {
				productUrl = "https://www.bifarma.com.br/" + productUrl;
			} else if(!productUrl.contains("http")) {
				productUrl = "https://" + productUrl;
			}
		}

		return productUrl;
	}
}
