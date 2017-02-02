package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilDrogariapachecoCrawler extends CrawlerRankingKeywords{

	public BrasilDrogariapachecoCrawler(Session session) {
		super(session);
	}

	private Elements id;

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 18;

		this.log("Página "+this.currentPage);
		
		//se a key contiver o +, substitui por %20, pois nesse market a pesquisa na url é assim
		String key = this.location.replaceAll(" ", "%20");
		
		//monta a url com a keyword e a página
		String url  = "http://www.drogariaspacheco.com.br/"+key+"?PS=50&PageNumber="+this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		this.id =  this.currentDoc.select("div.vitrine.resultItemsWrapper div.prateleira > ul > li[layout]");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(this.id.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalBusca == 0) setTotalBusca();
			
			for(Element e : this.id) {
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
		//se  o número de produtos pegos for menor que o resultado total da busca, existe proxima pagina
		if(this.arrayProducts.size() < this.totalBusca) return true;
		
		return false;
	}

	@Override
	protected void setTotalBusca() {
		Element totalElement = this.currentDoc.select("span.resultado-busca-numero span.value").first();
		
		try	{
			if(totalElement != null) this.totalBusca = Integer.parseInt(totalElement.text());
		} catch(Exception e) {
			this.logError(e.getMessage());
		}
		
		this.log("Total da busca: "+this.totalBusca);
	}
	
	private String crawlInternalId(Element e){
		String internalId = null;
		Element ids = e.select("ul.product-info li.product-id").first(); 
		
		if(ids != null){
			internalId = ids.text().trim();
		}
		
		return internalId;
	}
	
	private String crawlInternalPid(Element e){
		String internalPid = null;
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = null;
		Element urlElement = e.select("> a.productPrateleira").first();
		
		if(urlElement != null){
			urlProduct = urlElement.attr("href");
		}
		
		return urlProduct;
	}
}
