package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilManiavirtualCrawler extends CrawlerRankingKeywords{

	public BrasilManiavirtualCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "https://www.maniavirtual.com.br/search?cid=0&q=" + this.location.replace(" ", "%20") + "&pagenumber="+ this.currentPage;
				
		this.log("Link onde são feitos os crawlers: "+url);	
			
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		Elements products =  this.currentDoc.select(".search-results .product-item");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) {
				setTotalProducts();
			}
			
			for(Element e: products) {
				String internalPid 	= crawlInternalPid(e);
				String internalId 	= internalPid;
				String productUrl = crawlProductUrl(e);
				
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
		return this.arrayProducts.size() < this.totalProducts;
	}
	
	@Override
	protected void setTotalProducts() {
		Element total = this.currentDoc.select(".category-products-count text").first();
		
		if(total != null) {
			String totalText = total.ownText().split(" ")[0].replaceAll("[^0-9]", "").trim();
			
			if(!totalText.isEmpty()) {
				this.totalProducts = Integer.parseInt(totalText);
				
				this.log("Total da busca: " + this.totalProducts);
			}
		}
	}
	
	private String crawlInternalPid(Element e){
		String pid = null;
		Element sku = e.select(".sku").first();
		
		if(sku != null) {
			pid = sku.ownText().trim();
		}
		
		return pid;
	}
	
	private String crawlProductUrl(Element e){
		String productUrl = null;
		Element url = e.select(".product-title > a").first();
		
		if(url != null) {
			productUrl = url.attr("href");
			
			if(!productUrl.startsWith("https://www.maniavirtual.com.br/")) {
				productUrl = ("https://www.maniavirtual.com.br/" + productUrl).replace("br//", "br/");
			}
		}
		
		return productUrl;
	}
}
