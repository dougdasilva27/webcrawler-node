package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilRamsonsCrawler extends CrawlerRankingKeywords {
	
	public BrasilRamsonsCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 21;
		
		this.log("Página "+ this.currentPage);
		
		String keyword = this.keywordWithoutAccents.replaceAll(" ", "-");
		
		//monta a url com a keyword e a página
		String url = "http://www.ramsons.com.br/busca/3/0/0/MaisRecente/Decrescente/63/"+ this.currentPage +"////"+ keyword +".aspx";
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
	
		Elements products =  this.currentDoc.select(".main-content #listProduct > li");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1){
			for(Element e: products) {
				// InternalPid
				String internalPid 	= crawlInternalPid(e);
				
				// InternalId
				String internalId 	= crawlInternalId(e);
				
				// Url do produto
				String productUrl = crawlProductUrl(e);
				
				saveDataProduct(internalId, internalPid, productUrl);
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
				if(this.arrayProducts.size() == productsLimit) {
					break;
				}
			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado para a página atual!");
		}
		
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
		if(!(hasNextPage())) {
			setTotalProducts();
		}
	}

	@Override
	protected boolean hasNextPage() {
		Element nextPage = this.currentDoc.select("#barra_paginacao_topo_divBarraPaginacao .set-next:not(.off)").first();
		
		if(nextPage != null){
			return true;
		}
		
		return false;
	}

	private String crawlInternalId(Element e){
		String internalId = null;
		Element id = e.select("> input").first();
		
		if(id != null) {
			internalId = id.val();
		}
		
		return internalId;
	}
	
	private String crawlInternalPid(Element e){
		String internalPid = null;
		Element pidElement = e.select("img").first();
		
		if(pidElement != null){
			if(!pidElement.attr("src").contains("indisponivel")){
				String[] tokens2 = pidElement.attr("src").split("/");
				internalPid = tokens2[tokens2.length-2].replaceAll("[^0-9]", "").trim();
			}
		}
		
		return internalPid;
	}
	
	/**
	 * @param e
	 * @return
	 */
	private String crawlProductUrl(Element e){
		String urlProduct = null;
		Element url = e.select("a.link.url").first();
		
		if(url != null) {
			urlProduct = url.attr("href");
		}
		
		return urlProduct;
	}
}
