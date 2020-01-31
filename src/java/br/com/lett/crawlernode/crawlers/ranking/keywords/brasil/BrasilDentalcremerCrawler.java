package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.ranking.RankingSession;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import org.json.JSONArray;
import org.json.JSONObject;

public class BrasilDentalcremerCrawler extends CrawlerRankingKeywords{

	public BrasilDentalcremerCrawler(Session session) {
		super(session);
	}

	private int numberProducts;
	
	@Override
	protected void extractProductsFromCurrentPage() {
		this.log("Página "+ this.currentPage);
		
		//número de produtos por página do market
		this.pageSize = 9;

		//monta a url com a keyword e a página
		String url = "http://busca.dentalcremer.com.br/api/search?apikey=dentalcremer&order=bestselling&page=" + this.currentPage + "&q=" + this.keywordEncoded;
		this.log("Link onde são feitos os crawlers: " + url);

		if (((RankingSession) session).mustTakeAScreenshot() && this.currentPage <= 2) {
			String printUrl = "http://busca.dentalcremer.com.br/?q=" + this.keywordEncoded + "&page=" + this.currentPage;
			takeAScreenshot(printUrl);
		}

		JSONObject json = fetchJSONObject(url);
		boolean hasResults = false;

		JSONObject jsonProduct = new JSONObject();
		if (json.has("hits")) {
			jsonProduct = json.getJSONObject("hits");
		}

		try {
			jsonProduct.getJSONObject("_shards");
			hasResults = true;
		} catch (Exception e) {
			hasResults = false;
		}
		
		if(jsonProduct != null && hasResults) {
			if(jsonProduct.has("hits")){
				JSONArray jsArray = jsonProduct.getJSONArray("hits");
				this.numberProducts = jsArray.length();
				
				if(jsArray.length() >= 1) {
					for(int i =0; i < jsArray.length(); i++) {
						JSONObject j =  jsArray.getJSONObject(i).getJSONObject("_source");
						
						//monta a url
						String productUrl  = j.getString("url");
						
						//monta o pid
						String internalPid = j.getString("id");
						
						//mota o InternalId
						String internalId = j.getJSONArray("skus").getJSONObject(0).getString("sku");
						
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
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
		}
		
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
		if(this.arrayProducts.size() < productsLimit) setTotalBusca(jsonProduct);
		else										   this.totalProducts = 0;
	}

	@Override
	protected boolean hasNextPage() {
		return this.numberProducts >= 48;
	}
	
	protected void setTotalBusca(JSONObject j)
	{
		if(this.totalProducts == 0 && j != null) this.totalProducts += j.getInt("total");
	}

	

}
