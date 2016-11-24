package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;

public class BrasilSaraivaCrawler extends Crawler {

	private final String HOME_PAGE_HTTP = "http://www.saraiva.com.br";
	private final String HOME_PAGE_HTTPS = "https://www.saraiva.com.br";

	private final int LARGER_IMAGE_DIMENSION = 550;


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
			
			JSONObject productJSON = crawlChaordicMeta(doc);
			
			String internalId = crawlInternalId(doc);
			String internalPid = crawlInternalPid(productJSON);
			String name = crawlName(doc);
			boolean available = crawlAvailability(productJSON);
			String primaryImage = crawlPrimaryImage(doc);
			String secondaryImages = crawlSecondaryImages(doc);
			Integer stock = null;
			JSONArray marketplace = null;
			String description = crawlDescription(doc);
			
			// price is not displayed when sku is unavailable
			Float price = null;
			Prices prices = new Prices();
			if (available) {
				price = crawlPrice(doc);
				prices = crawlPrices(doc);
			}
			
			ArrayList<String> categories = crawlCategories(doc);
			String category1 = getCategory(categories, 0);
			String category2 = getCategory(categories, 1);
			String category3 = getCategory(categories, 2);

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

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return products;
	}

	private boolean isProductPage(Document document) {
		Element elementProduct = document.select("section.product-allinfo").first();
		return elementProduct != null;
	}

	/**
	 * Crawl the code from the displayed number on the main page.
	 * 
	 * e.g:
	 * Cafeteira Espresso Electrolux Chef Crema Silver Em400 - 220 Volts (Cód: 4054233)
	 * 
	 * internalId = 4054233
	 * 
	 * @param document
	 * @return
	 */
	private String crawlInternalId(Document document) {
		String internalId = null;

		Element elementSpan = document.select("section.product-info h1 span").first();
		if (elementSpan != null) {
			String spanText = elementSpan.text();
			List<String> parsedNumbers = MathCommonsMethods.parseNumbers(spanText);
			if (parsedNumbers.size() > 0) {
				internalId = parsedNumbers.get(0);
			}
		}

		return internalId;
	}
	
	/**
	 * InternalPid is the id field inside the chaordicMetadataJSON
	 * 
	 * @param document
	 * @return
	 */
	private String crawlInternalPid(JSONObject productJSON) {
		String internalPid = null;
		
		if (productJSON.has("id")) {
			internalPid = String.valueOf(productJSON.getInt("id"));
		}
		
		return internalPid;
	}

	private String crawlName(Document document) {
		String name = null;

		Element elementName = document.select("section.product-info h1").first();
		if(elementName != null) {
			name = elementName.ownText().trim();
		}

		return name;
	}

	private Float crawlPrice(Document document) {
		Float price = null;

		Element elementPrice = document.select("div.product-price-block div.simple-price span.final-price ").first();
		if (elementPrice != null) {
			price = MathCommonsMethods.parseFloat(elementPrice.ownText());
		}

		return price;
	}
	
	private Prices crawlPrices(Document document) {
		Prices prices = new Prices();
		
		Float bankSlipPrice = crawlPrice(document);
		prices.insertBankTicket(bankSlipPrice);
		
		Map<Integer, Float> installments = new HashMap<Integer, Float>();
		
		// max installments
		Element maxInstallmentsElement = document.select("div.product-price-block div.simple-price span.installments").first();
		if (maxInstallmentsElement != null) {
			String installmentText = maxInstallmentsElement.text().trim();
			
			int endIndexInstallmentNumber = installmentText.indexOf("x");
			
			String installmentNumberText = installmentText.substring(0, endIndexInstallmentNumber);
			String installmentPriceText = installmentText.substring(endIndexInstallmentNumber + 1, installmentText.length());
			
			List<String> parsedNumbers = MathCommonsMethods.parseNumbers(installmentNumberText);
			if (parsedNumbers.size() > 0) {
				installments.put(Integer.parseInt(parsedNumbers.get(0)), MathCommonsMethods.parseFloat(installmentPriceText));
			}
		}
		
		// 1x
		Element cashPriceOnCardElement = document.select("div.extra-discount.price-block span.special-price strong").first();
		if (cashPriceOnCardElement != null) {
			String cashPriceText = cashPriceOnCardElement.text();
			if (!cashPriceText.isEmpty()) {
				installments.put(1, MathCommonsMethods.parseFloat(cashPriceText));
			}
		}
		
		if (installments.size() > 0) {
			prices.insertCardInstallment(Card.VISA.toString(), installments);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installments);
			prices.insertCardInstallment(Card.DINERS.toString(), installments);
			prices.insertCardInstallment(Card.AURA.toString(), installments);
			prices.insertCardInstallment(Card.ELO.toString(), installments);
			prices.insertCardInstallment(Card.HIPERCARD.toString(), installments);
			prices.insertCardInstallment(Card.AMEX.toString(), installments);
		}
		
		return prices;
	}

	/**
	 * 
	 * @param productJSON
	 * @return
	 */
	private boolean crawlAvailability(JSONObject productJSON) {
		boolean available = true;
		
		if (productJSON.has("status")) {
			String status = productJSON.getString("status");
			if (status.equals("unavailable")) {
				available = false;
			}
		}
		
		return available;
	}

	/**
	 * Crawl an image with a default dimension of 430.
	 * There is a larger image with dimension of 550, but with javascript off
	 * this link disappear. So we modify the image URL and set the dimension parameter
	 * to the desired larger size.
	 * 
	 * Parameter to mody: &l
	 * 
	 * e.g:
	 * original: http://images.livrariasaraiva.com.br/imagemnet/imagem.aspx/?pro_id=9220079&qld=90&l=430&a=-1
	 * larger: http://images.livrariasaraiva.com.br/imagemnet/imagem.aspx/?pro_id=9220079&qld=90&l=550&a=-1
	 * 
	 * @param document
	 * @return
	 */
	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;

		// get original image URL
		Element elementPrimaryImage = document.select("div.product-image-center a img").first();
		if(elementPrimaryImage != null) {
			primaryImage = elementPrimaryImage.attr("src");
		}

		// modify the dimension parameter
		String biggerPrimaryImage = CommonMethods.modifyParameter(primaryImage, "l", String.valueOf(LARGER_IMAGE_DIMENSION));

		return biggerPrimaryImage;
	}

	/**
	 * Get all the secondary images URL from thumbs container.
	 * Analogous treatment to that performed on primary image URL must be applied,
	 * so we can get the largest images URL.
	 *  
	 * @param document
	 * @return
	 */
	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;

		Elements elementImages = document.select("section.product-image #thumbs-images a img");
		JSONArray secondaryImagesArray = new JSONArray();

		for(int i = 1; i < elementImages.size(); i++) { // skip the first because it's the same as the primary image
			String imageURL = elementImages.get(i).attr("src").trim();
			String biggerImageURL = CommonMethods.modifyParameter(imageURL, "l", String.valueOf(LARGER_IMAGE_DIMENSION));

			secondaryImagesArray.put(biggerImageURL);
		}			
		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}
	
	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(".breadcrumbs ol li");

		for (int i = 2; i < elementCategories.size(); i++) { // start with index 1 because the first item is the home page
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
		StringBuilder description = new StringBuilder();
		
		Element skuInformation = document.select("#product-information").first();
		if (skuInformation != null) {
			description.append(skuInformation.html());
		}
		
		Element skuAdditionalInformation = document.select("#product-additional").first();
		if (skuAdditionalInformation != null) {
			description.append(skuAdditionalInformation.html());
		}
		
		return description.toString();
	}
	
	/**
	 * {
	 * "page":
	 * 	{
	 * 		"name":"product",
	 * 		"timestamp":new Date(),
	 * 		"tags":[{"name":"Eletroport\u00e1teis"},{"name":"Cafeteiras"},{"name":"Cafeteiras - Expresso"}]
	 * 	},
	 * 	"product":
	 * 	{	
	 * 		"id":2496308,
	 * 		"skus":[{"sku":"4054233"}],
	 * 		"name":"Cafeteira Espresso Electrolux Chef Crema Silver Em400 - 220 Volts",
	 * 		"url":"www.saraiva.com.br\/cafeteira-espresso-electrolux-chef-crema-silver-em400-220-volts-4054233.html",
	 * 		"images":{"default":"images.livrariasaraiva.com.br\/imagemnet\/imagem.aspx\/?pro_id=4054233"},
	 * 		"status":"unavailable",
	 * 		"price":299,
	 * 		"description":"sku_description",
	 * 		"ean_code":"7896347127608",
	 * 		"isbn":null,
	 * 		"tags":[{"name":"Eletroport\u00e1teis"},{"name":"Cafeteiras"},{"name":"Cafeteiras - Expresso"}],
	 * 		"brand":"ELECTROLUX - Eletroport\u00e1teis",
	 * 		"details":{"product_free":"no","type":"simple"}
	 * 	}
	 * }
	 * @param document
	 * @return
	 */
	private JSONObject crawlChaordicMeta(Document document) {
		Elements scriptTags = document.getElementsByTag("script");
		JSONObject chaordicMeta = null;
		JSONObject skuJson = null;

		for (Element tag : scriptTags) {                
			for (DataNode node : tag.dataNodes()) {
				if (tag.html().trim().startsWith("window.chaordic_meta = ")) {
					chaordicMeta = new JSONObject
							(node.getWholeData().split(Pattern.quote("window.chaordic_meta = "))[1] +
							 node.getWholeData().split(Pattern.quote("window.chaordic_meta = "))[1].split(Pattern.quote("}}}"))[0]
							);
				}
			}
		}

		if (chaordicMeta != null && chaordicMeta.has("product")) {
			skuJson = chaordicMeta.getJSONObject("product");
		} else {
			skuJson = new JSONObject();
		}

		return skuJson;
	}
}
