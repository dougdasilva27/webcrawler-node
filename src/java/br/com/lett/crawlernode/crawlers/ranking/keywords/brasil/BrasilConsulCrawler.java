package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilConsulCrawler extends CrawlerRankingKeywords {

	public BrasilConsulCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 9;
			
		this.log("Página "+ this.currentPage);
		
		String key = this.keywordWithoutAccents.replaceAll(" ", "%20");
		
		//monta a url com a keyword e a página
		String url = "http://loja.consul.com.br/"+ key +"?PS=50&PageNumber="+ this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);	
			
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		Elements products =  this.currentDoc.select("div.vitrine div.prateleira > ul > li[layout] > article");

		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) setTotalProducts();
			
			for(Element e: products) {
				//seta o id com o seletor
				String internalPid 	= e.attr("data-idproduto");
				String internalId 	= null;
				
				//monta a url
				Element eUrl = e.select("a.prod-info").first();
				String productUrl  = eUrl.attr("href");
				
				saveDataProduct(internalId, internalPid, productUrl);
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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
			//tem próxima página
			return true;
		} else {
			//não tem próxima página
			return false;
		}
	}
	
	@Override
	protected void setTotalProducts()
	{
		Element totalElement = this.currentDoc.select("span.resultado-busca-numero > span.value").first();
		
		if(totalElement != null)
		{ 	
			try
			{				
				this.totalProducts = Integer.parseInt(totalElement.text().trim());
			}
			catch(Exception e)
			{
				this.logError(e.getMessage());
			}
			
			this.log("Total da busca: "+this.totalProducts);
		}
	}
}
