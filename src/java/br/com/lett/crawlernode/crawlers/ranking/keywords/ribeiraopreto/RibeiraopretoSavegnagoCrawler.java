package br.com.lett.crawlernode.crawlers.ranking.keywords.ribeiraopreto;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class RibeiraopretoSavegnagoCrawler extends CrawlerRankingKeywords{

	public RibeiraopretoSavegnagoCrawler(Session session) {
		super(session);
	}

	/**
	 * Código das cidades:
	 *  Ribeirão Preto - 1
	 *  Sertãozinho - 6
	 *	Jardianópolis - 11
	 *	Jaboticabal - 7
	 *	Franca - 3
	 *	Barretos - 10
	 *	Bebedouro - 9
	 *	Monte Alto - 12
	 *	Araraquara - 4
	 *	São carlos - 5
	 *	Matão - 8
	 */
	//	private static final int cityCode = ControllerKeywords.codeCity;
	private static final int cityCode = 1;

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 20;

		this.log("Página "+ this.currentPage);

		//monta a url com a keyword e a página
		String url = "http://www.savegnago.com.br/"+this.keywordEncoded+"?&sc="+cityCode+"&PageNumber="+this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);	

		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products =  this.currentDoc.select(".shelf__container .shelf__container > ul li[layout] > div");		

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
				String productUrl 	= crawlProductUrl(e);

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
		if(arrayProducts.size() < this.totalBusca){
			return true;
		}

		return false;
	}

	@Override
	protected void setTotalBusca() {
		Element totalElement = this.currentDoc.select("span.resultado-busca-numero span.value").first();

		try {
			if(totalElement != null) this.totalBusca = Integer.parseInt(totalElement.text());
		} catch(Exception e) {
			this.logError(e.getMessage());
		}

		this.log("Total da busca: "+this.totalBusca);
	}

	private String crawlInternalId(Element e){
		String internalId = null;

		return internalId;
	}

	private String crawlInternalPid(Element e){
		String internalPid = e.attr("data-product-id");
		
		return internalPid;
	}

	private String crawlProductUrl(Element e){
		String urlProduct = null;
		Element urlElement = e.select("h3 a").first();

		if(urlElement != null){
			urlProduct = urlElement.attr("href");
		}

		return urlProduct;
	}

}
