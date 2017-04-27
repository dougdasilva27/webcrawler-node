package br.com.lett.crawlernode.crawlers.ranking.keywords.florianopolis;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class FlorianopolisAngeloniCrawler extends CrawlerRankingKeywords{

	public FlorianopolisAngeloniCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 12;
	
		this.log("Página "+ this.currentPage);
		
		String keyword = this.location.replaceAll(" ", ",");
		
		//monta a url com a keyword e a página
		String url = "http://www.angeloni.com.br/super/filtrePor?q="+ keyword +"&itemsPerPage=150&page="+this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);	
			
		//chama função de pegar url
		this.currentDoc = fetchDocument(url);
		
		Elements products =  this.currentDoc.select(".lstProd > li");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalBusca == 0) setTotalBusca();			
			
			for(Element e: products) {
				// InternalPid
				String internalPid 	= crawlInternalPid(e);
				
				// InternalId
				String internalId 	= crawlInternalId(e);
				
				// Url do produto
				String productUrl = crawlProductUrl(e);
				
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
		Elements page = this.currentDoc.select("a.lnkPagNext");
		
		//se  elemeno page obtiver algum resultado.
		if(page.size() > 0) {
			//tem próxima página
			return true;
		} 
			
		return false;
	}
	
	@Override
	protected void setTotalBusca() {
		Element totalElement = this.currentDoc.select("span.itm01 strong").first();
		
		if(totalElement != null) {
			try {
				int x = totalElement.text().indexOf("ite");
				
				String token = totalElement.text().substring(0, x).trim();
				
				this.totalBusca = Integer.parseInt(token);
			} catch(Exception e) {
				this.logError(e.getMessage());
			}
			
			this.log("Total da busca: "+this.totalBusca);
		}
	}
	
	private String crawlInternalId(Element e){
		String internalId = null;
		Element idElement = e.select(".cod").first();
		
		if(idElement != null){
			internalId = idElement.text().replaceAll("[^0-9]", "").trim();
		}
		
		return internalId;
	}
	
	private String crawlInternalPid(Element e){
		String internalPid = null;
		Element pidElement = e.select(".cod").first();
		
		if(pidElement != null){
			internalPid = pidElement.text().replaceAll("[^0-9]", "").trim();
		}
		
		return internalPid;
	}
	

	private String crawlProductUrl(Element e){
		String urlProduct = null;
		Element urlElement = e.select(".boxInfoProd .descr a").first();
		
		if(urlElement != null){
			String tempUrl = urlElement.attr("href");
			
			if(tempUrl.contains("angeloni")){
				urlProduct = tempUrl;
			} else {
				urlProduct = "http://www.angeloni.com.br" + tempUrl;
			}
		}
		
		return urlProduct;
	}

}
