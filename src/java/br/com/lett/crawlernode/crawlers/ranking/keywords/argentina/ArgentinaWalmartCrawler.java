package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.cookie.Cookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class ArgentinaWalmartCrawler extends CrawlerRankingKeywords{

	public ArgentinaWalmartCrawler(Session session) {
		super(session);
	}

	private List<Cookie> cookies = new ArrayList<>();

	@Override
	protected void extractProductsFromCurrentPage() {
		this.log("Página "+ this.currentPage);
		
		//número de produtos por página do market
		this.pageSize = 12;
		
		String keyword = this.keywordWithoutAccents.replaceAll(" ", "%20");
		
		//monta a url com a keyword e a página
		String url = "http://www.walmart.com.ar/"+keyword+"?PageNumber="+this.currentPage+"&sc=15";
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url, cookies);

		Elements products =  this.currentDoc.select("div div.prateleira > ul > li[layout]");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) setTotalProducts();
			
			for(Element e: products) {
				// InternalPid
				String internalPid 	= crawlInternalPid(e);
				
				// InternalId
				String internalId 	= crawlInternalId(e);
				
				// Url do produto
				String productUrl = crawlProductUrl(e);
				
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
		if(arrayProducts.size() < this.totalProducts){
			return true;
		}
		
		return false;
	}
	
	@Override
	protected void setTotalProducts() {
		Element totalElement = this.currentDoc.select("span.resultado-busca-numero span.value").first();
		
		try {
			if(totalElement != null) {
				this.totalProducts = Integer.parseInt(totalElement.text());
			}
		} catch(Exception e) {
			this.logError(CommonMethods.getStackTrace(e));
		}
		
		this.log("Total da busca: "+this.totalProducts);
	}
	
	private String crawlInternalId(Element e){
		String internalId = null;
		
		return internalId;
	}
	
	private String crawlInternalPid(Element e){
		String internalPid = null;
		Element pidElement = e.select("> div > input[name=productId]").first();
		
		if(pidElement != null){
			internalPid = pidElement.attr("value");
		}
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = null;
		Element urlElement = e.select("a.prateleira__name").first();
		
		if(urlElement != null){
			urlProduct = urlElement.attr("href")+"?sc=15";
		}
		
		return urlProduct;
	}
}
