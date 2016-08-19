package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.Crawler;
import br.com.lett.crawlernode.kernel.CrawlerSession;
import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.util.Logging;

/************************************************************************************************************************************************************************************
 * Crawling notes (11/07/2016):
 * 
 * 1) For this crawler, we have one url per each sku. There is no page is more than one sku in it.
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done by looking for a specific html element of a sku.
 * 
 * 5) If the sku is unavailable, it's price is not displayed.
 * 
 * 6) There is no internalPid for skus in this ecommerce. The internalPid must be a number that is the same for all
 * the variations of a given sku.
 * 
 * 7) On crawling the descriptions, the pattern observed is that we have 3 tabs. The first one is the description, the second is
 * tech specs and third is for sku insurance. We consider only the first and second.
 * 
 * 8) We have one method for each type of information for a sku (please carry on with this pattern).
 * 
 * Examples:
 * ex1 (available): http://www.climario.com.br/split-inverter/22000-a-30000-btus/ar-condicionado-split-hi-wall-hitachi-inverter-22000-btuh-220v-frio-hitachi.html
 * ex2 (unavailable): http://www.climario.com.br/ar-de-janela/11000-a-19000-btus/ar-condicionado-de-janela-springer-minimaxi-12000-btus-220v-mecanico-frio-springer.html 
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilClimarioCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.climario.com.br/";
	private final String MAIN_DOMAIN = "http://www.climario.com.br";
	
	public BrasilClimarioCrawler(CrawlerSession session) {
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

			Logging.printLogDebug(logger, session, "Product page identified: " + session.getUrl());

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

			// Marketplace map
			Map<String, Float> marketplaceMap = crawlMarketplace(doc);

			// Marketplace
			JSONArray marketplace = assembleMarketplaceFromMap(marketplaceMap);

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
			Logging.printLogTrace(logger, "Not a product page" + this.session.getUrl());
		}
		
		return products;
	}



	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(Document document) {
		Element skuSchemaElement = document.select("div[itemtype=http://schema.org/Product]").first();
		if (skuSchemaElement != null) return true;
		return false;
	}
	
	
	/*******************
	 * General methods *
	 *******************/
	
	private String crawlInternalId(Document document) {
		String internalId = null;
		Element internalIdElement = document.select("#prod_id").first();

		if (internalIdElement != null) {
			internalId = internalIdElement.attr("value").toString().trim();			
		}

		return internalId;
	}

	private String crawlInternalPid(Document document) {
		String internalPid = null;

		return internalPid;
	}
	
	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select(".tituloProdDetalhe span[itemprop=name]").first();

		if (nameElement != null) {
			name = sanitizeName(nameElement.text());
		}

		return name;
	}

	private Float crawlMainPagePrice(Document document) {
		Float price = null;
		Element mainPagePriceElement = document.select(".precoPor .val").first();

		if (mainPagePriceElement != null) {
			price = Float.parseFloat( mainPagePriceElement.text().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
		}

		return price;
	}
	
	private boolean crawlAvailability(Document document) {
		Element notifyMeElement = document.select(".produtoIndisponivel").first();
		if (notifyMeElement != null) return false;
		return true;
	}

	private Map<String, Float> crawlMarketplace(Document document) {
		return new HashMap<String, Float>();
	}
	
	private JSONArray assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		return new JSONArray();
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select(".imgDetalhe a img").first();

		if (primaryImageElement != null) {
			primaryImage = primaryImageElement.attr("content").trim();
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select("#mycarousel li a img");

		for (int i = 1; i < imagesElement.size(); i++) { // starting from index 1, because the first is the primary image
			String image = MAIN_DOMAIN + imagesElement.get(i).attr("src").trim();
			secondaryImagesArray.put( modifyImageURL(image) );
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select("#breadCrumbDetalhe");

		String[] tokens = elementCategories.text().split(">");		
		for (int i = 1; i < tokens.length; i++) {
			categories.add( sanitizeName(tokens[i]) );
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
		Element descriptionElement = document.select("#abas1").first();
		Element specElement = document.select("#abas2").first();

		if (descriptionElement != null) description = description + descriptionElement.html();
		if (specElement != null) description = description + specElement.html();

		return description;
	}
	
	/**************************
	 * Specific manipulations *
	 **************************/
	
	private String sanitizeName(String name) {
		return name.replace("'","").replace("’","").trim();
	}
	
	
	/**
	 * Modify the imageURL to get a bigger version of the image.
	 * The logic used in this ecommerce is to modify the first two letters in the number code
	 * for the image, just before the .jpg. The observed pattern is that 'PP' is for the smallest version,
	 * and OP is for the biggest.
	 * 
	 * @param imageURL the image URL with the main domain already appended
	 * @return the modified imageURL for the biggest version
	 */
	private String modifyImageURL(String imageURL) {
		return imageURL.replace("__PP", "__OP");
	}

}
