package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilPoupafarmaCrawler extends CrawlerRankingKeywords{

	public BrasilPoupafarmaCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 32;
	
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "http://www.poupafarma.com.br/busca/" + this.keywordWithoutAccents.replace(" ", "-") + "?pagina=" + this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar o html
		this.currentDoc = fetchDocument(url);

		Elements products =  this.currentDoc.select(".listagem-prod .item-prod .nome a");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			if(totalProducts == 0) {
				setTotalProducts();
			}
			
			for(int i = arrayProducts.size(); i < products.size(); i++) {
				Element e = products.get(i);
				
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
		return this.currentDoc.select("#botao-paginacao").first() != null;
	}
	
	@Override
	protected void setTotalProducts() {
		Element total = this.currentDoc.select(".row .columns .large-12 > p").first();
		
		if(total != null && total.ownText().contains("de")) {
			String totalText = total.ownText().trim().split("de")[1].replaceAll("[^0-9]", "").trim();
			
			if(!totalText.isEmpty()) {
				this.totalProducts = Integer.parseInt(totalText);
			}
		}
		
		this.log("Total products: " + this.totalProducts);
	}
	
	private String crawlInternalId(Element e){
		String internalId = null;
		
		String[] tokens = e.attr("href").split("/");
		String id = tokens[tokens.length-1].split("-")[0].replaceAll("[^0-9]", "").trim();
		
		if(!id.isEmpty()) {
			internalId = id;
		}
		
	
		return internalId;
	}
	
	private String crawlProductUrl(Element e){
		String productUrl = e.attr("href");
			
		if(!productUrl.startsWith("http://www.poupafarma.com.br/")) {
			productUrl = ("http://www.poupafarma.com.br/" + productUrl).replace("br//", "br/");
		}
		
		return productUrl;
	}
}
