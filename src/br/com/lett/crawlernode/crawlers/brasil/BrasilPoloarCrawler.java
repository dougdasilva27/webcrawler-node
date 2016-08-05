package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.Crawler;
import br.com.lett.crawlernode.kernel.CrawlerSession;
import br.com.lett.crawlernode.models.Product;
import br.com.lett.crawlernode.util.Logging;


/*********************************************************************************************************************************************
 * Crawling notes (12/07/2016):
 * 
 * 1) For this crawler, we have one url per each sku
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made
 * 3) There is no marketplace in this ecommerce by the time this crawler was made
 * 4) The sku page identification is done simply looking for an specific html element
 * 5) if the sku is unavailable, it's price is not displayed.
 * 6) There is no internalPid for skus in this ecommerce. The internalPid must be a number that is the same for all
 * the variations of a given sku
 * 7) For the availability we crawl a script in the html. A script that has a variable named skuJson_0. It's been a common
 * script, that contains a jsonObject with certain informations about the sku. It's used only when the information needed
 * is too complicated to be crawled by normal means, or inexistent in other place. Although this json has other informations
 * about the sku, only the availability is crawled this way in this website.
 * 8) All the images are .png
 * 9) We have one method for each type of information for a sku (please carry on with this pattern).
 * 
 * Examples:
 * ex1 (available): http://www.poloar.com.br/ar-condicionado-multi-split-inverter-fujitsu-serie-g-14000-btu-h-1x-12000-e-1x-09000-quente-frio-220v/p
 * ex2 (unavailable): http://www.poloar.com.br/ar-condicionado-springer-duo-janela-10000-btu-h-frio-220v-eletronico-2/p
 *
 **********************************************************************************************************************************************/

public class BrasilPoloarCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.poloar.com.br/";

	private final String INTERNALID_SELECTOR 								= "#___rc-p-id";
	private final String INTERNALID_SELECTOR_ATTRIBUTE						= "value";

	private final String NAME_SELECTOR 										= ".fn.productName";
	private final String PRICE_SELECTOR 									= ".plugin-preco .preco-a-vista .skuPrice";

	private final String PRIMARY_IMAGE_SELECTOR 							= "#image a";
	private final String PRIMARY_IMAGE_SELECTOR_ATTRIBUTE 					= "href";

	private final String SECONDARY_IMAGES_SELECTOR 							= ".thumbs li a";
	private final String SECONDARY_IMAGES_SELECTOR_ATTRIBUTE				= "zoom";

	private final String CATEGORIES_SELECTOR 								= ".bread-crumb ul li a";
	
	private final String DESCRIPTION_SELECTOR 								= "#description";
	private final String SPECS_SELECTOR										= "#specification";

	public BrasilPoloarCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}


	@Override
	public List<Product> extractInformation(Document doc) {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getUrl()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			/* ***********************************
			 * crawling data of only one product *
			 *************************************/
			
			// crawl the skuJson
			JSONObject skuJson = crawlSkuJson(doc);

			// InternalId
			String internalId = crawlInternalId(doc);

			// Pid
			String internalPid = crawlInternalPid(doc);

			// Name
			String name = crawlName(doc);
			
			// Price
			Float price = crawlMainPagePrice(doc);

			// Availability
			boolean available = crawlAvailability(skuJson);

			// Categories
			ArrayList<String> categories = crawlCategories(doc);
			String category1 = getCategory(categories, 0);
			String category2 = getCategory(categories, 1);
			String category3 = getCategory(categories, 2);

			// Primary image
			String primaryImage = crawlPrimaryImage(doc);

			// Secondary images
			String secondaryImages = crawlSecondaryImages(doc);

			// Description
			String description = crawlDescription(doc);

			// Stock
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = crawlMarketplace(doc);

			// Creating the product
			Product product = new Product();
			product.setSeedId(this.session.getSeedId());
			product.setUrl(this.session.getUrl());
			product.setInternalId(internalId);
			product.setInternalPid(internalPid);
			product.setName(name);
			product.setPrice(price);
			product.setAvailable(available);
			product.setCategory1(category1);
			product.setCategory2(category2);
			product.setCategory3(category3);
			product.setPrimaryImage(primaryImage);
			product.setSecondaryImages(secondaryImages);
			product.setDescription(description);
			product.setStock(stock);
			product.setMarketplace(marketplace);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}
		
		return products;

	}



	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		if ( url.startsWith("http://www.poloar.com.br/") && url.endsWith("/p") ) return true;
		return false;
	}


	/*******************
	 * General methods *
	 *******************/

	private String crawlInternalId(Document document) {
		String internalId = null;
		Element internalIdElement = document.select(INTERNALID_SELECTOR).first();

		if (internalIdElement != null) {
			internalId = internalIdElement.attr(INTERNALID_SELECTOR_ATTRIBUTE).trim();
		}

		return internalId;
	}

	private String crawlInternalPid(Document document) {
		String internalPid = null;
		return internalPid;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select(NAME_SELECTOR).first();

		if (nameElement != null) {
			name = sanitizeName(nameElement.text());
		}

		return name;
	}

	private Float crawlMainPagePrice(Document document) {
		Float price = null;
		Element mainPagePriceElement = document.select(PRICE_SELECTOR).first();
		
		if (mainPagePriceElement != null) {
			price = Float.parseFloat( mainPagePriceElement.text().trim().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
		}

		return price;
	}

	private boolean crawlAvailability(JSONObject skuJson) {
		if (skuJson != null && skuJson.has("skus")) {
			JSONArray skus = skuJson.getJSONArray("skus");
			if (skus.length() > 0) {
				JSONObject sku = skus.getJSONObject(0);
				if (sku.has("available")) {
					return sku.getBoolean("available");
				}
			}
		}
		return false;
	}

	private JSONArray crawlMarketplace(Document document) {
		return new JSONArray();
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select(PRIMARY_IMAGE_SELECTOR).first();

		if (primaryImageElement != null) {
			primaryImage = primaryImageElement.attr(PRIMARY_IMAGE_SELECTOR_ATTRIBUTE);
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select(SECONDARY_IMAGES_SELECTOR);
	
		for (int i = 1; i < imagesElement.size(); i++) { // starting from index 1, because the first is the primary image
			secondaryImagesArray.put(imagesElement.get(i).attr(SECONDARY_IMAGES_SELECTOR_ATTRIBUTE));
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(CATEGORIES_SELECTOR);

		for (int i = 1; i < elementCategories.size(); i++) { // starting from index 1, because the first is the market name
			categories.add( elementCategories.get(i).text().trim() );
		}

		return categories;
	}

	private String getCategory(ArrayList<String> categories, int n) {
		if (n < categories.size()) {
			return categories.get(n);
		}
		return "";
	}

	private String crawlDescription(Document document) {
		String description = "";
		Element descriptionElement = document.select(DESCRIPTION_SELECTOR).first();
		Element specsElement = document.select(SPECS_SELECTOR).first();
		if (descriptionElement != null) description = description + descriptionElement.html();
		if (specsElement != null) description = description + specsElement.html();

		return description;
	}

	/**************************
	 * Specific manipulations *
	 **************************/

	private String sanitizeName(String name) {
		return name.replace("'","").replace("’","").trim();
	}
	
	/**
	 * Get the script having a json with the availability information
	 * @return
	 */
	private JSONObject crawlSkuJson(Document document) {
		Elements scriptTags = document.getElementsByTag("script");
		JSONObject skuJson = null;
		
		for (Element tag : scriptTags){                
			for (DataNode node : tag.dataNodes()) {
				if(tag.html().trim().startsWith("var skuJson_0 = ")) {

					skuJson = new JSONObject
							(
							node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1] +
							node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1].split(Pattern.quote("}]};"))[0]
							);

				}
			}        
		}
		
		return skuJson;
	}

}
