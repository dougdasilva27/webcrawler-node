package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilAdiasCrawler extends CrawlerRankingKeywords {

	public BrasilAdiasCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		// número de produtos por página do market
		this.pageSize = 24;

		this.log("Página " + this.currentPage);

		// monta a url com a keyword e a página
		String url = "https://www.adias.com.br/busca?busca=" + this.keywordEncoded + "&pagina=" + this.currentPage;
		this.log("Link onde são feitos os crawlers: " + url);

		// chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements id = this.currentDoc.select("div.spot");

		int count = 0;

		// se obter 1 ou mais links de produtos e essa página tiver resultado
		// faça:
		if (id.size() >= 1) {
			// se o total de busca não foi setado ainda, chama a função para
			// setar
			if (this.totalProducts == 0) {
				setTotalProducts();
			}

			for (Element e : id) {
				count++;
				// seta o id com o seletor
				String[] tokens = e.attr("id").split("-");
				String internalPid = null;
				String internalId = tokens[tokens.length - 1];

				// monta a url
				Element eUrl = e.select("div.spotContent > a").first();
				String productUrl = eUrl.attr("href");

				if (!productUrl.contains("adias")) {
					productUrl = ("https://www.adias.com.br/" + productUrl).replace("br//", "br/");
				}

				saveDataProduct(internalId, internalPid, productUrl);

				this.log("InternalId do produto da " + count + " da página " + this.currentPage + ": " + internalId);
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
		if (this.arrayProducts.size() < this.totalProducts) {
			return true;
		}

		return false;
	}

	@Override
	protected void setTotalProducts() {
		Element totalElement = this.currentDoc.select("div.mostrando.left.fbits-info-bottom > span").first();

		if (totalElement != null) {
			try {
				int x = totalElement.text().indexOf("produto");

				String token = totalElement.text().substring(0, x).trim();

				this.totalProducts = Integer.parseInt(token);
			} catch (Exception e) {
				this.logError(e.getMessage());
			}

			this.log("Total da busca: " + this.totalProducts);
		}
	}
}
