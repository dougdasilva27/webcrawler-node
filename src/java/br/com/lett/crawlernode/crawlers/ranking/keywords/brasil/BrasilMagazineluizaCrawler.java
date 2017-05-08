package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilMagazineluizaCrawler extends CrawlerRankingKeywords{

	public BrasilMagazineluizaCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 60;

		this.log("Página "+ this.currentPage);
		
		String key =  this.keywordWithoutAccents.replaceAll(" ", "%20");;
		
		//monta a url com a keyword e a página
		String url = "http://www.magazineluiza.com.br/busca/"+key+"/"+this.currentPage+"/";
		this.log("Link onde são feitos os crawlers: "+url);			
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements id = this.currentDoc.select("div.wrapper-content li[itemscope]");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(id.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalBusca == 0) setTotalBusca();				
			
			for(Element e: id) {
				// InternalPid
				String internalPid 	= crawlInternalPid(e);
				
				// InternalId
				String internalId 	= crawlInternalId(e);
				
				// Url do produto
				String urlProduct = crawlProductUrl(e);
				
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
		Element page = this.currentDoc.select("a.last-page").first();
		
		//se  elemeno page obtiver algum resultado
		if(page != null) {
			//tem próxima página
			return true;
		}
		
		return false;
	}

	@Override
	protected void setTotalBusca()
	{
		Element totalElement = this.currentDoc.select("div.header-search small").first();
		if(totalElement != null) {
			try {
				int x = totalElement.text().indexOf("(");
				int y = totalElement.text().indexOf("produto");
				
				String token = totalElement.text().substring(x+1, y).trim();
				
				this.totalBusca = Integer.parseInt(token);
			} catch(Exception e) {
				this.logError(e.getMessage());
			}
			
			this.log("Total da busca: "+this.totalBusca);
		}
	}
	
	private String crawlInternalId(Element e){
		String internalId = null;
		
		return internalId;
	}
	
	private String crawlInternalPid(Element e){
		String internalPid = null;
		
		if(e.hasAttr("id")){
			String[] tokens = e.attr("id").split("_");
			internalPid = tokens[tokens.length-1];
		}
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = null;
		Element urlElement = e.select("> a[itemprop]").first();
		
		if(urlElement != null){
			urlProduct = urlElement.attr("href");
			
			if(!urlProduct.startsWith("http://www.magazineluiza.com.br")){
				urlProduct = "http://www.magazineluiza.com.br" + urlProduct;
			}
		}
		
		return urlProduct;
	}
}
