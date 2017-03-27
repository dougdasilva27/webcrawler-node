package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;
import org.json.JSONArray;
import org.json.JSONObject;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class MexicoChedrauiCrawler extends CrawlerRankingKeywords{
	
	public MexicoChedrauiCrawler(Session session) {
		super(session);
	}

	private static final String TIENDA = "mexicociudadlabor";
	private static final String TIENDA_ID = "4294967169";
	private static final String REMOTE_ADDRESS = "74.205.80.195:8080";
//	private final static String REMOTE_ADDRESS = "174.143.32.125:8080";
	
	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 24;
			
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		// primeira página começa em 0 e assim vai.
		String url = "http://"+ REMOTE_ADDRESS +"/endeca-assembler/json/search?N="+ TIENDA_ID +"+"
				+ "&Ntt="+ this.keywordEncoded +"&Nrpp=24&No=" + this.arrayProducts.size();
		
		this.log("Link onde são feitos os crawlers: "+url);	
		
		// pega o json da api
		JSONObject jsonSearch = fetchJSONObject(url);
		
		// pega as informações importantes
		JSONObject productsInfo = getImportantInformations(jsonSearch);
		
		if(productsInfo.has("products")){
			JSONArray products = productsInfo.getJSONArray("products");
			
			//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
			if(products.length() >= 1) {
				//se o total de busca não foi setado ainda, chama a função para setar
				if(this.totalBusca == 0) {
					setTotalBusca(productsInfo);
				}
				
				for(int i = 0; i < products.length(); i++) {
					JSONObject product = products.getJSONObject(i);
	
					// InternalPid
					String internalPid = crawlInternalPid();
					
					// InternalId
					String internalId = crawlInternalId(product);
					
					// Url do produto
					String productUrl = crawlProductUrl(product);
					
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
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
		}

		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
	}

	@Override
	protected boolean hasNextPage() {
		// se não atingiu o total da busca ainda tem mais páginas.
		if(this.arrayProducts.size() < this.totalBusca){
			return true;
		}
		
		return false;
	}
	

	protected void setTotalBusca(JSONObject productsInfo) {
		if(productsInfo.has("totalBusca")) { 	
			try {
				this.totalBusca = productsInfo.getInt("totalBusca");
			} catch(Exception e) {
				this.logError(e.getMessage());
			}
			
			this.log("Total da busca: "+this.totalBusca);
		}
	}
	
	private String crawlInternalId(JSONObject product){
		String internalId = null;
		
		if(product.has("id")){
			internalId = product.getString("id");
		}
		
		return internalId;
	}
	
	private String crawlInternalPid(){
		String internalPid = null;
		
		return internalPid;
	}
	
	private String crawlProductUrl(JSONObject product){
		String urlProduct = null;
		
		if(product.has("idUrl")){
			urlProduct = "http://www.chedraui.com.mx/index.php/"+ TIENDA +"/catalog/product/view/id/"+ product.getString("idUrl") +"/";
		}
		
		return urlProduct;
	}
	
	private JSONObject getImportantInformations(JSONObject jsonSearch){
		JSONObject products = new JSONObject();
		
		if(jsonSearch.has("contents")){
			JSONArray contents = jsonSearch.getJSONArray("contents");
			
			if(contents.length() > 0){
				JSONObject content = contents.getJSONObject(0);
				
				if(content.has("mainContent")){
					JSONArray mainContent = content.getJSONArray("mainContent");
					
					if(mainContent.length() > 1){
						JSONObject info = mainContent.getJSONObject(1);
						
						if(info.has("totalNumRecs")){
							products.put("totalBusca", info.getInt("totalNumRecs"));
						}
						
						products.put("products", getJsonProducts(info));
					}
				}
			}
		}
		
		return products;
	}
	
	private JSONArray getJsonProducts(JSONObject jsonInfo){
		JSONArray products = new JSONArray();
		
		if(jsonInfo.has("records")){
			JSONArray records = jsonInfo.getJSONArray("records");
			
			for(int i = 0; i < records.length(); i++){
				JSONObject putJson = new JSONObject();
				JSONObject product = records.getJSONObject(i);
				
				if(product.has("attributes")){
					JSONObject attributes = product.getJSONObject("attributes");
					
					if(attributes.has("P_product_id")){
						JSONArray pid = attributes.getJSONArray("P_product_id");
						
						if(pid.length() > 0){
							putJson.put("idUrl", pid.getString(0));
						}
					}
					
					if(attributes.has("P_sku_id")){
						JSONArray sku = attributes.getJSONArray("P_sku_id");
						
						if(sku.length() > 0){
							putJson.put("id", sku.getString(0));
						}
					}
					
					products.put(putJson);
				}
				
			}
			
		}
		
		return products;
	}
}
