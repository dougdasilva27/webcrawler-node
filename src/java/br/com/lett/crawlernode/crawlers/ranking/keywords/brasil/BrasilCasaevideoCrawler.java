package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilCasaevideoCrawler extends CrawlerRankingKeywords {

	public BrasilCasaevideoCrawler(Session session) {
		super(session);
	}
	
	@Override
	protected void processBeforeFetch() {
		this.cookies = this.getCookies("http://www.casaevideo.com.br");
	}

	private List<Cookie> cookies = new ArrayList<>();

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 16;

		this.log("Página "+ this.currentPage);

		String keyword = this.keywordWithoutAccents.replaceAll(" ", "%20");

		//monta a url com a keyword e a página
		String url = "http://www.casaevideo.com.br/" + keyword + "?&utmi_p=_&utmi_pc=BuscaFullText&utmi_cp="+ keyword +"&PageNumber=" + this.currentPage;

		this.log("Link onde são feitos os crawlers: "+url);	
		
		this.currentDoc = fetchDocument(url, cookies);

		Elements products =  this.currentDoc.select(".prateleira ul li[layout] > span.box-item");
		Elements productsPid =  this.currentDoc.select(".prateleira ul li[id].helperComplement");
		
		Element emptySearch = this.currentDoc.select("#busca-vazia-extra-middle").first();
		
		int count = 0;
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1 && emptySearch == null) {
			for(Element e: products) {
				//seta o id com o seletor
				String internalPid 	= crawlInternalPid(productsPid.get(count));
				String internalId 	= crawlInternalId(e);

				//monta a url
				String productUrl = crawlProductUrl(e); 

				saveDataProduct(internalId, internalPid, productUrl);
				count++;
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
				if(this.arrayProducts.size() == productsLimit) {
					break;
				}
			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
		}
		
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
	}

	@Override
	protected boolean hasNextPage() {
		Elements lastPage = this.currentDoc.select(".prateleira ul li[id].helperComplement");
		
		if(lastPage.size() < this.pageSize) {
			return false;
		}
		
		return true;
	}
	
	private String crawlInternalId(Element e){
		String internalId = null;
		Element id = e.select(".buy-button-asynchronous-product-id").first();
		
		if(id != null) {
			internalId = id.val();
		}
		
		return internalId;
	}
	
	private String crawlInternalPid(Element e) {
		String internalPid = e.attr("id").split("_")[1];
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String productUrl = null;
		
		Element url = e.select("> a[title]").first();
			
		if(url != null){
			productUrl = url.attr("href");
		}
		
		
		return productUrl;
	}
	
	private List<Cookie> getCookies(String url){
		List<Cookie> cookiesList = new ArrayList<>();
		
		Map<String,String> mapCookies = fetchCookies("http://www.casaevideo.com.br/webapp/wcs/stores/servlet/pt/auroraesite");
		
		// Criando cookie
		BasicClientCookie cookieWSE = new BasicClientCookie("WC_SESSION_ESTABLISHED", "true");
		cookieWSE.setDomain("www.casaevideo.com.br");
		cookieWSE.setPath("/");
		cookiesList.add(cookieWSE);

		for(String key : mapCookies.keySet()){
			// Criando cookie
			BasicClientCookie cookieSession = new BasicClientCookie(key, mapCookies.get(key));
			cookieSession.setDomain("www.casaevideo.com.br");
			cookieSession.setPath("/");
			cookiesList.add(cookieSession);
		}
		
		
		return cookiesList;
	}
}
