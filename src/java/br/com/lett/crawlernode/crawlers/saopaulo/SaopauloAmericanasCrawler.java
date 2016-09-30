package br.com.lett.crawlernode.crawlers.saopaulo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.core.task.Crawler;
import br.com.lett.crawlernode.util.Logging;


/************************************************************************************************************************************************************************************
 * Crawling notes (19/07/2016):
 * 
 * 1) For this crawler, we have one url for mutiples skus.
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is marketplace for mutiple variations in this ecommerce accessed via the url to 
 *  mutiple variations "http://www.americanas.com.br/parceiros/" + internalID + "/?codItemFusion=" + variationID, 
 *  and for single product is a simply selector in htmlPage.
 *  
 * 4) First to get marketplaces in mutiple variations, is crawled ids from partners in main page looking a simply the html element and put this in map
 * 	  Second accessed the url "http://www.americanas.com.br/parceiros/" + internalID + "/?codItemFusion=" + variationID for getting marketPlaces
 * 
 * 5) The sku page identification is done simply looking the URL format or simply looking the html element.
 * 
 * 6) Even if a product is unavailable, its price is not displayed, then price is null.
 * 
 * 7) There is internalPid for skus in this ecommerce. The internalPid is a number that is the same for all
 * the variations of a given sku.
 * 
 * 8) The first image in secondary images is the primary image.
 * 
 * 9) When the sku has variations, the variation name it is added to the name found in the main page. 
 * 
 * 10) When the market crawled not appear on the page of the partners, the sku is unavailable.
 * 
 * 11) The Id from the main Page is the internalPid.
 * 
 * 12) InternalPid from the main page is used to make internalId final.
 * 
 * Examples:
 * ex1 (available): http://www.americanas.com.br/produto/127115083/smartphone-moto-g-4-dual-chip-android-6.0-tela-5.5-16gb-camera-13mp-preto
 * ex2 (unavailable): http://www.americanas.com.br/produto/119936092/pneu-toyo-tires-aro-18-235-60r18-107v
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class SaopauloAmericanasCrawler extends Crawler {
	private final String HOME_PAGE = "http://www.americanas.com.br/";

	private final String MAIN_SELLER_NAME_LOWER = "americanas.com";
	private final String MAIN_SELLER_ID_PARTNER = "02";

	public SaopauloAmericanasCrawler(CrawlerSession session) {
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

		if( isProductPage(session.getUrl(), doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());


			/* *********************************************************
			 * crawling data common to both the cases of product page  *
			 ***********************************************************/

			// Pid
			String internalPid = this.crawlInternalPid(doc);

			// Name
			String name = this.crawlMainPageName(doc);

			// Categories
			ArrayList<String> categories = this.crawlCategories(doc);
			String category1 = getCategory(categories, 0);
			String category2 = getCategory(categories, 1);
			String category3 = getCategory(categories, 2);

			// Primary image
			String primaryImage = this.crawlPrimaryImage(doc);

			// Secondary images
			String secondaryImages = this.crawlSecondaryImages(doc);

			// Description
			String description = this.crawlDescription(doc);

			// Estoque
			Integer stock = null;

			/* **************************************
			 * crawling data of multiple variations *
			 ****************************************/
			if( hasProductVariations(doc) ) {

				Logging.printLogDebug(logger, session, "Crawling information of more than one product...");

				// sku data in json
				ArrayList<String> skuOptions = this.crawlSkuOptions(doc);		

				for(String internalId : skuOptions){	
					// InternalId
					String variationInternalID = internalPid + "-" + internalId;

					//variation name
					String variation = crawlNameVariation(doc, internalId);

					// Name
					String nameVariations = name;

					if(variation != null){
						nameVariations += " " + variation;
					}

					// Map of partners
					Map<String, String> partners = getIdsForMarketplaces(doc, internalId);

					// Marketplace map
					Map<String, Float> marketplaceMap = this.crawlMarketplacesForMutipleVariations(internalPid, internalId, partners, internalPid);

					// Assemble marketplace from marketplace map
					JSONArray variationMarketplace = this.assembleMarketplaceFromMap(marketplaceMap);

					// Available
					boolean available = this.crawlAvailability(marketplaceMap);

					// Price
					Float variationPrice = this.crawlPrice(marketplaceMap);

					Product product = new Product();
					product.setUrl(this.session.getUrl());

					product.setInternalId(variationInternalID);
					product.setInternalPid(internalPid);
					product.setName(nameVariations);
					product.setPrice(variationPrice);
					product.setCategory1(category1);
					product.setCategory2(category2);
					product.setCategory3(category3);
					product.setPrimaryImage(primaryImage);
					product.setSecondaryImages(secondaryImages);
					product.setDescription(description);
					product.setStock(stock);
					product.setMarketplace(variationMarketplace);
					product.setAvailable(available);

					products.add(product);

				}
			
			} else {

				// idvariation
				String idVariation = this.crawlInternalIDSingleProduct(doc);

				// InternalId
				String internalID = internalPid + "-" + idVariation;

				// Marketplace map
				Map<String, Float> marketplaceMap = this.crawlMarketplacesForSingleProduct(doc);

				// Assemble marketplace from marketplace map
				JSONArray marketplace = this.assembleMarketplaceFromMap(marketplaceMap);

				// Available
				boolean available = this.crawlAvailability(marketplaceMap);

				// Price
				Float price = this.crawlPrice(marketplaceMap);

				Product product = new Product();
				product.setUrl(this.session.getUrl());

				product.setInternalId(internalID);
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

	private boolean isProductPage(String url, Document doc) {
		Element productElement = doc.select("section.a-main-product").first();

		if (productElement != null && url.startsWith("http://www.americanas.com.br/produto/")) return true;
		return false;
	}



	/************************************
	 * Multiple products identification *
	 ************************************/

	private boolean hasProductVariations(Document document) {

		Elements skuChooser = document.select(".mb-sku-choose .custom-label-sku input");

		if (skuChooser.size() > 1) {
			return true;
		} else {

			Element sku = document.select("meta[itemprop=sku/list]").first();

			if (skuChooser != null) {
				String ids = sku.attr("content");
				String[] tokens = ids.split(",");

				if(tokens.length > 1){
					return true;
				}

			} 
		}

		return false;

	}

	private String crawlInternalPid(Document document) {
		String internalId = null;
		Element elementInternalId = document.select(".a-cod-prod span[itemprop=productID]").first();
		if (elementInternalId != null) {
			internalId = elementInternalId.text().trim();
		}

		return internalId;
	}


	/*******************************
	 * Single product page methods *
	 *******************************/

	private String crawlInternalIDSingleProduct(Document document) {
		String internalIDMainPage = null;
		Element elementDataSku = document.select(".mp-pricebox-wrp").first();

		if(elementDataSku != null) {
			internalIDMainPage = elementDataSku.attr("data-sku");
		}
		else {
			elementDataSku = document.select("meta[itemprop=sku/list]").first();
			if(elementDataSku != null) {
				internalIDMainPage = elementDataSku.attr("content");
			}
		}

		return internalIDMainPage;
	}

	private Map<String, Float> crawlMarketplacesForSingleProduct(Document doc) {
		Map<String, Float>  marketplace = new HashMap<String, Float> ();
		
		Element elementPartnerName = doc.select(".pickup-store .stock-highlight").first();
		Element elementPartnerPrice = doc.select("span[itemprop=price/salesPrice]").first();
		
		if(elementPartnerName != null && elementPartnerPrice != null){
			String partnerPrincipalName = elementPartnerName.text().toLowerCase().trim();		
			Float partnerPrincipalPrice = Float.parseFloat(elementPartnerPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			
			marketplace.put(partnerPrincipalName, partnerPrincipalPrice);
		}
		
		
		Elements partnersElements = doc.select("ul.bp-list > li > div");
		
		for(Element e : partnersElements){
			
			String partnerName = e.select(".bp-name").text().toLowerCase().trim();
			Float partnerPrice;
			Element priceElement = e.select(".bp-link .bp-lnk").first();
			
			if( partnerName.equals(MAIN_SELLER_NAME_LOWER) && priceElement == null ){
				priceElement = doc.select("span[itemprop=\"price/salesPrice\"]").first();
				partnerPrice = Float.parseFloat(priceElement.attr("content").trim());
			} else {
				partnerPrice = Float.parseFloat(priceElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}
		 
			
			marketplace.put(partnerName, partnerPrice);
		}
		
		return marketplace;
	}
	/*********************************
	 * Multiple product page methods *
	 *********************************/


	private String crawlNameVariation(Document doc, String internalId) {
		String name = null;
		Element elementName = doc.select("input[value="+ internalId +"][data-value-name]").first();
		if(elementName == null) elementName = doc.select("option[content="+ internalId +"][data-value-name]").first();

		if(elementName != null){
			name = elementName.attr("data-value-name").replace("'","").replace("’","").trim();
		}

		return name;
	}

	private ArrayList<String> crawlSkuOptions(Document doc){
		ArrayList<String> internalIds = new ArrayList<String>();
				
		String ids = getIdsFromDataLayer(doc);

		if(ids != null){			
			String[] tokens = ids.split(",");			
			for(int i = 0; i < tokens.length; i++){
				internalIds.add(tokens[i].trim());
			}
		} else {
			Element elementsProductOptions = doc.select("meta[itemprop=\"sku/list\"]").first();

			if(elementsProductOptions != null){
				String[] tokens = elementsProductOptions.attr("content").split(",");			
				for(int i = 0; i < tokens.length; i++){
					internalIds.add(tokens[i].trim());
				}
			}
		}


		return internalIds;
	}

	private String getIdsFromDataLayer(Document doc){
		String internalIds = null;
		Elements scripts = doc.select("script[type=text/javascript]");

		for(Element e : scripts){
			String json = e.html();

			if(json.contains("var objProduct")){
				int x = json.indexOf("objProduct =") + 12;
				int y = json.indexOf("};", x);

				json = json.substring(x, y+1);

				JSONObject skus = new JSONObject(json);

				if(skus.has("objSKUsProduto")){
					internalIds = skus.getString("objSKUsProduto");
				}

				break;
			}
		}

		return internalIds;
	}

	private Map<String, Float> crawlMarketplacesForMutipleVariations(String internalID, String internalIDVariation, Map<String, String> idsMarketplaces, String pid) {
		Map<String, Float>  marketplace = new HashMap<String, Float> ();

		if(idsMarketplaces.size() > 0){
			String url = "http://www.americanas.com.br/parceiros/" + internalID + "/" + "?codItemFusion=" + internalIDVariation;

			Document docMarketplaceInfo = fetchMarketplace(internalID, url);		
			if(hasPidInMarketplacePage(docMarketplaceInfo, pid)){

				Elements lines = docMarketplaceInfo.select("table.offers-table tbody tr.partner-line");

				for(Element linePartner: lines) {

					String partnerName = linePartner.select(".part-info.part-name").first().text().trim().toLowerCase();
					Float partnerPrice = Float.parseFloat(linePartner.select(".value-prod").first().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));;

					String id = linePartner.attr("data-partner-id").trim();

					if(idsMarketplaces != null){
						if(idsMarketplaces.containsKey(id) || (partnerName.equals(MAIN_SELLER_NAME_LOWER) && idsMarketplaces.containsKey(MAIN_SELLER_ID_PARTNER))){
							marketplace.put(partnerName, partnerPrice);
						}
					} else {
						marketplace.put(partnerName, partnerPrice);
					}

				}
			}
		}

		return marketplace;
	}

	private boolean hasPidInMarketplacePage(Document doc, String internalPid){
		if(doc != null){
			Element hasPid = doc.select("input[name=codProdFusion]").first();

			if(hasPid != null){
				String pid = hasPid.attr("value").trim();

				if(pid.equals(internalPid)){
					return true;
				}
			}
		}

		return false;
	}

	private Map<String, String> getIdsForMarketplaces(Document doc, String internalID){
		Map<String, String> idsMarketplaces = new HashMap<String, String>();

		Elements ids = doc.select("span.custom-input-radio > input");
		String[] tokens = null;

		if(ids.size() >=1){
			for(Element e : ids){
				if(e.attr("value").equals(internalID)){
					String partnersCode = e.attr("data-partners");

					if(!partnersCode.isEmpty()){
						tokens = e.attr("data-partners").split(",");
					}

					break;
				}
			}

			if(tokens != null){
				for(int i = 0; i < tokens.length; i++){
					idsMarketplaces.put(tokens[i].trim(), internalID);
				}
			}
		}

		return idsMarketplaces;
	}



	/*******************
	 * General methods *
	 *******************/

	private Float crawlPrice(Map<String, Float> marketplaces) {
		Float price = null;

		for (String seller : marketplaces.keySet()) {
			if (seller.equals(MAIN_SELLER_NAME_LOWER)) {
				price = marketplaces.get(seller);
				break;
			}
		}		
		return price;
	}

	private boolean crawlAvailability(Map<String, Float> marketplaces) {
		boolean available = false;

		for (String seller : marketplaces.keySet()) {
			if (seller.equals(MAIN_SELLER_NAME_LOWER)) {
				available = true;
			}
		}

		return available;
	}

	private String crawlPrimaryImage(Document document) {

		String primaryImage = null;

		Element primaryImageElements = document.select(".mp-picture img").first();

		if(primaryImageElements.hasAttr("data-zoom-image")){
			primaryImage = primaryImageElements.attr("data-zoom-image");
			if(primaryImage.equals("#")){
				primaryImage = primaryImageElements.attr("src");
			}
		} else {
			primaryImage = primaryImageElements.attr("src");
		}

		if(primaryImage != null && primaryImage.contains("notFound.gif")) {
			primaryImage = null;
		}

		return primaryImage;
	}

	private String crawlMainPageName(Document document) {
		String name = null;
		Elements elementName = document.select(".mp-title h1 span");
		if(elementName.size() > 0) {
			name = elementName.text().replace("'","").replace("’","").trim();
		}
		return name;
	}

	private ArrayList<String> crawlCategories(Document document) {
		Elements elementCategories = document.select(".breadcrumb span[itemprop=\"name\"]");
		ArrayList<String> categories = new ArrayList<String>();

		for(int i = 0; i < elementCategories.size(); i++) {
			Element e = elementCategories.get(i);
			String tmp = e.text().toString();

			tmp = tmp.replace(">", "");
			categories.add(tmp);
		}

		return categories;
	}

	private String getCategory(ArrayList<String> categories, int n) {
		if (n < categories.size()) {
			return categories.get(n);
		}

		return "";
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;

		JSONArray secondaryImagesArray = new JSONArray();
		Elements elementFotoSecundaria = document.select(".a-carousel-item img");

		if (elementFotoSecundaria.size()>1) {
			for(int i = 1; i < elementFotoSecundaria.size(); i++) { //starts with index 1 because de primary image is the first image
				Element e = elementFotoSecundaria.get(i);

				if(e.hasAttr("data-szimg"))	secondaryImagesArray.put(e.attr("data-szimg"));
				else 						secondaryImagesArray.put(e.attr("src"));
			}

		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}	

	private JSONArray assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		JSONArray marketplace = new JSONArray();

		for(String sellerName : marketplaceMap.keySet()) {
			if ( !sellerName.equals(MAIN_SELLER_NAME_LOWER) ) {
				JSONObject seller = new JSONObject();
				seller.put("name", sellerName);
				seller.put("price", marketplaceMap.get(sellerName));

				marketplace.put(seller);
			}
		}

		return marketplace;
	}

	private Document fetchMarketplace(String internalID, String url) {
		Document docMarketplaceInfo = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, null);

		return docMarketplaceInfo;
	}


	private String crawlDescription(Document document) {
		String description = "";
		Element elementProductDetails = document.select("#productdetails").first();
		if(elementProductDetails != null) 	description = description + elementProductDetails.html();

		return description;
	}	

}
