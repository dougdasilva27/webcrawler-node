package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilClimarioCrawler extends CrawlerRankingKeywords {

	public BrasilClimarioCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 18;

		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "http://www.climario.com.br/buscapagina?ft="+ this.keywordEncoded +"&PS=18&sl=9e5e0cbc-9822-4c83-a25d-a99036df38b7&cc=18&sm=0&PageNumber="+ this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);			
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		Elements products = this.currentDoc.select(".vitrine li[layout]");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			for(Element e: products) {
				// InternalPid
				String internalPid 	= crawlInternalPid(e);
				
				// InternalId
				String internalId 	= crawlInternalId(e);
				
				// Url do produto
				String urlProduct = crawlProductUrl(e);
				
				saveDataProduct(internalId, internalPid, urlProduct);
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
				if(this.arrayProducts.size() == productsLimit) break;
			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
		}

		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
	}
	
	@Override
	protected boolean hasNextPage() {
		return true;
	}

	private String crawlInternalId(Element e){
		String internalId = null;
		
		return internalId;
	}
	
	private String crawlInternalPid(Element e){
		String internalPid = null;
		Element pid = e.select(".compare-product-checkbox").first();
		
		if(pid != null){
			internalPid = pid.attr("rel");
		}
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = null;
		Element eUrl = e.select("a[title]:not([class])").first();
		
		if(eUrl != null){
			urlProduct = eUrl.attr("href");
		}
		
		return urlProduct;
	}
}
