package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilLojastaqiCrawler extends CrawlerRankingKeywords{

	public BrasilLojastaqiCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 24;
			
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "http://busca.taqi.com.br/busca?q="+ this.keywordEncoded +"&page="+ this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);	
			
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		Elements products =  this.currentDoc.select(".box_item_default .dados_default");

		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {			
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) {
				setTotalProducts();
			}
			for(Element e : products) {
				
				// InternalPid
				String internalPid = crawlInternalPid();
				
				// Url do produto
				String productUrl = crawlProductUrl(e);
				
				// InternalId
				String internalId = crawlInternalId(productUrl);
				
				saveDataProduct(internalId, internalPid, productUrl);
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
				if(this.arrayProducts.size() == productsLimit) {
					break;
				}
				
			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
		}
	
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
	}

	@Override
	protected boolean hasNextPage() {
		//se  elemeno page obtiver algum resultado
		if(this.arrayProducts.size() < this.totalProducts){
			//tem próxima página
			return true;
		} 
			
		return false;
	}
	
	@Override
	protected void setTotalProducts()	{
		Element totalElement = this.currentDoc.select(".neemu-total-products-container").first();
		
		if(totalElement != null) { 	
			try	{				
				this.totalProducts = Integer.parseInt(totalElement.text().replaceAll("[^0-9]", "").trim());
			} catch(Exception e) {
				this.logError(CommonMethods.getStackTraceString(e));
			}
			
			this.log("Total da busca: "+this.totalProducts);
		}
	}
	
	private String crawlInternalId(String url) {			
		if(url != null) {
			String[] tokens = url.split("/");
			return tokens[tokens.length-1].trim();
		} 
		
		return null;
	}
	
	private String crawlInternalPid() {
		return null;
	}
	
	private String crawlProductUrl(Element e) {
		String productUrl = null;
		Element url = e.select("> a:not([class])").first();	
		
		if(url != null) {
			productUrl = url.attr("href");
			
			if(!productUrl.startsWith("http")) {
				productUrl = "http:" + productUrl;
			}
		}
		
		return productUrl;
	}
}
