package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilShopfatoCrawler extends CrawlerRankingKeywords{

	public BrasilShopfatoCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 36;
			
		this.log("Página "+ this.currentPage);
				
		//monta a url com a keyword e a página
		String url = "http://www.shopfato.com.br/buscapagina?ft=" + this.keywordEncoded + "&PS=24&sl=233ad32c-6df0-4b7f-a602-8a516ce43b36&cc=24&sm=0&PageNumber="+this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);	
			
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		Elements products =  this.currentDoc.select("div.n24colunas > ul > li");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			for(int i = 0; i < products.size(); i++) {
				Element e = products.get(i);
				if(e.hasAttr("layout")) {					
					// InternalPid
					String internalPid 	= crawlInternalPid(e);
					
					// InternalId
					String internalId 	= crawlInternalId(products.get(i+1));
					
					// Url do produto
					String urlProduct = crawlProductUrl(e);
					
					saveDataProduct(internalId, internalPid, urlProduct);
					
					this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
					if(this.arrayProducts.size() == productsLimit) break;
				}
			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
		}

		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
		if(!hasNextPage()) setTotalBusca();
	}

	@Override
	protected boolean hasNextPage() {
		if(this.result){
			return true;	
		}
		
		return false;
	}
	
	private String crawlInternalId(Element e){
		String internalId = null;
		
		String[] tokens = e.attr("id").split("_");
		internalId = tokens[tokens.length-1];
		
		return internalId;
	}
	
	private String crawlInternalPid(Element e){
		String internalPid = null;
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = null;
		Element eUrl = e.select("a.prd-list-item-link").first();

		if(eUrl != null){
			urlProduct = eUrl.attr("href");
		}
		
		return urlProduct;
	}
}
