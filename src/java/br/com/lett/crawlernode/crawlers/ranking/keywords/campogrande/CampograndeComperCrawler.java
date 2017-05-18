package br.com.lett.crawlernode.crawlers.ranking.keywords.campogrande;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class CampograndeComperCrawler extends CrawlerRankingKeywords{

	public CampograndeComperCrawler(Session session) {
		super(session);
	}

	private String cookieValue;
	
	private List<Cookie> cookies = new ArrayList<>();
	
	@Override
	protected void processBeforeFetch() {
		if(this.cookies.size() < 1){
			Map<String, String> cookies = fetchCookies("http://www.comperdelivery.com.br/");
		    
		    if(cookies.containsKey("ASP.NET_SessionId")){
			    this.cookieValue = cookies.get("ASP.NET_SessionId");
		    }
			
			BasicClientCookie cookieLoja = new BasicClientCookie("ASP.NET_SessionId", cookieValue);
	    	cookieLoja.setDomain("www.comperdelivery.com.br");
	    	cookieLoja.setPath("/");
	    	
	    	this.cookies.add(cookieLoja);
		}
	}
	
	@Override
	public void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 20;

		this.log("Página "+ this.currentPage);
		
		String keyword = this.keywordWithoutAccents.replaceAll(" ", "-");
		
		//monta a url com a keyword e a página
		String url  = "http://www.comperdelivery.com.br/busca/3/0/0/MaisVendidos/Decrescente/20/"+(this.currentPage)+"/"+keyword+".aspx";
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url, this.cookies);
	
		//this.currentDoc = DataFetcher.conectionUrlWithSingleCookie(url, "ASP.NET_SessionId", 5, 1, this.cookieValue, this.proxies, 20);
		this.log("Link onde são feitos os crawlers: "+ url);
			
		Elements products =  this.currentDoc.select("ul#listProduct > li .url[title]");
		
		//se essa página tiver resultado faça:	
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) setTotalProducts();
			
			for(Element e: products) {
				// Url do produto
				String productUrl 	= crawlProductUrl(e);
				
				// InternalPid
				String internalPid 	= crawlInternalPid(e);

				// InternalId
				String internalId 	= crawlInternalId(productUrl);

				saveDataProduct(internalId, internalPid, productUrl);

				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
				if(this.arrayProducts.size() == productsLimit) break;
			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
		}
			
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
			
	}
	
	@Override
	protected boolean hasNextPage(){
		Element page = this.currentDoc.select("li.set-next.off").first();
		
		//se  elemeno page obtiver algum resultado
		if(page != null) {
			//não tem próxima página
			return false;
		}
		
		return true;
		
	}
	
	@Override
	protected void setTotalProducts() {
		Element totalElement = this.currentDoc.select("p#divResultado strong").last();
		
		try {
			this.totalProducts = Integer.parseInt(totalElement.text());
		} catch(Exception e) {
			this.logError(e.getMessage());
		}
		
		this.log("Total da busca: "+this.totalProducts);
	}
	
	private String crawlInternalId(String url){
		String internalId = null;
		String[] tokens = url.split("-");
		
		internalId = tokens[tokens.length-1].replaceAll("[^0-9]", "");
		
		return internalId;
	}

	private String crawlInternalPid(Element e){
		String internalPid = null;
		Element pidElement = e.select("> img").first();
		
		if(pidElement != null){
			if(!pidElement.attr("src").contains("indisponivel")){
				String[] tokens = pidElement.attr("src").split("/");
				internalPid = tokens[tokens.length-2].replaceAll("[^0-9]", "").trim();
			}
		}
		
		return internalPid;
	}

	private String crawlProductUrl(Element e){
		String urlProduct = e.attr("href");

		return urlProduct;
	}
}
