package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilDrogafujiCrawler extends CrawlerRankingKeywords{

	public BrasilDrogafujiCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 24;
	
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "https://www.drogafuji.com.br/catalogsearch/result/index/?p="+ this.currentPage +"&q=" + this.keywordEncoded;
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar o html
		this.currentDoc = fetchDocument(url);

		Elements products =  this.currentDoc.select(".products-grid .item");
		
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
		return this.arrayProducts.size() < this.totalProducts;
	}
	
	@Override
	protected void setTotalProducts() {
		Element total = this.currentDoc.select(".amount").first();
		
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
		Element idElement = e.select("span[id^=product-price-]").first();
		
		if(idElement != null) {
			String[] tokens = idElement.attr("id").split("-");
			String id = tokens[tokens.length-1].replaceAll("[^0-9]", "").trim();
			
			if(!id.isEmpty()) {
				internalId = id;
			}
		}
		
		return internalId;
	}
	
	private String crawlProductUrl(Element e){
		String productUrl = null;
		Element url = e.select("> a").first();
		
		if(url != null) {
			productUrl = url.attr("href");
			
			if(!productUrl.startsWith("https://www.drogafuji.com.br/")) {
				productUrl = "https://www.drogafuji.com.br/" + productUrl;
			}
		}
		
		return productUrl;
	}
}
