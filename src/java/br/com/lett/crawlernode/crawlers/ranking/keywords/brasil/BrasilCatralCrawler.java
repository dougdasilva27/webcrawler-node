package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilCatralCrawler extends CrawlerRankingKeywords{

	public BrasilCatralCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 16;
	
		this.log("Página "+ this.currentPage);
		
		String keyword = this.keywordWithoutAccents.replaceAll(" ", "%20");
		
		//monta a url com a keyword e a página
		String url = "http://www.catral.com.br/"+ keyword +"?PS=50&PageNumber=" + this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products =  this.currentDoc.select(".prateleira li.product  > div > a[title]");
		Elements productsIds =  this.currentDoc.select(".prateleira li[id]");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {			
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) setTotalProducts();
			
			for(int i = 0; i < products.size(); i++) {
				Element e = products.get(i);
				
				if(i < productsIds.size()){
					// InternalPid
					String internalPid 	= crawlInternalPid(productsIds.get(i));
					
					// InternalId
					String internalId 	= crawlInternalId(e);
					
					// Url do produto
					String urlProduct = crawlProductUrl(e);
					
					saveDataProduct(internalId, internalPid, urlProduct);
					
					this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
					if(this.arrayProducts.size() == productsLimit) break;
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
	protected void setTotalProducts() {
		Element totalElement = this.currentDoc.select("span.resultado-busca-numero > span.value").first();
		
		if(totalElement != null) { 	
			try	{				
				this.totalProducts = Integer.parseInt(totalElement.text().trim());
			} catch(Exception e) {
				this.logError(e.getMessage());
			}
			
			this.log("Total da busca: "+this.totalProducts);
		}
	}
	
	private String crawlInternalId(Element e){
		String internalId = null;
		
		return internalId;
	}
	
	private String crawlInternalPid(Element e){
		String internalPid = null;
		String text = e.attr("id");
		
		if(text.contains("_")){
			internalPid = text.split("_")[1];
		}
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = e.attr("href");

		return urlProduct;
	}
}
