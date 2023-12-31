package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.ranking.RankingSession;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

		if(((RankingSession)session).mustTakeAScreenshot() && this.currentPage <= 2) {
			String printUrl = "http://www.soubarato.com.br/busca/?content="+ this.keywordEncoded +"&limit=28&source=nanook&offset=" + this.arrayProducts.size();
		}
		
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
		return this.arrayProducts.size() < this.totalProducts;
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
