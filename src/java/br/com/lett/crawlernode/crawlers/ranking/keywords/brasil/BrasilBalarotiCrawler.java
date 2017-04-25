package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilBalarotiCrawler extends CrawlerRankingKeywords{

	public BrasilBalarotiCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 32;
	
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "http://www.balaroti.com.br/busca/"+ this.keywordEncoded +"///pag"+ this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products =  this.currentDoc.select(".product-list-block h2 > a");	
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {			

			for(Element e : products) {
				
				// Url do produto
				String productUrl = crawlProductUrl(e);
				
				// InternalPid
				String internalPid 	= crawlInternalPid(productUrl);
				
				// InternalId
				String internalId 	= crawlInternalId(e);
				
				saveDataProduct(internalId, internalPid, productUrl);
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
				if(this.arrayProducts.size() == productsLimit) break;
				
			}
		} else {
			setTotalBusca();
			this.result = false;
			this.log("Keyword sem resultado!");
		}
	
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
	}

	@Override
	protected boolean hasNextPage() {
		Element seta = this.currentDoc.select(".seta img").last();
		
		if(seta != null){
			String imgSeta = seta.attr("src");
			
			if(imgSeta.contains("go.png")){
				return true;
			}
		}
		
		return false;
	}
	
	private String crawlInternalId(Element e){
		String internalId = null;
				
		return internalId;
	}
	
	private String crawlInternalPid(String url){
		String internalPid = null;
		
		if(url.contains("?")){
			url = url.split("?")[0];
		}
		
		String[] tokens = url.split("/");
		internalPid = tokens[tokens.length-1];
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = e.attr("href");
		
		return urlProduct;
	}
}
