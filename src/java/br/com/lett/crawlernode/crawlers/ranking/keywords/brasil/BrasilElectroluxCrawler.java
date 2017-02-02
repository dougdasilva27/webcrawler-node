package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilElectroluxCrawler extends CrawlerRankingKeywords {

	public BrasilElectroluxCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 12;
			
		this.log("Página "+ this.currentPage);
		
		String key = this.location.replaceAll(" ", "%20");
		
		//monta a url com a keyword e a página
		String url = "http://loja.electrolux.com.br/"+ key +"?PS=50&PageNumber="+this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);	
			
		//chama função de pegar a url	
		this.currentDoc = fetchDocument(url);
		
		Elements products =  this.currentDoc.select("div.prateleira.vitrine div.prateleira.vitrine > ul > li");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalBusca == 0) setTotalBusca();
			
			for(int i = 0; i < products.size(); i++) {
				if(products.get(i).hasAttr("layout")){
					//seta o id com o seletor
					String[] tokens 	= products.get(i+1).attr("id").split("_");
					String internalPid 	= tokens[tokens.length-1];
					String internalId 	= null;
					
					//monta a url
					Element eUrl = products.get(i).select("a.prateleira__flags").first();
					String productUrl  = eUrl.attr("href");
					
					saveDataProduct(internalId, internalPid, productUrl);
					
					this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
					if(this.arrayProducts.size() == productsLimit) break;
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
		
		//se  elemeno page não obtiver nenhum resultado
		if(this.arrayProducts.size() < this.totalBusca)
		{
			//tem próxima página
			return true;
		}
		else
		{
			//não tem próxima página
			return false;
			
		}
	}
	
	@Override
	protected void setTotalBusca()
	{
		Element totalElement = this.currentDoc.select("span.resultado-busca-numero > span.value").first();
		
		if(totalElement != null)
		{ 	
			try	{
				this.totalBusca = Integer.parseInt(totalElement.text());
			} catch(Exception e) {
				this.logError(e.getMessage());
			}
			
			this.log("Total da busca: "+this.totalBusca);
		}
	}
}
