package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilCasaeconstrucaoCrawler extends CrawlerRankingKeywords {

	public BrasilCasaeconstrucaoCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 48;
			
		this.log("Página "+ this.currentPage);
				
		//monta a url com a keyword e a página
		String url = "http://busca.cec.com.br/busca?q="+ this.keywordEncoded +"&results_per_page=96&page="+ this.currentPage;
		
		this.log("Link onde são feitos os crawlers: "+url);	
			
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		Elements id =  this.currentDoc.select("div.row.products > div > div.product");
		
		int count=0;
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(id.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalBusca == 0) setTotalBusca();
			
			for(Element e: id) {
				count++;
				//seta o id com o seletor
				String internalPid 	= null;
				String internalId 	= e.attr("data-product-id");
				
				//monta a url
				Element eUrl = e.select("a.name-and-brand").first();
				
				int x = eUrl.attr("href").indexOf("link=");
				int y = eUrl.attr("href").indexOf("&", x+5);
				
				String productUrl = eUrl.attr("href").substring(x+5, y);
				
				saveDataProduct(internalId, internalPid, productUrl);
				
				this.log("InternalId do produto da "+count+" da página "+ this.currentPage +": "+ internalId +" url: "+ productUrl);
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
		
		if(this.arrayProducts.size() < this.totalBusca){
			//tem próxima página
			return true;
		} else {
			//não tem próxima página
			return false;
		}
		
	}
	
	@Override
	protected void setTotalBusca()
	{
		Element totalElement = this.currentDoc.select("span.neemu-search-count").first();
		
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
