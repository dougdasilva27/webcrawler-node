package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilDrogarianovaesperancaCrawler extends CrawlerRankingKeywords{

	public BrasilDrogarianovaesperancaCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 12;
	
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "https://www.drogarianovaesperanca.com.br/busca/?Pagina="+ this.currentPage +"&ob=&q="+ this.keywordEncoded +"&G=&ppg=36";
		this.log("Link onde são feitos os crawlers: " + url);	
		
		//chama função de pegar o html
		this.currentDoc = fetchDocument(url);

		Elements products =  this.currentDoc.select(".produto-lista");

		if(totalProducts == 0) {
			setTotalProducts();
		}
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1 && this.totalProducts > 0) {
			for(Element e : products) {
				// InternalPid
				String internalPid = null;
				
				// Url do produto
				String productUrl = crawlProductUrl(e);
				
				// InternalId
				String internalId = crawlInternalId(productUrl);
				
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
		if(this.arrayProducts.size() < this.totalProducts) {
			return true;
		}
			
		return false;
	}
	
	@Override
	protected void setTotalProducts() {
		Element total = this.currentDoc.select(".resultados-busca span").first();
		
		if(total != null) {
			String totalText = total.ownText().replaceAll("[^0-9]", "").trim();
			
			if(!totalText.isEmpty()) {
				this.totalProducts = Integer.parseInt(totalText);
			}
		}
		
		this.log("Total products: " + this.totalProducts);
	}
	
	private String crawlInternalId(String url){
		String internalId = null;
		
		if(url != null) {
			String[] tokens = url.split("-");
			String text = tokens[tokens.length-1].replaceAll("[^0-9]", "").trim();
			
			if(!text.isEmpty()) {
				internalId = text;
			}
		}
		
		return internalId;
	}
	
	private String crawlProductUrl(Element e){
		String productUrl = null;
		Element url = e.select("> a").first();
		
		if(url != null) {
			productUrl = url.attr("href");
			
			if(!productUrl.startsWith("https://www.drogarianovaesperanca.com.br")) {
				productUrl = "https://www.drogarianovaesperanca.com.br" + productUrl;
			}
		}
		
		return productUrl;
	}
}
