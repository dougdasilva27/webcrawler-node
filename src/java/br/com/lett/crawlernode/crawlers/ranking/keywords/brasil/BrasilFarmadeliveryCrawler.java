package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilFarmadeliveryCrawler extends CrawlerRankingKeywords{

	public BrasilFarmadeliveryCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 36;

		this.log("Página "+ this.currentPage);

		//monta a url com a keyword e a página
		String url = "https://www.farmadelivery.com.br/catalogsearch/result/?p="+this.currentPage+"&q="+this.keywordEncoded;
		this.log("Link onde são feitos os crawlers: "+url);	

		//chama função de pegar url
		this.currentDoc = fetchDocument(url);

		Elements products =  this.currentDoc.select("div .products-grid"); 

		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) setTotalProducts();

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
		Element page = this.currentDoc.select("a.next").first();

		//se  elemeno page obtiver algum resultado.
		if(page != null) return true;

		//não tem próxima página
		return false;

	}


	@Override
	protected void setTotalProducts() {
		int x = 0;
		String token;
		Element totalElement = this.currentDoc.select("p.amount").first();

		try {
			if(totalElement.text().contains("de")) {
				x = totalElement.text().indexOf("de");
				token = totalElement.text().substring(x+2).trim();
			} else {
				x = totalElement.text().indexOf("Item");
				token = totalElement.text().substring(0, x).trim();
			}
			this.totalProducts = Integer.parseInt(token);
		} catch(Exception e) {
			this.logError(e.getMessage());
		}
		this.log("Total da busca: "+this.totalProducts);
	}
	
	private String crawlInternalId(Element e){
		String internalId = null;
		Element internalIdElement = e.select("div.price-box span[id]").first();
		
		if(internalIdElement != null){
			String[] tokens = internalIdElement.attr("id").split("-");
			internalId 	= tokens[tokens.length-1];
		}
		
		return internalId;
	}
	
	private String crawlInternalPid(Element e){
		String internalPid = null;
		Element pid =  e.select("> span").first();
		
		if(pid != null){
			String[] tokens2 = pid.text().split(":");
			internalPid = tokens2[tokens2.length-1].trim();
		}
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = null;
		Element urlElement = e.select("div.product-name h2 a").first();
		
		if(urlElement != null){
			urlProduct = urlElement.attr("href");
		}
		
		return urlProduct;
	}
}
