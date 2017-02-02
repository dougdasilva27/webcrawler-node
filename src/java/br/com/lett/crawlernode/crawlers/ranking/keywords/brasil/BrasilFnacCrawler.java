package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilFnacCrawler extends CrawlerRankingKeywords {

	public BrasilFnacCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 24;

		this.log("Página "+ this.currentPage);

		String keyword = this.location.replaceAll(" ", "%20");

		//monta a url com a keyword e a página
		String url = "http://www.fnac.com.br/"+ keyword +"?PageNumber="+ this.currentPage +"&PS=50";
		this.log("Link onde são feitos os crawlers: "+url);	

		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products =  this.currentDoc.select("div.prateleira ul > li[layout]");

		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalBusca == 0) {
				setTotalBusca();
			}

			for(Element e: products) {
				//seta o id com o seletor
				String internalPid = crawlInternalPid(e);
				
				String internalId = null;

				//monta a url
				String productUrl = crawlProductUrl(e);

				saveDataProduct(internalId, internalPid, productUrl);

				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
				if(this.arrayProducts.size() == productsLimit) {
					break;
				}
			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
		}

		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
	}

	@Override
	protected boolean hasNextPage() {
		if(this.arrayProducts.size() < this.totalBusca){
			//tem próxima página
			return true;
		} 

		return false;

	}

	@Override
	protected void setTotalBusca() {
		Element totalElement = this.currentDoc.select("span.resultado-busca-numero > span.value").first();

		if(totalElement != null) { 	
			try	{				
				this.totalBusca = Integer.parseInt(totalElement.text().trim());
			} catch(Exception e) {
				this.logError(CommonMethods.getStackTraceString(e));
			}

			this.log("Total da busca: "+this.totalBusca);
		}
	}
	
	private String crawlInternalPid(Element e){
		String internalPid = null;
		Element pid = e.select(".x-id-produto-input").first();
		
		if(pid != null){
			internalPid = pid.val();
		}
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = null;
		Element urlElement = e.select(".x-url-produto-input").first();
		
		if(urlElement != null){
			urlProduct = urlElement.val();
		}
		
		return urlProduct;
	}
}
