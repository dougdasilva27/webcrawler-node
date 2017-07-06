package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilIbyteCrawler extends CrawlerRankingKeywords {

	public BrasilIbyteCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 32;
	
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "http://busca.ibyte.com.br/?busca="+this.keywordEncoded+"&pagina="+this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products = this.currentDoc.select("div#spots ul.products-grid li.item");
		boolean noResults = this.currentDoc.select(".msg-naoencontrado").first() == null;

		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1 && noResults) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) setTotalProducts();
			
			for(Element e : products) {
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
		//se  o número de produtos pegos for menor que o resultado total da busca, existe proxima pagina
		if(this.arrayProducts.size() < this.totalProducts) return true;
		
		return false;
	}
	
	@Override
	protected void setTotalProducts() {
		Element totalElement = this.currentDoc.select("div.mostrando").first();
		
		if(totalElement != null) {
			try {
				int x = totalElement.text().indexOf("de");
				int y = totalElement.text().indexOf("para", x+2);
				
				String token = totalElement.text().substring(x+2, y).trim();
				
				this.totalProducts = Integer.parseInt(token);
			} catch(Exception e) {
				this.logError(e.getMessage());
			}
			
			this.log("Total da busca: "+this.totalProducts);
		}
	}

	private String crawlInternalId(Element e){
		String internalId = null;
		Element idElement = e.select("> a img").first();
		
		if(idElement != null) {
			String text = idElement.attr("data-original");
			
			if(text.contains("/")) {
				String[] tokens = text.split("/");
				String text2 = tokens[tokens.length-1].trim();
				
				if(text2.contains("-")) {
					String[] tokens2 = text2.split("-");
					internalId = tokens2[0].replaceAll("[^0-9]", "");
					
					if(internalId.isEmpty()) {
						internalId = tokens2[tokens2.length-2];
					}
				} else {
					String possibleInternalId = text2.replaceAll("[^0-9]", "").trim();
					
					if(!possibleInternalId.isEmpty()) {
						internalId = possibleInternalId;
					}
				}
			}
		}
		
		return internalId;
	}
	
	private String crawlInternalPid(Element e){
		String internalPid = null;
		
		Element idElement = e.select("> a").first();
		
		if(idElement != null){
			String[] tokens = idElement.attr("href").split("=");
			
			if(tokens[tokens.length-1] != null){
				internalPid = tokens[tokens.length-1].trim();
			}
		}
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = null;
		Element urlElement = e.select("> a").first();
		
		if(urlElement != null){
			String urlBusca = urlElement.attr("href");
			int x = urlBusca.indexOf("url=")+4;
			int y = urlBusca.indexOf(".html", x)+5;
			
			String urlTemp = urlBusca.substring(x, y);
			
			urlProduct = urlTemp.replaceAll("%2f", "/").replaceAll("%3a", ":");
		}
		
		return urlProduct;
	}
}
