package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilLeroymerlinCrawler extends CrawlerRankingKeywords {

	public BrasilLeroymerlinCrawler(Session session) {
		super(session);
	}

	private boolean isCategory;
	
	@Override
	public void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 40;

		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "http://www.leroymerlin.com.br/search?term="+ this.keywordEncoded +"&page="+ this.currentPage;
		
		if(this.currentPage > 1 && isCategory) url = this.currentDoc.baseUri().replaceAll("&page="+(this.currentPage-1), "") +"&page="+ this.currentPage;		
		
		this.log("Link onde são feitos os crawlers: "+url);			
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		Elements products = this.currentDoc.select("a.row.product-link");
		
		if(this.currentPage == 1){
			if(!this.currentDoc.baseUri().equals(url)){
				isCategory = true;
			} else {
				isCategory = false;
			}
		}
		
		if(isCategory) products = this.currentDoc.select("a.m-product-thumb");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			for(Element e: products) {
				String internalId;
				String internalPid;
				String productUrl;
				
				if(!isCategory){
					//seta o id da classe pai com o id retirado do elements
					String[] tokens = e.attr("href").split("_");
					internalPid 	= null;
					internalId 		= tokens[tokens.length-1];
					
					//monta a url
					productUrl = e.attr("href");
				} else {
					
					//monta a url
					int x = e.attr("href").indexOf("?");
					productUrl = e.attr("href").substring(0, x);
					
					//seta o id da classe pai com o id retirado do elements
					String[] tokens = productUrl.split("_");
					internalPid 	= null;
					internalId 		= tokens[tokens.length-1];
					
				}
				
				saveDataProduct(internalId, internalPid, productUrl);
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
				if(this.arrayProducts.size() == productsLimit) break;
				
			}
		}
		else
		{
			setTotalProducts();
			this.result = false;
			this.log("Keyword sem resultado!");
		}

		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
		if(!(hasNextPage())) setTotalProducts();
	}
	
	@Override
	protected boolean hasNextPage() {
		
		if(!isCategory){
			
			Elements idKeyword = this.currentDoc.select("a.row.product-link");
			
			if(idKeyword.size() < this.pageSize){
				//não tem próxima página
				return false;
			} else {
				//tem próxima página
				return true;
			}
			
		} else {
			
			Elements idCategory = this.currentDoc.select("a.m-product-thumb");
			
			if(idCategory.size() < this.pageSize){
				//não tem próxima página
				return false;
			} else {
				//tem próxima página
				return true;
			}
			
		}
	}

}
