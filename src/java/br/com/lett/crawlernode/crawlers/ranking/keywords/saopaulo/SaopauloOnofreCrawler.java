package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class SaopauloOnofreCrawler extends CrawlerRankingKeywords {

	public SaopauloOnofreCrawler(Session session) {
		super(session);
	}

	private String crawlInternalId(Element e) {
		String internalId = e.attr("data-productid");

		return internalId;
	}

	private String crawlInternalPid(Element e) {
		String internalPid = null;
		Element pid = e.select("div.product-image a img").first();

		if (!(pid.attr("src").contains("imagem_pescricao"))) {
			String[] tokens2 = pid.attr("src").split("%2f");
			String temp = tokens2[tokens2.length - 1];
			if (!temp.contains("imagem")) {
				int x;
				if (temp.contains("_")) {
					x = temp.indexOf("_");
				} else {
					x = temp.indexOf(".");
				}
				internalPid = tokens2[tokens2.length - 1].substring(0, x).trim();
			}
		}

		return internalPid;
	}

	private String crawlProductUrl(Element e) {
		String urlProduct = null;
		Element urlElement = e.select(" h2 p.product-name > a[title]").first();

		if (!urlElement.attr("href").contains("onofre")) {
			urlProduct = "http://www.onofre.com.br" + urlElement.attr("title");
		} else {
			urlProduct = urlElement.attr("title");
		}

		return urlProduct;
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		// número de produtos por página do market
		this.pageSize = 15;

		String key = this.location.replaceAll(" ", "%20");

		this.log("Página " + this.currentPage);

		// monta a url com a keyword e a página
		String url = "http://busca.onofre.com.br/search?w=" + key + "&srt=" + (this.currentPage - 1) * 300 + "&cnt=300";

		this.log("Link onde são feitos os crawlers: " + url);

		// chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		;

		Elements id = this.currentDoc.select("div.shelf-product");
		Elements result = this.currentDoc.select("div#sli_noresult");

		// se obter 1 ou mais links de produtos e essa página tiver resultado
		// faça:
		if (id.size() >= 1 && result.size() < 1) {
			// seta o total da busca
			if (this.totalBusca == 0)
				setTotalBusca();

			for (Element e : id) {

				// InternalPid
				String internalPid = crawlInternalPid(e);

				// InternalId
				String internalId = crawlInternalId(e);

				// Url do produto
				String urlProduct = crawlProductUrl(e);

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
		if (!(hasNextPage()))
			setTotalBusca();

	}

	@Override
	protected boolean hasNextPage() {
		return true;
	}

	@Override
	protected void setTotalBusca() {
		Element totalElement = this.currentDoc.select("div.item.pages.last span").first();

		if (totalElement != null) {
			try {
				String[] token = totalElement.text().split("de");

				this.totalBusca = Integer.parseInt(token[token.length - 1].trim());

			} catch (Exception e) {
				e.printStackTrace();
				this.logError(e.getMessage());
			}
		}

		this.log("Total da busca: " + this.totalBusca);
	}
}
