package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilMoblyCrawler extends CrawlerRankingKeywords{

	public BrasilMoblyCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 48;

		this.log("Página "+ this.currentPage);

		//monta a url com a keyword e a página
		String url = "http://busca.mobly.com.br/busca.php?q="+ this.keywordEncoded +"&results_per_page=96&page="+ this.currentPage;

		this.log("Link onde são feitos os crawlers: "+url);	

		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products =  this.currentDoc.select("ul.productsCatalog > li");
		Element noResult = this.currentDoc.select("#neemu-approximated-search").first();
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1 && noResult == null) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) {
				setTotalProducts();
			}

			for(Element e: products) {
				//seta o id com o seletor
				String internalPid 	= e.attr("id");
				String internalId 	= null;

				//monta a url
				Element eUrl = e.select("a").first();
				String productUrl;
				String temp = eUrl.attr("href");
				
				if(temp.contains("link=")){

					int x = eUrl.attr("href").indexOf("link=");
					int y = eUrl.attr("href").indexOf("&", x+5);

					productUrl = eUrl.attr("href").substring(x+5, y);
				} else {
					productUrl = temp;
				}
				
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
		if(this.arrayProducts.size() < this.totalProducts){
			//tem próxima página
			return true;
		} 
		
		return false;

	}

	@Override
	protected void setTotalProducts() {
		Element totalElement = this.currentDoc.select("li.itens-encontrados").first();

		if(totalElement != null) { 	
			String token = (totalElement.text().replaceAll("[^0-9]", "")).trim();
			if(!token.isEmpty()) {
				try {				
					this.totalProducts = Integer.parseInt(token);
				} catch(Exception e) {
					this.logError(CommonMethods.getStackTraceString(e));
				}
			}
			this.log("Total da busca: "+this.totalProducts);
		}
	}	
}
