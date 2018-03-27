package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;



/************************************************************************************************************************************************************************************
 * Crawling notes (10/08/2016):
 * 
 * 1) For this crawler, we have URLs with multiple variations of a sku.
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking the URL format.
 * 
 * 5) Even if a product is unavailable, its price is displayed.
 * 
 * 6) There is no internalPid for skus in this ecommerce. The internalPid must be a number that is the same for all
 * the variations of a given sku.
 * 
 * 7) To get variations, that is an url for them on the page of the first product.
 * 
 * Examples:
 * ex1 (available): http://www.cec.com.br/piso-e-azulejo/piso/ate-45x45/piso-albania-45x45-cm-caixa-2-00-m?produto=1171701
 * ex2 (unavailable): http://www.cec.com.br/jardinagem/acessorios-de-jardinagem/vasos-e-pratos/vaso-caneca-coracoes-pequena?produto=1183476
 * ex3 (variations): http://www.cec.com.br/ferramentas/eletrica/furadeira/furadeira-de-impacto-600w-3/8-127v?produto=1223208
 * 
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilCasaeconstrucaoCrawler extends Crawler {

	public BrasilCasaeconstrucaoCrawler(Session session) {
		super(session);
	}

	private final String HOME_PAGE = "http://www.cec.com.br/";

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<>();

		if ( isProductPage(doc, this.session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// InternalId
			String internalId = crawlInternalId(doc);

			// Pid
			String internalPid = crawlInternalPid(doc);

			// Stock
			Integer stock = null;

			// Marketplace map
			Map<String, Float> marketplaceMap = crawlMarketplace(doc);

			// Marketplace
			Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap);

			// Name
			String name = crawlName(doc);

			// Availability
			boolean available = crawlAvailability(doc);
			
			// Price
			Float price = crawlMainPagePrice(doc, available);

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

			// Prices
			Prices prices = crawlPrices(price, doc);
			
			// Creating the product
			Product product = new Product();
			product.setUrl(session.getOriginalURL());
			product.setInternalId(internalId);
			product.setInternalPid(internalPid);
			product.setName(name);
			product.setPrice(price);
			product.setPrices(prices);
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
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}
		
		return products;

	}



	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(Document doc, String url) {
		if ( !url.contains("nm_origem") && doc.select(".product-detail-header").first() != null) return true;
		return false;
	}	
	
	/*******************
	 * General methods *
	 *******************/

	private String crawlInternalId(Document document) {
		String internalId = null;

		Elements scriptTags = document.getElementsByTag("script");
		JSONObject skuJson = new JSONObject();

		for (Element tag : scriptTags){    
			String scrpit = tag.html().trim();
			if(scrpit.startsWith("var google_tag_params = ")) {
				String finalJson = scrpit.replaceAll("var google_tag_params = ", "").replace(";", "").trim();
				
				skuJson = new JSONObject(finalJson);

			}

		}
		
		if(skuJson.has("ecomm_prodid")){
			internalId = skuJson.getString("ecomm_prodid");
		}

		return internalId;
	}

	private String crawlInternalPid(Document document) {
		String internalPid = null;

		return internalPid;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("span[itemprop=name]").first();

		if (nameElement != null) {
			name = nameElement.ownText().toString().trim();
		}

		return name;
	}

	private Float crawlMainPagePrice(Document document, boolean available) {
		Float price = null;
		
		if(available){
			Element mainPagePriceElement = document.select("span.price").first();
	
			if (mainPagePriceElement != null) {
				price = Float.parseFloat( mainPagePriceElement.ownText().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
			}
		}

		return price;
	}

	private boolean crawlAvailability(Document document) {
		Element notifyMeElement = document.select("#Body_Body_divUnavailable").first();

		if (notifyMeElement != null) {
			return false;
		}

		return true;
	}

	private Map<String, Float> crawlMarketplace(Document document) {
		return new HashMap<String, Float>();
	}

	private Marketplace assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		return new Marketplace();
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select("#Body_Body_divImage .product-img-zoom #Body_Body_divProductZoom").first();

		if (primaryImageElement != null) {
			String attr = primaryImageElement.attr("style");
			int x = attr.indexOf("url('");
			int y = attr.indexOf("')", x+5);
			
			primaryImage = attr.substring(x+5, y);
			
		}
		
		if(primaryImage == null || !primaryImage.contains("http:")){
			primaryImageElement = document.select("#Body_Body_divImage .product-img-zoom #Body_Body_imgStandard").first();
			
			if(primaryImageElement != null){
				primaryImage = primaryImageElement.attr("src");
			}
		}

		return primaryImage.toLowerCase();
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select(".clearfix li img");

		for (int i = 1; i < imagesElement.size(); i++) { // starting from index 1, because the first is the primary image
			secondaryImagesArray.put( imagesElement.get(i).attr("src").trim().toLowerCase().replaceAll("thumb", "large").replaceAll("standart", "latge") ); // image large
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select("#breadcrumb li a");

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
		Element descriptionElement = document.select(".product-detail-body").first();

		if (descriptionElement != null) description = description + descriptionElement.html();

		return description;
	}

	/**
	 * Nesse market os produtos que verifiquei não tinham desconto ou juros 
	 * nos preços na página principal
	 * @param price
	 * @param doc
	 * @return
	 */
	private Prices crawlPrices(Float price, Document doc){
		Prices prices = new Prices();
		
		if(price != null){
			// Preço principal é o avista (boleto e cartão)
			Map<Integer, Float> installmentPriceMap = new HashMap<>();
			installmentPriceMap.put(1, price);
			prices.setBankTicketPrice(price);
			
			Element installmentSpecial = doc.select(".monthly-payment span").first();
			
			if(installmentSpecial != null){
				String text = installmentSpecial.text().toLowerCase();
				
				if(text.contains("x")){
					Integer installment = Integer.parseInt(text.split("x")[0].trim());
					Float value = MathUtils.parseFloat(text.split("x")[1].trim());
					
					installmentPriceMap.put(installment, value);
				}
			}
			
			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			
		}
		
		return prices;
	}
}
