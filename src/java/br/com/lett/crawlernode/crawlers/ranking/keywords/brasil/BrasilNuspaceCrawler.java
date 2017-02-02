package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilNuspaceCrawler extends CrawlerRankingKeywords {

	public BrasilNuspaceCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 36;
	
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "http://www.nuspace.com.br/catalogsearch/result/index/?limit=100&p="+this.currentPage+"&q="+this.keywordEncoded;
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products =  this.currentDoc.select("div.products-grid-container div.item-inner");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {		
			for(Element e : products) {
				//monta o InternalId
				Element inid 		= e.select("span.regular-price").first();
				String[] tokens 	= inid.attr("id").split("-");
				String internalId 	= tokens[tokens.length-1];
				
				//monta o pid
				String internalPid  = makePid(e);
				
				//monta a url
				Element urlElement = e.select("h2.product-name > a").first();
				String productUrl = urlElement.attr("title");
				
				saveDataProduct(internalId, internalPid, productUrl);
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
				if(this.arrayProducts.size() == productsLimit) break;
				
			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
		}
	
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
		if(!(hasNextPage())) setTotalBusca();
	}

	@Override
	protected boolean hasNextPage() {
		Elements page = this.currentDoc.select("a.next.i-next");
		
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
	
	private String makePid(Element e)
	{
		Element pid = e.select("div.actions > a > img").first();
		if(!pid.attr("src").contains("sem-foto"))
		{
			String[] tokens2 = pid.attr("src").split("/");
			String temp      = tokens2[tokens2.length-1];
			int x;
			int y = StringUtils.countMatches(temp, "_");
			if(y < 3)
			{
				if(temp.contains("_")) x = temp.indexOf("_");
				else				   x = temp.indexOf(".");
				return temp.substring(0,x);
			}
			else
			{
				return null;
			}
		}
		else
		{
			return null;
		}
	}
}
