package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilEfacilCrawler extends CrawlerRankingKeywords{

	public BrasilEfacilCrawler(Session session) {
		super(session);
	}

	private boolean isCategory;
	private String codCat;

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 12;
	
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "http://www.efacil.com.br/loja/busca?searchTerm="+this.keywordEncoded+"&pageSize=50&beginIndex="+this.arrayProducts.size();
		
		if(this.currentPage == 1) {
			//chama função de pegar a url
			this.currentDoc = fetchDocument(url);
			makeCodCat(url);
		}
		
		//monta a url de uma possível categoria
		String urlCat = "http://www.efacil.com.br/webapp/wcs/stores/servlet/CategoryNavigationResultsView?pageSize=50&beginIndex="+this.arrayProducts.size()
				+ "&manufacturer=&searchType=&resultCatEntryType=&catalogId=10051"
				+ "&categoryId="+this.codCat+"&langId=-6&storeId=10154&sType=SimpleSearch&filterFacet=&metaData=";

		//se for categoria deverá entrar em outra url;
		if(this.currentPage > 1){
			if(this.isCategory) {
				this.currentDoc = fetchDocument(urlCat);	
			} else {
				this.currentDoc = fetchDocument(url);
			}
		}
		
		this.log("Link onde são feitos os crawlers: "+ url);	
				
		Elements id =  this.currentDoc.select("div.products-list div.product");
	
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(id.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalBusca == 0) setTotalBusca();
			
			for(Element e : id) {
				//seta o id da classe pai com o id retirado do elements
				Element ids 		= e.select("div.block-product").first();
				String[] tokens 	= ids.attr("id").split("_");
				String internalPid 	= tokens[tokens.length-1];
				String internalId 	= null;
				
				//monta a url
				Element urlElement = ids.select("a").first();
				String urlProduct = urlElement.attr("href");
				
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
		//se  elemeno page obtiver 50 resultados
		if(this.arrayProducts.size() < this.totalBusca)
		{
			//tem próxima página
			return true;
		}
		else
		{
			//não tem próxima página
			return false;
		}
	}
	
	@Override
	protected void setTotalBusca() {
		Element totalElement = this.currentDoc.select("span#totalCountSpan").first();
		
		if(totalElement != null) {
			try {			
				this.totalBusca = Integer.parseInt(totalElement.text());
			} catch(Exception e) {
				this.logError(e.getMessage());
			}
			
			this.log("Total da busca: "+this.totalBusca);
		}
	}
	
	private void makeCodCat(String url) {
		if(this.currentDoc.baseUri().equals(url)) {
			isCategory = false;
		} else {
			Element code  = this.currentDoc.select("input#categoryId").first();
			if(code != null) {
				this.codCat = code.attr("value");
	
				isCategory = true;
			} else {
				isCategory = false;
			}
		}	
	}

}
