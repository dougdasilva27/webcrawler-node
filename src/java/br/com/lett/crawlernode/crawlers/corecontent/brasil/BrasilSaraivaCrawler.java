package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;

public class BrasilSaraivaCrawler extends Crawler {

	private final String HOME_PAGE_HTTP = "http://www.saraiva.com.br";
	private final String HOME_PAGE_HTTPS = "https://www.saraiva.com.br";


	public BrasilSaraivaCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE_HTTP) || href.startsWith(HOME_PAGE_HTTPS));
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// ID interno
			String internalId = null;
			Element elementInternalId = doc.select(".contentGeral .content .referenc").first();
			if(elementInternalId != null) {
				internalId = elementInternalId.text().substring(elementInternalId.text().indexOf(':') + 1).replace(")", "").trim();
			}

			// Pid
			String internalPid = internalId;

			// Nome
			String name = null;
			Element elementName = doc.select(".contentGeral .content h1").first();
			if(elementName != null) {
				name = elementName.text();
			}

			// Disponibilidade
			boolean available = true;
			Element elementAlertMe = doc.select(".alert_me").first();
			if(elementAlertMe != null) {
				if( !elementAlertMe.attr("style").equals("") ) {
					if( elementAlertMe.attr("style").equals("display: block;") ) {
						available = false;
					}
				} else {
					String style = elementAlertMe.select("#alertme_container").first().attr("style");
					if(style.equals("display: block;")) {
						available = false;
					}
				}
			}

			// Preço
			Float price = null;
			if(available) {
				Element elementPrice = doc.select(".price_complete_info .old_info div .finalPrice b").first();
				if(elementPrice != null) {
					price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
				}
			}

			// Categoria
			String category1 = "";
			String category2 = "";
			String category3 = "";
			Elements elementCategories = doc.select(".breadcrumbs.productView ol li [itemprop=title]");
			ArrayList<String> categories = new ArrayList<String>();
			for(Element e : elementCategories) {
				if( !e.text().equals("Início") ) {
					categories.add(e.text());
				}
			}
			for (String c : categories) {
				if (category1.isEmpty()) {
					category1 = c.trim();
				} else if (category2.isEmpty()) {
					category2 = c.trim();
				} else if (category3.isEmpty()) {
					category3 = c.trim();
				}
			}

			// Imagem primária
			String primaryImage = null;
			Element elementPrimaryImage = doc.select(".product-img-box .product-image img").first();
			if(elementPrimaryImage != null) {
				primaryImage = elementPrimaryImage.attr("src");
			}

			// Imagens secundárias
			Element elementProduct = doc.select(".mainProduct").first();
			Elements elementImages = elementProduct.select(".jcarousel-skin-pika li a img");
			String secondaryImages = null;
			JSONArray secondaryImagesArray = new JSONArray();

			for(Element image : elementImages) {
				if( !image.attr("src").equals(primaryImage) ) {
					secondaryImagesArray.put(image.attr("src"));
				}
			}			
			if (secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			String description = "";
			Element elementDescription = doc.select(".boxDetailCaracter").first();
			if(elementDescription != null) {
				description = description + elementDescription.html();
			}

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			Product product = new Product();
			
			product.setUrl(this.session.getOriginalURL());
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

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}
		
		return products;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(Document document) {
		Element elementProduct = document.select(".mainProduct").first();
		return elementProduct != null;
	}
}
