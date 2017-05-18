package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilUnicaarcondicionadoCrawler extends CrawlerRankingKeywords{

	public BrasilUnicaarcondicionadoCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 36;

		this.log("Página "+ this.currentPage);

		//monta a url com a keyword e a página
		String url = "http://www.unicaarcondicionado.com.br/catalogsearch/result/index/?q="+ this.keywordEncoded +"&p="+ this.currentPage; 

		this.log("Link onde são feitos os crawlers: "+url);	

		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products =  this.currentDoc.select("ul.products-grid > li.item > span");		
		Element emptySearch = this.currentDoc.select(".suggest").first();
		Element suggestSearch = this.currentDoc.select(".note-msg").first();
		
		if(products.size() >= 1 && emptySearch == null && suggestSearch == null) {			
			for(Element e : products) {

				// InternalPid
				String internalPid = crawlInternalPid(e);

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
			setTotalProducts();
		}

		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");

		if(!hasNextPage()) {
			setTotalProducts();
		}
	}

	@Override
	protected boolean hasNextPage() {
		Element page = this.currentDoc.select("a.next.i-next").first();

		//se  elemeno page não obtiver nenhum resultado
		if(page == null){
			//não tem próxima página
			return false;
		} 

		return true;
	}

	private String crawlInternalId(Element e){
		Element price = e.select("div.price-box span[id]").first();

		if(price != null && !price.attr("id").isEmpty()) {
			String[] tokens = price.attr("id").split("-");

			if(!tokens[tokens.length-1].trim().isEmpty()) {
				return tokens[tokens.length-1];
			}
		}

		Element img = e.select("img[id^=product-collection-image-]").first();
		
		if(img != null) {
			String[] tokens = img.attr("id").split("-");

			if(!tokens[tokens.length-1].trim().isEmpty()) {
				return tokens[tokens.length-1];
			}
		}
		
		return null;
	}

	private String crawlInternalPid(Element e) {
		return null;
	}

	private String crawlProductUrl(Element e){
		String productUrl = null;

		Element url = e.select(".product-name a").first();
		
		if(url != null) {
			productUrl = url.attr("href");
		}

		return productUrl;
	}
}
