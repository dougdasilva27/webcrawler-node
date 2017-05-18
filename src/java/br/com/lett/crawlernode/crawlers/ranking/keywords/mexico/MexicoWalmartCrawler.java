package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class MexicoWalmartCrawler extends CrawlerRankingKeywords{
	public MexicoWalmartCrawler(Session session) {
		super(session);
	}

	/**
	 * In this crawle i had to use GSON of google because in api of walmart has duplicate keys in json
	 * JSONObject not accept this
	 */
	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 25;
			
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		// primeira página começa em 0 e assim vai.
		String url = "https://www.walmart.com.mx/WebControls/hlSearch.ashx?search="+ this.keywordEncoded +"&start=" + this.arrayProducts.size();
		this.log("Link onde são feitos os crawlers: "+url);	
			
		//chama função de pegar a url
		JsonObject jsonSearch = fetchJsonObjectGoogle(url);
		
		if(jsonSearch.has("docs")){
			JsonArray products = jsonSearch.getAsJsonArray("docs");
			
			//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
			if(products.size() >= 1) {
				//se o total de busca não foi setado ainda, chama a função para setar
				if(this.totalProducts == 0) this.setTotalBusca(jsonSearch);
				
				for(int i = 0; i < products.size(); i++) {
					JsonObject product = products.get(i).getAsJsonObject();
					
					// InternalPid
					String internalPid = crawlInternalPid();
					
					// InternalId
					String internalId = crawlInternalId(product);
					
					// Url do produto
					String productUrl = crawlProductUrl(product);
					
					saveDataProduct(internalId, internalPid, productUrl);
					
					this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
					if(this.arrayProducts.size() == productsLimit) break;
				}
			} else {
				this.result = false;
				this.log("Keyword sem resultado!");
			}
			
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
		}

		
			
		

		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
	}

	@Override
	protected boolean hasNextPage() {
		// se não atingiu o total da busca ainda tem mais páginas.
		if(this.arrayProducts.size() < this.totalProducts){
			return true;
		}
		
		return false;
		
	}
	
	protected void setTotalBusca(JsonObject products) {
		
		if(products.has("numFound")) { 	
			try {
				this.totalProducts = Integer.parseInt(products.get("numFound").getAsString());
			} catch(Exception e) {
				this.logError(e.getMessage());
			}
			
			this.log("Total da busca: "+this.totalProducts);
		}
	}
	
	private String crawlInternalId(JsonObject product){
		String internalId = null;
		
		if(product.has("upc")) {
			internalId = product.get("upc").getAsString();
		}
		
		return internalId;
	}
	
	private String crawlInternalPid(){
		String internalPid = null;
		
		return internalPid;
	}
	
	private String crawlProductUrl(JsonObject product){
		String urlProduct = null;
		
		if(product.has("u")){
			urlProduct = product.get("u").getAsString();
			
			if(!urlProduct.startsWith("https://www.walmart.com.mx")){
				urlProduct = "https://www.walmart.com.mx" + urlProduct;
			}
		}
		
		return urlProduct;
	}
}
