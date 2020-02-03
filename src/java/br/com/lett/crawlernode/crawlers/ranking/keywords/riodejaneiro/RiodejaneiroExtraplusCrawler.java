package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class RiodejaneiroExtraplusCrawler extends CrawlerRankingKeywords {
	
	public RiodejaneiroExtraplusCrawler(Session session) {
		super(session);
	}

	@Override
	public void extractProductsFromCurrentPage() 
	{
		//número de produtos por página do market
		this.pageSize = 20;

		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "http://www.extraplus.com.br/busca?page="+this.currentPage+"&q="+this.keywordEncoded;
		this.log("Link onde são feitos os crawlers: "+url);			
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		Elements id = this.currentDoc.select("div.prodCelula");
		
		int count=0;
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(id.size() >= 1)
		{
			for(Element e: id)
			{
				count++;
				//seta o id da classe pai com o id retirado do elements
				Element pidUrl	   = e.select("div.nome > a").first();
				String[] tokens    = pidUrl.attr("href").split("/");
				String token 	   = tokens[tokens.length-1];
				int x 			   = token.indexOf("-");
				String internalPid = token.substring(0, x);
				
				//monta o InternalId
				Element inid 	   = e.select("div.produtoQnt label").first();
				String[] tokens2   = inid.attr("for").split("_");
				String internalId  = tokens2[tokens2.length-1];
				
				//monta a url
				String productUrl = pidUrl.attr("href");
				
				saveDataProduct(internalId, internalPid, productUrl);
				
				this.log("InternalId do produto da "+count+" da página "+ this.currentPage+ ": " + internalId);
				if(this.arrayProducts.size() == productsLimit) break;
				
			}
		}
		else
		{
			this.result = false;
			this.log("Keyword sem resultado!");
		}

		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
		if(!(hasNextPage())) setTotalProducts();
	}
	
	@Override
	protected boolean hasNextPage() {
		Elements page = this.currentDoc.select("span.next_page.disabled");

		//se  elemeno page obtiver algum resultado
		//não tem próxima página
		//tem próxima página
		return page.size() <= 0;
	}
}
