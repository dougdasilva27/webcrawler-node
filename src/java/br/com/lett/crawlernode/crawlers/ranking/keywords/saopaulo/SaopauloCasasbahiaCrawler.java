package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class SaopauloCasasbahiaCrawler extends CrawlerRankingKeywords {

	public SaopauloCasasbahiaCrawler(Session session) {
		super(session);
	}

	private boolean isCategory;
	private String urlCategory;

	private String crawlInternalId(Element e) {
		String internalId = null;

		return internalId;
	}

	private String crawlInternalPid(Element e) {
		String internalPid = null;

		internalPid = e.attr("data-id");

		return internalPid;
	}

	private String crawlProductUrl(String internalPid) {
		String urlProduct = "http://produto.casasbahia.com.br/?IdProduto=" + internalPid;

		return urlProduct;
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		this.log("Página " + this.currentPage);

		// monta a url com a keyword e a página

		String url = "http://buscas2.casasbahia.com.br/?strBusca=" + this.keywordEncoded + "&paginaAtual="
				+ this.currentPage;
		this.log("Link onde são feitos os crawlers: " + url);

		if (this.currentPage > 1) {
			if (isCategory) {
				url = this.urlCategory + "&paginaAtual=" + this.currentPage;
			}
		}

		this.log("Link onde são feitos os crawlers: " + url);

		// chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements id = this.currentDoc.select("a.link.url");

		if (this.currentPage == 1) {
			if (this.currentDoc.baseUri().contains("Filtro")) {
				isCategory = true;
				this.urlCategory = this.currentDoc.baseUri();
			} else {
				isCategory = false;
			}
		}

		// número de produtos por página do market
		if (!isCategory)
			this.pageSize = 20;

		Elements result = this.currentDoc.select(".naoEncontrado");

		// se obter 1 ou mais links de produtos e essa página tiver resultado
		// faça:
		if (id.size() >= 1 && result.size() < 1) {
			// se o total de busca não foi setado ainda, chama a função para
			// setar
			if (this.totalBusca == 0)
				setTotalBusca();
			for (Element e : id) {
				// InternalPid
				String internalPid = crawlInternalPid(e);

				// InternalId
				String internalId = crawlInternalId(e);

				// Url do produto
				String productUrl = crawlProductUrl(internalPid);

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
		Element page = this.currentDoc.select("li.next a").first();

		// se elemeno page obtiver algum resultado
		if (page != null) {
			return true;
		}

		return false;

	}

	@Override
	protected void setTotalBusca() {
		Element totalElement = null;
		if (!isCategory) {
			totalElement = this.currentDoc.select(".resultado .resultado strong").first();

			if (totalElement != null) {
				try {
					this.totalBusca = Integer.parseInt(totalElement.text());
				} catch (Exception e) {
					this.logError(e.getMessage());
				}
			}
			this.log("Total da busca: " + this.totalBusca);
		} else {
			if (this.arrayProducts.size() < 100 && !hasNextPage()) {
				this.totalBusca = this.arrayProducts.size();
			}
		}
	}
}
