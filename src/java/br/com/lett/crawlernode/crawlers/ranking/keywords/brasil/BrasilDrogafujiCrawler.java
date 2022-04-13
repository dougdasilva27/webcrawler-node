package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilDrogafujiCrawler extends CrawlerRankingKeywords{

	public BrasilDrogafujiCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 12;
	
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
      String url = "https://www.drogafuji.com.br/buscapagina?ft=" + this.keywordEncoded + "&PS=" + this.pageSize + "&sl=ef3fcb99-de72-4251-aa57-71fe5b6e149f&cc=3&sm=0&PageNumber=" + this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar o html
		this.currentDoc = fetchDocument(url);

		Elements products =  this.currentDoc.select(".prateleira.vitrine.n3colunas > ul > li[layout]");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			for(Element e : products) {
				// InternalPid
				String internalPid = null;
				
				// InternalId
				String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "span[data-sku]", "data-sku");
				
				// Url do produto
				String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-name > a", "href");
				
				saveDataProduct(internalId, internalPid, productUrl);
				
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
		return this.arrayProducts.size() > 0 && this.arrayProducts.size() % this.pageSize == 0;
	}

}
