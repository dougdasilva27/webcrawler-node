package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class RiodejaneiroDrogariavenancioCrawler extends CrawlerRankingKeywords{

	public RiodejaneiroDrogariavenancioCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 20;
	
		String keyword = this.keywordWithoutAccents.replaceAll(" ", "%20");
		
		this.log("Página "+this.currentPage);
			
		//monta a url com a keyword e a página
		String url = "http://www.drogariavenancio.com.br/busca.asp?PalavraChave="+ keyword +"&nrRows=60&idPage=" + this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);			
			
			
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		Elements products = this.currentDoc.select("div.produtos ul");
		Elements results = this.currentDoc.select(".resultadoSemelhante");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1 && results.size() < 1) {
			for(Element e: products) {
				// Url do produto
				String productUrl = crawlProductUrl(e);

				// InternalPid
				String internalPid = crawlInternalPid(productUrl);

				// InternalId
				String internalId = crawlInternalId(productUrl);

				saveDataProduct(internalId, internalPid, productUrl);;

				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
				if(this.arrayProducts.size() == productsLimit) break;
			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
		}
	
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
		
		if(!(hasNextPage())) setTotalBusca();
	}

	@Override
	protected boolean hasNextPage() {
		Element nextPage = this.currentDoc.select(".last").first();

		//se  elemento page obtiver algum resultado
		if(nextPage != null) {
			if(nextPage.hasClass("inactive")) {
				return false;
			} else {
				return true;
			}
		}

		return false;
	}
	
	private String crawlInternalId(String url){
		String internalId = null;

		return internalId;
	}

	private String crawlInternalPid(String url){
		String internalPid = null;		
		String[] tokens = url.split("/");
		
		internalPid = tokens[tokens.length-2];
		
		return internalPid;
	}

	private String crawlProductUrl(Element e){
		String urlProduct = e.attr("title");
		Element url = e.select(".nome a").first();
		
		if(url != null){
			urlProduct = url.attr("href");
		}
		
		return urlProduct;
	}
}
