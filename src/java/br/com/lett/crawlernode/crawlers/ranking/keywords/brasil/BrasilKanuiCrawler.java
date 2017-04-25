package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilKanuiCrawler extends CrawlerRankingKeywords{

	public BrasilKanuiCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 100;

		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "http://www.kanui.com.br/catalog/todos-os-produtos/?q="+this.keywordEncoded+"&page="+this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);			
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		Elements products =  this.currentDoc.select("section.products.products-catalog ul li[id]");

		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalBusca == 0) setTotalBusca();					
			
			for(Element e: products) {
				//seta o id da classe pai com o id retirado do elements
				String internalPid 	= e.attr("id");
				String internalId 	= null;
				
				//monta a url
				Element urlElement = e.select("> div > a").first();
				String productUrl = urlElement.attr("href");
				
				saveDataProduct(internalId, internalPid, productUrl);
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
				if(this.arrayProducts.size() == productsLimit) break;
				
			}
		}
		else
		{
			this.result = false;
			this.log("Keyword sem resultado!");
		}
	
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
		
	}

	@Override
	protected boolean hasNextPage() {
		Elements page = this.currentDoc.select("a.pagination-next");
		
		//se  elemeno page obtiver algum resultado
		if(page.size() > 0)
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
		Element totalElement = this.currentDoc.select("strong.txt-items-found").first();
		
		if(totalElement != null)
		{
			try
			{
				int x = totalElement.text().indexOf("ite");
				
				String token = totalElement.text().substring(0, x).trim();
				
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
