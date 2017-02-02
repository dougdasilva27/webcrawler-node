package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilCdiscountCrawler extends CrawlerRankingKeywords {

	public BrasilCdiscountCrawler(Session session) {
		super(session);
	}

	private boolean isCategory;
	
	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 20;
			
		this.log("Página "+ this.currentPage);
		
		String key = this.location.replaceAll(" ", "%20");
		
		//monta a url com a keyword e a página
		String url = "http://busca.cdiscount.com.br/?strBusca="+ key +"&paginaAtual="+ this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);	
			
		
		if(this.currentPage > 1 && isCategory) url = this.currentDoc.baseUri().replaceAll("&paginaAtual="+(this.currentPage-1), "") +"&paginaAtual="+ this.currentPage;
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		Elements products =  this.currentDoc.select("ul.vitrineProdutos > li > div > a[data-id]");
		
		if(this.currentPage == 1){
			if(!this.currentDoc.baseUri().equals(url)){
				isCategory = true;
			} else {
				isCategory = false;
			}
		}
		
		if(isCategory) products = this.currentDoc.select("ul.vitrineProdutos > li");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1 && (!isCategory || (isCategory && nextPageFromCategory()))) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalBusca == 0) setTotalBusca();
			
			for(Element e: products) {
				String internalPid;
				String internalId;
				String productUrl;
				
				if(!isCategory){
					//seta o id com o seletor
					internalPid = e.attr("data-id");
					internalId 	= null;
					
					//monta a url
					productUrl = e.attr("href");
				} else {
					//seta o id com o seletor
					String[] tokens = e.attr("id").split("-");
					internalPid = null;
					internalId 	= tokens[tokens.length-1];
					
					//monta a url
					Element eUrl = e.select("> div > a").first();
					productUrl = eUrl.attr("href");
				}
				
				saveDataProduct(internalId, internalPid, productUrl);
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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
		
		if(this.arrayProducts.size() < this.totalBusca){
			if(isCategory){
			
				return nextPageFromCategory();
				
			} else {
				return true;
			}
		} else {
			return false;
		}
		
	}
	
	@Override
	protected void setTotalBusca()
	{
		Element totalElement = this.currentDoc.select("div.resultado strong").first();
		
		if(totalElement != null)
		{ 	
			try
			{
				if(isCategory){
					
					this.totalBusca = Integer.parseInt(totalElement.text());
					
				} else {
					String token = CommonMethods.removeParentheses(totalElement.text()).trim();
					
					this.totalBusca = Integer.parseInt(token);
				}
			}
			catch(Exception e)
			{
				this.logError(e.getMessage());
			}
			
			this.log("Total da busca: "+this.totalBusca);
		}
	}
	
	private boolean nextPageFromCategory(){
		Element pageAtual = this.currentDoc.select("li.atual > strong").first();
		
		if(!pageAtual.text().equals(Integer.toString(this.currentPage))) 	return false;
		else																return true;
	}
}
