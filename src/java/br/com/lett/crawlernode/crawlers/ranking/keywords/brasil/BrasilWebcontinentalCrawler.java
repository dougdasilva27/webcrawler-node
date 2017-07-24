package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.json.JSONArray;
import org.json.JSONObject;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.ranking.RankingSession;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilWebcontinentalCrawler extends CrawlerRankingKeywords {

	public BrasilWebcontinentalCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 12;
			
		this.log("Página "+ this.currentPage);
		
		String keyword = this.keywordWithoutAccents.replaceAll(" ", "%20");
		
		//monta a url com a keyword e a página
		String url = "https://www.webcontinental.com.br/ccstoreui/v1/search?Ntt="+ keyword +"&No="+ this.arrayProducts.size() +"&Nrpp=24";
		this.log("Link onde são feitos os crawlers: "+url);	
		
		if(((RankingSession)session).mustTakeAScreenshot() && this.currentPage <= 2) {
			String printUrl = "https://www.webcontinental.com.br/searchresults?Ntt=ar"+ keyword +"&Nty=1&No=0&Nrpp=12&Rdm=856&searchType=simple&type=search&page=" + this.currentPage;
			takeAScreenshot(printUrl);
		}
		
		//chama função de pegar a url
		JSONObject apiSearch = fetchJSONObject(url);
		
		if(apiSearch.has("resultsList")) {
			JSONObject results = apiSearch.getJSONObject("resultsList");
			
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0){
				setTotalBusca(results);
			}
			
			JSONArray products = new JSONArray();
			
			if(results.has("records")) {
				products = results.getJSONArray("records");
			}

			for(int i = 0; i < products.length(); i++) {
				JSONObject product = products.getJSONObject(i);
				// InternalPid
				String internalPid 	= crawlInternalPid(product);

				// InternalId
				String internalId = crawlInternalId(product);

				//monta a url
				String productUrl = crawlProductUrl(product);

				saveDataProduct(internalId, internalPid, productUrl);

				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
				if(this.arrayProducts.size() == productsLimit) {
					break;
				}
			}
		} else {
			this.result = false;
			this.log("Keyword sem resultados!");
		}
		
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");

	}

	@Override
	protected boolean hasNextPage() {
		//se os produtos cadastrados não atingiram o total tem proxima pagina
		if(this.arrayProducts.size() < this.totalProducts) {
			return true;
		}
		
		return false;
	}


	protected void setTotalBusca(JSONObject results) {
		if(results.has("totalNumRecs")) {
			this.totalProducts = results.getInt("totalNumRecs");
		}
		
		this.log("Total da busca: "+this.totalProducts);
	}


	private String crawlInternalPid(JSONObject product){
		String internalPid = null;
		
		if(product.has("attributes")) {
			JSONObject attributes = product.getJSONObject("attributes");
			
			if(attributes.has("product.repositoryId")) {
				JSONArray id = attributes.getJSONArray("product.repositoryId");
				
				if(id.length() > 0) {
					internalPid = id.getString(0);
				}
			}
		}
		
		return internalPid;
	}

	private String crawlProductUrl(JSONObject product){
		String urlProduct = null;
		
		if(product.has("records")) {
			JSONArray records = product.getJSONArray("records");
			
			if(records.length() > 0) {
				JSONObject record = records.getJSONObject(0);
				
				if(record.has("attributes")) {
					JSONObject attributes = record.getJSONObject("attributes");
					
					if(attributes.has("product.route")) {
						JSONArray url = attributes.getJSONArray("product.route");
						
						if(url.length() > 0) {
							urlProduct = "https://www.webcontinental.com.br" + url.getString(0);
						}
					}
				}
			}
		}
		
		return urlProduct;
	}
	
	private String crawlInternalId(JSONObject product){
		String internalId = null;
		
		if(product.has("records")) {
			JSONArray records = product.getJSONArray("records");
			
			if(records.length() > 0) {
				JSONObject record = records.getJSONObject(0);
				
				if(record.has("attributes")) {
					JSONObject attributes = record.getJSONObject("attributes");
					
					if(attributes.has("sku.repositoryId")) {
						JSONArray id = attributes.getJSONArray("sku.repositoryId");
						
						if(id.length() == 1) {
							internalId = id.getString(0);
						}
					}
				}
			}
		}
		
		return internalId;
	}
}
