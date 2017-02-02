package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class SaopauloAmericanasCrawler extends CrawlerRankingKeywords{

	public SaopauloAmericanasCrawler(Session session) {
		super(session);
	}
	
	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 24;
	
		this.log("Página "+ this.currentPage);
		
		String keyword = location.replaceAll(" ", "+");
		
		//monta a url com a keyword e a página
		String url = "http://www.americanas.com.br/busca/?conteudo="+ keyword +"&limite=24&offset=" + this.arrayProducts.size();
		this.log("Link onde são feitos os crawlers: "+url);
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url, null);
		
		Elements products =  this.currentDoc.select(".card-product");		
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1)	{
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalBusca == 0){
				setTotalBusca();
			}
			
			for(Element e: products) {				
				// Url do produto
				String productUrl = crawlProductUrl(e);
				
				// InternalPid
				String internalPid = crawlInternalPid(productUrl);
				
				// InternalId
				String internalId = crawlInternalId(e);
				
				saveDataProduct(internalId, internalPid,productUrl);
		
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
				if(this.arrayProducts.size() == productsLimit){
					break;
				}
			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
		}

		this.log("Finalizando Crawler de produtos da página "+ this.currentPage +" - até agora "+this.arrayProducts.size()+" produtos crawleados");
		
	}

	@Override
	protected boolean hasNextPage() {
		Element page = this.currentDoc.select(".card.card-pagination .pagination-icon:not(.pagination-icon-previous)").first();;
		
		//se  elemeno page obtiver algum resultado
		if(page != null) {
			//tem próxima página
			return true;
		}
		
		return false;
	}
	
	@Override
	protected void setTotalBusca() {
		Element totalElement = this.currentDoc.select(".display-sm-inline-block > span").first();
		
		if(totalElement != null) {
			try {
				
				String token = totalElement.ownText().replaceAll("[^0-9]", "").trim();
				
				this.totalBusca = Integer.parseInt(token);
			} catch(Exception e) {
				this.logError(CommonMethods.getStackTraceString(e));
			}
			
			this.log("Total da busca: "+this.totalBusca);
		}
	}
	
	private String crawlInternalId(Element e){		
		return null;
	}
	
	private String crawlInternalPid(String url){
		String internalPid = null;
		
		if(url.contains("produto/")){
			String[] tokens = url.split("/");
			internalPid = tokens[tokens.length-1];
		}
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = null;
		Element urlElement 	= e.select("> a").first();
		
		if(urlElement != null){
			urlProduct = urlElement.attr("href");
			
			if(urlProduct.contains("?")){
				urlProduct = urlProduct.split("\\?")[0];
			}
			
			if(!urlProduct.startsWith("http://www.americanas.com.br")) {
				urlProduct = "http://www.americanas.com.br" + urlProduct;
			}
		}
		
		return urlProduct;
	}
	
}
