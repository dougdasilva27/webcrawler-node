package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilCentraltecCrawler extends CrawlerRankingKeywords{

	public BrasilCentraltecCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 9;
	
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "http://www.centraltec.com.br/catalogsearch/result/index/?limit=30&p=" + this.currentPage + "&q=" + this.keywordEncoded;
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products =  this.currentDoc.select(".products-grid li.item .infobox");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {			
			if(totalProducts == 0) {
				setTotalProducts();
			}
			
			for(Element e : products) {
				// InternalPid
				String internalPid = null;
				
				// InternalId
				String internalId = crawlInternalId(e);
				
				// Url do produto
				String urlProduct = crawlProductUrl(e);
				
				saveDataProduct(internalId, internalPid, urlProduct);
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
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

		//se  elemeno page obtiver algum resultado
		//tem próxima página
		return this.arrayProducts.size() < this.totalProducts;

	}
	
	@Override
	protected void setTotalProducts() {
		Element total = this.currentDoc.select(".amount .caixa").first();
		
		if(total != null) {
			String totalText = total.ownText().trim().split(" ")[0].replaceAll("[^0-9]", "");
			
			if(!totalText.isEmpty()) {
				this.totalProducts = Integer.parseInt(totalText);
			}
		}
		
		this.log("Total products: " + this.totalProducts);
	}
	
	private String crawlInternalId(Element e){
		String internalId = null;
		Element id = e.select(".set-btn .link-wishlist").first();

		if(id != null) {
			String[] urlTokens = id.attr("href").split("product/");
			internalId = urlTokens[urlTokens.length-1].replaceAll("[^0-9]", "").trim();			
		}
		
		return internalId;
	}
	
	
	private String crawlProductUrl(Element e){
		String productUrl = null;
		Element url = e.select(".product-name a").first();
		
		if(url != null) {
			productUrl = url.attr("href");
			
			if(!productUrl.startsWith("http://www.centraltec.com.br/")) {
				productUrl = "http://www.centraltec.com.br/" + productUrl;
			}
		}
		
		return productUrl;
	}
}
