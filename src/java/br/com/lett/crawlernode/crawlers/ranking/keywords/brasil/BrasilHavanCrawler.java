package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilHavanCrawler extends CrawlerRankingKeywords {

	public BrasilHavanCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {

		this.pageSize = 24;
			
		this.log("Página "+ this.currentPage);
		
		String key = this.keywordWithoutAccents.replaceAll(" ", "%20");
		

		String url = "https://www.havan.com.br/catalogsearch/result//index/?p="+ this.currentPage+ "&q="+key;
		this.log("Link onde são feitos os crawlers: "+url);	
			

		this.currentDoc = fetchDocument(url);
		
		Elements products =  this.currentDoc.select(".product .product-item-info");
		

		if(products.size() >= 1) {

			if(this.totalProducts == 0) {
				setTotalProducts();
			}
			
			for(Element e: products) {

				String internalPid 	= CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product div", "value");
				String productUrl  = CrawlerUtils.scrapUrl(e, ".hover-itens .more-info", "href", "https:", "www.havan.com.br");
				
				saveDataProduct(null, internalPid, productUrl);
				
				this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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
		//tem próxima página
		return this.arrayProducts.size() < this.totalProducts;

	}
	
	@Override
	protected void setTotalProducts() {
		Element totalElement = this.currentDoc.select(".toolbar-amount .toolbar-number:last-child").first();
		
		if(totalElement != null) { 	
			try	{				
				this.totalProducts = Integer.parseInt(totalElement.text().trim());
			} catch(Exception e) {
				this.logError(CommonMethods.getStackTraceString(e));
			}
			
			this.log("Total da busca: "+this.totalProducts);
		}
	}
}
