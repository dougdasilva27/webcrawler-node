package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilMiamistoreCrawler extends CrawlerRankingKeywords{

	public BrasilMiamistoreCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "https://www.miami.com.br/pesquisa?t="+ this.keywordEncoded +"&pg="+ this.currentPage;
				
		this.log("Link onde são feitos os crawlers: "+url);	
			
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		Elements products =  this.currentDoc.select("div.wd-browsing-grid-list ul li div[data-product-id]");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(!products.isEmpty()) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) {
				setTotalProducts();
			}

			for(Element e: products) {
				String internalPid 	= e.attr("data-product-id");
				String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "div[data-sku-selected][data-pid]", "data-sku-selected");
				String productUrl = CrawlerUtils.scrapUrl(e, " .link-produto", "href", "https", "www.miami.com.br");
				
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
		return this.currentDoc.select("a.page-next").first() != null;
	}
	
	@Override
	protected void setTotalProducts() {
		Element total = this.currentDoc.select(".product-count span").first();
		
		if(total != null) {
			String totalText = total.ownText().replaceAll("[^0-9]", "").trim();
			
			if(!totalText.isEmpty()) {
				this.totalProducts = Integer.parseInt(totalText);
				
				this.log("Total da busca: " + this.totalProducts);
			}
		}
	}
}
