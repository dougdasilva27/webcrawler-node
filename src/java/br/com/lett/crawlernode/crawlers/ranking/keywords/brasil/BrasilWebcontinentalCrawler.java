package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilWebcontinentalCrawler extends CrawlerRankingKeywords {

	public BrasilWebcontinentalCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 125;
			
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		//https://www.webcontinental.com.br/ccstoreui/v1/search?Ntt=lg&No=0&Nrpp=24
		//&visitorId=1311WAa4I7-AzQ3Kjl1bI9L__SgrZLzmea82d1NdWqk3mqUCE76&visitId=-4a2b143f%3A15c11174874%3A132-129.80.155.73&language=pt_BR&searchType=simple
		String url = "http://www.webcontinental.com.br/busca/" + this.keywordEncoded + "/page="+this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);	
			
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		Elements products =  this.currentDoc.select("ul.listaProdutos > li");
		
		int count=0;
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {			
			for(Element e: products) {
				count++;
				//seta o id com o seletor
				String internalPid 	= null;
				String internalId 	= e.attr("data-prid");
				
				//monta a url
				Element eUrl = e.select("div.campo a").first();
				String productUrl = eUrl.attr("href");
				
				if(!productUrl.contains("webcontinental")){
					productUrl = "http://www.webcontinental.com.br" + productUrl;
				}
				
				saveDataProduct(internalId, internalPid, productUrl);
				
				this.log("InternalId do produto da "+count+" da página "+ this.currentPage+ ": " + internalId);
				if(this.arrayProducts.size() == productsLimit) break;
			}
		} else {
			setTotalBusca();
			this.result = false;
			this.log("Keyword sem resultado!");
		}

		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
		if(!hasNextPage()) setTotalBusca();
	}

	@Override
	protected boolean hasNextPage() {
		Element page = this.currentDoc.select("div.disabled span.next").first();
		
		//se  elemeno page não obtiver nenhum resultado
		if(page != null) {
			//não tem próxima página
			return false;
		}
		
		return true;
	}
}
