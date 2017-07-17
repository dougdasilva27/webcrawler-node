package br.com.lett.crawlernode.crawlers.ranking.keywords.fortaleza;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class FortalezaPaguemenosCrawler extends CrawlerRankingKeywords {
	
	public FortalezaPaguemenosCrawler(Session session) {
		super(session);
	}

	private static final String COOKIE_ESTADO = "StoreCodePagueMenos";
	private static final String COOKIE_VALUE = "52";
	private List<Cookie> cookies = new ArrayList<>();

	private String crawlInternalId(Element e) {
		String internalId = null;
		
		Element inid = e.select("div.figure a > img.photo").first();

		if (inid != null) {
			if (!inid.attr("src").contains("indisponivel")) {
				String[] tokens2 = inid.attr("src").split("/");
				internalId = tokens2[tokens2.length - 2];
			}
		}

		return internalId;
	}

	private String crawlInternalPid(Element e) {
		String internalPid = null;
		Element pid = e.select("input").first();

		if (pid != null) {
			internalPid = pid.attr("value");
		}
		
		return internalPid;
	}

	private String crawlProductUrl(Element e) {
		String urlProduct = null;
		Element eUrl = e.select(" a.link.url[title]").first();

		if (eUrl != null) {
			urlProduct = eUrl.attr("href");
		}

		return urlProduct;
	}

	@Override
	public void extractProductsFromCurrentPage() {
		// número de produtos por página do market
		this.pageSize = 20;

		this.log("Página " + this.currentPage);

		String key = this.keywordWithoutAccents.replaceAll(" ", "%20");

		// monta a url com a keyword e a página
		String url = "http://loja.paguemenos.com.br/busca/3/0/0/Nome/Crescente/20/" + this.currentPage + "////" + key
				+ ".aspx";
		this.log("Link onde são feitos os crawlers: " + url);

		// chama função de pegar a url
		this.currentDoc = fetchDocument(url, cookies);

		Elements products = this.currentDoc.select("#listProduct > li");

		// se essa página tiver resultado faça:
		if (products.size() >= 1) {
			for (Element e : products) {
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
			setTotalProducts();
			this.result = false;
			this.log("Keyword sem resultado!");
		}

		this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
				+ this.arrayProducts.size() + " produtos crawleados");
		if (!(hasNextPage()))
			setTotalProducts();
	}

	@Override
	protected boolean hasNextPage() {
		Element page = this.currentDoc.select(".set-next.off").first();

		if (page != null) {
			return false;
		}

		return true;

	}

	@Override
	protected void processBeforeFetch() {
		if (this.cookies.size() < 1) {
			// Criando cookie da loja 52 = Fortaleza
			BasicClientCookie cookie = new BasicClientCookie(COOKIE_ESTADO, COOKIE_VALUE);
			cookie.setDomain("loja.paguemenos.com.br");
			cookie.setPath("/");

			this.cookies.add(cookie);
		}
	}
}
