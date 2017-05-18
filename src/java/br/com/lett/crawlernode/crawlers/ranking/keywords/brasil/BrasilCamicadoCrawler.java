package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilCamicadoCrawler extends CrawlerRankingKeywords {

	public BrasilCamicadoCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 12;
			
		this.log("Página "+ this.currentPage);
		
		String key = this.keywordWithoutAccents.replaceAll(" ", "-");
		
		//monta a url com a keyword e a página
		String url = "http://www.camicado.com.br/s/"+ key +"/s/0?q="+ this.keywordEncoded +"&ProductGroup1_ps=36&ProductGroup1_p="+ this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);	
			
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		Elements id =  this.currentDoc.select("ul.products-grid > li");
		
		int count=0;
		
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(id.size() >= 1)
		{
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) setTotalProducts();
			
			for(Element e: id)
			{
				count++;
				//seta o id com o seletor
				String internalPid 	= null;
				String internalId 	= e.attr("data-content-id");
				
				//monta a url
				Element eUrl = e.select("h2.product-title > a").first();
				String productUrl  = eUrl.attr("href");
				
				if(!productUrl.contains("camicado")) productUrl = "http://www.camicado.com.br" + productUrl;
				
				saveDataProduct(internalId, internalPid, productUrl);
				
				this.log("InternalId do produto da "+ count +"º posição da página "+ this.currentPage +": "+ internalId);
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
		if(this.arrayProducts.size() < this.totalProducts){
			//tem próxima página
			return true;
		} else {
			//não tem próxima página
			return false;
		}
	}
	
	@Override
	protected void setTotalProducts()
	{
		Element totalElement = this.currentDoc.select("input.productGroupCount").first();
		
		if(totalElement != null)
		{ 	
			try
			{				
				this.totalProducts = Integer.parseInt(totalElement.attr("value"));
			}
			catch(Exception e)
			{
				this.logError(e.getMessage());
			}
			
			this.log("Total da busca: "+this.totalProducts);
		}
	}
}
