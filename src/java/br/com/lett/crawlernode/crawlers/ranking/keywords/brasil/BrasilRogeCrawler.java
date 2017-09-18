package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilRogeCrawler extends CrawlerRankingKeywords{

	public BrasilRogeCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 20;
	
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "https://www.roge.com.br/search?q=" + this.keywordEncoded + "&pagenumber="+ this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products =  this.currentDoc.select(".item-box .product-item");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {			
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) {
				setTotalProducts();
			}
			for(Element e : products) {
				
				// InternalPid
				String internalPid = crawlInternalPid(e);
				
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
		return this.currentDoc.select(".next-page") != null;
	}
	
	private String crawlInternalId(Element e){
		return e.attr("data-productid");
	}
	
	private String crawlInternalPid(Element e){
		String internalPid = null;
		Element pid = e.select(".product-title > a small").first();
		
		if(pid != null) {
			internalPid = pid.ownText().replace("(", "").replace(")", "").trim();
		}
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String productUrl = null;
		Element url = e.select(".product-title > a").first();
		
		if(url != null) {
			productUrl = url.attr("href");
			
			if(!productUrl.startsWith("https://www.roge.com.br/")) {
				productUrl = ("https://www.roge.com.br/" + productUrl).replace("br//", "br/");
			}
		}
		
		return productUrl;
	}
}
