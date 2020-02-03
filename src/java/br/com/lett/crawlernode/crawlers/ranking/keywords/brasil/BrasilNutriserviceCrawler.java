package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilNutriserviceCrawler extends CrawlerRankingKeywords{

	public BrasilNutriserviceCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 28;
	
		this.log("Página "+ this.currentPage);
		
		String keyword = this.keywordWithoutAccents.replace(" ", "%20");
		
		//monta a url com a keyword e a página
		String url = "http://www.nutriservice.com.br/resultadopesquisa?pag="+ this.currentPage +"&departamento=&buscarpor="+ keyword +"&smart=0";
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar o html
		this.currentDoc = fetchDocument(url);

		Elements products =  this.currentDoc.select(".vitrine_geral .produto h3 a");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			if(totalProducts == 0) {
				setTotalProducts();
			}
			
			for(Element e : products) {
				// InternalPid
				String internalPid = null;
				
				// InternalId
				String internalId = null;
				
				// Url do produto
				String productUrl = e.attr("href");
				
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
		return this.arrayProducts.size() < this.totalProducts;
	}
	
	@Override
	protected void setTotalProducts() {
		Element total = this.currentDoc.select(".produtos_quantidade").first();
		
		if(total != null) {
			String text = total.ownText().trim();
			
			if(text.contains("de")) {
				int x = text.indexOf("de");
				String totalText = text.substring(x).replaceAll("[^0-9]", "").trim();

				if(!totalText.isEmpty()) {
					this.totalProducts = Integer.parseInt(totalText);
				}
			}
		}
		
		this.log("Total products: " + this.totalProducts);
	}
}
