package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.core.task.Crawler;
import br.com.lett.crawlernode.util.Logging;

public class BrasilMassoftCrawler extends Crawler {

	private final String HOME_PAGE = "http://massoft.com.br/prestashop/";

	public BrasilMassoftCrawler(CrawlerSession session) {
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

			// ID interno
			String internalID = null;
			Element elementBody = doc.select("#product").first();
			if(elementBody != null) {
				String[] classes = elementBody.attr("class").split("\\s");
				internalID = classes[1].split("-")[1];
			}

			// Nome
			String name = null;
			Element elementName = doc.select(".pb-center-column.col-xs-12.col-sm-4 h1[itemprop=name]").first();
			if(elementName != null) {
				name = elementName.text();
			}

			// Preço
			Float price = null;
			Element elementPrice = doc.select(".box-info-product .content_prices #our_price_display").first();
			if(elementPrice == null) {
				price = null;
			} 
			else {
				price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

			// Disponibilidade
			boolean available = true;

			// Categorias
			String category1 = "";
			String category2 = "";
			String category3 = "";
			Elements elementCategories = doc.select(".breadcrumb.clearfix .navigation_page [itemtype] [itemprop=title]");

			for(int i = 0; i < elementCategories.size(); i++) {
				if(category1.isEmpty()) {
					category1 = elementCategories.get(i).text();
				} 
				else if(category2.isEmpty()) {
					category2 = elementCategories.get(i).text();
				} 
				else if(category3.isEmpty()) {
					category3 = elementCategories.get(i).text();
				}
			}

			// Imagens
			Elements elementImages = doc.select("#thumbs_list_frame li a img");
			String primaryImage = null;
			String secondaryImages = null;
			JSONArray secondaryImagesArray = new JSONArray();

			for(Element e : elementImages) {
				if(primaryImage == null) {
					primaryImage = e.attr("src").replace("-cart", "-large");
				} 
				else {
					secondaryImagesArray.put(e.attr("src").replace("-cart", "-large"));
				}

			}

			if(secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			String description = "";				
			Elements elementsDescription = doc.select(".page-product-box");				
			for(Element e : elementsDescription) {
				if( !e.select("h3").text().equals("Comentários") ) {
					description = description + e.html();
				}
			}

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;
			
			Product product = new Product();
			product.setUrl(this.session.getUrl());
			product.setInternalId(internalID);
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
			
		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}
		
		return products;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(Document document) {
		Element elementProduct = document.select(".primary_block.row").first();
		return (elementProduct != null);
	}
}
