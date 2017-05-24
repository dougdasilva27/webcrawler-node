package br.com.lett.crawlernode.crawlers.ranking.categories.florianopolis;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingCategories;
import br.com.lett.crawlernode.util.CommonMethods;

public class FlorianopolisAngeloniCrawler extends CrawlerRankingCategories {

	public FlorianopolisAngeloniCrawler(Session session) {
		super(session);
	}
	
	private boolean cat1 = false;
	
	@Override
	protected void processBeforeFetch() {
		super.processBeforeFetch();
		
		Document doc = fetchDocument(this.location);
		
		Elements breadCrumbs = doc.select("#breadcrumb .boxInIn ul li > a"); // primeiro e segundo elemento não são categorias
		
		if(breadCrumbs.size() < 3) {
			this.cat1 = true;
		}
		
		//número de produtos por página do market
		this.pageSize = 150;
	}

	@Override
	protected void extractProductsFromCurrentPage() {	
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url;
		
		if(this.cat1) {
			url = this.location.replace("index", "ofertas") + "&itemsPerPage=150&page=" + this.currentPage;
		} else {
			url = this.location.replace("index", "produtos") + "&itemsPerPage=150&page=" + this.currentPage;
		}
		
		this.log("Link onde são feitos os crawlers: "+ url);	
			
		//chama função de pegar url
		this.currentDoc = fetchDocument(url);
		
		Elements products =  this.currentDoc.select(".lstProd > li");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) {
				setTotalProducts();			
			}
			
			for(Element e: products) {
				// InternalPid
				String internalPid = crawlInternalPid(e);
				
				// InternalId
				String internalId = internalPid;
				
				// Url do produto
				String productUrl = crawlProductUrl(internalId);
				
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
		if(this.arrayProducts.size() < this.totalProducts) {
			Elements page = this.currentDoc.select("a.lnkPagNext");
			
			//se  elemeno page obtiver algum resultado.
			if(page.size() > 0) {
				//tem próxima página
				return true;
			} 
		}	
		
		return false;
	}
	
	@Override
	protected void setTotalProducts() {
		Element totalElement = this.currentDoc.select("span.itm01 strong").first();
		
		if(totalElement != null) {
			try {
				int x = totalElement.text().indexOf("ite");
				
				String token = totalElement.text().substring(0, x).trim();
				
				this.totalProducts = Integer.parseInt(token);
			} catch(Exception e) {
				this.logError(CommonMethods.getStackTrace(e));
			}
			
			this.log("Total da busca: "+this.totalProducts);
		}
	}
	
	private String crawlInternalPid(Element e){
		String internalPid = null;
		Element pidElement = e.select(".cod").first();
		
		if(pidElement != null){
			internalPid = pidElement.text().replaceAll("[^0-9]", "").trim();
		}
		
		return internalPid;
	}
	

	private String crawlProductUrl(String internalId){		
		return "http://www.angeloni.com.br/super/produto?idProduto=" + internalId;
	}

}
