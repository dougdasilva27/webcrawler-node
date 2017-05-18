package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilStrarCrawler extends CrawlerRankingKeywords{

	public BrasilStrarCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 12;
			
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "http://www.strar.com.br/catalogsearch/result/index/?limit=192&q="+ this.keywordEncoded;
		
		if(this.currentPage > 1) url += "&p="+ this.currentPage; 
		
		this.log("Link onde são feitos os crawlers: "+url);	
			
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		Elements id =  this.currentDoc.select("ul.products-grid > li.item");
		
		int count=0;
		
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(id.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) setTotalProducts();
			
			for(Element e: id) {
				count++;
				//seta o id com o seletor
				Element idPrices 	= e.select("div.price-box span[id]").first();
				String[] tokens 	= idPrices.attr("id").split("-");
				String internalPid 	= null;
				String internalId 	= tokens[tokens.length-1];
				
				//monta a url
				Element eUrl 	= e.select("> a").first();
				String productUrl 	= eUrl.attr("href");
				
				saveDataProduct(internalId, internalPid, productUrl);
				
				this.log("InternalId do produto da "+count+" da página "+ this.currentPage+ ": " + internalId);
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
		Element page = this.currentDoc.select("a.next.i-next").first();
		
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
	protected void setTotalProducts()
	{
		Element totalElement = this.currentDoc.select("div.pager p.amount").first();
		
		if(totalElement != null)
		{ 	
			try
			{
				String token = (totalElement.text().replaceAll("[^0-9]", "")).trim();
				
				this.totalProducts = Integer.parseInt(token);
			}
			catch(Exception e)
			{
				this.logError(e.getMessage());
			}
			
			this.log("Total da busca: "+this.totalProducts);
		}
	}
}
