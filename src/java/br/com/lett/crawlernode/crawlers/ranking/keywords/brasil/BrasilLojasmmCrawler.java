package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilLojasmmCrawler extends CrawlerRankingKeywords{

	public BrasilLojasmmCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 30;
	
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "https://www.lojasmm.com/comprar/encontrar.php?set="+ this.keywordEncoded +"&mais="+ this.arrayProducts.size();
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		Elements products =  this.currentDoc.select(".listaprod .li .contentDescription");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {			
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalBusca == 0) setTotalBusca();
			for(Element e : products) {
				// InternalPid
				String internalPid 	= crawlInternalPid(e);
				
				// InternalId
				String internalId 	= crawlInternalId(e);
				
				// Url do produto
				String urlProduct = crawlProductUrl(e);
				
				saveDataProduct(internalId, internalPid, urlProduct);
		
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
				if(this.arrayProducts.size() == productsLimit) break;
			}
		} else {
			setTotalBusca();
			this.result = false;
			this.log("Keyword sem resultado!");
		}
	
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
	}

	@Override
	protected boolean hasNextPage() {
		Element nextPage = this.currentDoc.select(".ProxAnte form[name=p1]").first();
		
		if(nextPage != null) return true;
			
		return false;
		
	}
	
	@Override
	protected void setTotalBusca()	{
		Element totalElement = this.currentDoc.select(".result-count strong span").first();
		
		if(totalElement != null) { 	
			try	{				
				this.totalBusca = Integer.parseInt(totalElement.text().replaceAll("[^0-9]", "").trim());
			} catch(Exception e) {
				this.logError(e.getMessage());
			}
			
			this.log("Total da busca: "+this.totalBusca);
		}
	}
	
	private String crawlInternalId(Element e){
		String internalId = null;
		
		return internalId;
	}
	
	private String crawlInternalPid(Element e){
		String internalPid = null;
		Element interanlPidElement = e.select("meta[itemprop=productID]").first();
		
		if(interanlPidElement != null){
			internalPid = interanlPidElement.attr("content");
		}
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = null;
		Element urlElement = e.select("> a").first();	
		
		if(urlElement != null){
			String href = urlElement.attr("href");
			
			if(href.startsWith("..")){
				urlProduct = "https://www.lojasmm.com" + href.replace("..", "");
			} else if(!href.startsWith("https://www.lojasmm.com")) {
				urlProduct = "https://www.lojasmm.com" + href;
			} else {
				urlProduct = href;
			}
		}
		
		return urlProduct;
	}
}
