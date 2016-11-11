package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.BrasilFastshopCrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

/**
 * e.g:
 * 
 * With marketplace: http://www.fastshop.com.br/loja/tratamentodear/arcondicionado1/ar-condicionado-split-hi-wall-midea-elite-30-000-btus-quente-frio-220v-4363-fast?cm_re=FASTSHOP%3ASub-departamento%3AAr%2BCondicionado-_-Vitrine%2B36-_-4395
 * 
 * @author Samir Leao
 *
 */
public class BrasilFastshopCrawler extends Crawler {

	private final String HOME_PAGE_HTTP = "http://www.fastshop.com.br/";
	private final String HOME_PAGE_HTTPS = "https://www.fastshop.com.br";

	public BrasilFastshopCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && ( (href.startsWith(HOME_PAGE_HTTP) || href.startsWith(HOME_PAGE_HTTPS)) ); 
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getOriginalURL(), doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			JSONObject dataLayer = BrasilFastshopCrawlerUtils.crawlFullSKUInfo(doc);
			
			String script = getDataLayerJson(doc);
			JSONObject dataLayerObject = null;
			try {
				dataLayerObject = new JSONObject(script);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (dataLayerObject == null) {
				dataLayerObject = new JSONObject();
			}

			// internal pid
			String internalPid = crawlInternalPid(doc);

			// name
			String name = crawlName(dataLayer);

			// Categoria
			String category1 = "";
			String category2 = "";
			String category3 = "";
			Elements elementCategories = doc.select("#widget_breadcrumb ul li:not(.current)");
			ArrayList<String> categories = new ArrayList<String>();
			for(Element e : elementCategories) {
				if( e.select("a").first() != null ) {
					String tmp = e.select("a").first().text();
					if( !tmp.equals("Home") ) {
						categories.add(tmp);
					} 
				} else {
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

			// primary image
			String primaryImage = crawlPrimaryImage(doc);

			// secondary images
			String secondaryImages = crawlSecondaryImages(doc);

			// description
			String description = crawlDescription(doc);

			// Estoque
			Integer stock = null;
			
			Element variationSelector = doc.select(".options_dropdown").first();

			String idSelector = "#entitledItem_" + internalPid;
			Element elementInfo = doc.select(idSelector).first();
			JSONArray jsonInfo = null;
			if (elementInfo != null) {
				jsonInfo = new JSONArray(elementInfo.text());
			}
			
			/*
			 * Produto sem variação
			 */
			if(variationSelector == null && jsonInfo.length() < 2){

				// ID interno
				String internalId = null;
				if (jsonInfo.getJSONObject(0).has("catentry_id")) {
					internalId = jsonInfo.getJSONObject(0).getString("catentry_id").trim();
				}

				// Disponibilidade
				boolean available = true;
				Element elementUnavailable = doc.select(".price_holder .unavailableProductLabel").first();
				if(elementUnavailable != null) {
					available = false;
				}

				String partnerName = null;
				if (dataLayerObject.has("mktPlacePartner")) {
					partnerName = dataLayerObject.getString("mktPlacePartner");
				}
				if(partnerName != null && !partnerName.isEmpty()) {
					if( !partnerName.equals("Fastshop") || !partnerName.equals("fastshop") ) {
						available = false;
					}
				}

				// Preço
				Float price = null;
				if(available) {
					if (dataLayerObject.has("installmentTotalValue")) {
						if(!dataLayerObject.getString("installmentTotalValue").isEmpty()){
							price = Float.parseFloat( dataLayerObject.getString("installmentTotalValue") );
						} else if (dataLayerObject.has("productSalePrice")) {
							if(!dataLayerObject.getString("productSalePrice").isEmpty()){
								price = Float.parseFloat( dataLayerObject.getString("productSalePrice") );
							}
						}
					}
					else if (dataLayerObject.has("productSalePrice")) {
						price = Float.parseFloat( dataLayerObject.getString("productSalePrice") );
					}
				}

				// Marketplace
				JSONArray marketplace = new JSONArray();
				if(partnerName != null && !partnerName.isEmpty()) {
					JSONObject partner = new JSONObject();
					Float partnerPrice = null;
					if (dataLayerObject.has("installmentTotalValue")) {
						if (!dataLayerObject.getString("installmentTotalValue").isEmpty()) {
							partnerPrice = Float.parseFloat( dataLayerObject.getString("installmentTotalValue") );
						}
						else if (dataLayerObject.has("productSalePrice")) {
							if (!dataLayerObject.getString("productSalePrice").isEmpty()) {
								partnerPrice = Float.parseFloat( dataLayerObject.getString("productSalePrice") );
							}
						}
					}
					
					
					partner.put("name", partnerName);
					partner.put("price", partnerPrice);
					
					marketplace.put(partner);
				}

				// Page of Prices
				Document docPrices = fetchPrices(internalId, price);
				
				// Prices
				Prices prices = crawlPrices(docPrices);
				
				Product product = new Product();
				product.setUrl(this.session.getOriginalURL());
				product.setInternalId(internalId);
				product.setInternalPid(internalPid);
				product.setName(name);
				product.setPrice(price);
				product.setPrices(prices);
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

			else { // Produto com variação

				for (int i = 0; i < jsonInfo.length(); i++) {
					JSONObject productInfo = jsonInfo.getJSONObject(i);

					// InternalId
					String internalId = productInfo.getString("catentry_id").trim();

					// Nome
					String variationName = name + " ";
					if ( !productInfo.getJSONObject("Attributes").isNull("Voltagem_110V") ) {
						if (productInfo.getJSONObject("Attributes").get("Voltagem_110V").equals("1"));
						variationName = variationName + "110V";
					}
					else if ( !productInfo.getJSONObject("Attributes").isNull("Voltagem_220V") ) {
						if (productInfo.getJSONObject("Attributes").get("Voltagem_220V").equals("1"));
						variationName = variationName + "220V";
					}

					// Disponibilidade
					boolean variationAvailable = productInfo.getString("ShippingAvailability").equals("1");

					// Preço					
					Float price = null;
					if (variationAvailable) {
						price = crawlPriceFromApi(internalId, internalPid);
					}
					
					// Page of Prices
					Document docPrices = fetchPrices(internalId, price);
					
					// Prices
					Prices prices = crawlPrices(docPrices);

					Product product = new Product();
					product.setUrl(this.session.getOriginalURL());
					product.setInternalId(internalId);
					product.setInternalPid(internalPid);
					product.setName(variationName);
					product.setPrice(price);
					product.setPrices(prices);
					product.setCategory1(category1);
					product.setCategory2(category2);
					product.setCategory3(category3);
					product.setPrimaryImage(primaryImage);
					product.setSecondaryImages(secondaryImages);
					product.setDescription(description);
					product.setStock(stock);
					//product.setMarketplace(marketplace);
					product.setAvailable(variationAvailable);
					
					products.add(product);
				}
			}


		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}
		
		return products;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url, Document doc) {
		Element elementProductInfoViewer = doc.select("#widget_product_info_viewer").first();
		return elementProductInfoViewer != null;
	}

	private String getDataLayerJson(Document doc) {
		String script = searchScript(doc);
		String dataLayer = script.substring(0, script.length()-5) + "}";

		Scanner scanner = new Scanner(dataLayer);
		String jsonDataLayerObject = "{";

		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if ( line.contains("productId") || line.contains("productName") || line.contains("mktPlacePartner") || line.contains("installmentTotalValue") || line.contains("productSalePrice")) {
				jsonDataLayerObject = jsonDataLayerObject + line.substring(0, line.length()) + "\n";
			}
		}
		
		if (scanner != null) {
			scanner.close();
		}

		return jsonDataLayerObject;
	}

	private String searchScript(Document doc) {
		Elements scripts = doc.select("script");
		for(Element script : scripts) {
			String scriptBody = script.html().toString();
			if(scriptBody.contains("dataLayer")) {
				return scriptBody;
			}
		}
		return null;
	}
	
	private String crawlInternalPid(Document document) {
		String internalPid = null;
		Element elementInternalPid = document.select(".main_header").first();
		if (elementInternalPid != null) {
			internalPid = elementInternalPid.attr("id").split("_")[2].trim();
		}
		
		return internalPid;
	}
	
	private String crawlName(JSONObject dataLayer) {
		String name = null;
		if (dataLayer.has("productName")) {
			name = dataLayer.getString("productName");
		}
		
		return name;
	}
	
	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element elementPrimaryImage = document.select(".image_container #productMainImage").first();
		if(elementPrimaryImage != null) {
			primaryImage = "http:" + elementPrimaryImage.attr("src");
		}
		
		return primaryImage;
	}
	
	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();
		Elements elementsSecondaryImages = document.select(".other_views ul li a img");
		for (Element e : elementsSecondaryImages) {
			String secondaryImage = e.attr("src");
			if( !secondaryImage.contains("PRD_447_1.jpg") ) {
				secondaryImagesArray.put("http:" + e.attr("src"));
			}
		}
		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}
		
		return secondaryImages;
	}
	
	private String crawlDescription(Document document) {
		String description = "";
		Element productTabContainer = document.select("#productTabContainer").first();
		if (productTabContainer != null) {
			description = productTabContainer.text().trim();
		}
		return description;
	}
	
	/**
	 * This method creates a marketplace map, associating each parter name
	 * with the price of the sku in this partner. This information is extracted
	 * from the dataLayer json, on the field 'mktPlacePartner'.
	 * 
	 * Observed pattern for fields in dataLayer:
	 * 'mktPlacePartner': in this ecommerce we couldn't observe any sku example with more than one different seller from main ecommerce.
	 * 'installmentTotalValue': this field is always empty on the case that we have one marketplace partner different from the main ecommerce.
	 * 'productSalePrice': always not empty and in case the installmentTotalValue is empty, we use the productSalePrice to get the sku price.
	 * 
	 * Obs: the main ecommerce ("fastshop") is not included in this map, in case that it's name is on the 'mktPlacePartner'. But
	 * was observed that the name of the main ecommece was never included on the mktPlacePartner field.
	 * 
	 * @param dataLayer
	 * @return
	 */
//	private Map<String, Float> assembleMarketplaceMap(JSONObject dataLayer) {
//		Map<String, Float> marketplaceMap = new HashMap<String, Float>();
//		
//		// get the name on the field 'mktPlacePartner' on dataLayer json
//		String partnerName = null;
//		if (dataLayer.has("mktPlacePartner")) {
//			String marketplace = dataLayer.getString("mktPlacePartner");
//			if(marketplace != null && !marketplace.isEmpty()) {
//				if (!marketplace.equals("fastshop")) {
//					partnerName = marketplace.toLowerCase().trim();
//				}
//			}
//		}
//		
//		// get the price
//		if(partnerName != null) {
//			Float partnerPrice = null;
//			if (dataLayer.has("installmentTotalValue")) {
//				if (!dataLayer.getString("installmentTotalValue").isEmpty()) {
//					partnerPrice = Float.parseFloat( dataLayer.getString("installmentTotalValue") );
//				}
//				else if (dataLayer.has("productSalePrice")) {
//					if (!dataLayer.getString("productSalePrice").isEmpty()) {
//						partnerPrice = Float.parseFloat( dataLayer.getString("productSalePrice") );
//					}
//				}
//			}
//			
//			if ( (partnerName != null && !partnerName.isEmpty()) && (partnerPrice != null) ) {
//				marketplaceMap.put(partnerName, partnerPrice);
//			}
//		}
//		
//		return marketplaceMap;
//	}
	
//	private JSONArray crawlMarkeplace(Map<String, Float> marketplaceMap) {
//		JSONArray marketplace = new JSONArray();
//		
//		for (String partner : marketplaceMap.keySet()) {
//			
//		}
//	}
	
	private Float crawlPriceFromApi(String internalId, String internalPid) {
		Float price = null;
		
		String url = "http://www.fastshop.com.br/webapp/wcs/stores/servlet/GetCatalogEntryDetailsByIDView?storeId=10151"
				+ "&langId=-6&catalogId=11052&catalogEntryId="+ internalId +"&productId="+ internalPid +"&hotsite=fastshop";
		
		String json = DataFetcher.fetchString(DataFetcher.POST_REQUEST, session, url, "", null);
		
		int x = json.indexOf("/*");
		int y = json.indexOf("*/", x + 2);
		
		json = json.substring(x+2, y);
		
		JSONObject jsonPrice = new JSONObject();
		try{
			jsonPrice = new JSONObject(json);
		} catch(Exception e){
			e.printStackTrace();
		}
		
		if(jsonPrice.has("catalogEntry")){
			JSONObject jsonCatalog = jsonPrice.getJSONObject("catalogEntry");
			
			if(jsonCatalog.has("formattedTotalAVista")){
				price = Float.parseFloat(jsonCatalog.getString("formattedTotalAVista").trim().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			} else if(jsonCatalog.has("installmentRow3")){
				price = Float.parseFloat(jsonCatalog.getString("installmentRow3").trim().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}
		}
		
		return price;
	}

	
	private Document fetchPrices(String internalId, Float price){
		Document doc = new Document(internalId);
		
		if(price != null){
			String url = "http://www.fastshop.com.br/webapp/wcs/stores/servlet/AjaxFastShopPaymentOptionsView?"
					+ "catEntryIdentifier="+ internalId +"&hotsite=fastshop&storeId=10151";
			
			doc = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, cookies);
		}
		
		return doc;
	}
	
	private Prices crawlPrices(Document docPrices){
		Prices prices = new Prices();
		Map<Integer,Float> installmentPriceMap = new HashMap<>();
		
		Element priceBank = docPrices.select(".boleto #price1x").first();
		
		if(priceBank != null){
			Float banckTicket = Float.parseFloat(priceBank.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());
			
			prices.insertBankTicket(banckTicket);
		}
		
		Elements installmentsElements = docPrices.select("#paymentMethod_VISA tbody tr");
		
		for(Element e : installmentsElements){
			Elements tags = e.select("td");
			Element parcel = tags.get(0);
			
			if(parcel != null){
				String temp = parcel.text().toLowerCase();
				Integer installment;
				
				if(!temp.contains("vista")){
					int x = temp.indexOf("x");
					
					installment = Integer.parseInt(temp.substring(0, x).trim());
				} else {
					installment = 1;
				}
				
				Element parcelValue = tags.get(1);
				
				if(parcelValue != null){
					Float value = Float.parseFloat(parcelValue.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());
					
					installmentPriceMap.put(installment, value);
				}
			}
		}
		
		prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);	
		
		return prices;
	}
}
