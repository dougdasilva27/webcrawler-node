package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilKabumCrawler extends CrawlerRankingKeywords{

	public BrasilKabumCrawler(Session session) {
		super(session);
	}

	private Elements id;
	private String baseUrl;
	private boolean isCategory;
	

	@Override
	protected void extractProductsFromCurrentPage() {
		this.log("Página "+ this.currentPage);
		
		String url;
		
		//monta a url com a keyword e a página
		if(this.currentPage > 1) {
			if(!isCategory)	url = "http://www.kabum.com.br/cgi-local/site/listagem/listagem.cgi?string="+this.keywordEncoded+"&dep=&sec=&cat=&sub=&pagina="+this.currentPage+"&ordem=5&limite=30";
			else			url = this.baseUrl+"&pagina="+this.currentPage;
		} else {
			url = "http://www.kabum.com.br/cgi-local/site/listagem/listagem.cgi?string="+this.keywordEncoded+"&dep=&sec=&cat=&sub=&pagina="+this.currentPage+"&ordem=5&limite=30";
		}
		
		this.log("Link onde são feitos os crawlers: "+url);
		
		//número de produtos or página
		this.pageSize = 30;
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
	
		if(this.currentPage == 1) {
			this.baseUrl = this.currentDoc.baseUri();
			if(this.baseUrl.equals(url)) isCategory = false;
			else						 isCategory = true;
		}
			
		this.id =  this.currentDoc.select("div.listagem-titulo_descr span.H-titulo a[href]");
		
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(this.id.size() >=1) {
			for(Element e: this.id) {
				//seta o id da classe pai com o id retirado do elements this.id
				String[] tokens = e.attr("href").split("/");
				
				String internalPid = null;
				String internalId; 
				String urlProduct = e.attr("href");
				
				if( urlProduct.contains("tag") ) {
					internalId 	= tokens[tokens.length-3];
				} else {
					internalId 	= tokens[tokens.length-2];
				}
				
				saveDataProduct(internalId, internalPid, urlProduct);
			
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
				if(this.arrayProducts.size() == productsLimit) break;
				
			}
		}
		else
		{	
			this.result = false;
			this.log("Keyword sem resultado!");
		}
		
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
		if(!(hasNextPage())) setTotalBusca();
	}

	@Override
	protected boolean hasNextPage() 
	{
		if(this.id.size() < 30)	return false;	
		else					return true;
	}
}
