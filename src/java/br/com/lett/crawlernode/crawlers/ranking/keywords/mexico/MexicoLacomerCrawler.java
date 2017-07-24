package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;
import org.json.JSONArray;
import org.json.JSONObject;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.ranking.RankingSession;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class MexicoLacomerCrawler extends CrawlerRankingKeywords{
	
	public MexicoLacomerCrawler(Session session) {
		super(session);
	}

	private static final String LACOMER_TIENDA_ID = "14";
	
	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 30;
			
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		// primeira página começa em 0 e assim vai.
		String url = "http://www.lacomer.com.mx/GSAServices/searchArt?col=lacomer_2&orden=-1"
				+ "&p="+ this.currentPage +"&pasilloId=false&s="+ this.keywordEncoded +"&succId=" + LACOMER_TIENDA_ID;
		
		this.log("Link onde são feitos os crawlers: "+url);	
			
		//chama função de pegar a url
		JSONObject search = fetchJSONObject(url);

		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(search.has("res") && search.getJSONArray("res").length() > 0) {
			JSONArray products = search.getJSONArray("res");
			
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) {
				setTotalBusca(search);
			}
			
			for(int i = 0; i < products.length(); i++) {
				
				JSONObject product = products.getJSONObject(i);	
				
				// InternalPid
				String internalPid = crawlInternalPid();
				
				// InternalId
				String internalId = crawlInternalId(product);
				
				// Url do produto
				String productUrl = crawlProductUrl(internalId);
				
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
		if(this.arrayProducts.size() < this.totalProducts) {
			return true;
		}
		
		return false;
	}
	
	protected void setTotalBusca(JSONObject search) {
		if(search.has("total")) { 	

			this.totalProducts = search.getInt("total");

		
			this.log("Total da busca: "+this.totalProducts);
		}
	}
	
	private String crawlInternalId(JSONObject product){
		String internalId = null;
		
		if(product.has("artEan")){ 
			internalId = product.getString("artEan");
		}
		
		return internalId;
	}
	
	private String crawlInternalPid(){
		return null;
	}
	
	private String crawlProductUrl(String internalId) {		
		return "http://www.lacomer.com.mx/lacomer/doHome.action?succId=14&pasId=63&artEan="+ internalId +"&ver=detallearticulo&opcion=detarticulo" ;
	}
}
