package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.task.Crawler;
import br.com.lett.crawlernode.core.task.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;

public class BrasilKanuiCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.kanui.com.br/";

	public BrasilKanuiCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			// Nome
			Elements element_preName = doc.select("meta[property=og:title]");
			String preName = element_preName.attr("content").replace("'","").replace("’","").trim();

			// Preço
			Float price = null;
			Element elementPrice = doc.select("span[property=gr:hasCurrencyValue]").first();
			if(elementPrice == null) {
				price = null;
			} else {
				price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

			// A Kanui não divide seus produtos em uma categoria específica (não aparece na página do produto)
			String category1 = "";
			String category2 = "";
			String category3 = "";

			// Imagens
			Elements elementImages = doc.select("ul.nav-thumbs-detail li").select("a");
			String primaryImage = null;
			String secondaryImages = null;
			JSONArray secundarias_array = new JSONArray();

			for(Element e : elementImages) {

				if(primaryImage == null) {
					primaryImage = e.attr("data-zoom-image");
				} 
				else {
					secundarias_array.put(e.attr("data-zoom-image"));
				}

			}

			if(secundarias_array.length() > 0) {
				secondaryImages = secundarias_array.toString();
			}

			// Descrição
			String description = "";   
			Element element_descricao_1 = doc.select("#pane-details").first();
			Element element_descricao_2 = doc.select("#pane-reviews").first();
			if(element_descricao_1 != null) description = 					 element_descricao_1.text().replace(".", ".\n").replace("'","").replace("’","").trim();
			if(element_descricao_2 != null) description = description + "\n\n" + element_descricao_2.text().replace(".", ".\n").replace("'","").replace("’","").trim();

			// Marketplace
			JSONArray marketplace = null;

			JSONObject json_stock = null;
			Element element_stock = doc.select("#stock-store").first();
			if(element_stock != null) {
				json_stock = new JSONObject(element_stock.attr("data-stock"));
			}

			// Pegando os produtos usando o endpoint da Dafiti

			Elements elements_products_sizes = doc.select("ul.product-sizes-list li[rel=tooltip]");

			for(int i = 0; i < elements_products_sizes.size(); i++) {

				// Nome - forma final
				String name = preName;

				// Id interno
				String internalId = elements_products_sizes.get(i).select("input").attr("value");

				// Pid
				String internalPid = internalId.split("-")[0];

				if(elements_products_sizes.size() > 1) {
					name = name + " (tamanho " + elements_products_sizes.get(i).select("label").text().trim() + ")";
				}

				// Estoque
				Integer stock = Integer.parseInt(json_stock.get(internalId).toString());

				// Disponibilidade
				boolean available = stock > 0;

				Product product = new Product();
				product.setUrl(this.session.getUrl());
				product.setInternalId(internalId);
				product.setInternalPid(internalPid);
				product.setName(name);
				product.setPrice(price);
				product.setCategory1(category1);
				product.setCategory2(category2);
				product.setCategory3(category3);
				product.setPrimaryImage(primaryImage);
				product.setSecondaryImages(secondaryImages);
				product.setDescription(description);
				product.setStock(stock);
				product.setMarketplace(marketplace);
				product.setAvailable(available);

				products.add(product);						

			}

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}

		return products;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(Document document) {
		return (document.select("div[ng-controller=DetailCtrl]").first() != null);
	}
	
}
