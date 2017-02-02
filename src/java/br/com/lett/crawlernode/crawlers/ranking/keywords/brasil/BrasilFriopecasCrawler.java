package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilFriopecasCrawler extends CrawlerRankingKeywords {

	public BrasilFriopecasCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 12;

		this.log("Página "+ this.currentPage);
		
		String key = this.location.replaceAll(" ", "%20");
			
		
		//monta a url com a keyword e a página
		String url = "http://www.friopecas.com.br/buscapagina?"
					+"sl=122bd37a-dd64-4686-a501-c323397f54ee&PS=50&cc=10&sm=0&PageNumber="+ this.currentPage +"&ft="+ key;
		
		this.log("Link onde são feitos os crawlers: "+url);			
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
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
					Element eUrl = products.get(i).select("a.p-name").first();
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
		Elements ids = this.currentDoc.select("div.shelf__product-item");
		
		if(ids.size() < 50){
			return false;
		} else {
			return true;
		}
	}

}
