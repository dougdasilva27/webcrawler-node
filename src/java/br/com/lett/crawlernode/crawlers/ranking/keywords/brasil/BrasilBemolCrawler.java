package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilBemolCrawler extends CrawlerRankingKeywords{
	
	public BrasilBemolCrawler(Session session) {
		super(session);
	}

	private String redirectUrl;
	
	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 20;
	
		this.log("Página "+ this.currentPage);
		
		String originalUrl = "https://www.bemol.com.br/webapp/wcs/stores/servlet/SearchDisplay?storeId=10001&catalogId=10001&langId=-6"
				+ "&pageSize=50&beginIndex="+ this.arrayProducts.size() +"&sType=SimpleSearch&showResultsPage=true&searchTerm="+ this.keywordEncoded;
		
		
		// Nesse market uma keyword pode redirecionar para uma categoria
		// com isso pegamos a url redirecionada e acrescentamos a página.
		if(this.currentPage ==1){			
			this.log("Link onde são feitos os crawlers: "+originalUrl);	
			
			//chama função de pegar a url
			this.currentDoc = fetchDocument(originalUrl);
			
			this.redirectUrl = this.currentDoc.baseUri();
		} else {
			String url;
			
			if(this.redirectUrl.contains("SearchDisplay")){
				url = originalUrl;
			} else {
				url = this.redirectUrl + "?beginIndex=" + this.arrayProducts.size();
			}
			
			//chama função de pegar a url
			this.currentDoc = fetchDocument(url);
		}
		

		Elements products =  this.currentDoc.select(".item-div h3 > a");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {			
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalBusca == 0) setTotalBusca();
			for(Element e : products) {
				
				// InternalId
				String internalId 	= crawlInternalId(e);
				
				// InternalPid
				String internalPid 	= crawlInternalPid(internalId);
				
				// Url do produto
				String urlProduct 	= crawlProductUrl(e);
				
				saveDataProduct(internalId, internalPid, urlProduct);
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
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
		} 
			
		return false;
		
	}
	
	@Override
	protected void setTotalBusca()	{
		Element totalElement = this.currentDoc.select("#pagIndexTitleBottom").first();
		
		if(totalElement != null) { 	
			try	{				
				String text = totalElement.text();
				int x = text.indexOf("de");
				
				this.totalBusca = Integer.parseInt(text.substring(x).replaceAll("[^0-9]", "").trim());
			} catch(Exception e) {
				this.logError(e.getMessage());
			}
			
			this.log("Total da busca: "+this.totalBusca);
		}
	}
	
	private String crawlInternalId(Element e){
		String internalId = null;
		String text = e.attr("id");
		
		if(text.contains("_")){
			internalId = text.split("_")[2];
		}
		
		return internalId;
	}
	
	private String crawlInternalPid(String internalId){
		String internalPid = internalId;
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = e.attr("href");
		
		return urlProduct;
	}
}
