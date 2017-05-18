package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilArcondicionadoCrawler extends CrawlerRankingKeywords{

	public BrasilArcondicionadoCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 24;
	
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "http://www.arcondicionado.com.br/busca?busca="+ this.keywordEncoded +"&pagina=" + this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		JSONArray products = crawlProducts(this.currentDoc);	
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.length() >= 1) {			
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) {
				setTotalProducts();
			}
			for(int i = 0; i < products.length(); i++) {
				JSONObject json = products.getJSONObject(i);
				
				// InternalPid
				String internalPid 	= crawlInternalPid(json);
				
				// InternalId
				String internalId 	= crawlInternalId();
				
				// Url do produto
				String productUrl 	= crawlProductUrl(json); 
				
				saveDataProduct(internalId, internalPid, productUrl);
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
				if(this.arrayProducts.size() == productsLimit){
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
				
		//se  elemeno page obtiver algum resultado
		if(this.arrayProducts.size() < this.totalProducts){
			//tem próxima página
			return true;
		} 
			
		return false;
		
	}
	
	@Override
	protected void setTotalProducts()	{
		Element totalElement = this.currentDoc.select(".mostrando span").first();
		
		if(totalElement != null) { 	
			try	{				
				this.totalProducts = Integer.parseInt(totalElement.text().replaceAll("[^0-9]", "").trim());
			} catch(Exception e) {
				this.logError(e.getMessage());
			}
			
			this.log("Total da busca: "+this.totalProducts); 
		}
	}
	
	private JSONArray crawlProducts(Document doc){
		JSONArray products = new JSONArray();
		Elements scripts = doc.select("script:not([type])");
		
		for(Element e : scripts){
			String script = e.outerHtml();
			
			if(script.contains("Fbits.ListaProdutos.Produtos")){
				int x = script.indexOf("Produtos = [")+11;
				int y = script.indexOf(";", x);
				
				String json = script.substring(x, y).trim();
				
				if(json.startsWith("[") && json.endsWith("]")){
					products = new JSONArray(json);
				}
			}
		}
		
		return products;
	}
	
	private String crawlInternalId(){
		String internalId = null;
		
		return internalId;
	}
	
	private String crawlInternalPid(JSONObject json){
		String internalPid = null;
		
		if(json.has("ProdutoId")){
			internalPid = json.get("ProdutoId").toString();
		}
		
		return internalPid;
	}
	
	private String crawlProductUrl(JSONObject json){
		String urlProduct = null;		
		
		if(json.has("Link")){
			urlProduct = json.getString("Link");
			
			if(!urlProduct.contains("arcondicionado.com")){
				urlProduct = "https://www.arcondicionado.com.br" + urlProduct;
			}
		}
		
		return urlProduct;
	}
}
