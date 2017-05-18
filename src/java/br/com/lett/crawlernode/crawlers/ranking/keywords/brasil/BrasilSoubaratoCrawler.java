package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilSoubaratoCrawler extends CrawlerRankingKeywords {

	public BrasilSoubaratoCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 24;

		this.log("Página "+ this.currentPage);

		String key = this.keywordWithoutAccents.replaceAll(" ", "%20");		

		String url = "http://busca.soubarato.com.br/mobile_search_v2?"
				+"format=json&results_per_page=60&page="+ this.currentPage +"&query="+ key;

		JSONObject jsonPage = fetchJSONObject(url);


		JSONArray jsonArrayPage;
		try {
			jsonArrayPage = jsonPage.getJSONArray("products");
		} catch (JSONException e) {
			jsonArrayPage = new JSONArray();
			e.printStackTrace();
		}

		if(jsonArrayPage.length() >= 1){
			setTotalBusca(jsonPage);

			for(int i = 0; i < jsonArrayPage.length(); i++){

				JSONObject jsonProduct = jsonArrayPage.getJSONObject(i);

				//ids
				String internalId  = null;
				String internalPid = Integer.toString(jsonProduct.getInt("product_id"));

				//url
				String productUrl = jsonProduct.getString("url");

				saveDataProduct(internalId, internalPid, productUrl);

				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
				if(this.arrayProducts.size() == productsLimit) break;

			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
		}



		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
	}

	@Override
	protected boolean hasNextPage() {
		//se os produtos cadastrados não atingiram o total tem proxima pagina
		if(this.arrayProducts.size() < this.totalProducts) return true;
		else									  		return false;
	}


	private void setTotalBusca(JSONObject jsonPage)
	{
		try{

			JSONObject jsonTotal = jsonPage.getJSONObject("query_metadata");

			this.totalProducts = jsonTotal.getInt("total_result_count");

		} catch(Exception e) {
			e.printStackTrace();
			this.logError(e.getMessage() +" Erro ao parsear jsonTotal");
		}
		this.log("Total da busca: "+this.totalProducts);
	}

}
