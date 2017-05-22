package br.com.lett.crawlernode.crawlers.ranking.categories.campogrande;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingCategories;
import br.com.lett.crawlernode.util.CommonMethods;

public class CampograndeComperCrawler extends CrawlerRankingCategories {

	public CampograndeComperCrawler(Session session) {
		super(session);
	}

	private String cookieValue;
	private List<Cookie> cookies = new ArrayList<>();
	
	@Override
	protected void processBeforeFetch() {
		if(this.cookies.isEmpty()){
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
		
		//monta a url com a keyword e a página
		String url = getCategoryUrl();
		
		//chama função de pegar a url
		if(url != null) {
			this.currentDoc = fetchDocument(url, this.cookies);
		} else {
			this.currentDoc = new Document("");
		}
	
		this.log("Link onde são feitos os crawlers: "+ url);
			
		Elements products =  this.currentDoc.select("ul#listProduct > li .url[title]");
		
		//se essa página tiver resultado faça:	
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) {
				setTotalProducts();
			}
			
			for(Element e: products) {
				// Url do produto
				String productUrl 	= crawlProductUrl(e);
				
				// InternalPid
				String internalPid 	= crawlInternalPid(e);

				// InternalId
				String internalId 	= crawlInternalId(productUrl);

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
			this.logError(CommonMethods.getStackTrace(e));
		}
		
		this.log("Total da busca: "+this.totalProducts);
	}
	
	private String crawlInternalId(String url){
		String[] tokens = url.split("-");
		
		return tokens[tokens.length-1].replaceAll("[^0-9]", "");
		
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
		return e.attr("href");
	}
	
	private String getCategoryUrl() {
		String url = this.location;
		
		if(this.currentPage > 1) {
			String[] tokens = url.split("/");
			String idCategory = tokens[tokens.length-2].replaceAll("[^0-9]", "");
			
			if(!idCategory.isEmpty()) {
				url = "http://www.comperdelivery.com.br/categoria/1/"+ idCategory +"/0//MaisVendidos/Decrescente/20/"+ this.currentPage +"//0/0/.aspx";
			} else {
				return null;
			}
		}
		
		return url;
	}
}
