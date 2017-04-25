package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.Select;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class ColombiaCarullaCrawler extends CrawlerRankingKeywords{

	public ColombiaCarullaCrawler(Session session) {
		super(session);
	}

	private List<Cookie> cookies = new ArrayList<>();
	
	@Override
	protected void processBeforeFetch() {
		if(this.cookies.isEmpty()){
			this.webdriver = startWebDriver("http://www.carulla.com/");
			
			try {
				new Select( this.webdriver.driver.findElement(By.id("ddlSelectCity"))).selectByVisibleText("Bogotá");
			    this.webdriver.driver.findElement(By.cssSelector("option[value=\"BG\"]")).click();
			    this.webdriver.driver.findElement(By.linkText("Continuar")).click();
			    
			    Set<org.openqa.selenium.Cookie> cookiesSelenium = this.webdriver.driver.manage().getCookies();
			    
			    for(org.openqa.selenium.Cookie c : cookiesSelenium){
			    	if(!c.getName().startsWith("x-")){
				    	BasicClientCookie cookie = new BasicClientCookie(c.getName(), c.getValue());
						cookie.setDomain(c.getDomain());
						cookie.setPath(c.getPath());
						this.cookies.add(cookie);
			    	}
			    }
			} catch(Exception e) {
				this.logError(CommonMethods.getStackTraceString(e));
			}
		}
	}
	
	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 20;
	
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "http://www.carulla.com/browse?No="+ this.arrayProducts.size() +"&Nrpp=80&Ntt=" + this.keywordEncoded;
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar o html
		this.currentDoc = fetchDocument(url, this.cookies);

		Elements products = this.currentDoc.select(".search.smallProduct");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {			
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalBusca == 0) setTotalBusca();
			for(Element e : products) {		
				// InternalId
				String internalId = crawlInternalId(e);
				
				// Url do produto
				String productUrl = crawlProductUrl(e);
				
				// InternalPid
				String internalPid = crawlInternalPid(productUrl);
				
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
	protected boolean hasNextPage() {
		//se  elemeno page obtiver algum resultado
		if(this.arrayProducts.size() < this.totalBusca){
			//tem próxima página
			return true;
		} 
			
		return false;
	}
	
	@Override
	protected void setTotalBusca()	{
		Element totalElement = this.currentDoc.select(".plpPaginationTop .pull-left").first();
		
		if(totalElement != null) { 	
			try	{
				String text = totalElement.ownText().toLowerCase().trim();
				int x = text.indexOf("de")+2;
				
				this.totalBusca = Integer.parseInt(text.substring(x).replaceAll("[^0-9]", ""));
			} catch(Exception e) {
				this.logError(e.getMessage());
			}
			
			this.log("Total da busca: "+this.totalBusca);
		}
	}
	
	private String crawlInternalId(Element e){
		String internalId = e.attr("data-skuId");
		
		return internalId;
	}
	
	private String crawlInternalPid(String url){
		String internalPid = null;
		
		String[] tokens = url.split("/");
		internalPid = tokens[tokens.length-2].trim();
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String productUrl = null;
		Element eUrl = e.select(".productBrand a").first();
		
		if(eUrl != null) {
			productUrl = "http://www.carulla.com" + eUrl.attr("href");
		}
		
		return productUrl;
	}
}
