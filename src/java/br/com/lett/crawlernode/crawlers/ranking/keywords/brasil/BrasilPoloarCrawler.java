package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilPoloarCrawler extends CrawlerRankingKeywords {
	
	public BrasilPoloarCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 12;

		this.log("Página "+ this.currentPage);

		String key = this.keywordWithoutAccents.replaceAll(" ", "%20");

		//monta a url com a keyword e a página
		String url = "http://www.poloar.com.br/"+ key +"?PageNumber="+ this.currentPage +"&PS=50";
		this.log("Link onde são feitos os crawlers: "+url);	

		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products =  this.currentDoc.select("div.prateleira ul > li[layout]");
		Elements productsId =  this.currentDoc.select("div.prateleira ul > li.helperComplement");

		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalBusca == 0) {
				setTotalBusca();
			}
			
			int index = 0;

			for(Element e: products) {
				//seta o id com o seletor
				String internalPid 	= crawlInternalPid(productsId, index);
				String internalId 	= null;

				//monta a url
				Element eUrl = e.select(".product-name a").first();
				String productUrl  = eUrl.attr("href");

				saveDataProduct(internalId, internalPid, productUrl);

				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
				if(this.arrayProducts.size() == productsLimit) {
					break;
				}
				
				index++;
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
	
	private String crawlInternalPid(Elements products, int index){
		if(products.size() >= index) {
			String[] tokens = products.get(index).attr("id").split("_");
			return tokens[tokens.length-1];
		}
		
		return null;
	}
}
