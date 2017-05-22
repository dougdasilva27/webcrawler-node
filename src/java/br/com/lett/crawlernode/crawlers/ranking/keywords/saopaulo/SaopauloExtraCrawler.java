package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class SaopauloExtraCrawler extends CrawlerRankingKeywords {

	public SaopauloExtraCrawler(Session session) {
		super(session);
	}

	private List<Cookie> cookies = new ArrayList<>();

	private String crawlInternalId(String url) {
		String internalId = null;

		String[] tokens = url.split("/");
		internalId = tokens[tokens.length - 1];

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
	protected void processBeforeFetch() {
		this.cookies = getCookies();
	}
	
	@Override
	protected void extractProductsFromCurrentPage() {
		// número de produtos por página do market
		this.pageSize = 12;

		this.log("Página " + this.currentPage);

		// monta a url com a keyword e a página
		String url = "http://busca.deliveryextra.com.br/search?lbc=deliveryextra&w=" + this.keywordEncoded
				+ "&cnt=36&srt=" + this.arrayProducts.size();
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
				;

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

		// Criando cookie da loja 21 = São Paulo capital
		BasicClientCookie cookie = new BasicClientCookie("ep.selected_store", "241");
		cookie.setDomain("busca.deliveryextra.com.br");
		cookie.setPath("/");
		listCookies.add(cookie);

		// Criando cookie simulando um usuário logado
		BasicClientCookie cookie2 = new BasicClientCookie("ep.store_name_241", "S%26%23xe3%3Bo%20Paulo");
		cookie2.setDomain("busca.deliveryextra.com.br");
		cookie2.setPath("/");
		listCookies.add(cookie2);
		
		// Criando cookie simulando um usuário logado
		BasicClientCookie cookie3 = new BasicClientCookie("ep.currency_code_241", "BRL");
		cookie3.setDomain("busca.deliveryextra.com.br");
		cookie3.setPath("/");
		listCookies.add(cookie3);
		
		// Criando cookie simulando um usuário logado
		BasicClientCookie cookie4 = new BasicClientCookie("ep.language_code_241", "pt-BR");
		cookie4.setDomain("busca.deliveryextra.com.br");
		cookie4.setPath("/");
		listCookies.add(cookie4);
		
		return listCookies;
	}

	@Override
	protected boolean hasNextPage() {
		Element page = this.currentDoc.select(".sli_next_page").first();

		// se elemento page obtiver algum resultado
		if (page != null)
			return true;

		return false;
	}

	@Override
	protected void setTotalProducts() {
		Element totalElement = this.currentDoc.select(".sli_result_set_after_value").first();

		try {
			if (totalElement != null)
				this.totalProducts = Integer.parseInt(totalElement.text());
		} catch (Exception e) {
			this.logError(e.getMessage());
		}

		this.log("Total da busca: " + this.totalProducts);
	}
}
