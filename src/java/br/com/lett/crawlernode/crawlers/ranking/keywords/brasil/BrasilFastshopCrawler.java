package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilFastshopCrawler extends CrawlerRankingKeywords{

	public BrasilFastshopCrawler(Session session) {
		super(session);
	}

	private String urlCategory;
	private boolean isCategory = false;

	@Override
	protected void extractProductsFromCurrentPage() {		
		
		this.log("Página "+ this.currentPage);
		
		//monta url temporária de parãmetro para verificação do isCategory
		String urlTemp = "http://www.fastshop.com.br/webapp/wcs/stores/servlet/SearchDisplay?searchTerm="+ this.keywordEncoded +"&pageSize=50&"
				+ "beginIndex=" + this.arrayProducts.size() + "&storeId=10151&catalogId=11052&langId=-6&sType=SimpleSearch"
						+ "&resultCatEntryType=2&showResultsPage=true&searchSource=Q&hotsite=fastshop";
		
		String url;
		//monta a url de acordo com o tipo: categoria ou busca.
		if(isCategory) 	url = this.urlCategory+"&beginIndex="+this.arrayProducts.size();
		else			url = urlTemp;
		
		
		this.currentDoc = fetchDocument(url);
		
		if(this.currentPage == 1){
			//verifica se a keyword é de categoria ou busca.
			isCategory(url);
		}
		
		this.log("Link onde são feitos os crawlers: "+url);			
		
		Elements products = this.currentDoc.select("div.row > div");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) setTotalProducts();
			
			for(Element e: products) {
				// InternalPid
				String internalPid 	= crawlInternalPid(e);
				
				// InternalId
				String internalId 	= crawlInternalId(e);
				
				// Url do produto
				String productUrl = crawlProductUrl(e, internalPid);
				
				saveDataProduct(internalId, internalPid, productUrl);
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
				if(this.arrayProducts.size() == productsLimit) break;
			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
		}
	
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
		
		//número de produtos por página do market
		if(this.currentPage == 1)
		{
			if(isCategory) 	this.pageSize = 12;
			else		 	this.pageSize = 9;
		}
	}

	@Override
	protected boolean hasNextPage() 
	{
		//se tiver menos que 50 elementos na página, não tem próxima página
		if(this.arrayProducts.size() < this.totalProducts){
			return true;
		}
		
		return false;
	}
	
	private void isCategory(String url)
	{
	
		if(!url.equals(this.currentDoc.baseUri())){
			Element codigoCatElement = this.currentDoc.select("div.compare_controls.disabled a").first();
			
			if(codigoCatElement != null){
				String[] tokens = codigoCatElement.attr("href").split(",");
				int x = tokens[tokens.length-1].indexOf("'");
				int y = tokens[tokens.length-1].indexOf("'", x+1);
				
				String codigoCat = tokens[tokens.length-1].substring(x+1, y);
				
				this.isCategory = true;
				//monta a url com a keyword e a página
				this.urlCategory =  "http://www.fastshop.com.br/webapp/wcs/stores/servlet/CategoryNavigationResultsView?pageSize=12"
						+ "&categoryId="+codigoCat+"&storeId=10151&catalogId=11052&langId=-6&sType=SimpleSearch&resultCatEntryType=2"
								+ "&showResultsPage=true&searchSource=Q&hotsite=fastshop";
			}
		}
	}
	
	@Override
	protected void setTotalProducts()
	{
		Element totalElement = this.currentDoc.select("div#catalog_search_result_information").first();
		
		if(totalElement != null)
		{
			try
			{
				int x = totalElement.text().indexOf("totalResultCount:");
				int y = totalElement.text().indexOf(",", x+("totalResultCount:".length()));
				
				String token = (totalElement.text().substring(x+("totalResultCount:".length()), y)).trim();
				
				this.totalProducts = Integer.parseInt(token);
			}
			catch(Exception e)
			{
				this.logError(e.getMessage());
			}
			
			this.log("Total da busca: "+this.totalProducts);
		}
		
	}
	
	private String crawlInternalId(Element e){
		String internalId = null;
	
		return internalId;
	}
	
	private String crawlInternalPid(Element e){
		String internalPid = null;
		Element ids = e.select("div.catEntryIDPriceList").first();
		
		if(ids != null){
			String[] tokens = ids.attr("id").split("_");
			internalPid 	= tokens[tokens.length-1];
		}
		
		return internalPid;
	}
	
	/**
	 * Há casos de urls virem diferentes, então foi estebelecido um padrão.
	 * @param e
	 * @param internalPìd
	 * @return
	 */
	private String crawlProductUrl(Element e, String internalPìd){
		String urlProduct = null;
//		Element urlElement = e.select("div.product_name > a").first();
//		
//		if(urlElement != null){
//			urlProduct = urlElement.attr("href");
//		}
//		
//		if(urlProduct.contains("ProductDisplay")){	
			
			urlProduct = "http://www.fastshop.com.br/webapp/wcs/stores/servlet/ProductDisplay?productId=" + internalPìd + "&storeId=10151";
			
//		} else if(urlProduct.contains("?")){
//						
//			int x = urlProduct.indexOf("?");
//			
//			urlProduct = urlProduct.substring(0, x);
//		}
//		
		return urlProduct;
	}

}
