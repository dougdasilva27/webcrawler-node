package br.com.lett.crawlernode.crawlers.ranking.categories.riodejaneiro;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingCategories;
import br.com.lett.crawlernode.util.CommonMethods;

public class RiodejaneiroExtraCrawler extends CrawlerRankingCategories {

	public RiodejaneiroExtraCrawler(Session session) {
		super(session);
	}

	private List<Cookie> cookies = new ArrayList<>();

	@Override
	protected void processBeforeFetch() {		
		Map<String, String> cookiesMap = DataFetcher.fetchCookies(session, "https://chk.deliveryextra.com.br/address/chooseAddress", cookies, 1);
		
		for(Entry<String, String> entry : cookiesMap.entrySet()) {
			if(!entry.getKey().contains("241") && !entry.getKey().contains("ep.selected_store")) {
				// Criando cookie simulando um usuário logado
				BasicClientCookie cookie4 = new BasicClientCookie(entry.getKey(), entry.getValue());
				cookie4.setDomain("deliveryextra.com.br");
				cookie4.setExpiryDate(new Date(System.currentTimeMillis() + 604800000L + 604800000L));
				cookie4.setPath("/");
				cookies.add(cookie4);
			}
		}
		
		this.cookies.addAll(getCookies());
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		// número de produtos por página do market
		this.pageSize = 12;

		this.log("Página " + this.currentPage);

		// monta a url com a keyword e a página
		String url;

		if(this.categoryUrl.contains("?")) {
			url = this.categoryUrl + "&p=" + (this.currentPage - 1);
		} else {
			url = this.categoryUrl + "?p=" + (this.currentPage - 1);
		}

		this.log("Link onde são feitos os crawlers: " + url);

		// chama função de pegar a url
		this.currentDoc = fetchDocument(url, this.cookies);

		Elements id = this.currentDoc.select(".boxProduct .showcase-item__name a");
		Elements result = this.currentDoc.select("div#sli_noresult");

		// se obter 1 ou mais links de produtos e essa página tiver resultado
		// faça:
		if (id.size() >= 1 && result.size() < 1) {
			// se o total de busca não foi setado ainda, chama a função para
			// setar
			if (this.totalProducts == 0)
				setTotalProducts();

			for (Element e : id) {
				// Url do produto
				String productUrl = crawlProductUrl(e);

				// InternalPid
				String internalPid = crawlInternalPid(e);

				// InternalId
				String internalId = crawlInternalId(productUrl);

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

	private List<Cookie> getCookies() {

		List<Cookie> listCookies = new ArrayList<>();

		// Criando cookie da loja 42 = Rio de Janeiro
		BasicClientCookie cookie = new BasicClientCookie("ep.selected_store", "42");
		cookie.setDomain("deliveryextra.com.br");
		cookie.setExpiryDate(new Date(System.currentTimeMillis() + 604800000L + 604800000L));
		cookie.setPath("/");
		listCookies.add(cookie);

		// Criando cookie simulando um usuário logado
		BasicClientCookie cookie2 = new BasicClientCookie("ep.store_name_42", "Rio%20de%20Janeiro");
		cookie2.setDomain("deliveryextra.com.br");
		cookie2.setExpiryDate(new Date(System.currentTimeMillis() + 604800000L + 604800000L));
		cookie2.setPath("/");
		listCookies.add(cookie2);

		// Criando cookie simulando um usuário logado
		BasicClientCookie cookie3 = new BasicClientCookie("ep.currency_code_42", "BRL");
		cookie3.setDomain("deliveryextra.com.br");
		cookie3.setExpiryDate(new Date(System.currentTimeMillis() + 604800000L + 604800000L));
		cookie3.setPath("/");
		listCookies.add(cookie3);

		// Criando cookie simulando um usuário logado
		BasicClientCookie cookie4 = new BasicClientCookie("ep.language_code_42", "pt-BR");
		cookie4.setDomain("deliveryextra.com.br");
		cookie4.setExpiryDate(new Date(System.currentTimeMillis() + 604800000L + 604800000L));
		cookie4.setPath("/");
		listCookies.add(cookie4);
		
		return listCookies;
	}

	@Override
	protected boolean hasNextPage() {
		Element page = this.currentDoc.select(".nextPage > a").first();

		// se elemento page obtiver algum resultado
		if (page != null) {
			return true;
		}

		return false;
	}

	@Override
	protected void setTotalProducts() {
		Element totalElement = this.currentDoc.select("p.description").first();

		if(totalElement != null) {
			String total = totalElement.ownText();

			if(total.contains("de")) {
				String[] tokens = total.split("de");

				try {
					this.totalProducts = Integer.parseInt(tokens[tokens.length-1].replaceAll("[^0-9]", ""));
				} catch (Exception e) {
					this.logError(CommonMethods.getStackTrace(e));
				}

				this.log("Total da busca: " + this.totalProducts);
			}
		}
	}

	private String crawlInternalId(String url) {
		String internalId = null;

		String[] tokens = url.split("/");
		internalId = tokens[tokens.length - 2];

		return internalId;
	}

	private String crawlInternalPid(Element e) {
		String internalPid = null;

		return internalPid;
	}

	private String crawlProductUrl(Element e) {
		return e.attr("href");
	}
}
