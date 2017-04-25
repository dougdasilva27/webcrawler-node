package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilEstrela10Crawler extends CrawlerRankingKeywords{

	public BrasilEstrela10Crawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 36;
			
		this.log("Página "+ this.currentPage);
		
		String key = this.location.replaceAll(" ", "%20");
		
		//monta a url com a keyword e a página
		String url = "http://www.estrela10.com.br/pesquisa?t="+ key +"&pg="+ this.currentPage;
				
		this.log("Link onde são feitos os crawlers: "+url);	
			
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		Elements products =  this.currentDoc.select("div.wd-browsing-grid-list > ul > li > div");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalBusca == 0) setTotalBusca();
			
			for(Element e: products) {
				//seta o id com o seletor
				Element ids = e.select("div.nwe10-middle > div").first();
				String internalPid 	= ids.attr("data-pid");
				String internalId 	= ids.attr("data-property-path").replaceAll("[^0-9]", "");
				
				if(internalId.length() < 1) internalId =  internalPid;
				
				//monta a url
				Element eUrl = e.select("a.thumb").first();
				String productUrl = eUrl.attr("href");
				
				if(!productUrl.contains("estrela10")){
					productUrl = "http://www.estrela10.com.br" + productUrl;
				}
				
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
		Element page = this.currentDoc.select("a.page-next").first();
		
		//se  elemeno page não obtiver nenhum resultado
		if(page == null){
			//não tem próxima página
			return false;
		} else {
			//tem próxima página
			return true;
			
		}
	}
	
	@Override
	protected void setTotalBusca()
	{
		Element totalElement = this.currentDoc.select("div.product-count span").first();
		
		if(totalElement != null)
		{ 	
			try
			{
				String token = (totalElement.text().replaceAll("[^0-9]", "")).trim();
				
				this.totalBusca = Integer.parseInt(token);
			}
			catch(Exception e)
			{
				this.logError(e.getMessage());
			}
			
			this.log("Total da busca: "+this.totalBusca);
		}
	}
}
