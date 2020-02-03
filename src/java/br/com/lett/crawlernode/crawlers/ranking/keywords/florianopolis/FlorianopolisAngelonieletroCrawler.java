package br.com.lett.crawlernode.crawlers.ranking.keywords.florianopolis;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class FlorianopolisAngelonieletroCrawler extends CrawlerRankingKeywords{

	public FlorianopolisAngelonieletroCrawler(Session session) {
		super(session);
	}

	private String redirectUrl;
	
	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 12;
	
		this.log("Página "+ this.currentPage);
		
		// Nesse market uma keyword pode redirecionar para uma categoria
		// com isso pegamos a url redirecionada e acrescentamos a página.
		if(this.currentPage == 1){
			String url = "http://www.angeloni.com.br/eletro/busca?Ntt="+ this.keywordEncoded;
			this.log("Link onde são feitos os crawlers: "+url);	
			
			//chama função de pegar a url
			this.currentDoc = fetchDocument(url);
			
			if(this.session.getRedirectedToURL(url) != null) {
				this.redirectUrl = this.session.getRedirectedToURL(url);
			} else {
				this.redirectUrl = url;
			}
			
		} else {
			String url;
			
			if(redirectUrl.contains("?")){
				url = redirectUrl + "&No="+ (this.arrayProducts.size());
			} else {
				url = redirectUrl + "?No="+ (this.arrayProducts.size());
			}
			
			this.log("Link onde são feitos os crawlers: "+ url);	
			
			//chama função de pegar a url
			this.currentDoc = fetchDocument(url);
		}


		Elements products =  this.currentDoc.select(".produtos .item");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {			
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) {
				setTotalProducts();
			}
			for(Element e : products) {
				
				// InternalPid
				String internalPid 	= crawlInternalPid(e);
				
				// InternalId
				String internalId 	= crawlInternalId(e);
				
				// Url do produto
				String urlProduct = crawlProductUrl(e);
				
				saveDataProduct(internalId, internalPid, urlProduct);
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
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

		//se  elemeno page obtiver algum resultado
		//tem próxima página
		return this.arrayProducts.size() < this.totalProducts;

	}
	
	@Override
	protected void setTotalProducts()	{
		Element totalElement = this.currentDoc.select(".resultado").first();
		
		if(totalElement != null) { 	
			try	{				
				this.totalProducts = Integer.parseInt(totalElement.text().replaceAll("[^0-9]", "").trim());
			} catch(Exception e) {
				this.logError(CommonMethods.getStackTrace(e));
			}
			
			this.log("Total da busca: "+this.totalProducts);
		}
	}
	
	private String crawlInternalId(Element e){
		String internalId = null;
		
		Element id = e.select(".checkbox > input").first();
		
		if(id != null){
			internalId = id.attr("name").replaceAll("[^0-9]", "").trim();
		}
		
		return internalId;
	}
	
	private String crawlInternalPid(Element e){
		String internalPid = null;
		
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = null;
		Element urlElement = e.select("h2 a").first();
		
		if(urlElement != null) {
			urlProduct = urlElement.attr("href");
		}
		
		return urlProduct;
	}
}
