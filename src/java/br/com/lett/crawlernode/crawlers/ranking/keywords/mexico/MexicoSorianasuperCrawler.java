package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class MexicoSorianasuperCrawler extends CrawlerRankingKeywords{
	
	public MexicoSorianasuperCrawler(Session session) {
		super(session);
	}

	private List<Cookie> cookies = new ArrayList<>();
	
	@Override
	protected void processBeforeFetch() {
		if (this.cookies.isEmpty()) {
			Map<String,String> cookiesMap = DataFetcher.fetchCookies(session, "http://www.sorianadomicilio.com", cookies, 1);
			
			for(Entry<String, String> cookieEntry : cookiesMap.entrySet()) {
				if(cookieEntry.getKey().equals("ASP.NET_SessionId")) {
					BasicClientCookie cookie = new BasicClientCookie(cookieEntry.getKey(), cookieEntry.getValue());
					cookie.setDomain("www.sorianadomicilio.com");
					cookie.setPath("/");
					
					this.cookies.add(cookie);
				}
			}
			
			BasicClientCookie cookie = new BasicClientCookie("NombreTiendaProduccion", "NombreTienda=San Pedro");
			cookie.setDomain("www.sorianadomicilio.com");
			cookie.setPath("/");
			this.cookies.add(cookie);
			
			BasicClientCookie cookie2 = new BasicClientCookie("NumTdaProduccion", "NumTda=14");
			cookie2.setDomain("www.sorianadomicilio.com");
			cookie2.setPath("/");
			this.cookies.add(cookie2);
		}
	}
	
	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 25;
			
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		// primeira página começa em 0 e assim vai.
		String url = "http://www.sorianadomicilio.com/site/default.aspx?p=10234&Txt_Bsq_Descripcion="
				+ this.keywordEncoded +"&Marca=0&Linea=0&Paginacion="+ this.currentPage +"&nuor=0";
		
		this.log("Link onde são feitos os crawlers: "+url);	
			
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url, this.cookies);
		
		Elements products =  this.currentDoc.select("ul .carLi .artresultadoh");

		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalBusca == 0) {
				setTotalBusca();
			}
			
			for(Element e: products) {

				// InternalPid
				String internalPid = crawlInternalPid(e);
				
				// InternalId
				String internalId = crawlInternalId(e);
				
				// Url do produto
				String productUrl = crawlProductUrl(e);
				
				saveDataProduct(internalId, internalPid, productUrl);
				
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
		if(this.arrayProducts.size() < this.totalBusca) {
			return true;
		}
		
		return false;
	}
	
	@Override
	protected void setTotalBusca() {
		Element totalElement = this.currentDoc.select(".txt_g_12px").first();
		
		if(totalElement != null) { 	
			try {
				this.totalBusca = Integer.parseInt(totalElement.ownText().trim().split(" ")[0]);
			} catch(Exception e) {
				this.logError(CommonMethods.getStackTrace(e));
			}
			
			this.log("Total da busca: "+this.totalBusca);
		}
	}
	
	private String crawlInternalId(Element e){
		String internalId = null;
		Element id = e.select("input[type=HIDDEN][name=s]").first();
		
		if(id != null){ 
			internalId = id.val();
		}
		
		return internalId;
	}
	
	private String crawlInternalPid(Element e){
		String internalPid = null;
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = null;
		Element urlElement 	= e.select(".artDi3 a").first();
		
		if(urlElement != null){
			urlProduct = urlElement.attr("href");
			
			if(!urlProduct.startsWith("http://www.sorianadomicilio.com")){
				urlProduct = "http://www.sorianadomicilio.com/site/default.aspx" + urlProduct;
			}
		}
		
		return urlProduct;
	}
}
