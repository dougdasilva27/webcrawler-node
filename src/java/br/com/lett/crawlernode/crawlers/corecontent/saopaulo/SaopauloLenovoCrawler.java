package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.fetcher.Fetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;

/**
 * Date: 15/12/16
 * 
 * 1 - This market has mote then one sku per page
 * 2 - Has no installments information
 * 3 - The principal name is  equal to the variations
 * 4 - The same happen to the images, categories and availability
 * 
 * @author gabriel
 *
 */
public class SaopauloLenovoCrawler extends Crawler {

	private static final String HOME_PAGE = "http://shop.lenovo.com";

	public SaopauloLenovoCrawler(Session session) {
		super(session);
		this.config.setFetcher(Fetcher.WEBDRIVER);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<>();

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
			
			boolean available = crawlAvailability(doc);
			String name = crawlName(doc);
			String internalPid = crawlInternalPid(doc);
			String primaryImage = crawlPrimaryImage(doc);
			String secondaryImages = crawlSecondaryImages(doc);
			CategoryCollection categories = crawlCategories(doc);
			Integer stock = null;
			JSONArray marketplace = null;

			Elements productsElements = doc.select(".tabbedBrowse-productListings li:not([id])");

			// Multiple product page 
			if(productsElements.size() > 0){

				for(Element e : productsElements) {
					String internalId = crawlInternalIdVariation(e);
					Float price = crawlPrice(e, available);
					Prices prices = crawlPrices(price);
					String description = crawlDescriptionVariation(doc);
					
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
				}

			}
			// Only one Sku in this page	
			else {
				String internalId = crawlInternalId(doc);
				Float price = crawlPrice(doc, available);
				Prices prices = crawlPrices(price);
				String description = crawlDescription(doc);

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
			}


		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return products;
	}

	private boolean isProductPage(Document doc) {
		Element elementProduct = doc.select("meta[name=PartNumber]").first();
		return elementProduct != null;
	}


	/**
	 * In cases when page of sku has no one sku, internalId = internaPid
	 * This happen because the first product in models has internalId equal a internalPid
	 * @param doc
	 * @return
	 */
	private String crawlInternalId(Document doc) {
		String internalId;

		Element skuId = doc.select(".partNumber").first();
		if (skuId != null) {
			internalId = skuId.text();
		} else {
			internalId = crawlInternalPid(doc);
		}

		return internalId;
	}

	private String crawlInternalIdVariation(Element e) {
		String internalId = null;

		Element skuId = e.select(".tabbedBrowse-productListing-body .partnumber span").first();
		if (skuId != null) {
			internalId = skuId.text().trim();
		}

		return internalId;
	}

	private String crawlInternalPid(Document doc) {
		String internalPid = null;
		Element pid = doc.select("meta[name=PartNumber]").first();

		if(pid != null) {
			internalPid = pid.attr("content");
		}

		return internalPid;
	}

	private String crawlName(Document document) {
		String name = null;

		Element elementName = document.select("meta[name=Title][content]").first();
		if(elementName != null) {
			name = elementName.attr("content").trim();
		}

		return name;
	}

	private Float crawlPrice(Element document, boolean available) {
		Float price = null;

		if(available) {
			Element elementPrice = document.select(".pricingSummary .lengthy-final-priceText").first();
			if (elementPrice != null) {
				price = MathCommonsMethods.parseFloat(elementPrice.ownText());
			}
		}

		return price;
	}

	private boolean crawlAvailability(Document doc) {
		return doc.select(".button-called-out-alt#find-dealer").first() == null;
	}


	/**
	 * Has no installments information, only one price
	 * 
	 * @param doc
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(Float price) {
		Prices prices = new Prices();

		if(price != null) {
			Float bankSlipPrice = price;
			prices.insertBankTicket(bankSlipPrice);

			Map<Integer, Float> installmentsMap = new HashMap<>();
			installmentsMap.put(1, price);

			prices.insertCardInstallment(Card.VISA.toString(), installmentsMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentsMap);
		}

		return prices;
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;

		Element elementPrimaryImage = document.select(".productImg > img").first();
		if(elementPrimaryImage != null) {
			primaryImage = elementPrimaryImage.attr("src");
		}

		return primaryImage;
	}


	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;

		JSONArray secondaryImagesArray = new JSONArray();
		JSONArray secondaryImagesArrayApi = crawlJsonSecondaryImages(document);

		for(int i = 0; i < secondaryImagesArrayApi.length(); i++) {
			JSONObject image = secondaryImagesArrayApi.getJSONObject(i);

			if(image.has("big")) {
				secondaryImagesArray.put(image.get("big"));
			} else if(image.has("thumb")) {
				secondaryImagesArray.put(image.get("thumb"));
			}
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private JSONArray crawlJsonSecondaryImages(Document doc){
		JSONArray images = new JSONArray();

		Element mediaGallery = doc.select(".mediaGallery > script").first();

		if(mediaGallery != null) {
			String script = mediaGallery.outerHtml();

			String galleryJsonFile = "_GALLERY_JSON_FILE=";

			if(script.contains(galleryJsonFile)) {
				int x = script.indexOf(galleryJsonFile) + galleryJsonFile.length();
				int y = script.indexOf(';', x);

				String url = HOME_PAGE + script.substring(x, y).replaceAll("\"", "");				
				String docJson = this.webdriver.loadUrl(url);

				//JSONObject jsonImages = DataFetcher.fetchJSONObject(DataFetcher.GET_REQUEST, session, url, null, cookies);

				Document docApi = Jsoup.parse(docJson);

				Element jsonElement = docApi.select("pre").first();

				if(jsonElement != null) {
					JSONObject jsonImages = new JSONObject(jsonElement.text());	

					if(jsonImages.has("image")) {
						images = jsonImages.getJSONArray("image");
					}
				}
			}
		}

		return images;
	}

	private CategoryCollection crawlCategories(Document document) {
		CategoryCollection categories = new CategoryCollection();
		Elements elementCategories = document.select(".breadcrumb-wrapper span a:not([href=#]) span");

		for (Element e : elementCategories) {
			categories.add( e.text().trim() );
		}

		return categories;
	}

	private String crawlDescription(Document document) {
		StringBuilder description = new StringBuilder();

		Element caracteristicas = document.select("[id^=tab-caracter]").first();
		if (caracteristicas != null) {
			description.append(caracteristicas.html());
		}

		Element specInformation = document.select("[id^=tab-especifica]").first();
		if (specInformation != null) {
			description.append(specInformation.html());
		}

		Element skuInformation = document.select(".longscroll-singlesku-content").first();
		if (skuInformation != null) {
			description.append(skuInformation.html());
		}

		return description.toString();
	}

	private String crawlDescriptionVariation(Document document) {
		StringBuilder description = new StringBuilder();

		Element caracteristicas = document.select("[id^=tab-caracter]").first();
		if (caracteristicas != null) {
			description.append(caracteristicas.html());
		}

		Element specInformation = document.select("[id^=tab-especifica]").first();
		if (specInformation != null) {
			description.append(specInformation.html());
		}

		Element skuInformation = document.select(".tabbedBrowse-productListing-footer .tabbedBrowse-productListing-expandableContent-features").first();
		if (skuInformation != null) {
			description.append(skuInformation.html());
		}

		return description.toString();
	}
}