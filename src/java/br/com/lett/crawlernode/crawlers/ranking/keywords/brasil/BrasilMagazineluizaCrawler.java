package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.Iterator;

import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilMagazineluizaCrawler extends CrawlerRankingKeywords{

	public BrasilMagazineluizaCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 60;

		this.log("Página "+ this.currentPage);
		
		String key =  this.keywordWithoutAccents.replaceAll(" ", "%20");
		
		//monta a url com a keyword e a página
		String url = "http://www.magazineluiza.com.br/busca/"+key+"/"+this.currentPage+"/";
		this.log("Link onde são feitos os crawlers: "+url);			
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements id = this.currentDoc.select("div.wrapper-content li[itemscope] > a");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(id.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) {
				setTotalProducts();				
			}
			
			for(Element e: id) {
				JSONObject product = new JSONObject(e.attr("data-product"));
				
				if(product.has("stockTypes")) {
					JSONObject variations = product.getJSONObject("stockTypes");
					
					@SuppressWarnings("unchecked")
					Iterator<String> variationsList = variations.keys();
					
					int count = 0;
					while(variationsList.hasNext()) {
						// InternalId
						String internalId = variationsList.next();
						
						// InternalPid
						String internalPid 	= crawlInternalPid(product);
						
						// Url do produto
						String urlProduct = crawlProductUrl(e, internalId, internalPid);
						
						if(count == 0) {
							this.position++;
							count++;
						}

						saveDataProduct(internalId, null, urlProduct, this.position);
						
						this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
						if(this.arrayProducts.size() == productsLimit) {
							break;
						}
					}
					
					
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
		Element page = this.currentDoc.select("a.last-page").first();
		
		//se  elemeno page obtiver algum resultado
		if(page != null) {
			//tem próxima página
			return true;
		}
		
		return false;
	}

	@Override
	protected void setTotalProducts(){
		Element totalElement = this.currentDoc.select("div.header-search small").first();
		if(totalElement != null) {
			try {
				int x = totalElement.text().indexOf("(");
				int y = totalElement.text().indexOf("produto");
				
				String token = totalElement.text().substring(x+1, y).trim();
				
				this.totalProducts = Integer.parseInt(token);
			} catch(Exception e) {
				this.logError(CommonMethods.getStackTrace(e));
			}
			
			this.log("Total da busca: "+this.totalProducts);
		}
	}
	
	private String crawlInternalPid(JSONObject product){
		String internalPid = null;
		
		if(product.has("product")){
			internalPid = product.getString("product");
		}
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e, String internalId, String internalPid){
		String urlProduct = e.attr("href");
			
		if(internalId != null && internalPid != null) {
			urlProduct = urlProduct.replace(internalPid, internalId);
		}
		
		if(!urlProduct.startsWith("http://www.magazineluiza.com.br")) {
			urlProduct = "http://www.magazineluiza.com.br" + urlProduct;
		}
		
		return urlProduct;
	}
}
