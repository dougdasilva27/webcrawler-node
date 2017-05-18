package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilSipolattiCrawler extends CrawlerRankingKeywords{
	
	public BrasilSipolattiCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 21;
			
		this.log("Página "+ this.currentPage);
		
		String key = this.keywordWithoutAccents.replaceAll(" ", "%20");
		
		//monta a url com a keyword e a página
		String url = "http://www.sipolatti.com.br/busca/3/0/0//MaisRecente/Decrescente/63/"+ this.currentPage +"////"+ key +".aspx";
		this.log("Link onde são feitos os crawlers: "+url);	
			
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		Map<String, String> pidsDataLayer = getPidsFromDataLayer();
		
		Elements products =  this.currentDoc.select("ul#listProduct > li");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) setTotalProducts();
			
			for(Element e: products) {
				//seta o id com o seletor
				Element pid = e.select("> input").first();
				String internalPid 	= pidsDataLayer.get(pid.attr("value"));
				String internalId 	= null;
				
				//monta a url
				Element eUrl = e.select("a[title].link.url").first();
				String productUrl	 = eUrl.attr("href");
				
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
		Element page = this.currentDoc.select("li.set-next.off").first();
		
		//se  elemeno page não obtiver nenhum resultado
		if(page != null)
		{
			//não tem próxima página
			return false;
		}
		else
		{
			if(this.arrayProducts.size() < this.totalProducts) return true;
			else											return false;
			
		}
	}
	
	@Override
	protected void setTotalProducts()
	{
		Element totalElement = this.currentDoc.select("div.filter-details > p strong").get(2);
		
		if(totalElement != null)
		{ 	
			try
			{
				String token = (totalElement.text().replaceAll("[^0-9]", "")).trim();
				
				this.totalProducts = Integer.parseInt(token);
			}
			catch(Exception e)
			{
				this.logError(e.getMessage());
			}
			
			this.log("Total da busca: "+this.totalProducts);
		}
	}
	
	private Map<String, String> getPidsFromDataLayer(){
	
		Map<String, String> pidsDataLayer = new HashMap<String, String>();
		
		JSONObject pidsJson = crawlSkuJson(this.currentDoc);
		
		if(pidsJson.has("RKProduct")){
			
			JSONArray productsArray = pidsJson.getJSONArray("RKProduct");
			
			for(int i = 0; i < productsArray.length(); i++){
				JSONObject jsonTemp = productsArray.getJSONObject(i);
				
				pidsDataLayer.put(jsonTemp.getString("idWeb"), jsonTemp.getString("id"));
				
			}	
		}
		
		return pidsDataLayer;
	}
	
	/**
	 * Get the script having a json with the availability information
	 * @return
	 */
	private JSONObject crawlSkuJson(Document document) {
		Elements scriptTags = document.getElementsByTag("script");
		JSONObject skuJson = null;
		
		for (Element tag : scriptTags){   
			
			String script = tag.html();
			
			if(script.trim().startsWith("var dataLayer = ")) {

				int x = script.indexOf("{");
				int y = script.indexOf("})", x);
				
				String dataLayer = script.substring(x, y+1);
				
				skuJson = new JSONObject(dataLayer);

				break;
			}       
		}
		
		return skuJson;
	}
}
