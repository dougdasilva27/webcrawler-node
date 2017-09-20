package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class ArgentinaFarmacityCrawler extends CrawlerRankingKeywords{

	public ArgentinaFarmacityCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		this.log("Página "+ this.currentPage);
		
		this.pageSize = 30;
		
		//monta a url com a keyword e a página
		String url = "https://www.farmacity.com/buscar/" + this.keywordEncoded + "?page=" + this.currentPage;
		this.log("Link onde são feitos os crawlers: " + url);	
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url, null);

		Elements products =  this.currentDoc.select(".products .item[data-product-id]");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) {
				setTotalProducts();
			}
			
			for(Element e: products) {
				String internalPid = crawlInternalPid(e);
				String internalId = null;
				String productUrl = crawlProductUrl(e);
				
				saveDataProduct(internalId, internalPid, productUrl);
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
				if(this.arrayProducts.size() == productsLimit) {
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
		return arrayProducts.size() < this.totalProducts;
	}
	
	@Override
	protected void setTotalProducts() {
		Element totalElement = this.currentDoc.select(".page-list .total").first();
		
		if(totalElement != null) {
			String text = totalElement.ownText().replaceAll("[^0-9]", "").trim();
			
			if(!text.isEmpty()) {
				this.totalProducts = Integer.parseInt(totalElement.text());
			}
		}
		
		this.log("Total da busca: " + this.totalProducts);
	}

	private String crawlInternalPid(Element e){
		return e.attr("data-product-id");
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = null;
		Element urlElement = e.select(".product-pic > a").first();
		
		if(urlElement != null){
			urlProduct = urlElement.attr("href");
			
			if(!urlProduct.startsWith("https://www.farmacity.com/")) {
				urlProduct = ("https://www.farmacity.com/" + urlProduct).replace("com//", "com/");
			}
		}
		
		return urlProduct;
	}
}
