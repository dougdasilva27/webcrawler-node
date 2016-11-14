package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.crawlers.extractionutils.BrasilFastshopCrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;

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

	public BrasilFastshopCrawler(CrawlerSession session) {
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

			JSONArray jsonInfo = BrasilFastshopCrawlerUtils.crawlSkusInfo(doc);

			// internal pid
			String internalPid = crawlInternalPid(doc);

			// name
			String name = crawlName(doc);

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

			/*
			 * Produto sem variação
			 */
			if (variationSelector == null && jsonInfo.length() < 2) {

				// internal id
				String internalId = null;
				if (jsonInfo.getJSONObject(0).has("catentry_id")) {
					internalId = jsonInfo.getJSONObject(0).getString("catentry_id").trim();
				}

				// availability
				boolean available = true;
				Element unavailableElement = doc.select("#buy_holder_unavailable_button").first();
				Element soldBy = doc.select("div.mktPartnerProductPageAlign span[class^=mktPartner]").first();
				if (unavailableElement != null || soldBy != null) {
					available = false;
				}

				// Price
				Float price = null;
				if (available) {
					price = crawlPriceFromApi(internalId, internalPid);
				}

				// Prices
				Document docPrices = fetchPrices(internalId, price);
				Prices prices = null;
				if (available) {
					prices = crawlPrices(docPrices);
				} else {
					prices = new Prices();
				}
				
				// Marketplace
				JSONArray marketplace = new JSONArray();
				Element mktElement = doc.select("div.mktPartnerProductPageAlign span[class^=mktPartner]").first();
				if (mktElement != null) {
					JSONObject seller = new JSONObject();
					
					Float mktPrice = crawlPriceFromApi(internalId, internalPid);
					Prices mktPrices = crawlPrices(docPrices);
					
					seller.put("name", mktElement.text().toLowerCase().trim());
					seller.put("price", mktPrice);
					seller.put("prices", mktPrices.getPricesJson());
					
					if (mktPrice != null || mktPrices.getBankTicketPrice() != null) {
						marketplace.put(seller);
					}
				}

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

					// name
					String variationName = name + " ";
					if ( !productInfo.getJSONObject("Attributes").isNull("Voltagem_110V") ) {
						if (productInfo.getJSONObject("Attributes").get("Voltagem_110V").equals("1"));
						variationName = variationName + " 110V";
					}
					else if ( !productInfo.getJSONObject("Attributes").isNull("Voltagem_220V") ) {
						if (productInfo.getJSONObject("Attributes").get("Voltagem_220V").equals("1"));
						variationName = variationName + " 220V";
					}

					// availability
					boolean variationAvailability = false;
					if (productInfo.has("ShippingAvailability")) {
						variationAvailability = productInfo.getString("ShippingAvailability").equals("1");
					}

					// price					
					Float price = null;
					if (variationAvailability) {
						price = crawlPriceFromApi(internalId, internalPid);
					}

					// prices
					Document docPrices = fetchPrices(internalId, price);
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
					product.setAvailable(variationAvailability);

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

	private String crawlInternalPid(Document document) {
		String internalPid = null;
		Element elementInternalPid = document.select(".main_header").first();
		if (elementInternalPid != null) {
			internalPid = elementInternalPid.attr("id").split("_")[2].trim();
		}

		return internalPid;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("#widget_product_info_viewer h1[id^=product_name_]").first();
		if (nameElement != null) {
			name = nameElement.text().trim();
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
				String text = jsonCatalog.getString("installmentRow3");
				if (!text.isEmpty()) {
					price = MathCommonsMethods.parseFloat(text);
				}
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

	private Prices crawlPrices(Document docPrices) {
		Prices prices = new Prices();

		// bank slip
		Element bankSlipPriceElement = docPrices.select(".boleto #price1x").first();
		if(bankSlipPriceElement != null){
			Float banckTicket = Float.parseFloat(bankSlipPriceElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());
			prices.insertBankTicket(banckTicket);
		}

		Elements installmentsElements = docPrices.select("div[id^=paymentMethod_] tbody");
		
		for (Element table : installmentsElements) {
			Card card = getCardFromTableElement(table.parent().parent());
			if (card != Card.UNKNOWN_CARD) {
				Map<Integer, Float> installments = getInstallmentsFromTableElement(installmentsElements.first());
				prices.insertCardInstallment(card.toString(), installments);
			}
		}			

		return prices;
	}

	private Map<Integer, Float> getInstallmentsFromTableElement(Element tableElement) {
		Map<Integer,Float> installmentPriceMap = new TreeMap<Integer, Float>();

		Elements installmentsElements = tableElement.select("tr");

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
		
		return installmentPriceMap;
	}
	
	private Card getCardFromTableElement(Element tableElement) {
		String idString = tableElement.attr("id").toLowerCase();
		if (idString.contains(Card.VISA.toString())) return Card.VISA;
		if (idString.contains(Card.MASTERCARD.toString()) || idString.contains("master card")) return Card.MASTERCARD;
		if (idString.contains(Card.DINERS.toString())) return Card.DINERS;
		if (idString.contains(Card.AMEX.toString())) return Card.AMEX;
		if (idString.contains(Card.HIPERCARD.toString())) return Card.HIPERCARD;
		if (idString.contains(Card.ELO.toString())) return Card.ELO;
		
		return Card.UNKNOWN_CARD;
	}
}
