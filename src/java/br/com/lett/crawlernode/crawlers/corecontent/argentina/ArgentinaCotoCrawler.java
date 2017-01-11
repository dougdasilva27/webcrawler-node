package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;

/**
 * Date: 07/12/2016
 * 
 * 1) Only one sku per page.
 * 
 * Price crawling notes:
 * 1) In the time this crawler was made, we doesn't found any unnavailable product.
 * 2) There is no bank slip (boleto bancario) payment option.
 * 3) There is installments for card payment, but was found only shopCard payment method.
 * 
 * @author Gabriel Dornelas
 *
 */
public class ArgentinaCotoCrawler extends Crawler {

	private final String HOME_PAGE = "https://www.cotodigital3.com.ar/";

	public ArgentinaCotoCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<>();

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			String internalId = crawlInternalId(doc);
			String internalPid = crawlInternalPid(session.getOriginalURL());
			String name = crawlName(doc);
			Float price = crawlPrice(doc);
			Prices prices = crawlPrices(price, doc);
			boolean available = crawlAvailability(doc);
			CategoryCollection categories = crawlCategories(doc);
			String primaryImage = crawlPrimaryImage(doc);
			String secondaryImages = crawlSecondaryImages(doc);
			String description = crawlDescription(doc);
			Integer stock = null;
			JSONArray marketplace = crawlMarketplace();

			// Creating the product
			Product product = ProductBuilder.create()
					.setUrl(session.getOriginalURL())
					.setInternalId(internalId)
					.setInternalPid(internalPid)
					.setName(name)
					.setPrice(price)
					.setPrices(prices)
					.setAvailable(available)
					.setCategory1(categories.getCategory(0))
					.setCategory2(categories.getCategory(1))
					.setCategory3(categories.getCategory(2))
					.setPrimaryImage(primaryImage)
					.setSecondaryImages(secondaryImages)
					.setDescription(description)
					.setStock(stock)
					.setMarketplace(marketplace)
					.build();

			products.add(product);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return products;

	}

	private boolean isProductPage(Document doc) {
		if (doc.select("#atg_store_content").first() != null) {
			return true;
		}
		return false;
	}

	private String crawlInternalId(Document document) {
		String internalId = null;

		Element internalIdElement = document.select(".span_codigoplu").first();
		if (internalIdElement != null) {
			internalId = internalIdElement.text().replaceAll("[^0-9]", "");
		}

		return internalId;
	}

	private String crawlInternalPid(String url) {
		String internalPid = null;

		String[] tokens = url.split("-");
		String id = tokens[tokens.length-2].replaceAll("[^0-9]", "").trim();
		
		if(!id.isEmpty()){
			internalPid = id;
		}
		
		return internalPid;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("h1.product_page").first();

		if (nameElement != null) {
			name = nameElement.ownText().trim();
		}

		return name;
	}

	private Float crawlPrice(Document document) {
		Float price = null;

		String priceText = null;
		Element salePriceElement = document.select(".price_discount").first();		

		if (salePriceElement != null) {
			priceText = salePriceElement.ownText();
			price = Float.parseFloat(priceText.replaceAll("\\$", "").replaceAll(",", "").trim());
		} else {
			salePriceElement = document.select(".atg_store_productPrice .atg_store_newPrice").first();
			if (salePriceElement != null) {
				priceText = salePriceElement.ownText();
				price = Float.parseFloat(priceText.replaceAll("\\$", "").replaceAll(",", "").trim());
			}
		}

		return price;
	}

	private boolean crawlAvailability(Document document) {
		boolean available = false;

		Element outOfStockElement = document.select(".add_products").first();
		if (outOfStockElement != null) {
			available = true;
		}

		return available;
	}

	private JSONArray crawlMarketplace() {
		return new JSONArray();
	}


	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select("a.gall-item").first();

		if (primaryImageElement != null) {			
			primaryImage = primaryImageElement.attr("href").trim();
			
			if(primaryImage.contains("?")){
				primaryImage = primaryImage.split("\\?")[0];
			}
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select(".zoomThumbLink > img");

		for (int i = 1; i < imagesElement.size(); i++) { // first index is the primary image
			String image = imagesElement.get(i).attr("data-large").trim();
			secondaryImagesArray.put( image );	
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private CategoryCollection crawlCategories(Document document) {
		CategoryCollection categories = new CategoryCollection();

		Elements elementCategories = document.select("#atg_store_breadcrumbs a:not(.atg_store_navLogo) p");
		for (int i = 0; i < elementCategories.size(); i++) { 
			categories.add( elementCategories.get(i).ownText().trim() );
		}

		return categories;
	}

	private String crawlDescription(Document document) {
		StringBuilder description = new StringBuilder();
		Element descriptionElement = document.select("#content-tabs").first();

		if(descriptionElement != null) {
			description.append(descriptionElement.html());
		}
		
		return description.toString();
	}

	/**
	 * There is no bankSlip price.
	 * 
	 * Some cases has this:
	 * 6 x $259.83
	 * 
	 * Only card that was found in this market was the market's own
	 * 
	 * @param doc
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(Float price, Document doc) {
		Prices prices = new Prices();
		
		if (price != null) {
			Map<Integer,Float> installmentPriceMap = new TreeMap<>();
			installmentPriceMap.put(1, price);
	
			Element installments = doc.select(".discount_cuotas_container").first();
			
			if(installments != null){
				Element installmentElement = installments.select(".quantity_cuota").first();
				Element priceElement = installments.select(".price_cuota").first();
				
				if(installmentElement != null && priceElement != null){
					Integer installment = Integer.parseInt(installmentElement.text());
					Float value = Float.parseFloat(priceElement.text().replaceAll(",", "").replaceAll("\\$", "").trim());
					
					installmentPriceMap.put(installment, value);
				}
				
			}
			
			prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);
		}
		
		return prices;
	}

}
