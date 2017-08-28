package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilDrogariapovaoCrawler extends CrawlerRankingKeywords{

	public BrasilDrogariapovaoCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 24;
	
		this.log("Página "+ this.currentPage);
		
		String keyword = this.keywordWithoutAccents.replace(" ", "%20");
		
		//monta a url com a keyword e a página
		String url = "http://www.drogariaspovao.com.br/index.php?controle=004_shopping_busca&cart_buscar="+ keyword +"&pagenum="+ this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar o html
		this.currentDoc = fetchDocument(url);

		Elements products =  this.currentDoc.select("table[bgcolor=\"#ffffff\"] tbody tr[align=center]");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			if(totalProducts == 0) {
				setTotalProducts();
			}
			
			for(Element e : products) {
				// InternalPid
				String internalPid = null;
				
				// InternalId
				String internalId = crawlInternalId(e);
				
				// Url do produto
				String productUrl = crawlProductUrl(e);
				
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
		Element lastPageElement = this.currentDoc.select(".arial12_666").last();
		
		if(lastPageElement != null) {
			String textPage = lastPageElement.text().replaceAll("[^0-9]", "").trim();
			Integer lastPage = textPage.isEmpty() ? 0 : Integer.parseInt(textPage);
			
			if(lastPage > this.currentPage) {
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	protected void setTotalProducts() {
		Element total = this.currentDoc.select("td[align=left] .arial12_666").first();
		
		if(total == null) {
			total = this.currentDoc.select("td[align=left] .textmiddle").first();
		}
		
		if(total != null) {
			String totalText = total.ownText().trim().replaceAll("[^0-9]", "");
			
			if(!totalText.isEmpty()) {
				this.totalProducts = Integer.parseInt(totalText);
			}
		}
		
		this.log("Total products: " + this.totalProducts);
	}
	
	private String crawlInternalId(Element e){
		String internalId = null;
		Element idElement = e.select("input.qtytextbox[id]").first();
		
		if(idElement != null) {
			internalId = idElement.attr("id");
		}
		
		return internalId;
	}
	
	private String crawlProductUrl(Element e){
		String productUrl = null;
		Element url = e.select("a").first();
		
		if(url != null) {
			productUrl = url.attr("href");
			
			if(!productUrl.startsWith("http://www.drogariaspovao.com.br/")) {
				productUrl = "http://www.drogariaspovao.com.br/" + productUrl;
			}
		}
		
		return productUrl;
	}
}
