package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilAbxclimatizacaoCrawler extends CrawlerRankingKeywords{

	public BrasilAbxclimatizacaoCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 9;
			
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "https://abxarcondicionado.com.br/catalogsearch/result/?q="+ this.keywordEncoded +"&p="+ this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);	
			
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		Elements products =  this.currentDoc.select(".item-products.item .product-item");
				
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {			
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) {
				setTotalProducts();
			}
			
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
		Element nextPage = this.currentDoc.select(".next.i-next").first();
		
		if(nextPage != null) return true;
		
		return false;
	}
	
	@Override
	protected void setTotalProducts() {
		Element totalElement = this.currentDoc.select(".amount:not([href])").first();
		
		if(totalElement != null) {
			try {
				int x = totalElement.text().indexOf("de");
				String token = totalElement.text().substring(x).replaceAll("[^0-9]", "").trim();
				
				this.totalProducts = Integer.parseInt(token);
				
			} catch(Exception e) {
				this.logError(e.getMessage());
			}
			
			this.log("Total da busca: "+this.totalProducts);
		}
	}
	
	private String crawlInternalId(Element e){
		String internalId = null;
		Element internalIdElement = e.select(".sku").first();
		
		if(internalIdElement != null){
			internalId = internalIdElement.text().trim();
		}
		
		return internalId;
	}
	
	private String crawlInternalPid(Element e){
		String internalPid = null;
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = null;
		Element urlElement = e.select("> a").first();	
		
		if(urlElement != null){
			urlProduct = urlElement.attr("href");
		}
		
		return urlProduct;
	}
}
