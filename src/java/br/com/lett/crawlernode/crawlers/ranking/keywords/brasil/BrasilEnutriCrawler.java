package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilEnutriCrawler extends CrawlerRankingKeywords{

	public BrasilEnutriCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 12;
	
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "https://www.enutri.com.br/catalogsearch/result/index/?keyword=" + this.keywordEncoded 
				+ "&p=" + this.currentPage + "&q=" + this.keywordEncoded;
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar o html
		this.currentDoc = fetchDocument(url);

		Elements products =  this.currentDoc.select(".products-grid li .ma-box-content");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
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
		 return this.currentDoc.select(".i-next").first() != null;
	}
	
	private String crawlInternalId(Element e){
		String internalId = null;
		Element idElement = e.select(".actions li a").first();
		
		if(idElement != null) {
			String[] tokens = idElement.attr("href").split("product/");
			internalId = tokens[1].split("/")[0];
		}
		
		return internalId;
	}
	
	private String crawlProductUrl(Element e){
		String productUrl = null;
		Element url = e.select(".product-name > a").first();
		
		if(url != null) {
			productUrl = url.attr("href");
			
			if(!productUrl.startsWith("https://www.enutri.com.br/")) {
				productUrl = "https://www.enutri.com.br/" + productUrl;
			}
		}
		
		return productUrl;
	}
}
