package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class MexicoSorianaCrawler extends CrawlerRankingKeywords{
	
	public MexicoSorianaCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 20;
			
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		// primeira página começa em 0 e assim vai.
		String url = "https://www.soriana.com/soriana/es/search?q="+ this.keywordEncoded +"&page="+(this.currentPage-1);
		this.log("Link onde são feitos os crawlers: "+url);	
			
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		Elements products =  this.currentDoc.select(".productGridItem");

		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalBusca == 0) {
				setTotalBusca();
			}
			
			for(Element e: products) {

				// InternalPid
				String internalPid = crawlInternalPid(e);
				
				// InternalId
				String internalId = crawlInternalId(e);
				
				// Url do produto
				String productUrl = crawlProductUrl(e);
				
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
		Element page = this.currentDoc.select("li.last").first();
		
		//se  elemeno page não obtiver nenhum resultado
		if( page == null ) {
			//não tem próxima página
			return false;
		}
		
		return true;
		
	}
	
	@Override
	protected void setTotalBusca() {
		Element totalElement = this.currentDoc.select(".results h1").first();
		
		if(totalElement != null) { 	
			try {
				this.totalBusca = Integer.parseInt(totalElement.text().replaceAll(this.keywordWithoutAccents, "").replaceAll("[^0-9]", "").trim());
			} catch(Exception e) {
				this.logError(e.getMessage());
			}
			
			this.log("Total da busca: "+this.totalBusca);
		}
	}
	
	private String crawlInternalId(Element e){
		String internalId = null;
		Element id = e.select("input[name=productCodePost]").first();
		
		if(id != null){ 
			internalId = id.attr("value");
		}
		
		return internalId;
	}
	
	private String crawlInternalPid(Element e){
		String internalPid = null;
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = null;
		Element urlElement 	= e.select(" > a").first();
		
		if(urlElement != null){
			urlProduct = urlElement.attr("href");
			
			if(!urlProduct.startsWith("https://www.soriana.com")){
				urlProduct = "https://www.soriana.com" + urlProduct;
			}
		}
		
		return urlProduct;
	}
}
