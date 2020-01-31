package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ArgentinaCotoCrawler extends CrawlerRankingKeywords{

	public ArgentinaCotoCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 12;
	
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "https://www.cotodigital3.com.ar/sitios/cdigi/browse?_dyncharset=utf-8"
				+ "&Ntt="+ this.keywordEncoded +"+%7C1004&Ntk=All%7Cproduct.sDisp_091&No=" + this.arrayProducts.size(); 
		
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		Elements products =  this.currentDoc.select("#products > li");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {			
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) setTotalProducts();
			for(Element e : products) {
				
				// InternalPid
				String internalPid = crawlInternalPid(e);
				
				// InternalId
				String internalId = crawlInternalId(e);
				
				// Url do produto
				String productUrl = crawlProductUrl(e);
				
				saveDataProduct(internalId, internalPid, productUrl);
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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
		//se  elemeno page obtiver algum resultado
		//tem próxima página
		return this.arrayProducts.size() < this.totalProducts;
	}
	
	@Override
	protected void setTotalProducts()	{
		Element totalElement = this.currentDoc.select("#resultsCount").first();
		
		if(totalElement != null) { 	
			try	{				
				this.totalProducts = Integer.parseInt(totalElement.text().replaceAll("[^0-9]", "").trim());
			} catch(Exception e) {
				this.logError(CommonMethods.getStackTrace(e));
			}
			
			this.log("Total da busca: "+this.totalProducts);
		}
	}
	
	private String crawlInternalId(Element e){
		String internalId = null;
		
		String url = e.attr("href");
		if(url.contains("/p/")){
			String[] tokens = url.split("/");
			internalId = tokens[tokens.length-1].replaceAll("[^0-9]", "");
		}
		
		return internalId;
	}
	
	private String crawlInternalPid(Element e){
		String internalPid = e.attr("id").replaceAll("[^0-9]", "").trim();
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String productUrl = null;
		Element eUrl = e.select(".product_info_container > a").first();
		
		if(eUrl != null){
			productUrl = "https://www.cotodigital3.com.ar" + eUrl.attr("href");
		}
		
		return productUrl;
	}
}
