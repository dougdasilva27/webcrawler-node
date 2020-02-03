package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilEletrocityCrawler extends CrawlerRankingKeywords {

	public BrasilEletrocityCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 36;
			
		this.log("Página "+ this.currentPage);
		
		String keyword = this.keywordWithoutAccents.replaceAll(" ", "%20");
		
		//monta a url com a keyword e a página
		String url = "http://www.eletrocity.com.br/"+ keyword +"?PageNumber="+ this.currentPage;
		this.log("Link onde são feitos os crawlers: "+ url);	
			
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		// Alguns sites da vTex, o id do produto se separa do container com as informações do mesmo.
		Elements products =  this.currentDoc.select(".gt-vitrine > ul > li[layout]");
		Elements productsIds =  this.currentDoc.select(".gt-vitrine > ul > li[id]");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) setTotalProducts();
			
			for(int i = 0; i < products.size(); i++) {
				Element e = products.get(i);
				// InternalPid
				String internalPid = crawlInternalPid(productsIds, i);
				
				// InternalId
				String internalId  = crawlInternalId(e);
				
				// Url do produto
				String urlProduct  = crawlProductUrl(e);
				
				saveDataProduct(internalId, internalPid, urlProduct);
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
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

		//se  elemeno page não obtiver nenhum resultado
		//tem próxima página
		return this.arrayProducts.size() < this.totalProducts;
	}
	
	@Override
	protected void setTotalProducts() {
		Element totalElement = this.currentDoc.select("span.resultado-busca-numero span.value").first();
		
		if(totalElement != null) { 	
			try	{
				this.totalProducts = Integer.parseInt(totalElement.text());
			} catch(Exception e) {
				this.logError(e.getMessage());
			}
			
			this.log("Total da busca: "+this.totalProducts);
		}
	}
	
	private String crawlInternalId(Element e){
		String internalId = null;
		
		return internalId;
	}
	
	private String crawlInternalPid(Elements productsIds, int index){
		String internalPid = null;
		
		if(index < productsIds.size()){
			Element e = productsIds.get(index);
			
			internalPid = e.attr("id").split("_")[1];
		}
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = null;
		Element urlElement = e.select("h3 a").first();
		
		if(urlElement != null){
			urlProduct = urlElement.attr("href");
		}
		
		return urlProduct;
	}
}
