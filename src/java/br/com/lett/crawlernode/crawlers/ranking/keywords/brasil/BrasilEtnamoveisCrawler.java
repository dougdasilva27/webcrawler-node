package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilEtnamoveisCrawler extends CrawlerRankingKeywords{

	public BrasilEtnamoveisCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 40;
			
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "https://www.etna.com.br/etna/browse?No="+ this.arrayProducts.size() +"&Nrpp=60&Ntt="+ this.keywordEncoded;
		this.log("Link onde são feitos os crawlers: "+url);	
			
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		Elements products =  this.currentDoc.select("div.produto");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) setTotalProducts();
			
			for(Element e: products) {
				//seta o id com o seletor
				Element pid 		= e.select("input[name=\"/atg/commerce/catalog/comparison/ProductListHandler.productId\"]").first();
				String internalPid 	= pid.attr("value");
				String internalId 	= e.attr("id");
				
				//monta a url
				Element eUrl = e.select("> a").first();
				String urlProduct  = "https://www.etna.com.br"+ eUrl.attr("href");
				
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
		
		if(this.arrayProducts.size() < this.totalProducts){
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	protected void setTotalProducts() {
		Element totalElement = this.currentDoc.select("header.titulo > h1").first();
		
		if(totalElement != null) { 	
			try {
				String token = (totalElement.text().replaceAll("[^0-9]", "")).trim();
				
				this.totalProducts = Integer.parseInt(token);
			} catch(Exception e) {
				this.logError(e.getMessage());
			}
			
			this.log("Total da busca: "+this.totalProducts);
		}
	}
}
