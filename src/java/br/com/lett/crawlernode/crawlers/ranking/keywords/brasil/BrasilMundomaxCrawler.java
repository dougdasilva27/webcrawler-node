package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilMundomaxCrawler extends CrawlerRankingKeywords{

	public BrasilMundomaxCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 24;
	
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "http://www.mundomax.com.br/search?q="+ this.keywordEncoded +"&hits=96&p="+ this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products =  this.currentDoc.select(".list_product .products-content");	
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {			
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) setTotalProducts();
			for(Element e : products) {
				
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
				
		//se  elemeno page obtiver algum resultado
		if(this.arrayProducts.size() < this.totalProducts){
			//tem próxima página
			return true;
		} 
			
		return false;
		
	}
	
	@Override
	protected void setTotalProducts()	{
		Element totalElement = this.currentDoc.select(".paging h4").first();
		
		if(totalElement != null) { 	
			try	{				
				this.totalProducts = Integer.parseInt(totalElement.ownText().replaceAll("[^0-9]", "").trim());
			} catch(Exception e) {
				this.logError(e.getMessage());
			}
			
			this.log("Total da busca: "+this.totalProducts);
		}
	}
	
	private String crawlInternalId(Element e){
		String internalId = null;
		Element internalIdElement = e.select(".skuList").first();
		
		if(internalIdElement != null){
			String text = internalIdElement.attr("id");
			internalId = text.split("_")[1];
		}
		
		return internalId;
	}
	
	private String crawlInternalPid(Element e){
		String internalPid = null;
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = null;
		
		Element urlElement = e.select(".prod_descr a").first();
		
		if(urlElement != null){
			urlProduct = urlElement.attr("href").replace("destaque/", "");
		}
		
		return urlProduct;
	}
}
