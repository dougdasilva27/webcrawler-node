package br.com.lett.crawlernode.crawlers.corecontent.brasil;


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
import br.com.lett.crawlernode.util.MathCommonsMethods;

/**
 * Date: 18/01/2017
 * 
 * 1) Only one sku per page.
 * 
 * Price crawling notes:
 * 	1 - This crawler has two types of product page
 *  2 - These two types has one product per page
 *  3 - In first type has no secondary images and has installments
 *  4 - In second type has secondary images but has no installments informations, only sight price.
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilDellCrawler extends Crawler {

	private static final String HOME_PAGE = "http://www.dell.com/";

	public BrasilDellCrawler(Session session) {
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

		if ( isProductPage(session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			/* ***********************************
			 * crawling data of only one product *
			 *************************************/
	
			String name;
			String internalId;
			Float price;
			Prices prices;
			String primaryImage;
			String secondaryImages;
			String description;
			CategoryCollection categories;
			
			if(!isSpecialProduct(session.getOriginalURL())){
				internalId = crawlInternalId(doc);
				name = crawlName(doc);
				price = crawlMainPagePrice(doc);
				prices = crawlPrices(doc, price);
				primaryImage = crawlPrimaryImage(doc);
				secondaryImages = crawlSecondaryImages();
				description = crawlDescription(doc);
				categories = crawlCategories(doc);
			} else {
				internalId = crawlInternalIdSpecialProduct(doc);
				name = crawlNameSpecialProduct(doc);
				price = crawlMainPagePriceSpecialProduct(doc);
				prices = crawlPricesSpecialProduct(price);
				primaryImage = crawlPrimaryImageSpecialProduct(doc);
				secondaryImages = crawlSecondaryImagesSpecialProduct(doc, primaryImage);
				description = crawlDescriptionSpecialProduct(doc);
				categories = crawlCategoriesSpecialProduct(doc);
			}
			
			String internalPid = crawlInternalPid();
			boolean available = crawlAvailability(price);
			Integer stock = null;
			JSONArray marketplace = assembleMarketplaceFromMap();

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


	/*******************************
	 * Product page identification *
	 *******************************/

	boolean isProductPage(String url) {
		return url.contains("/p/") || url.contains("productdetail");
	}


	/*******************
	 * General methods *
	 *******************/

	boolean isSpecialProduct(String url) {
		return url.contains("productdetail.aspx");
	}
	
	private String crawlInternalId(Document document) {
		String internalId = null;
		Element internalIdElements = document.select("meta[name=currentOcId]").first();

		if(internalIdElements != null) {
			internalId = internalIdElements.attr("content");
		}

		return internalId;
	}
	
	private String crawlInternalIdSpecialProduct(Document document) {
		String internalId = null;
		Element internalIdElement = document.select("td.para_small").first();
		
		if(internalIdElement !=  null) {
			String text = internalIdElement.ownText().trim();
			int x = text.indexOf('|')+1;
			
			String code = text.substring(x);
			int y = code.indexOf(':')+1;
			
			internalId = code.substring(y).trim();
		}
		
		return internalId;
	}

	private String crawlInternalPid() {
		return null;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("#mastheadPageTitle").first();

		if (nameElement != null) {
			name = nameElement.text().toString().trim();
		}

		return name;
	}
	
	private String crawlNameSpecialProduct(Document document) {
		String name = null;
		Element nameElement = document.select("span[itemprop=name]:not([class])").first();

		if (nameElement != null) {
			name = nameElement.text().toString().trim();
		}

		return name;
	}

	private Float crawlMainPagePrice(Document document) {
		Float price = null;
		Element specialPrice = document.select(".prcFlBar .switchCTA div > a").first();		

		if (specialPrice != null) {
			String text = specialPrice.ownText();
			
			if(!text.isEmpty()){
				int x = text.indexOf('.')+1;
				
				price = MathCommonsMethods.parseFloat(text.substring(x));
			}
		}

		return price;
	}
	
	private Float crawlMainPagePriceSpecialProduct(Document document) {
		Float price = null;
		Element specialPrice = document.select(".pricing_sale_price").first();		

		if (specialPrice != null) {
			price = MathCommonsMethods.parseFloat(specialPrice.ownText());
		} else {
			specialPrice = document.select(".pricing_retail_nodiscount_price").first();
			
			if (specialPrice != null) {
				price = MathCommonsMethods.parseFloat(specialPrice.ownText());
			}
		}

		return price;
	}

	private boolean crawlAvailability(Float price) {
		return price != null;
	}

	private JSONArray assembleMarketplaceFromMap() {
		return new JSONArray();
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select(".bigHeroImg").first();

		if (primaryImageElement != null) {			
			primaryImage = primaryImageElement.attr("arc").trim();
		}

		return primaryImage;
	}
	
	/**
	 * In that case has no secondaryImages
	 * @return
	 */
	private String crawlSecondaryImages() {
		return null;
	}
	
	private String crawlPrimaryImageSpecialProduct(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select("#product_main_image a").first();

		if (primaryImageElement != null) {				
			primaryImage = getImage(primaryImageElement);
		}

		return primaryImage;
	}

	
	private String crawlSecondaryImagesSpecialProduct(Document doc, String primaryImage) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = doc.select(".thumbnailRow a");
		for (int i = 0; i < imagesElement.size(); i++) { 
			String image = getImage(imagesElement.get(i));

			if(!image.equals(primaryImage)) {
				secondaryImagesArray.put( image );	
			}
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	/**
	 * Ex: onclick="expandImageRotate ( 'xImage_33478620','http://snpi.dell.com/snp/images/products/large/pt-br~460-BBQC/460-BBQC.jpg',event,this,'' );"
	 * 
	 * if has no image url, we get a ThumbImage in src of image.
	 * @param e
	 * @return
	 */
	private String getImage(Element e) {
		String image = null;
		
		String text = e.attr("onclick");
		
		if(text.contains("snpi.dell")){
			int x = text.indexOf('(');
			
			String expandImage = text.substring(x);
			
			String[] tokens = expandImage.split(",");
			
			image = tokens[1].replaceAll("'", "").trim();
		} else {
			Element thumbImage = e.select("img").first();
			
			if(thumbImage != null) {
				image = thumbImage.attr("src");
			}
		}
		
		return image;
	}
	
	private CategoryCollection crawlCategories(Document doc) {
		CategoryCollection categories = new CategoryCollection();
		Elements categoriesElements = doc.select("ul.bCrumb > li a");
		
		for(int i = 1; i < categoriesElements.size(); i++){ // start with index 1 because the fisrt item is the home páge
			Element e = categoriesElements.get(i);
			
			categories.add(e.text().trim());
		}
		
		return categories;
	}
	
	private CategoryCollection crawlCategoriesSpecialProduct(Document doc) {
		CategoryCollection categories = new CategoryCollection();
		Elements categoriesElements = doc.select("div[class^=para_crumb_] a");
		
		for(int i = 0; i < categoriesElements.size(); i++){
			Element e = categoriesElements.get(i);
			
			categories.add(e.text().trim());
		}
		
		return categories;
	}


	private String crawlDescription(Document document) {
		String description = "";
		Element descriptionElement = document.select("#AnchorZone1").first();
		Element descriptionElement2 = document.select("AnchorZone2").first();
		Element descriptionElement3 = document.select("AnchorZone3").first();

		if (descriptionElement != null){
			description = description + descriptionElement.html();
		}
		
		if (descriptionElement2 != null){
			description = description + descriptionElement2.html();
		}

		if (descriptionElement3 != null){
			description = description + descriptionElement3.html();
		}
		return description;
	}

	private String crawlDescriptionSpecialProduct(Document document) {
		String description = "";
		Element descriptionElement = document.select("#cntTabsCnt").first();
		
		if (descriptionElement != null){
			description = description + descriptionElement.html();
		}
		
		return description;
	}
	
	private Prices crawlPrices(Document doc, Float price){
		Prices prices = new Prices();

		if(price != null){
			Map<Integer,Float> installmentPriceMap = new TreeMap<>();

			Element sightPriceElement = doc.select(".retailSmallPrice .price").first();
			
			if(sightPriceElement != null) {
				Float sightPrice = MathCommonsMethods.parseFloat(sightPriceElement.ownText());
				
				// bank ticket
				prices.insertBankTicket(sightPrice);
				
				// 1x card
				installmentPriceMap.put(1, sightPrice);
			}
			
			// card payment conditions
			Element installmentElement = doc.select(".prcFlBar .switchCTA div > a").first();
			
			if(installmentElement != null) {
				String text = installmentElement.ownText().toLowerCase();
				int x = text.indexOf('x');
				int y = text.indexOf('.', x);
				
				Integer installment = Integer.parseInt(text.substring(0, x).replaceAll("[^0-9]", "").trim());
				Float value = MathCommonsMethods.parseFloat(text.substring(x, y));
				
				installmentPriceMap.put(installment, value);
			}

			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);

		}

		return prices;
	}

	/**
	 * In that case, only exists sight price
	 * @param price
	 * @return
	 */
	private Prices crawlPricesSpecialProduct(Float price){
		Prices prices = new Prices();
		Map<Integer,Float> installmentPriceMap = new TreeMap<>();
		
		if(price != null){
			installmentPriceMap.put(1, price);
			prices.insertBankTicket(price);
		}
		
		prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
		prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
		prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
		prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
		
		return prices;
	
	}
}