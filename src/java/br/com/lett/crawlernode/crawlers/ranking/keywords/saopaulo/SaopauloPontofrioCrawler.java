package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class SaopauloPontofrioCrawler extends CrawlerRankingKeywords {

	private boolean isCategory;
	private String urlCategory;
	
	public SaopauloPontofrioCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		this.log("Página " + this.currentPage);

		// monta a url com a keyword e a página
		String keyword = this.keywordWithoutAccents.replaceAll(" ", "%20");

		String url = "http://search.pontofrio.com.br/?strBusca=" + keyword + "&paginaAtual=" + this.currentPage;
		
		if (this.currentPage > 1) {
			if (isCategory) {
				url = this.urlCategory + "&paginaAtual=" + this.currentPage;
			}
		}
		
		this.log("Link onde são feitos os crawlers: " + url);

		// chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products = this.currentDoc.select("a.link.url");

		// número de produtos por página do market
		if (!isCategory) {
			this.pageSize = 20;
		}

        if (this.currentPage == 1) {
			String redirectUrl = this.session.getRedirectedToURL(url);
			if (redirectUrl != null && redirectUrl.contains("Filtro")) {
				isCategory = true;
				this.urlCategory = redirectUrl;
			} else {
				isCategory = false;
			}
		}
        
		Elements result = this.currentDoc.select(".naoEncontrado");

		// se obter 1 ou mais links de produtos e essa página tiver resultado
		// faça:
		if (products.size() >= 1 && result.size() < 1) {
            if (this.totalProducts == 0) {
                setTotalProducts();
            }
			for (Element e : products) {
				// InternalPid
				String internalPid = crawlInternalPid(e);

				// InternalId
				String internalId = crawlInternalId();

				// Url do produto
				String urlProduct = crawlProductUrl(internalPid);

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
		Element page = this.currentDoc.select("li.next a").first();

        return page != null;
    }

	@Override
	protected void setTotalProducts() {
        Element totalElement = this.currentDoc.select(".resultado .resultado strong").first();

        if (totalElement != null) {
            try {
                this.totalProducts = Integer.parseInt(totalElement.text());
            } catch (Exception e) {
                this.logError(e.getMessage());
            }
        }
        this.log("Total da busca: " + this.totalProducts);
	}

    private String crawlInternalId() {
        return null;
    }

    private String crawlInternalPid(Element e) {
        return e.attr("data-id");
    }

    private String crawlProductUrl(String internalPid) {
        return "http://produto.pontofrio.com.br/?IdProduto=" + internalPid;
    }
}
