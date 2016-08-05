package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.Crawler;
import br.com.lett.crawlernode.kernel.CrawlerSession;
import br.com.lett.crawlernode.models.Product;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;


/*****************************************************************************************************************************
 * Crawling notes (12/07/2016):
 * 
 * 1) For this crawler, we have one url per each sku.
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 4) The sku page identification is done simply looking for an specific html element.
 * 5) if the sku is unavailable, it's price is not displayed. Yet the crawler tries to crawl the price.
 * 6) There is no internalPid for skus in this ecommerce. The internalPid must be a number that is the same for all
 * the variations of a given sku.
 * 7) We have one method for each type of information for a sku (please carry on with this pattern).
 * 
 * Examples:
 * ex1 (available): https://www.dufrio.com.br/ar-condicionado-frio-split-cassete-inverter-35000-btus-220v-lg.html
 * ex2 (unavailable): https://www.dufrio.com.br/ar-condicionado-frio-split-piso-teto-36000-btus-220v-1-lg.html
 *
 ******************************************************************************************************************************/

public class BrasilDufrioCrawler extends Crawler {
	
	private final String HOME_PAGE = "https://www.dufrio.com.br/";
	private final String DOMAIN = "https://www.dufrio.com.br";
	
	private final String SKU_PAGE_IDENTIFICATION_SELECTOR 	= ".detalhe-do-produto";
	private final String INTERNALID_SELECTOR 				= ".codigo-do-produto .dufrio span";
	private final String NAME_SELECTOR 						= "h1.nome-do-produto";
	private final String PRICE_SELECTOR 					= ".grupo-de-precos span.por";
	private final String AVAILABILITY_SELECTOR 				= ".estoque-nao-disponivel";
	private final String PRIMARY_IMAGE_SELECTOR 			= ".imagem-do-produto img";
	private final String SECONDARY_IMAGES_SELECTOR 			= ".mini-galeria .item img";
	private final String CATEGORIES_SELECTOR 				= ".breadcrumb div.breadcrumb-item a span";
	private final String DESCRIPTION_SELECTOR 				= ".caracteristicas";

	public BrasilDufrioCrawler(CrawlerSession session) {
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

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			/* ***********************************
			 * crawling data of only one product *
			 *************************************/

			// InternalId
			String internalId = crawlInternalId(doc);

			// Pid
			String internalPid = crawlInternalPid(doc);

			// Name
			String name = crawlName(doc);

			// Price
			Float price = crawlMainPagePrice(doc);

			// Availability
			boolean available = crawlAvailability(doc);

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
		if ( document.select(SKU_PAGE_IDENTIFICATION_SELECTOR).first() != null ) return true;
		return false;
	}


	/*******************
	 * General methods *
	 *******************/

	private String crawlInternalId(Document document) {
		String internalId = null;
		Element internalIdElement = document.select(INTERNALID_SELECTOR).first();

		if (internalIdElement != null) {
			internalId = internalIdElement.text();
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
			price = Float.parseFloat( mainPagePriceElement.text().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
		}

		return price;
	}

	private boolean crawlAvailability(Document document) {
		Element notifyMeElement = document.select(AVAILABILITY_SELECTOR).first();
		if (notifyMeElement != null) return false;
		return true;
	}

	private JSONArray crawlMarketplace(Document document) {
		return new JSONArray();
	}
	
	/**
	 * Crawl the primary image.
	 * The URL crawled must be modified. It contains the size of the image as parameters in the URL.
	 * It was observed that the biggest size is 900x900. These two values are modified using an auxiliar method
	 * from the class URLUtils. Note that the same modification must be done on the secondary images.
	 * 
	 * @param document
	 * @return the URL from the primary image
	 */
	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select(PRIMARY_IMAGE_SELECTOR).first();

		if (primaryImageElement != null) {
			String image = modifyImageSizeParameters( removeWhiteSpace(primaryImageElement.attr("src")) );
			if (image != null) {
				primaryImage = DOMAIN + image;
			} else {
				Logging.printLogError(logger, session, "Error modifyng URL parameter for image size.");
			}
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select(SECONDARY_IMAGES_SELECTOR);

		for (int i = 1; i < imagesElement.size(); i++) { // starting from index 1, because the first is the primary image
			String image = modifyImageSizeParameters( removeWhiteSpace(imagesElement.get(i).attr("src")) );
			if (image != null) {
				secondaryImagesArray.put(DOMAIN + image);
			} else {
				Logging.printLogError(logger, session, "Error modifyng URL parameter for image size.");
			}
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}
		
		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(CATEGORIES_SELECTOR);

		for (int i = 1; i < elementCategories.size() - 2; i++) { // starting from index 1, because the first is the market name
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
		if (descriptionElement != null) description = description + descriptionElement.html();
		
		return description;
	}

	/**************************
	 * Specific manipulations *
	 **************************/

	private String sanitizeName(String name) {
		return name.replace("'","").replace("’","").trim();
	}
	
	/**
	 * Remove white spaces from the string.
	 * Used to remove white from the images URL.
	 * 
	 * @param string
	 * @return
	 */
	private String removeWhiteSpace(String string) {
		return string.replaceAll(" ", "");
	}

	/**
	 * Modify URL parameter for image size.
	 * 
	 * @param url
	 * @return
	 */
	private String modifyImageSizeParameters(String url) {
		String imageWithNewPw = CommonMethods.modifyParameter(url, "pw", "900");
		if (imageWithNewPw != null) {
			return CommonMethods.modifyParameter(imageWithNewPw, "ph", "900");
		}
		return null;
	}
	
}
