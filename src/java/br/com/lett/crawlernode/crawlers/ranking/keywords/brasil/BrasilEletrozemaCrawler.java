package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilEletrozemaCrawler extends CrawlerRankingKeywords{

	public BrasilEletrozemaCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 24;

		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "https://www.zema.com/busca.asp?palavrachave="+this.keywordEncoded+"&idpage="+this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);			
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		Elements products =  this.currentDoc.select(".produtos .getLink");
		Elements results = this.currentDoc.select("div.resultadoSemelhante");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1 && results.size() < 1) {
			for(Element e: products) {
				//seta o id da classe pai com o id retirado do elements
				String[] tokens = e.attr("href").split("/");
				
				String internalPid 	= null;
				String internalId 	= tokens[tokens.length-2];
				
				String urlProduct = e.attr("href").replaceAll("`", "%60");
				
				saveDataProduct(internalId, internalPid, urlProduct);
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
				if(this.arrayProducts.size() == productsLimit) break;
			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado na página atual!");
		}
	
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");	
		if(!(hasNextPage())) setTotalBusca();
	}

	@Override
	protected boolean hasNextPage() {
		Elements page = this.currentDoc.select("a.last.inactive");
		
		//se  elemeno page obtiver algum resultado.
		if(page.size() > 0)
		{
			//não tem próxima página
			return false;
		}
		else
		{
			//tem próxima página
			return true;
		}
	}

}
