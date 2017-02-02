package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilUnicaarcondicionadoCrawler extends CrawlerRankingKeywords{

	public BrasilUnicaarcondicionadoCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 36;
			
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "http://www.unicaarcondicionado.com.br/catalogsearch/result/index/?q="+ this.keywordEncoded +"&p="+ this.currentPage; 
		
		this.log("Link onde são feitos os crawlers: "+url);	
			
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		Elements products =  this.currentDoc.select("ul.products-grid > li.item > span");		
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {			
			for(Element e: products) {
				//seta o id com o seletor
				Element idPrices 	= e.select("div.price-box span[id]").first();
				String[] tokens 	= idPrices.attr("id").split("-");
				String internalPid 	= null;
				String internalId 	= tokens[tokens.length-1];
				
				//monta a url
				Element eUrl 	= e.select("> a").first();
				String productUrl 	= eUrl.attr("href");
				
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
		if(!hasNextPage()) setTotalBusca();
	}

	@Override
	protected boolean hasNextPage() {
		Element page = this.currentDoc.select("a.next.i-next").first();
		
		//se  elemeno page não obtiver nenhum resultado
		if(page == null){
			//não tem próxima página
			return false;
		} else {
			//tem próxima página
			return true;
			
		}
	}
}
