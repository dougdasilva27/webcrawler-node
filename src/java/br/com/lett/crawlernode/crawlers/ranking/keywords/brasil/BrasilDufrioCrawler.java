package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.ArrayList;
import java.util.List;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.LettProxy;
import br.com.lett.crawlernode.core.fetcher.methods.GETFetcher;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilDufrioCrawler extends CrawlerRankingKeywords {

	public BrasilDufrioCrawler(Session session) {
		super(session);
	}

	private static final String USER_AGENT = DataFetcher.randUserAgent();
	private LettProxy proxyToBeUsed = null;
	private List<Cookie> cookies = new ArrayList<>();

	@Override
	protected void processBeforeFetch() {
		Document doc = Jsoup.parse(GETFetcher.fetchPageGET(session, "https://www.dufrio.com.br/",
				cookies, USER_AGENT, null, 1));

		this.proxyToBeUsed = session.getRequestProxy(session.getOriginalURL());

		Element script = doc.select("script").first();

		if (script != null) {
			String eval = script.html().trim();

			if (eval.endsWith(";")) {
				int y = eval.indexOf(";}}") + 3;
				int x = eval.indexOf(';', y) + 1;

				String b = eval.substring(y, x);

				if (b.contains("(")) {
					int z = b.indexOf('(') + 1;
					int u = b.indexOf(')', z);

					String result = b.substring(z, u);

					eval = "var document = {};" + eval.replace(b, "") + " " + result + " = " + result
							+ ".replace(\"location.reload();\", \"\"); " + b;
				}
			}

			ScriptEngineManager factory = new ScriptEngineManager();
			ScriptEngine engine = factory.getEngineByName("js");
			try {
				String cookieString = engine.eval(eval).toString();
				if (cookieString != null && cookieString.contains("=")) {
					String cookieValues =
							cookieString.contains(";") ? cookieString.split(";")[0] : cookieString;

					String[] tokens = cookieValues.split("=");

					if (tokens.length > 1) {

						BasicClientCookie cookie = new BasicClientCookie(tokens[0], tokens[1]);
						cookie.setDomain("www.dufrio.com.br");
						cookie.setPath("/");
						this.cookies.add(cookie);

						BasicClientCookie cookie2 = new BasicClientCookie("newsletter", "Visitante");
						cookie2.setDomain("www.dufrio.com.br");
						cookie2.setPath("/");
						this.cookies.add(cookie2);
					}
				}

			} catch (ScriptException e) {
				this.logError(CommonMethods.getStackTrace(e));
			}
		}
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		// número de produtos por página do market
		this.pageSize = 9;

		String keyword = this.keywordWithoutAccents.replaceAll(" ", "%20");

		this.log("Página " + this.currentPage);

		// monta a url com a keyword e a página
		String url =
				"https://www.dufrio.com.br/busca/" + this.currentPage + "?busca=" + keyword + "&ipp=36";
		this.log("Link onde são feitos os crawlers: " + url);

		// chama função de pegar a url
		this.currentDoc = Jsoup
				.parse(GETFetcher.fetchPageGET(session, url, cookies, USER_AGENT, this.proxyToBeUsed, 1));

		Elements products =
				this.currentDoc.select(".produtosCategoria > div.column .flex-child-auto .boxProduto");

		// se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if (!products.isEmpty()) {
			// se o total de busca não foi setado ainda, chama a função para setar
			if (this.totalProducts == 0) {
				setTotalProducts();
			}

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
		Element nextPage = this.currentDoc.select("a.ultima[disabled]").first();

		if (nextPage != null) {
			return false;
		}

		return true;

	}

	@Override
	protected void setTotalProducts() {
		Element totalElement = this.currentDoc.select(".qtdEncontrados span").first();

		if (totalElement != null) {
			String text = totalElement.text();

			if (text.contains("de")) {
				try {
					int x = text.indexOf("de") + 2;

					this.totalProducts = Integer.parseInt(text.substring(x).replaceAll("[^0-9]", "").trim());
				} catch (Exception e) {
					this.logError(CommonMethods.getStackTrace(e));
				}
			}

			this.log("Total da busca: " + this.totalProducts);
		}
	}

	private String crawlInternalId(Element e) {
		return null;
	}

	private String crawlInternalPid(Element e) {
		String internalPid = null;
		Element checkBox = e.select(".boxCheckbox input").first();

		if (checkBox != null) {
			internalPid = checkBox.val();
		}

		return internalPid;
	}

	private String crawlProductUrl(Element e) {
		String urlProduct = null;
		Element urlElement = e.select(".linkProd").first();

		if (urlElement != null) {
			urlProduct = urlElement.attr("href");
		}

		return urlProduct;
	}
}
