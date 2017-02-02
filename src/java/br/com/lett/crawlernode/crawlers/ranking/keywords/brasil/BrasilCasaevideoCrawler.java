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

	private List<Cookie> cookies = new ArrayList<Cookie>();

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 12;

		this.log("Página "+ this.currentPage);

		String key = this.location.replaceAll(" ", "%20");

		//monta a url com a keyword e a página
		String url = "http://www.casaevideo.com.br/webapp/wcs/stores/servlet/SearchDisplay?searchTerm="+ key 
				+"&pageSize=150&beginIndex="+this.arrayProducts.size();

		this.log("Link onde são feitos os crawlers: "+url);	
		
		
		if(this.currentPage == 1) this.cookies = this.getCookies(url);
		
		this.currentDoc = fetchDocument(url, cookies);

		Elements products =  this.currentDoc.select("div.product > div.product_info");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			setTotalBusca();

			for(Element e: products) {
				//seta o id com o seletor
				Element pid = e.select("> div.product_price").first();
				String[] tokens 	= pid.attr("id").split("_");
				String internalPid 	= tokens[tokens.length-1];
				String internalId 	= null;

				//monta a url
				String productUrl  = "http://www.casaevideo.com.br/webapp/wcs/stores/servlet/ProductDisplay?productId="+ internalPid;

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
		if(this.arrayProducts.size() < this.totalBusca){
			//tem próxima página
			return true;
		} else {
			//não tem próxima página
			return false;
		}
	}

	@Override
	protected void setTotalBusca() {
		Element totalElement = this.currentDoc.select("span#searchTotalCount").first();

		if(totalElement != null) { 	
			try {				
				this.totalBusca = Integer.parseInt(totalElement.text().replaceAll("[^0-9]", "").trim());
			} catch(Exception e) {
				this.logError(e.getMessage());
			}

			this.log("Total da busca: "+this.totalBusca);
		}
	}
	
	private List<Cookie> getCookies(String url){
		List<Cookie> cookiesList = new ArrayList<Cookie>();
		
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
		
//		// Criando cookie
//		BasicClientCookie cookieUser = new BasicClientCookie("WC_PERSISTENT", "wJJO5caQ2GjWEGkrQaY%2bqsAtA%2fY%3d%0a%3b2016%2d09%2d06+09%3a51%3a30%2e27%5f1473165479279%2d2101281%5f10152%5f%2d1002%2c%2d6%2cBRL%5f10152");
//		cookieUser.setDomain("www.casaevideo.com.br");
//		cookieUser.setPath("/");

	}
}
