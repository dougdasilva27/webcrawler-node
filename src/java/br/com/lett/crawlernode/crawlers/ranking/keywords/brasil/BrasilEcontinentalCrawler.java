package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilEcontinentalCrawler extends CrawlerRankingKeywords{

	public BrasilEcontinentalCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 48;
	
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "https://www.econtinental.com.br/produtos?busca="+ this.keywordEncoded +"&pagina="+ this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products =  this.currentDoc.select("li.product");
		
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
			this.setTotalBusca();
			this.result = false;
			this.log("Keyword sem resultado!");
		}
	
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
	}

	@Override
	protected boolean hasNextPage() {
		Element nextPage = this.currentDoc.select(".item.last").first();
		
		if(nextPage != null){
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
		Element pid = e.select("[name=id_produto]").first();
		
		if(pid != null){
			internalPid = pid.attr("value");
		}
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = null;
		Element urlElement = e.select("> div > a[title]").first();
		
		if(urlElement != null){
			urlProduct = urlElement.attr("href");
			
			if(!urlProduct.contains("econtinental")){
				urlProduct = "https://www.econtinental.com.br" + urlProduct;
			}
		}
		
		return urlProduct;
	}
}
