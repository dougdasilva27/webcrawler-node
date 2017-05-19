package br.com.lett.crawlernode.crawlers.ranking.categories.saopaulo;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingCategories;
import br.com.lett.crawlernode.util.CommonMethods;

public class SaopauloExtramarketplaceCrawler extends CrawlerRankingCategories {

	public SaopauloExtramarketplaceCrawler(Session session) {
		super(session);
	}

	private String crawlProductUrl(String internalPid) {
		return "http://produto.extra.com.br/?IdProduto=" + internalPid;
	}

	private static final String HOME_PAGE = "http://www.extra.com.br/";
	
	@Override
	protected void extractProductsFromCurrentPage() {
		this.log("Página " + this.currentPage);

		this.pageSize = 21;
		
		// monta a url com a catehoria e a página
		String url = this.location + "&paginaAtual=" + this.currentPage;

		this.log("Link onde são feitos os crawlers: " + url);

		// chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		JSONObject shareInfo = CommonMethods.getSpecificJsonFromHtml(this.currentDoc, "script[type=\"text/javascript\"]", "varsiteMetadata=", ";");
		List<String> products = crawlShareJSON(shareInfo);
		Elements result = this.currentDoc.select(".naoEncontrado");

		if(!isThePrincipalCategorie(url)) {
			this.pageSize = 21;
		} else {
			this.pageSize = products.size();
		}
		
		// se obter 1 ou mais links de produtos e essa página tiver resultado
		if (!products.isEmpty() && result.size() < 1) {
			// se o total de busca não foi setado ainda, chama a função para
			if (this.totalProducts == 0) {
				setTotalProducts();
			}
			
			for (String internalPid : products) {
				// InternalId
				String internalId = null;

				// Url do produto
				String urlProduct = crawlProductUrl(internalPid);

				saveDataProduct(internalId, internalPid, urlProduct);

				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: "
						+ internalPid + " - Url: " + urlProduct);
				
				if (this.arrayProducts.size() == productsLimit) {
					break;
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
		Element page = this.currentDoc.select("li.next a").first();

		// se elemeno page obtiver algum resultado
		if (page != null) {
			return true;
		}

		return false;

	}

	@Override
	protected void setTotalProducts() {
		Element totalElement = this.currentDoc.select(".resultadoBusca .resultado strong").first();
		
		if(totalElement != null) {
			this.totalProducts = Integer.parseInt(totalElement.ownText().trim());
			
			this.log("Total Products: " + this.totalProducts);
		}
	}

	private boolean isThePrincipalCategorie(String url) {
		return url.replace(HOME_PAGE, "").split("/").length == 1 ? true : false;
	}
	
	private List<String> crawlShareJSON(JSONObject shareInfo) {
		List<String> products = new ArrayList<>();
		
		if(shareInfo.has("page")) {
			JSONObject page = shareInfo.getJSONObject("page");
			
			if(page.has("listItems")) {
				JSONArray list = page.getJSONArray("listItems");
				for(int i = 0; i < list.length(); i++) {
					JSONObject product = list.getJSONObject(i);
					
					if(product.has("idProduct")) {
						products.add(product.getString("idProduct"));
					}
				}
			}
		}
		
		return products;
	}
}
