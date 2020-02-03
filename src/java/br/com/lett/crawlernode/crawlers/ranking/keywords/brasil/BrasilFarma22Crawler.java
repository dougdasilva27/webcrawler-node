package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilFarma22Crawler extends CrawlerRankingKeywords{

	public BrasilFarma22Crawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		this.log("Página "+this.currentPage);

		//monta a url com a keyword e a página
		String url  = "http://www.farma22.com.br/" + this.keywordWithoutAccents.replaceAll(" ", "%20") + "?PS=50&PageNumber=" + this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);

		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products = this.currentDoc.select("div.vitrine.resultItemsWrapper div.prateleira > ul > li[layout]");
		Elements producstIDs = this.currentDoc.select("div.vitrine.resultItemsWrapper div.prateleira > ul > li[id]");
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) {
				setTotalProducts();
			}
			
			int index = 0;
			
			for(Element e : products) {
				String internalPid 	= crawlInternalPid(producstIDs.get(index));
				String internalId = null;
				String productUrl = crawlProductUrl(e);

				saveDataProduct(internalId, internalPid, productUrl);

				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: "
						+ internalPid + " - Url: " + productUrl);

				if (this.arrayProducts.size() == productsLimit) {
					break;
				}
				
				index++;
			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
		}

		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");

	}

	@Override
	protected boolean hasNextPage() {
		//se  o número de produtos pegos for menor que o resultado total da busca, existe proxima pagina
		return this.arrayProducts.size() < this.totalProducts;
	}

	@Override
	protected void setTotalProducts() {
		Element totalElement = this.currentDoc.select("span.resultado-busca-numero span.value").first();

		if(totalElement != null) {
			String text = totalElement.text().replaceAll("[^0-9]", "").trim();
			
			if(!text.isEmpty()) {
				this.totalProducts = Integer.parseInt(text);
			}
		}

		this.log("Total da busca: "+this.totalProducts);
	}

	private String crawlInternalPid(Element e){
		String[] tokens = e.attr("id").split("_");

		return tokens[tokens.length-1];
	}

	private String crawlProductUrl(Element e){
		String productUrl = null;
		Element urlElement = e.select(".shelf-product-name > a").first();

		if(urlElement != null){
			productUrl = urlElement.attr("href");
		}

		return productUrl;
	}
}
