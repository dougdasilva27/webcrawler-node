package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilPolishopCrawler extends CrawlerRankingKeywords {

	public BrasilPolishopCrawler(Session session) {
		super(session);
	}

	private boolean isCategory;
	
	@Override
	protected void extractProductsFromCurrentPage() {
			
		this.log("Página "+ this.currentPage);
		
		String key = this.keywordWithoutAccents.replaceAll(" ", "%20");
		
		//monta a url com a keyword e a página
		String url = "http://www.polishop.com.br/"+ key +"?&utmi_p=_&utmi_pc=BuscaFullText&utmi_cp="+ key +"&PS=60&PageNumber="+ this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);	
			
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		Elements id = this.currentDoc.select("div.vitrine ul > li[layout]");
		
		if(id.size() < 1 & this.currentPage == 1){
			id = this.currentDoc.select("ul > li[layout]");
			
			if(id.size() > 1) isCategory = true;
		} else {
			isCategory = false;
		}
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(id.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) setTotalProducts();
			
			for(Element e: id) {
				//seta o id com o seletor
				Element eInid 		= e.select("div.id").first();
				String internalPid 	= null;
				String internalId 	= eInid.text();
				
				//monta a url
				String productUrl;
				if(!isCategory){
					Element eUrl 	= e.select("h3.product-name > a").first();
					productUrl 			= eUrl.attr("href");
				} else {
					Element eUrl 	= e.select("div.url").first();
					productUrl 			= eUrl.text();
				}
				
				saveDataProduct(internalId, internalPid, productUrl);
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
				if(this.arrayProducts.size() == productsLimit) break;
			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
		}

		//número de produtos por página do market
		if(isCategory) 	this.pageSize = 0;
		else 			this.pageSize = 12;
		
		
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
	}

	@Override
	protected boolean hasNextPage() {
		
		if(!isCategory){
			if(this.arrayProducts.size() < this.totalProducts){
				//tem próxima página
				return true;
			} else {
				//não tem próxima página
				return false;
			}
		} else {
			return false;
		}
	}
	
	@Override
	protected void setTotalProducts()
	{
		Element totalElement = this.currentDoc.select("span.resultado-busca-numero > span.value").first();
		
		if(totalElement != null)
		{ 	
			try
			{				
				this.totalProducts = Integer.parseInt(totalElement.text().trim());
			}
			catch(Exception e)
			{
				this.logError(e.getMessage());
			}
			
			this.log("Total da busca: "+this.totalProducts);
		}
	}
}
