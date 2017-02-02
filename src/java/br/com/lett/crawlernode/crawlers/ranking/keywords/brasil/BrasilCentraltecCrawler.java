package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilCentraltecCrawler extends CrawlerRankingKeywords{

	public BrasilCentraltecCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 12;
	
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "http://www.centraltec.com.br/busca?p="+ this.keywordEncoded +"&page="+ this.currentPage +"&top=36";
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products =  this.currentDoc.select(".vitrine_nome > a");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {			
			for(Element e : products) {
				
				// InternalPid
				String internalPid 	= crawlInternalPid(e);
				
				// InternalId
				String internalId 	= crawlInternalId(e);
				
				// Url do produto
				String urlProduct = crawlProductUrl(e);
				
				saveDataProduct(internalId, internalPid, urlProduct);
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
				if(this.arrayProducts.size() == productsLimit) break;
			}
		} else {
			setTotalBusca();
			this.result = false;
			this.log("Keyword sem resultado!");
		}
	
		if(!hasNextPage()) setTotalBusca();
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
	
	private String crawlInternalId(Element e){
		String internalId = null;
		
		return internalId;
	}
	
	private String crawlInternalPid(Element e){
		String internalPid = null;
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = null;
		String tempUrl = e.attr("href");
		
		if(tempUrl.contains("centraltec.com.br")){
			urlProduct = tempUrl;
		} else {
			if(tempUrl.startsWith("/")){
				urlProduct = "http://www.centraltec.com.br" + tempUrl;
			} else {
				urlProduct = "http://www.centraltec.com.br/" + tempUrl;
			}
		}
		
		return urlProduct;
	}
}
