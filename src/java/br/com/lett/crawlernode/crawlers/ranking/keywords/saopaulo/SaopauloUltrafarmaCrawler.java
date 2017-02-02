package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class SaopauloUltrafarmaCrawler extends CrawlerRankingKeywords {

	public SaopauloUltrafarmaCrawler(Session session) {
		super(session);
	}

	private String crawlInternalId(String url) {
		String internalId = null;

		String[] tokens = url.split("/");
		String[] tokens2 = tokens[tokens.length - 2].split("-");
		internalId = tokens2[tokens2.length - 1];

		return internalId;
	}

	private String crawlInternalPid(Element e) {
		String internalPid = null;

		return internalPid;
	}

	private String crawlProductUrl(Element e) {
		String urlProduct = e.attr("title");

		return urlProduct;
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		// número de produtos por página do market
		this.pageSize = 12;

		this.log("Página " + this.currentPage);
		// monta a url com a keyword e a página
		String url = "http://busca.ultrafarma.com.br/search?w=" + this.keywordEncoded + "&srt="
				+ this.arrayProducts.size();
		this.log("Link onde são feitos os crawlers: " + url);

		// chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products = this.currentDoc.select("a.nome_produtos_home");

		// se obter 1 ou mais links de produtos e essa página tiver resultado
		// faça:
		if (products.size() >= 1) {
			// se o total de busca não foi setado ainda, chama a função para
			// setar
			if (this.totalBusca == 0)
				setTotalBusca();

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
		Element page = this.currentDoc.select("a#botao_seta_direita").first();

		// se elemento page obtiver algum resultado
		if (page != null)
			return true;

		return false;
	}

	@Override
	protected void setTotalBusca() {
		Element totalElement = this.currentDoc.select("span.sli_bct_total_records").first();

		try {
			if (totalElement != null)
				this.totalBusca = Integer.parseInt(totalElement.text());
		} catch (Exception e) {
			this.logError(e.getMessage());
		}
		this.log("Total da busca: " + this.totalBusca);
	}

}
