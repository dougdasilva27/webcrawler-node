package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilDufrioCrawler extends CrawlerRankingKeywords{

	public BrasilDufrioCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 9;
	
		String keyword = this.location.replaceAll(" ", "%20");
		
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "https://www.dufrio.com.br/busca/"+ this.currentPage +"?busca="+ keyword + "&ipp=36";
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products =  this.currentDoc.select(".produtosCategoria > div.column .flex-child-auto .boxProduto");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {			
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalBusca == 0) {
				setTotalBusca();
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
		Element nextPage = this.currentDoc.select("a.ultima[disabled]").first();
		
		if(nextPage != null){
			return false;
		}
			
		return true;
		
	}
	
	@Override
	protected void setTotalBusca()	{
		Element totalElement = this.currentDoc.select(".qtdEncontrados span").first();
		
		if(totalElement != null) { 	
			String text = totalElement.text();
			
			if(text.contains("de")) {
				try	{
					int x = text.indexOf("de") + 2;
					
					this.totalBusca = Integer.parseInt(text.substring(x).replaceAll("[^0-9]", "").trim());
				} catch(Exception e) {
					this.logError(CommonMethods.getStackTrace(e));
				}
			}
			
			this.log("Total da busca: "+this.totalBusca);
		}
	}
	
	private String crawlInternalId(Element e){
		return null;
	}
	
	private String crawlInternalPid(Element e){
		String internalPid = null;
		Element checkBox = e.select(".boxCheckbox input").first();
		
		if(checkBox != null) {
			internalPid = checkBox.val();
		}
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = null;
		Element urlElement = e.select(".linkProd").first();
		
		if(urlElement != null){
			urlProduct = urlElement.attr("href");
		}
	
		return urlProduct;
	}
}
