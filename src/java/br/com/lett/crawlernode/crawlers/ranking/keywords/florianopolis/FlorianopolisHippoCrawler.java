package br.com.lett.crawlernode.crawlers.ranking.keywords.florianopolis;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class FlorianopolisHippoCrawler extends CrawlerRankingKeywords{

	public FlorianopolisHippoCrawler(Session session) {
		super(session);
	}

	private Elements id;

	@Override
	protected void extractProductsFromCurrentPage() {			
		
		this.log("Página "+ this.currentPage);
		//monta a url com a keyword e a página
		String url = "http://www.hippo.com.br/produtos/?busca="+this.keywordEncoded+"&bt_buscar=";
		this.log("Link onde são feitos os crawlers: "+url);
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
			
		this.id =  this.currentDoc.select("div.product-block h3 a.btn-quickview-box");
		
		int count= 0;		
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(this.id.size() >=1)
		{
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) setTotalProducts();
			
			for(Element e: this.id)
			{
				count++;
				//monta o InternalPid e o InternalId
				String internalPid 	= e.attr("data-codigo");
				String internalId 	= e.attr("data-id");
				
				//monta a url
				String productUrl = e.attr("data-path");
				
				saveDataProduct(internalId, internalPid, productUrl);
				
				this.log("InternalPid do produto da "+count+"° posição  da página "+ this.currentPage+ ": " + internalPid);
				if(this.arrayProducts.size() == productsLimit) break;
				
			}
		}
		else
		{	
			this.result = false;
			this.log("Keyword sem resultado!");
		}
		
		//número de produtos por página do market
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
			
		
	}

	@Override
	protected boolean hasNextPage() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	protected void setTotalProducts()
	{
		Element totalElement = this.currentDoc.select("span.total_count strong").first();
		
		if(totalElement != null)
		{
			try
			{
				int x = totalElement.text().indexOf("ite");
				
				String token = totalElement.text().substring(0, x).trim();
				
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
