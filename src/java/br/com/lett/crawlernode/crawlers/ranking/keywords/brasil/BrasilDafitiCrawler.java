package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilDafitiCrawler extends CrawlerRankingKeywords{

	public BrasilDafitiCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 96;

		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "http://www.dafiti.com.br/catalog/?q="+this.keywordEncoded+"&page="+this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);			
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products =  this.currentDoc.select("div.product-box div[id]");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) setTotalProducts();
						
			for(Element e : products) {
				//seta o id da classe pai com o id retirado do elements
				String internalPid 	= e.attr("id");
				String internalId 	= null;
				
				//monta a url
				Element urlElement = e.select(" > a").first();
				String productUrl = urlElement.attr("href");
				
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
		Elements page = this.currentDoc.select("li a[title=\"Próximo\"]");
		
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
	protected void setTotalProducts()
	{
		Element totalElement = this.currentDoc.select("div.items-products.select-options-item span").first();
		
		try
		{
			if(totalElement != null) this.totalProducts = Integer.parseInt(totalElement.text());
		}
		catch(Exception e)
		{
			this.logError(e.getMessage());
		}
		
		this.log("Total da busca: "+this.totalProducts);
	}

}
