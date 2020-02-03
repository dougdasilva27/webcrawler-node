package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilIbyteCrawler extends CrawlerRankingKeywords {

	public BrasilIbyteCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 12;
	
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "http://www.ibyte.com.br/catalogsearch/result/index/?p="+ this.currentPage +"&limit=60&q="+ this.keywordEncoded;
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products = this.currentDoc.select("ul.products-grid li.item");
		boolean noResults = this.currentDoc.select(".msg-naoencontrado").first() == null;

		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1 && noResults) {
			for(Element e : products) {
				// InternalPid
				String internalPid 	= crawlInternalPid(e);
				
				// InternalId
				String internalId 	= crawlInternalId(e);
				
				// Url do produto
				String urlProduct = crawlProductUrl(e);
				
				saveDataProduct(internalId, internalPid, urlProduct);
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
				if(this.arrayProducts.size() == productsLimit){
					break;
				}
			}
		} else {
			setTotalProducts();
			this.result = false;
			this.log("Keyword sem resultado!");
		}
	
		if(!hasNextPage()) {
			setTotalProducts();
		}
		
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
	}

	@Override
	protected boolean hasNextPage() {
		return this.currentDoc.select(".next.disable").first() == null && this.arrayProducts.size() >= 12;
	}

	private String crawlInternalId(Element e){
		String internalId = null;
		Element idElement = e.select(".sku").first();
		
		if(idElement != null) {
			String text = idElement.ownText().replaceAll("[^0-9]", "").trim();
			
			if(!text.isEmpty()) {
				internalId = text;
			}
		}
		
		return internalId;
	}
	
	private String crawlInternalPid(Element e){
		String internalPid = null;
		Element idElement = e.select(".trustvox-shelf-container").first();
		
		if(idElement != null){
			internalPid = idElement.attr("data-trustvox-product-code");
		}
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = null;
		Element urlElement = e.select(".product-name a").first();
		
		if(urlElement != null){
			urlProduct = urlElement.attr("href");
		}
		
		return urlProduct;
	}
}
