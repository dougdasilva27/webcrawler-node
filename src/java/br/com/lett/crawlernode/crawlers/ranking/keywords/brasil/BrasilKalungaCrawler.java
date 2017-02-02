package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilKalungaCrawler extends CrawlerRankingKeywords{


	public BrasilKalungaCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 35;

		this.log("Página "+ this.currentPage);

		String url = "http://www.kalunga.com.br/Busca.aspx/BindSearch";
		String payload = "{\"pageIndex\":\""+this.currentPage+"\",\"idClassificacao\":\"0\",\"idGrupo\":\"0\""
				+ ",\"tipoOrdenacao\":\"1\",\"termoBuscado\":\""+this.keywordEncoded+"\",\"itensFiltro\":\"\",\"visao\":\"L\"}";

		Map<String,String> headers = new HashMap<String, String>();
		headers.put("Content-Type", "application/json; charset=iso-8859-1");

		String jsonString = fetchStringPOST(url, payload, headers, null);
		
		JSONObject jsonBusca = new JSONObject();
		JSONArray jsonArrayPage = new JSONArray();
		
		try {
			jsonBusca = new JSONObject(jsonString);
			
			if(jsonBusca.has("d")){
				jsonArrayPage = jsonBusca.getJSONArray("d");
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if(jsonArrayPage.get(0) != null){
			if(CommonMethods.isString(jsonArrayPage.get(0))) {
				String temp = jsonArrayPage.getString(0);

				this.currentDoc = Jsoup.parse(temp);
			} else {
				this.result = false;
			}
		} else {
			this.result = false;
		}


		if(this.result) {
			int count = 0;
			Elements products = this.currentDoc.select("ul.listas > li > a");

			if(products.size() >= 1) {
				//se o total de busca não foi setado ainda, chama a função para setar
				if(this.totalBusca == 0){
					String total = "0";
					if(CommonMethods.isString(jsonArrayPage.get(2))){
						total = jsonArrayPage.getString(2);
					}
					setTotalBusca(total);
				}

				for(Element e : products) {
					count++;

					// InternalPid
					String internalPid 	= crawlInternalPid(e);

					// InternalId
					String internalId 	= internalPid;

					//monta a url
					String productUrl = crawlProductUrl(e);

					saveDataProduct(internalId, internalPid, productUrl);

					this.log("InternalPid do produto da "+count+" da página "+ this.currentPage+ ": " + internalPid + " url: " + productUrl);
					if(this.arrayProducts.size() == productsLimit) break;
				}
			} else {
				this.result = false;
				this.log("Keyword sem resultados!");
			}
		}
		else
		{
			this.log("Keyword sem resultados!");
		}
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");

	}

	@Override
	protected boolean hasNextPage() {
		//se os produtos cadastrados não atingiram o total tem proxima pagina
		if(this.arrayProducts.size() < this.totalBusca) return true;
		else									  		return false;
	}


	protected void setTotalBusca(String total)
	{
		try
		{
			if(total != null) this.totalBusca = Integer.parseInt(total);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			this.logError(e.getMessage() +" Erro ao parsear total: " + total);
		}
		this.log("Total da busca: "+this.totalBusca);
	}


	private String crawlInternalPid(Element e){
		String internalPid = null;

		String[] tokens = e.attr("href").split("/");
		internalPid 	= tokens[tokens.length-1].replaceAll("[^0-9]", "");

		return internalPid;
	}

	private String crawlProductUrl(Element e){
		String urlProduct = null;
		urlProduct =  e.attr("href");

		if(!urlProduct.contains("kalunga")){
			urlProduct = "http://www.kalunga.com.br/" + urlProduct;
		}

		return urlProduct;
	}
}
