package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilDrogarianetCrawler extends CrawlerRankingKeywords{

	public BrasilDrogarianetCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 16;
	
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "http://www.drogarianet.com.br/catalogsearch/result/index/?itens=48&p="+ this.currentPage +"&q="+ this.keywordEncoded;
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar o html
		this.currentDoc = fetchDocument(url);

		Elements products =  this.currentDoc.select(".Produtos .Produto");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			if(totalProducts == 0) {
				setTotalProducts();
			}
			
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
		if(this.arrayProducts.size() < this.totalProducts) {
			return true;
		}
			
		return false;
	}
	
	@Override
	protected void setTotalProducts() {
		Element total = this.currentDoc.select(".ToolbarContagem").first();
		
		if(total != null) {
			String totalText = total.ownText().replaceAll("[^0-9]", "").trim();
			
			if(!totalText.isEmpty()) {
				this.totalProducts = Integer.parseInt(totalText);
			}
		}
		
		this.log("Total products: " + this.totalProducts);
	}
	
	private String crawlInternalId(Element e){
		String internalId = null;
		Element idElement = e.select("span[id^=product-price-]").first();
		
		if(idElement != null) {
			String[] tokens = idElement.attr("id").split("-");
			String id = tokens[tokens.length-1].replaceAll("[^0-9]", "").trim();
			
			if(!id.isEmpty()) {
				internalId = id;
			}
		}
		
		return internalId;
	}
	
	private String crawlProductUrl(Element e){
		String productUrl = null;
		Element url = e.select(".Nome > a").first();
		
		if(url != null) {
			productUrl = url.attr("href");
			
			if(!productUrl.startsWith("http://www.drogarianet.com.br/")) {
				productUrl = ("http://www.drogarianet.com.br/" + productUrl).replace("br//", "br/");
			}
		}
		
		return productUrl;
	}
}
