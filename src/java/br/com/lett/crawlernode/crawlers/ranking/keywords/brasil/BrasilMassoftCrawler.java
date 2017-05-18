package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilMassoftCrawler extends CrawlerRankingKeywords{

	public BrasilMassoftCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 7;

		this.log("Página "+this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "http://massoft.com.br/prestashop/procurar?controller=search&orderby=position&orderway=desc&search_query="+this.keywordEncoded+"&submit_search=";
		this.log("Link onde são feitos os crawlers: "+url);		
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products =  this.currentDoc.select("a.button.ajax_add_to_cart_button.btn.btn-default");
			
		int x = 0;
		int count=0;
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			for(Element e: products) {
				count++;
				//seta o id da classe pai com o id retirado do elements
					String[] tokens = e.attr("href").split("&");
					String token = tokens[tokens.length-3];
					x = token.indexOf("id_proudct=");
					saveDataProduct(token.substring(x+1), token.substring(x+1), null);
					this.log("Id do produto da "+count+"° posição  da página "+this.currentPage+": " + token.substring(x+1));
					if(this.arrayProducts.size() == productsLimit) break;
				
			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
		}
			
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
		if(!(hasNextPage())) setTotalProducts();
	}

	@Override
	protected boolean hasNextPage() {
		// TODO Auto-generated method stub
		return false;
	}

}
