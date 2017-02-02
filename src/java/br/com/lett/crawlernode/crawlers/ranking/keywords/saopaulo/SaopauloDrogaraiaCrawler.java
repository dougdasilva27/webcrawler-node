package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class SaopauloDrogaraiaCrawler extends CrawlerRankingKeywords {

	public SaopauloDrogaraiaCrawler(Session session) {
		super(session);
	}

	private String crawlInternalId(List<String> internalIds, int index) {
		String internalId = internalIds.get(index);

		return internalId;
	}

	private List<String> crawlInternalIdsFromScript(Document doc) {
		List<String> internalIds = new ArrayList<>();
		Elements scripts = doc.select("script[language=javascript][type=text/javascript]");

		for (Element e : scripts) {
			String script = e.outerHtml().toLowerCase();

			if (script.contains("spark.addpagesku")) {
				int x = script.indexOf(");spark.addpagesku");
				int y = script.indexOf("spark.write", x + 2);

				script = script.substring(x + 2, y).trim();

				String[] tokens = script.split(";");

				for (String s : tokens) {
					internalIds.add(s.replaceAll("[^0-9]", "").trim());
				}

				break;
			}
		}

		return internalIds;
	}

	private String crawlInternalPid(Element e) {
		String internalPid = null;
		Element pid = e.select("> a > img[id]").first();

		if (pid != null) {
			String[] tokens = pid.attr("id").split("-");
			internalPid = tokens[tokens.length - 1];
		}

		return internalPid;
	}

	private String crawlProductUrl(Element e) {
		String urlProduct = null;
		Element urlElement = e.select("div.productInfo div > a").first();

		if (urlElement != null) {
			urlProduct = urlElement.attr("href");
		}

		return urlProduct;
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		// número de produtos por página do market
		this.pageSize = 12;

		this.log("Página " + this.currentPage);
		// monta a url com a keyword e a página
		String url = "http://busca.drogaraia.com.br/search?w=" + this.keywordEncoded + "&cnt=150&srt="
				+ this.arrayProducts.size();
		this.log("Link onde são feitos os crawlers: " + url);

		// chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		List<String> internalIds = crawlInternalIdsFromScript(this.currentDoc);

		Elements id = this.currentDoc.select("div.container:not(.min-limit)");

		int indexOfProduct = 0;

		// se obter 1 ou mais links de produtos e essa página tiver resultado
		// faça:
		if (id.size() >= 1) {
			for (Element e : id) {
				// InternalPid
				String internalPid = crawlInternalPid(e);

				// InternalId
				String internalId = crawlInternalId(internalIds, indexOfProduct);

				// Url do produto
				String productUrl = crawlProductUrl(e);

				saveDataProduct(internalId, internalPid, productUrl);

				indexOfProduct++;

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
		if (!(hasNextPage()))
			setTotalBusca();
	}

	@Override
	protected boolean hasNextPage() {
		Element page = this.currentDoc.select("a.next.i-next.button2.btn-more").first();

		// se elemeno page obtiver algum resultado
		if (page != null) {
			// tem próxima página
			return true;
		}

		return false;

	}

	@Override
	protected void setTotalBusca() {
		Element totalElement = this.currentDoc.select("p.amount").first();

		if (totalElement != null) {
			try {

				String token = totalElement.text().replaceAll("[^0-9]", "").trim();

				this.totalBusca = Integer.parseInt(token);
			} catch (Exception e) {
				this.logError(e.getMessage());
			}

			this.log("Total da busca: " + this.totalBusca);
		}
	}
}
