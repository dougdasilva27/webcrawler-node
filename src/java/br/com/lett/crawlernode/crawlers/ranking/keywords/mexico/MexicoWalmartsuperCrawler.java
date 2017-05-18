package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class MexicoWalmartsuperCrawler extends CrawlerRankingKeywords{

	public MexicoWalmartsuperCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 32;
			
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		// primeira página começa em 0 e assim vai.
		String url = "https://super.walmart.com.mx/search/?storeId=0000009999&Ntt=" + this.keywordEncoded + "&No=" + this.arrayProducts.size();
		this.log("Link onde são feitos os crawlers: "+url);	
		
		this.currentDoc = fetchDocument(url);
		
		Elements products =  this.currentDoc.select(".h-product[itemtype]");

		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) setTotalProducts();
			
			for(Element e: products) {

				// InternalPid
				String internalPid = crawlInternalPid(e);
				
				// InternalId
				String internalId = crawlInternalId(e);
				
				// Url do produto
				String productUrl = crawlProductUrl(e);
				
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
		// se não atingiu o total da busca ainda tem mais páginas.
		if(this.arrayProducts.size() < this.totalProducts){
			return true;
		}
		
		return false;
	}
	
	@Override
	protected void setTotalProducts() {
		Element totalElement = this.currentDoc.select(".itemCount > span").last();
		
		if(totalElement != null) { 	
			try {
				this.totalProducts = Integer.parseInt(totalElement.text());
			} catch(Exception e) {
				this.logError(e.getMessage());
			}
			
			this.log("Total da busca: "+this.totalProducts);
		}
	}
	
	private String crawlInternalId(Element e){
		String internalId = null;
		Element id = e.select("#add-to-mylist").first();
		
		if(id != null){ 
			internalId = id.attr("data-productid");
		}
		
		return internalId;
	}
	
	private String crawlInternalPid(Element e){
		String internalPid = null;
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = null;
		Element urlElement 	= e.select(" .product-title > a").first();
		
		if(urlElement != null){
			urlProduct = urlElement.attr("href");
			
			if(!urlProduct.startsWith("https://super.walmart.com.mx")){
				urlProduct = "https://super.walmart.com.mx" + urlProduct;
			}
		}
		
		return urlProduct;
	}
}
