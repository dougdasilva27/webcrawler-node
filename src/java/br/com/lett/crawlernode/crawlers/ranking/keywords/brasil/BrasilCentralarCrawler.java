package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilCentralarCrawler extends CrawlerRankingKeywords{

	public BrasilCentralarCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 12;
			
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "http://www.centralar.com.br/loja/busca.php?pg="+ this.currentPage +"&palavra="+ this.keywordEncoded +"&paginacao=s";
		this.log("Link onde são feitos os crawlers: "+url);	
			
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		Elements products =  this.currentDoc.select("div.float.parteA > a");

		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalBusca == 0) setTotalBusca();
			
			for(Element e: products) {
				//seta o id com o seletor
				String[] tokens = e.select("> img").first().attr("src").split("/");
				String[] tokens2 = tokens[tokens.length-1].split("_");
				
				String internalPid 	= null;
				String internalId 	= tokens2[tokens2.length-3].replaceAll("[^0-9]", "");
				
				//monta a url
				String productUrl= e.attr("href");
				
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
		Elements page = this.currentDoc.select("div.float.parteA > a");
		
		//se  elemeno page não obtiver nenhum resultado
		if(page.size() < this.pageSize) {
			//não tem próxima página
			return false;
		} else {
			//tem próxima página
			return true;
			
		}
	}
	
	@Override
	protected void setTotalBusca() {
		Element totalElement = this.currentDoc.select("div.conteudinhos[style]").first();
		
		if(totalElement != null) { 	
			try {
				String token = (totalElement.text().replaceAll("[^0-9]", "")).trim();
				
				this.totalBusca = Integer.parseInt(token);
			} catch(Exception e) {
				this.logError(e.getMessage());
			}
			
			this.log("Total da busca: "+this.totalBusca);
		}
	}
}
