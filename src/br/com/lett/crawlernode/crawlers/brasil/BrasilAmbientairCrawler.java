package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.base.Crawler;
import br.com.lett.crawlernode.models.CrawlerSession;
import br.com.lett.crawlernode.models.Product;
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
 * 4) The sku page identification is done simply looking for an specific html element.
 * 
 * 5) if the sku is unavailable, it's price is not displayed.
 * 
 * 6) There is no internalPid for skus in this ecommerce. The internalPid must be a number that is the same for all
 * the variations of a given sku.
 * 
 * 7) We have one method for each type of information for a sku (please carry on with this pattern).
 * 
 * Examples:
 * ex1 (available, multiple images): http://www.ambientair.com.br/split-hi-wall/split-hi-wall-midea-liva-eco-9000-btuh-frio-220v-42mfcb09m5-38kcv09m5.html
 * ex2 (unavailable): http://www.ambientair.com.br/condicionadores-de-ar-janela/ar-condicionado-portatil-springer-nova-mpn12crv2-12000-btuh-frio-c-contr-remoto-220v.html 
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilAmbientairCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.ambientair.com.br/";
	private final String PROTOCOL = "http://";
	
	public BrasilAmbientairCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}


	@Override
	public void extractInformation(Document doc) {
		super.extractInformation(doc);

		if ( isProductPage(doc) ) {

			Logging.printLogDebug(logger, "Product page identified: " + this.session.getUrl());

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
//			product.setSeedId(seedId);
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

			// execute finalization routines of this sku crawling
//			executeFinishingRoutines(product, truco);


		} else {
			Logging.printLogTrace(logger, "Not a product page " + this.session.getUrl());

//			if ( Main.mode.equals(Main.MODE_INSIGHTS) ) {
//				this.crawlerController.scheduleUrlToReprocess(url);
//			}
		}

	}



	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(Document document) {
		if ( document.select("#descricao").first() != null ) return true;
		return false;
	}
	
	
	/*******************
	 * General methods *
	 *******************/
	
	private String crawlInternalId(Document document) {
		String internalId = null;
		Elements internalIdElements = document.select("input[name=variacao]");

		if (internalIdElements.size() > 0) {
			for (Element id : internalIdElements) {
				if (id.attr("value") != null && !id.attr("value").isEmpty()) {
					internalId = id.attr("value").trim();
					break;
				}
			}		
		}

		return internalId;
	}

	private String crawlInternalPid(Document document) {
		String internalPid = null;

		return internalPid;
	}
	
	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("h2.produto").first();

		if (nameElement != null) {
			name = sanitizeName(nameElement.text());
		}

		return name;
	}

	private Float crawlMainPagePrice(Document document) {
		Float price = null;
		Element mainPagePriceElement = document.select(".preco .precoPor").first();

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

	private JSONArray crawlMarketplace(Document document) {
		return new JSONArray();
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select("#foto #produto img").first();

		if (primaryImageElement != null) {
			primaryImage = PROTOCOL + "www.ambientair.com.br" + primaryImageElement.attr("data-zoom-image").trim();
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select("#foto #extras ul li a");

		for (int i = 1; i < imagesElement.size(); i++) { // starting from index 1, because the first is the primary image
			secondaryImagesArray.put( PROTOCOL + "www.ambientair.com.br" + imagesElement.get(i).attr("data-zoom-image").trim() );
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select("#breadcrumb a");

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
		Element descriptionElement = document.select("#abas1").first();
		Element specElement = document.select("#abas2").first();

		if (descriptionElement != null) description = description + descriptionElement.html();
		if (specElement != null) description = description + specElement.html();

		return description;
	}

//	private void executeFinishingRoutines(Product product, ProcessedModel truco) {
//		try {
//
//			if ( this.missingProduct(product) && Main.mode.equals(Main.MODE_INSIGHTS) ) {
//				this.crawlerController.scheduleUrlToReprocess( product.getUrl() );
//			}
//
//			else {
//
//				// print information on console
//				this.printExtractedInformation(product);
//
//				// upload image to s3
//				if (product.getPrimaryImage() != null && !product.getPrimaryImage().equals("")) {
//					db.uploadImageToAmazon(this, product.getPrimaryImage(), product.getInternalId());
//				}
//
//				// persist information on database
//				this.persistInformation(product, this.marketId, truco, product.getUrl());
//
//			}
//
//		} catch (Exception e1) {
//			e1.printStackTrace();
//		}
//	}
	
	/**************************
	 * Specific manipulations *
	 **************************/
	
	private String sanitizeName(String name) {
		return name.replace("'","").replace("’","").trim();
	}
	

}
