package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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

		JSONObject apiSearch = fetchJsonApi();
		
		if(apiSearch.has("html") && CommonMethods.isString(apiSearch.get("html"))) {
			this.currentDoc = Jsoup.parse(apiSearch.getString("html"));
		} else {
			this.currentDoc = new Document("");
		}
	
		Elements products = this.currentDoc.select("ul.listas > li > a");

		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0){
				setTotalBusca(apiSearch);
			}

			for(Element e : products) {
				// InternalPid
				String internalPid 	= crawlInternalPid(e);

				// InternalId
				String internalId 	= internalPid;

				//monta a url
				String productUrl = crawlProductUrl(e);

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


	protected void setTotalBusca(JSONObject apiSearch) {
		if(apiSearch.has("quantidade")) {
			try {
				this.totalProducts = Integer.parseInt(apiSearch.getString("quantidade"));
			} catch(Exception e) {
				this.logError(CommonMethods.getStackTraceString(e));
			}
		}
		
		this.log("Total da busca: "+this.totalProducts);
	}


	private String crawlInternalPid(Element e){
		String internalPid;

		String[] tokens = e.attr("href").split("/");
		internalPid 	= tokens[tokens.length-1].replaceAll("[^0-9]", "");

		return internalPid;
	}

	private String crawlProductUrl(Element e){
		String urlProduct;
		urlProduct = e.attr("href");

		if(!urlProduct.contains("kalunga")){
			urlProduct = "http://www.kalunga.com.br/" + urlProduct;
		}

		return urlProduct;
	}
	
	private JSONObject fetchJsonApi() {
		String url = "http://kalunga.com.br/webapi/Busca/BindSearch";
		String payload = "{\"pageIndex\":\""+this.currentPage+"\",\"idClassificacao\":\"0\",\"idGrupo\":\"0\""
				+ ",\"tipoOrdenacao\":\"1\",\"termoBuscado\":\""+this.keywordEncoded+"\",\"itensFiltro\":\"\",\"visao\":\"L\"}";

		Map<String,String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json; charset=UTF-8");
		headers.put("X-Requested-With", "XMLHttpRequest");

		String jsonString = fetchStringPOST(url, payload, headers, null);
		JSONObject apiSearch = new JSONObject();
		
		if(jsonString.startsWith("{")) {
			apiSearch = new JSONObject(jsonString);
		}
		
		return apiSearch;
	}
}
