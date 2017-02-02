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
		this.pageSize = 20;
		
		this.log("Página "+ this.currentPage);
		
		String keyword = this.location.replaceAll(" ", "-");
		
		//monta a url com a keyword e a página
		String url = "http://busca.ramsons.com.br/busca/"+keyword+"/0/MA%3d%3d/GB/0/0/0/0/0/3/"+(this.currentPage-1)+"/150/0/0/1/0.aspx";
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
	
		Elements products =  this.currentDoc.select("ul#produtos_listagem li > p.photo_container > a");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1){
			for(Element e: products) {
				// InternalPid
				String internalPid 	= crawlInternalPid(e);
				
				// InternalId
				String internalId 	= crawlInternalId(e);
				
				// Url do produto
				String productUrl = crawlProductUrl(e, internalId);
				
				saveDataProduct(internalId, internalPid, productUrl);
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
				if(this.arrayProducts.size() == productsLimit) break;
			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado para a página atual!");
		}
		
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
		if(!(hasNextPage())) setTotalBusca();
	}

	@Override
	protected boolean hasNextPage() {
		Element nextPage = this.currentDoc.select("#ctlPaginacaoInferior_next").first();
		
		if(nextPage != null){
			return true;
		}
		
		return false;
	}

	private String crawlInternalId(Element e){
		String internalId = null;
		
		String tokens  = e.attr("href");
		int x = tokens.indexOf("product=") + "product=".length();
		int y = tokens.indexOf("&", x);
		
		internalId 	= tokens.substring(x, y);
		
		return internalId;
	}
	
	private String crawlInternalPid(Element e){
		String internalPid = null;
		Element pidElement = e.select("> img").first();
		
		if(pidElement != null){
			if(!pidElement.attr("src").contains("indisponivel")){
				String[] tokens2 = pidElement.attr("src").split("/");
				internalPid = tokens2[tokens2.length-2].replaceAll("[^0-9]", "").trim();
			}
		}
		
		return internalPid;
	}
	
	/**
	 * Url must be:
	 * "http://www.ramsons.com.br/" + prodNameParsed + "-" + interenalId + ".aspx/p"
	 * 
	 * because the crawler only identifies products if the url is in this format
	 * @param e
	 * @param internalId
	 * @return
	 */
	private String crawlProductUrl(Element e, String internalId){
		String urlProduct = null;
		String buscaUrl = e.attr("href");
		
		if(buscaUrl.contains("busca.")){
			String[] tokens = buscaUrl.split("=");
			urlProduct = tokens[tokens.length-1].replaceAll("%2f", "/").replaceAll("%3a", ":");
		}
		
		if(internalId != null){
			if(urlProduct.contains("busca")){
				String[] tokens = urlProduct.split("/");
				String prodNameParsed = tokens[tokens.length-1].replace(".aspx", "").trim();
				
				urlProduct = "http://www.ramsons.com.br/" + prodNameParsed + "-" + internalId +".aspx/p";
			}
		}
		
		return urlProduct;
	}
}
