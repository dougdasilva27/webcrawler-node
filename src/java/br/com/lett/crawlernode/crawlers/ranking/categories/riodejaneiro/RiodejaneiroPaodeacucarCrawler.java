package br.com.lett.crawlernode.crawlers.ranking.categories.riodejaneiro;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.fetcher.methods.GETFetcher;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingCategories;
import br.com.lett.crawlernode.util.CommonMethods;

public class RiodejaneiroPaodeacucarCrawler extends CrawlerRankingCategories {

	public RiodejaneiroPaodeacucarCrawler(Session session) {
		super(session);
	}

	private List<Cookie> cookies = new ArrayList<>();
	private Map<String,String> headers = new HashMap<>();

	@Override
	public void extractProductsFromCurrentPage() {
		// número de produtos por página do market
		this.pageSize = 12;

		this.log("Página " + this.currentPage);

		// monta a url com a keyword e a página
		String url;
		
		if(this.location.contains("?")) {
			url = this.location + "&p=" + (this.currentPage - 1);
		} else {
			url = this.location + "?p=" + (this.currentPage - 1);
		}
		
		this.log("Link onde são feitos os crawlers: " + url);

		// chama função de pegar a url
		this.currentDoc = Jsoup.parse(GETFetcher.fetchPageGETWithHeaders(session, url, cookies, headers, 1));

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
		Element page = this.currentDoc.select(".nextPage > a").first();

		// se elemento page obtiver algum resultado
		if (page != null) {
			return true;
		}

		return false;
	}

	@Override
	protected void processBeforeFetch() {
		this.headers.put("Host", "www.paodeacucar.com");
		this.headers.put("Connection", "keep-alive");
		this.headers.put("Cache-Control", "max-age=0");
		this.headers.put("Upgrade-Insecure-Requests", "1");
		this.headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
		this.headers.put("Accept-Encoding", "gzip, deflate, sdch");
		this.headers.put("Accept-Language", "pt-BR,pt;q=0.8,en-US;q=0.6,en;q=0.4");
		
		if (this.cookies.isEmpty()) {
			BasicClientCookie cookie = new BasicClientCookie("ep.selected_store", "7");
			cookie.setDomain(".paodeacucar.com");
			cookie.setPath("/");
			cookie.setExpiryDate(new Date(System.currentTimeMillis() + 604800000L));
			
			BasicClientCookie cookie2 = new BasicClientCookie("ep.store_name_7", "%22Rio%20de%20Janeiro%22");
			cookie2.setDomain(".paodeacucar.com");
			cookie2.setPath("/");
			cookie2.setExpiryDate(new Date(System.currentTimeMillis() + 604800000L));
						
			BasicClientCookie cookie6 = new BasicClientCookie("ep.language_code_7", "pt-BR");
			cookie6.setDomain(".paodeacucar.com");
			cookie6.setPath("/");
			cookie6.setExpiryDate(new Date(System.currentTimeMillis() + 604800000L));
			
			BasicClientCookie cookie8 = new BasicClientCookie("ep.currency_code_7", "BRL");
			cookie8.setDomain(".paodeacucar.com");
			cookie8.setPath("/");
			cookie8.setExpiryDate(new Date(System.currentTimeMillis() + 604800000L));


			this.cookies.add(cookie2);
			this.cookies.add(cookie);
			this.cookies.add(cookie6);
			this.cookies.add(cookie8);
		}
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
		String[] tokens = url.split("/");
		
		return tokens[tokens.length - 2];
	}
	
	private String crawlInternalPid(Element e) {
		return null;
	}

	private String crawlProductUrl(Element e) {
		return e.attr("href");
	}
}
