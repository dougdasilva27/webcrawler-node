package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilKabumCrawler extends CrawlerRankingKeywords{

	public BrasilKabumCrawler(Session session) {
		super(session);
	}

	private String baseUrl;
	private boolean isCategory;

	@Override
	protected void extractProductsFromCurrentPage() {
		this.log("Página "+ this.currentPage);
		
		String url;
		
		//monta a url com a keyword e a página
		if(this.currentPage > 1) {
			if(!isCategory)	{
				url = "http://www.kabum.com.br/cgi-local/site/listagem/listagem.cgi?string="+this.keywordEncoded+"&pagina="+this.currentPage;
			} else {
				url = this.baseUrl+"&pagina="+this.currentPage;
			}
		} else {
			url = "http://www.kabum.com.br/cgi-local/site/listagem/listagem.cgi?string="+this.keywordEncoded+"&pagina="+this.currentPage;
		}
		
		this.log("Link onde são feitos os crawlers: "+url);
		
		//número de produtos or página
		this.pageSize = 30;
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url, null);
		
		if(this.currentPage == 1) {
			this.baseUrl = this.session.getRedirectedToURL(url);
			
			if(baseUrl != null) {
			
				if(!baseUrl.startsWith("http")) {
					baseUrl = "http:" + baseUrl;
				}
				
				if(url.equals(baseUrl)) {
					isCategory = false;
				} else{
					isCategory = true;
				}
			} else {
				isCategory = true;
			}
		}
			
		Elements products =  this.currentDoc.select("div.listagem-titulo_descr span.H-titulo a[href]");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			for(Element e: products) {
				//seta o id da classe pai com o id retirado do elements this.id
				String[] tokens = e.attr("href").split("/");
				
				String internalPid = null;
				String internalId; 
				String urlProduct = e.attr("href");
				
				if( urlProduct.contains("?tag") ) {
					internalId = tokens[tokens.length-3];
					urlProduct = urlProduct.split("\\?")[0];
				} else {
					internalId =tokens[tokens.length-2];
				}
				
				saveDataProduct(internalId, internalPid, urlProduct);
			
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
				if(this.arrayProducts.size() == productsLimit) {
					break;
				}
				
			}
		} else {	
			setTotalBusca();
			this.result = false;
			this.log("Keyword sem resultado!");
		}
		
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
		
		if(!(hasNextPage())) {
			setTotalBusca();
		}
	}

	@Override
	protected boolean hasNextPage() {
		Elements products =  this.currentDoc.select("div.listagem-titulo_descr span.H-titulo a[href]");
		
		if(products.size() < 30){
			return false;	
		}
		
		return true;
	}
}
