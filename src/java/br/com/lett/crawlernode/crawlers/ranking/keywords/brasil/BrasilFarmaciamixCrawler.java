package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilFarmaciamixCrawler extends CrawlerRankingKeywords{

	public BrasilFarmaciamixCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 12;
	
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "https://www.farmaciamix.com.br/busca/?Pagina=" + this.currentPage + "&q=" + this.keywordEncoded;
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar o html
		this.currentDoc = fetchDocument(url);

		Elements products =  this.currentDoc.select(".list-produto");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1 && this.currentDoc.select("#FormBuscaVazia").first() == null) {
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
		return this.currentDoc.select(".bt-prox").first() != null;
	}
	
	@Override
	protected void setTotalProducts() {
		Element total = this.currentDoc.select(".col-listage > p span").last();
		
		if(total != null && total.ownText().contains("de")) {
			String totalText = total.ownText().trim().split("de")[1].replaceAll("[^0-9]", "").trim();
			
			if(!totalText.isEmpty()) {
				this.totalProducts = Integer.parseInt(totalText);
			}
		}
		
		this.log("Total products: " + this.totalProducts);
	}
	
	private String crawlInternalId(Element e){
		String internalId = null;
		
		Element idInd = e.select(".bt-indispo").first();
		Element idDis = e.select(".BtComprarProduto").first();
		
		if(idDis != null) {
			internalId = idDis.attr("atributo");
		} else if(idInd != null) {
			internalId = idInd.attr("rel");
		}
	
		return internalId;
	}
	
	private String crawlProductUrl(Element e){
		String productUrl = null;
		Element url = e.select("> a").first();
		
		if(url != null) {
			productUrl = url.attr("href");
			
			if(!productUrl.startsWith("https://www.farmaciamix.com.br/")) {
				productUrl = ("https://www.farmaciamix.com.br/" + productUrl).replace("br//", "br/");
			}
		}
		
		return productUrl;
	}
}
