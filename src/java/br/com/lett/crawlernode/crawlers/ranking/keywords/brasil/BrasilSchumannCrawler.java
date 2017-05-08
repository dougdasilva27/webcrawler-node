package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilSchumannCrawler extends CrawlerRankingKeywords{

	public BrasilSchumannCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		this.log("Página "+ this.currentPage);
		
		//número de produtos por página do market
		this.pageSize = 28;
		
		String keyword = this.keywordWithoutAccents.replaceAll(" ", "%20");
		
		//monta a url com a keyword e a página
		String url = "http://www.schumann.com.br/"+keyword+"?PageNumber="+this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements id =  this.currentDoc.select("div div.prateleira > ul > li[layout]");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(id.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalBusca == 0) setTotalBusca();
			
			for(Element e: id) {
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
			this.result = false;
			this.log("Keyword sem resultado!");
		}
	
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
	}

	@Override
	protected boolean hasNextPage() {
		if(arrayProducts.size() < this.totalBusca){
			return true;
		}
		
		return false;
	}
	
	@Override
	protected void setTotalBusca() {
		Element totalElement = this.currentDoc.select("span.resultado-busca-numero span.value").first();
		
		try {
			if(totalElement != null) this.totalBusca = Integer.parseInt(totalElement.text());
		} catch(Exception e) {
			this.logError(e.getMessage());
		}
		
		this.log("Total da busca: "+this.totalBusca);
	}
	
	private String crawlInternalId(Element e){
		String internalId = null;
		
		return internalId;
	}
	
	private String crawlInternalPid(Element e){
		String internalPid = null;
		Element pidElement = e.select(".id-produto").first();
		
		if(pidElement != null){
			internalPid = pidElement.text();
		}
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = null;
		Element urlElement = e.select(".data > h3 a").first();
		
		if(urlElement != null){
			urlProduct = urlElement.attr("href");
		}
		
		return urlProduct;
	}
}
