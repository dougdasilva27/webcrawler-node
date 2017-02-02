package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilPoloarCrawler extends CrawlerRankingKeywords {
	
	public BrasilPoloarCrawler(Session session) {
		super(session);
	}

	private boolean isCategory;
	
	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 12;

		this.log("Página "+ this.currentPage);			
		
		String key = this.location.replaceAll(" ", "%20");
		
		//monta a url com a keyword e a página
		String url = "http://www.poloar.com.br/"+ key;
		
		if(this.currentPage > 1){
			if(!this.isCategory){
				url = "http://www.poloar.com.br/buscapagina?ft="+ key +"&PS=12"
					+ "&sl=19ccd66b-b568-43cb-a106-b52f9796f5cd&cc=3&sm=0&PageNumber="+ this.currentPage;
			}
		}
		
		this.log("Link onde são feitos os crawlers: "+url);			
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		Element elementIsCategory = this.currentDoc.select("div.title-category > h2").first();
		
		if(this.currentPage == 1){
			if(elementIsCategory.text().equals("Resultado da Busca:")) 	this.isCategory = false;
			else 														this.isCategory = true;
		}
		
		Elements products =  this.currentDoc.select("div.prateleira > ul > li");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {			
			for(int i = 0; i < products.size(); i++) {
				if(products.get(i).hasAttr("layout")){
					//seta o id com o seletor
					String[] tokens 	= products.get(i+1).attr("id").split("_");
					String internalPid 	= null;
					String internalId 	= tokens[tokens.length-1];
					
					//monta a url
					Element eUrl = products.get(i).select("b.product-name > a").first();
					String productUrl  = eUrl.attr("href");
					
					saveDataProduct(internalId, internalPid, productUrl);
					
					this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
					if(this.arrayProducts.size() == productsLimit) break;
				}
			}
		} else {
			setTotalBusca();
			this.result = false;
			this.log("Keyword sem resultado!");
		}

		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
		if(!(hasNextPage())) setTotalBusca();
	}
	
	@Override
	protected boolean hasNextPage() {
		Elements ids = this.currentDoc.select("div.prateleira > ul > li[layout]");
		
		if(this.currentPage > 1){
			if(ids.size() < 12){
				return false;
			} else {
				return true;
			}
		} else {
			if(this.isCategory){
				return false;
			} else {
				if(ids.size() < 12){
					return false;
				} else {
					return true;
				}
			}
		}
	}
}
