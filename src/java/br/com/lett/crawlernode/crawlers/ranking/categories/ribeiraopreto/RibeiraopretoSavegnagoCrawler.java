package br.com.lett.crawlernode.crawlers.ranking.categories.ribeiraopreto;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingCategories;
import br.com.lett.crawlernode.util.CommonMethods;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class RibeiraopretoSavegnagoCrawler extends CrawlerRankingCategories {

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
	private static final int CITY_CODE = 1;

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 12;

		this.log("Página "+ this.currentPage);

		//monta a url com a keyword e a página
		String url;
		
		if(this.categoryUrl.contains("?")) {
			url = this.categoryUrl + "&sc="+ CITY_CODE +"&PageNumber="+this.currentPage;
		} else {
			url = this.categoryUrl + "?sc="+ CITY_CODE +"&PageNumber="+this.currentPage;
		}
		
		this.log("Link onde são feitos os crawlers: "+url);	

		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products =  this.currentDoc.select(".shelf__container .shelf__container > ul li[layout] > div");		

		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) {
				setTotalProducts();
			}

			for(Element e: products) {
				// InternalPid
				String internalPid 	= crawlInternalPid(e);

				// InternalId
				String internalId 	= crawlInternalId(e);

				// Url do produto
				String productUrl 	= crawlProductUrl(e);

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
		return arrayProducts.size() < this.totalProducts;
	}

	@Override
	protected void setTotalProducts() {
		Element totalElement = this.currentDoc.select("span.resultado-busca-numero span.value").first();

		try {
			if(totalElement != null) {
				this.totalProducts = Integer.parseInt(totalElement.text());
			}
		} catch(Exception e) {
			this.logError(CommonMethods.getStackTrace(e));
		}

		this.log("Total da busca: "+this.totalProducts);
	}

	private String crawlInternalId(Element e){
		return null;
	}

	private String crawlInternalPid(Element e){
		return e.attr("data-product-id");
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
