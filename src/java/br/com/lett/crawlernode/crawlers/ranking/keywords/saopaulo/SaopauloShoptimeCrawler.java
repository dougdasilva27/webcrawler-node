package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class SaopauloShoptimeCrawler extends CrawlerRankingKeywords {

	public SaopauloShoptimeCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 24;

		this.log("Página "+ this.currentPage);

		//monta a url com a keyword e a página
		String url = "https://www.shoptime.com.br/busca/?conteudo="+ this.keywordEncoded +"&limite=24&offset=" + this.arrayProducts.size();
		this.log("Link onde são feitos os crawlers: "+url);

		//chama função de pegar a url
		this.currentDoc = fetchDocument(url, null);

		Elements products = this.currentDoc.select(".card-product .card-product-url");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1)	{
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0){
				setTotalProducts();
			}

			for(Element e : products) {
				// InternalPid
				String internalPid = crawlInternalPid(e);

				// Url do produto
				String productUrl = crawlProductUrl(internalPid);

				// InternalId
				String internalId = crawlInternalId();

				saveDataProduct(internalId, internalPid,productUrl);

				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
				if(this.arrayProducts.size() == productsLimit){
					break;
				}
			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
		}

		this.log("Finalizando Crawler de produtos da página "+ this.currentPage +" - até agora "+this.arrayProducts.size()+" produtos crawleados");

	}

	@Override
	protected boolean hasNextPage() {
		if(this.arrayProducts.size() < totalProducts) {
			return true;
		}

		return false;
	}

	@Override
	protected void setTotalProducts() {
		Element e = this.currentDoc.select(".form-group.display-sm-inline-block span[data-reactid=27]").first();
		
		if(e != null) {
			String total = e.ownText().replaceAll("[^0-9]", "").trim();
			
			if(!total.isEmpty()) {
				this.totalProducts = Integer.parseInt(total);
				this.log("Total Seacrh: " + this.totalProducts);
			}
		}
	}

	private String crawlInternalId(){
		return null;
	}

	private String crawlInternalPid(Element e){
		String internalPid = null;
		String href = e.attr("href");
		
		if(href.contains("?")) {
			href = href.split("[?]")[0];
		} 
		
		if(!href.isEmpty()) {
			String[] tokens = href.split("/");
			internalPid = tokens[tokens.length-1];
		}

		return internalPid;
	}

	private String crawlProductUrl(String internalPid){
		return "https://www.shoptime.com.br/produto/" + internalPid;
	}
}
