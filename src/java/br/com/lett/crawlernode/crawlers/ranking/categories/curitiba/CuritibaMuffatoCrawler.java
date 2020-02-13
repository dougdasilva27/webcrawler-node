package br.com.lett.crawlernode.crawlers.ranking.categories.curitiba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingCategories;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class CuritibaMuffatoCrawler extends CrawlerRankingCategories{
	
	public CuritibaMuffatoCrawler(Session session) {
		super(session);
	}

	private static final String HOME_PAGE = "http://delivery.supermuffato.com.br/";
	private boolean cat1 = false;

	@Override
	protected void processBeforeFetch() {
		super.processBeforeFetch();

		// número de produtos por página do market
		if (StringUtils.countMatches(this.categoryUrl.replace(HOME_PAGE, ""), "/") > 0) {
			this.pageSize = 24;
		} else {
			this.pageSize = 12;
			this.cat1 = true;
		}
		
		this.log("Page Size: " + this.pageSize);
	}
	
	@Override
	protected void extractProductsFromCurrentPage() {
		this.log("Página "+ this.currentPage);
		
		// monta a url com a keyword e a página
		String url;
		
		if(!this.cat1) {
			if(this.categoryUrl.contains("?")) {
				url = this.categoryUrl + "&sc=10&PageNumber=" + this.currentPage;
			} else {
				url = this.categoryUrl + "?sc=10&PageNumber=" + this.currentPage;
			}
		} else {
			url = this.location;
		}
		
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		Elements products;
		
		if(!this.cat1) {
			products =  this.currentDoc.select(".vitrine div[id^=ResultItems_] ul > li[layout] > div");
		} else {
			products =  this.currentDoc.select(".resultItemsWrapper ul > li[layout] > div");
		}
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {			
			for(Element e: products) {
				// InternalPid
				String internalPid 	= crawlInternalPid(e);
				
				// InternalId
				String internalId 	= crawlInternalId(e);
				
				// Url do produto
				String productUrl = crawlProductUrl(e);
				
				saveDataProduct(internalId, internalPid, productUrl);
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
				if(this.arrayProducts.size() == productsLimit) {
					break;
				}
				
			}
		} else {
			setTotalProducts();
			this.result = false;
			this.log("Keyword sem resultado para a página atual!");
		}
		
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
		
		if(!hasNextPage()) {
			setTotalProducts();
		}
	}

	@Override
	protected boolean hasNextPage() {
		Elements products = this.currentDoc.select(".vitrine div[id^=ResultItems_] ul > li[layout] > div");

		return products.size() >= this.pageSize && !this.cat1;
	}
	
	private String crawlInternalId(Element e){
		return e.attr("data-product-id");
	}
	
	private String crawlInternalPid(Element e){
		return null;
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = null;
		Element urlElement = e.select(".prd-list-item-desc > a").first();
		
		if(urlElement != null){
			urlProduct = urlElement.attr("href");
			
			if(!urlProduct.contains("supermuffato")){
				urlProduct = "http://delivery.supermuffato.com.br" + urlProduct;
			} else if(!urlProduct.contains("http")){
				urlProduct = "http:" + urlProduct;
			}
		}
		
		return urlProduct;
	}

}
