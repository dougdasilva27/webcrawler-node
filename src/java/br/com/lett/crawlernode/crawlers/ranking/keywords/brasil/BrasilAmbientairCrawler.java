package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilAmbientairCrawler extends CrawlerRankingKeywords {

	public BrasilAmbientairCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 8;
			
		this.log("Página "+ this.currentPage);
		
		String key = this.location.replaceAll(" ", "%20");
		
		//monta a url com a keyword e a página
		String url = "http://www.ambientair.com.br/busca.html?loja=110&acao=BU&busca="+ key +"&passo=exibeTodos&ordem=PROD_NOME&pagina="+this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);	
			
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		Elements id =  this.currentDoc.select("div.divProdutos ul > li");
		
		int count=0;
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(id.size() >= 1)
		{			
			for(Element e : id)
			{
				count++;
				//seta o id com o seletor
				Element eForId 		= e.select("input.compare").first();
				String internalId 	= eForId.attr("value");
				String internalPid 	= null;
				
				//monta a url
				Element eUrl = e.select("> a.produto").first();
				String productUrl	 = eUrl.attr("href");
				
				saveDataProduct(internalId, internalPid, productUrl);
				
				this.log("InternalId do produto da "+count+" da página "+ this.currentPage+ ": " + internalId + " url: " + productUrl);
				if(this.arrayProducts.size() == productsLimit) break;	
			}
		}
		else
		{
			this.result = false;
			setTotalBusca();
			this.log("Keyword sem resultado na página atual!");
		}

		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
	}

	@Override
	protected boolean hasNextPage() {
		
		return true;
	}
}
