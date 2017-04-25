package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilDimedCrawler extends CrawlerRankingKeywords{

	public BrasilDimedCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		this.log("Página "+ this.currentPage);
		
		//número de produtos por página do market
		this.pageSize = 9;
		
		//monta a url com a keyword e a página
		String url = "https://www.dimed.com.br/clientes/produto/searchProduto?busca="+this.keywordEncoded+"&offset="+this.arrayProducts.size();
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products =  this.currentDoc.select("div#itens");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalBusca == 0) setTotalBusca();
			
			for(Element e: products) {
				//seta e monta os ids
				Element ids     = e.select("ul li.codigo").first();
				String[] tokens = ids.text().split(":");
				
				String internalPid 	= tokens[tokens.length-1].trim();
				String internalId 	= tokens[tokens.length-1].trim();
				
				//monta url
				Element eUrl = e.select("div#descricao a").first();
				String productUrl  = "https://www.dimed.com.br"+eUrl.attr("href");
				
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
		Elements page = this.currentDoc.select("a.nextLink");
		//se  elemeno page obtiver algum resultado
		if(page.size() >= 1)
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
		Element totalElement = this.currentDoc.select("div.mensagem strong").first();
		
		try
		{
			if(totalElement != null) this.totalBusca = Integer.parseInt(totalElement.text());
		}
		catch(Exception e)
		{
			this.logError(e.getMessage());
		}
		
		this.log("Total da busca: "+this.totalBusca);
	}
}
