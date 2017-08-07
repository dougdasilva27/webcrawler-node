package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilCallfarmaCrawler extends CrawlerRankingKeywords{

	public BrasilCallfarmaCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 20;
	
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "https://www.callfarma.com.br/busca/" + this.keywordEncoded + "&limit=" + this.arrayProducts.size();
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar o html
		this.currentDoc = fetchDocument(url);

		Elements products =  this.currentDoc.select("#contentBoxProduto div[class^=boxProduto] .detalhes");
		
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
		Element finalPage = this.currentDoc.select(".linkAtual").first();
		
		if(finalPage != null) {
			String page = finalPage.ownText().replaceAll("[^0-9]", "").trim();
			
			if(!page.isEmpty() && (this.currentPage < Integer.parseInt(page))) {
				return true;
			}
		}
			
		return false;
	}
	
	private String crawlInternalId(Element e){
		String internalId = null;
		Element id = e.select("#codigoInterno").first();
		
		if(id != null) {
			internalId = id.ownText().trim();
		}
		
		return internalId;
	}
	
	private String crawlProductUrl(Element e){
		String productUrl = null;
		Element url = e.select("> a").first();
		
		if(url != null) {
			productUrl = url.attr("href");
			
			if(!productUrl.startsWith("https://www.callfarma.com.br")) {
				productUrl = "https://www.callfarma.com.br" + productUrl;
			}
		}
		
		return productUrl;
	}
}
