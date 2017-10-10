package br.com.lett.crawlernode.crawlers.ranking.categories.brasil;

import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingCategories;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilMagazineluizaCrawler extends CrawlerRankingCategories {

	public BrasilMagazineluizaCrawler(Session session) {
		super(session);
	}

	private static final String HOME_PAGE = "http://www.magazineluiza.com.br/";
	private boolean cat1 = false;

	@Override
	protected void processBeforeFetch() {
		super.processBeforeFetch();

		// número de produtos por página do market
		if (StringUtils.countMatches(this.location.replace(HOME_PAGE, ""), "/") > 3) {
			this.pageSize = 60;
		} else {
			this.pageSize = 0;
			this.cat1 = true;
		}
		
		this.log("Page Size: " + this.pageSize);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		this.log("Página " + this.currentPage);

		// monta a url com a keyword e a página
		String url;
		
		if(this.cat1) {
			url = this.categoryUrl;
		} else {
			url = this.categoryUrl + "/" + this.currentPage + "/";
		}

		this.log("Link onde são feitos os crawlers: " + url);

		// chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products = this.currentDoc.select("div.wrapper-content li[itemscope]");

		// se obter 1 ou mais links de produtos e essa página tiver resultado
		// faça:
		if (products.size() >= 1) {
			// se o total de busca não foi setado ainda, chama a função para
			// setar
			if (this.totalProducts == 0) {
				setTotalProducts();
			}

			for (Element e : products) {
				JSONObject product = new JSONObject(e.attr("data-product"));
				
				if(product.has("stockTypes")) {
					JSONObject variations = product.getJSONObject("stockTypes");
					
					@SuppressWarnings("unchecked")
					Iterator<String> variationsList = variations.keys();
					
					int count = 0;
					while(variationsList.hasNext()) {
						// InternalId
						String internalId = variationsList.next();
						
						// InternalPid
						String internalPid 	= crawlInternalPid(product);
						
						// Url do produto
						String urlProduct = crawlProductUrl(e, internalId, internalPid);
						
						if(count == 0) {
							this.position++;
							count++;
						}

						saveDataProduct(internalId, null, urlProduct, this.position);
						
						this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
						if(this.arrayProducts.size() == productsLimit) {
							break;
						}
					}
					
					
				}
			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
		}

		this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
				+ this.arrayProducts.size() + " produtos crawleados");

	}

	@Override
	protected boolean hasNextPage() {
		Element page = this.currentDoc.select("a.last-page").first();

		// se elemeno page obtiver algum resultado
		if (page != null && !this.cat1) {
			// tem próxima página
			return true;
		}

		return false;
	}

	@Override
	protected void setTotalProducts() {
		Element totalElement = this.currentDoc.select(".product-showcase-bottom .right span").first();
		if (totalElement != null) {
			try {
				int x = totalElement.text().indexOf("de") + 2;
				int y = totalElement.text().indexOf("produto");

				String token = totalElement.text().substring(x, y).trim();

				this.totalProducts = Integer.parseInt(token);
			} catch (Exception e) {
				this.logError(CommonMethods.getStackTrace(e));
			}

			this.log("Total da busca: " + this.totalProducts);
		}
	}

	private String crawlInternalPid(JSONObject product){
		String internalPid = null;
		
		if(product.has("product")){
			internalPid = product.getString("product");
		}
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e, String internalId, String internalPid){
		String urlProduct = e.attr("href");
			
		if(internalId != null && internalPid != null) {
			urlProduct = urlProduct.replace(internalPid, internalId);
		}
		
		if(!urlProduct.startsWith("http://www.magazineluiza.com.br")) {
			urlProduct = "http://www.magazineluiza.com.br" + urlProduct;
		}
		
		return urlProduct;
	}
}
