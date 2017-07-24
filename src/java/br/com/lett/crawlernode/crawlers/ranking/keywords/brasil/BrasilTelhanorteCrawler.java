package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.ranking.RankingSession;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilTelhanorteCrawler extends CrawlerRankingKeywords {

	public BrasilTelhanorteCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 48;

		this.log("Página "+ this.currentPage);

		String url = "http://busca.telhanorte.com.br/api/search?apikey=telhanorte&no-cache=1468348873262&page="+this.currentPage+"&q="+this.keywordEncoded;
		this.log("Link onde são feitos os crawlers: "+url);	
		
		if(((RankingSession)session).mustTakeAScreenshot() && this.currentPage <= 2) {
			String printUrl = "http://busca.telhanorte.com.br/busca?q="+ this.keywordEncoded +"&page=" + this.currentPage;
			takeAScreenshot(printUrl);
		}
		
		JSONObject json = fetchJSONObject(url);

		if(json.has("hits")){

			JSONObject jsonPage = json.getJSONObject("hits");

			JSONArray jsonArrayPage;
			try {
				jsonArrayPage = jsonPage.getJSONArray("hits");
			} catch (JSONException e) {
				jsonArrayPage = new JSONArray();
				e.printStackTrace();
			}
	
			if(jsonArrayPage.length() >= 1){
	
				setTotalBusca(jsonPage);
	
				for(int i = 0; i < jsonArrayPage.length(); i++){
	
					JSONObject jsonProduct = jsonArrayPage.getJSONObject(i).getJSONObject("_source");
	
					//ids
					String internalId   = null;
					String internalPid  = Integer.toString(jsonProduct.getInt("id"));
	
					//url
					String urlProduct = jsonProduct.getString("url");
	
					saveDataProduct(internalId, internalPid, urlProduct);
	
					this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
					if(this.arrayProducts.size() == productsLimit) break;
	
				}
			} else {
	
				this.result = false;
				this.log("Keyword sem resultado!");
			}

		}

		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
	}

	@Override
	protected boolean hasNextPage() {
		//se os produtos cadastrados não atingiram o total tem proxima pagina
		if(this.arrayProducts.size() < this.totalProducts) return true;
		else									  		return false;
	}


	private void setTotalBusca(JSONObject json) {
		try{

			if(json.has("total")){
				this.totalProducts = json.getInt("total");
			}

		} catch(Exception e) {
			e.printStackTrace();
			this.logError(e.getMessage() +" Erro ao parsear jsonTotal");
		}
		this.log("Total da busca: "+this.totalProducts);
	}	
}
