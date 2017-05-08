package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilSupermuffatoCrawler extends CrawlerRankingKeywords{

	public BrasilSupermuffatoCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 24;
	
		this.log("Página "+ this.currentPage);
		
		String keyword = keywordWithoutAccents.replaceAll(" ", "%20");
		
		//monta a url com a keyword e a página
		String url = "http://www.supermuffato.com.br/"+ keyword +"?PageNumber="+ this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements id =  this.currentDoc.select("li[layout]");		
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(id.size() >= 1) {
			
			int indexProduct = 0;
			
			for(Element e : id) {				
				// InternalPid
				String internalPid 	= crawlInternalPid(indexProduct);
				
				// InternalId
				String internalId = crawlInternalId(e);
				
				// Url do produto
				String urlProduct = crawlProductUrl(e);
				
				indexProduct++;
				
				saveDataProduct(internalId, internalPid, urlProduct);
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
				if(this.arrayProducts.size() == productsLimit) break;
			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
			setTotalBusca();
		}
	
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
		if(!(hasNextPage())) setTotalBusca();
	}

	@Override
	protected boolean hasNextPage() {
		Elements page = this.currentDoc.select(".prd-list-item-desc > a");
		
		//se  elemeno page obtiver algum resultado
		if(page.size() >= 24){
			//tem próxima página
			return true;
		}
		
		return false;
	}
	

	private String crawlInternalId(Element e){
		String internalId = null;
		
		return internalId;
	}
	
	private String crawlInternalPid(int indexProduct){
		String internalPid = null;
		Element pid = this.currentDoc.select("li[id].helperComplement").get(indexProduct);		
		
		if(pid != null){
			internalPid = pid.attr("id").replaceAll("[^0-9]", "").trim();
		}
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = null;
		Element urlElement = e.select(".prd-list-item-desc > a").first();
		
		if(urlElement != null){
			urlProduct = urlElement.attr("href").trim();
		}
		
		return urlProduct;
	}
}
