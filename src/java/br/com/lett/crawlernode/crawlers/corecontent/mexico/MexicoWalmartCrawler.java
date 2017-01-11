package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.jsoup.nodes.Document;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;

/**
 * Date: 25/11/2016
 * 1) Only one sku per page.
 * 
 * Price crawling notes:
 * 1) For this market was not found product unnavailable
 * 2) Page of product disappear when javascripit is off, so is accessed this api: "https://www.walmart.com.mx/WebControls/hlGetProductDetail.ashx?upc="+id
 * 3) InternalId of product is in url and a json, but to fetch api is required internalId, so it is crawl in url
 * 4) Has no bank ticket in this market
 * 5) Has no internalPid in this market
 * 6) IN api when have a json, sometimes has duplicates keys, so is used GSON from google.
 * 
 * @author Gabriel Dornelas
 *
 */
public class MexicoWalmartCrawler extends Crawler {

	private final String HOME_PAGE = "https://www.walmart.com.mx/";

	public MexicoWalmartCrawler(Session session) {
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
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
			
			// InternalId
			String internalId = crawlInternalId(session.getOriginalURL());
			
			// Api de detalhes do produto
			JsonObject jsonProduct = crawlJsonOfProduct(internalId);

			// Json of product only important informations
			JsonObject productInformations = assembleJsonOfProduc(jsonProduct, internalId);
			
			// InternalPid
			String internalPid = crawlInternalPid(doc);
			
			// Name
			String name = crawlName(productInformations);
			
			// Price
			Float price = crawlPrice(productInformations);
			
			// Prices
			Prices prices = crawlPrices(productInformations, price);
			
			// Avaiability
			boolean available = crawlAvailability(productInformations);
			
			// Categories
			CategoryCollection categories = crawlCategories();
			
			// Primary Image
			String primaryImage = crawlPrimaryImage(internalId);
			
			// SecondaryImages
			String secondaryImages = crawlSecondaryImages(productInformations, internalId);
			
			// Description
			String description = crawlDescription(productInformations);
			
			// Stock
			Integer stock = null;
			
			// Marketplace
			JSONArray marketplace = crawlMarketplace(doc);

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

			System.out.println("secondary images: " + secondaryImages);
			
			products.add(product);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return products;

	}

	private boolean isProductPage(String url) {
		if(url.contains("_")){
			if(url.contains("?")){
				url = url.split("\\?")[0];
			}
			
			String[] tokens = url.split("_");
			String id = tokens[tokens.length-1].replaceAll("[^0-9]", "").trim();
			
			if(!id.isEmpty()){
				return true;
			}
		}
		
		return false;
	}

	private String crawlInternalId(String url) {
		String internalId = null;

		if(url.contains("_")){
			if(url.contains("?")){
				url = url.split("\\?")[0];
			}
			
			String[] tokens = url.split("_");
			internalId = tokens[tokens.length-1].replaceAll("[^0-9]", "").trim();
		}

		return internalId;
	}

	/**
	 * There is no internalPid.
	 * 
	 * @param document
	 * @return
	 */
	private String crawlInternalPid(Document document) {
		String internalPid = null;

		return internalPid;
	}

	private String crawlName(JsonObject product) {
		String name = null;
		
		if(product.has("name")){
			name = product.get("name").getAsString();
		}
	
		return name;
	}

	private Float crawlPrice(JsonObject product) {
		Float price = null;

		if(product.has("price")){
			price = Float.parseFloat(product.get("price").getAsString());
		}
		
		return price;
	}

	private boolean crawlAvailability(JsonObject product) {
		boolean available = false;
		
		if(product.has("avaiability")){
			available = product.get("avaiability").getAsBoolean();
		}

		return available;
	}

	private JSONArray crawlMarketplace(Document document) {
		return new JSONArray();
	}

	/**
	 * In time was made, all products had images
	 * 
	 * Its make like this:
	 * https://www.walmart.com.mx/images/products/img_large/"+ internalId +"l.jpg
	 * 
	 * @param internalId
	 * @return
	 */
	private String crawlPrimaryImage(String internalId) {
		String primaryImage = "https://www.walmart.com.mx/images/products/img_large/"+ internalId +"l.jpg";

		return primaryImage;
	}

	/**
	 * In json has key "ni", number of images
	 * if ni was > 1 has secondary Images
	 * 
	 * Is make like this
	 * 
	 * 2 image - https://www.walmart.com.mx/images/products/img_large/"+ internalId +"-1l.jpg
	 * 3 image - https://www.walmart.com.mx/images/products/img_large/"+ internalId +"-2l.jpg
	 * ...
	 * 
	 * @param product
	 * @param internalId
	 * @return
	 */
	private String crawlSecondaryImages(JsonObject product, String internalId) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		if(product.has("numberOfImages")){
			int ni = product.get("numberOfImages").getAsInt();
			
			if(ni > 1){
				for(int i = 2; i <= ni; i++){
					secondaryImagesArray.put("https://www.walmart.com.mx/images/products/img_large/"+ internalId +"-"+ (i-1) +"l.jpg");
				}
			}
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private CategoryCollection crawlCategories() {
		CategoryCollection categories = new CategoryCollection();
		String urlForCategories = session.getOriginalURL().replace(HOME_PAGE, "");
		String[] tokens = urlForCategories.split("/");
		
		for(int i = 0; i < tokens.length-1; i++){ // tokens -1 because the last item is Name
			String category = tokens[i].replaceAll("-", " ");
			
			categories.add(category);
		}

		return categories;
	}

	private String crawlDescription(JsonObject product) {
		StringBuilder description = new StringBuilder();

		if(product.has("description")){
			String descriptionHtml = "<p class=\"description\" itemprop=\"description\" data-reactid=\".8.5.$descriptionParagraph0\">" +
					product.get("description").getAsString() + "</p>";
			
			description.append(descriptionHtml);
		}		

		return description.toString();
	}

	/**
	 * There is no bankSlip price.
	 * 
	 * There is no card payment options, other than cash price.
	 * So for installments, we will have only one installment for each
	 * card brand, and it will be equals to the price crawled on the sku
	 * main page.
	 * 
	 * @param doc
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(JsonObject product, Float price) {
		Prices prices = new Prices();

		if(price != null){
			Map<Integer,Float> installmentPriceMap = new TreeMap<Integer, Float>();

			installmentPriceMap.put(1, price);

			if(product.has("prices")){
				JsonObject jsonPrices = product.get("prices").getAsJsonObject();
				
				for(int i = 1; i < 13; i++){
					if(jsonPrices.has(i+"")){
						installmentPriceMap.put(i, Float.parseFloat(jsonPrices.get(i+"").getAsString().replaceAll("\\$", "").replaceAll(",", "")));
					}
				}
				
			}
			
			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
		}

		

		return prices;
	}
	
	/**
	 * In this function is crawl only important information of json of api
	 * Example:
	 * {
		"avaiability":true,
		"numberOfImages":3,
		"price":"9990.00",
		"name":"Minisplit LG Art Cool 17,000 BTU's",
		"description":"blablabla",
		"prices":{
			"12":"$832.50",
			"3":"$3,330.00",
			"15":"$666.00",
			"6":"$1,665.00",
			"18":"$555.00",
			"9":"$1,110.00"
		 }
	  }
	 * @param jsonProduct
	 * @param internalId
	 * @return
	 */
	private JsonObject assembleJsonOfProduc(JsonObject jsonProduct, String internalId){
		JsonObject product = new JsonObject();
		
		
		if(jsonProduct.has("c")){
			JsonObject jsonC = jsonProduct.get("c").getAsJsonObject();
			
			if(jsonC.has("facets")){
				JsonObject jsonFacets = jsonC.get("facets").getAsJsonObject();
				
				if(jsonFacets.has("_" + internalId)){
					JsonObject jsonOfProduct = jsonFacets.get("_" + internalId).getAsJsonObject();
					
					// Name
					if(jsonOfProduct.has("n")){
						product.addProperty("name", jsonOfProduct.get("n").getAsString());
					}
					
					// Number of Images
					if(jsonOfProduct.has("ni")){
						product.addProperty("numberOfImages", jsonOfProduct.get("ni").getAsInt());
					}
					
					// Description
					if(jsonOfProduct.has("d")){
						product.addProperty("description", jsonOfProduct.get("d").getAsString());
					}
					
					// Price
					if(jsonOfProduct.has("p")){
						product.addProperty("price", jsonOfProduct.get("p").getAsString());
					}
					
					// Avaiability
					if(jsonOfProduct.has("av")){
						String av = jsonOfProduct.get("av").getAsString();
						
						if(av.equals("1")){
							product.addProperty("avaiability", true);
						} else {
							product.addProperty("avaiability", false);
						}
					}
					
					// Prices
					if(jsonOfProduct.has("pro")){
						JsonObject jsonPro = jsonOfProduct.get("pro").getAsJsonObject();
						
						if(jsonPro.has("msi")){
							product.add("prices",jsonPro.get("msi").getAsJsonObject());
						}
					}
					
					
				}
			}
		}
		
		return product;
	}
	
	/**
	 * This json had informations of Product
	 * Requisição GET
	 * 
	 * Example: https://www.walmart.com.mx/WebControls/hlGetProductDetail.ashx?upc=00880608419985
	 * Parser: http://json.parser.online.fr/
	 * 
	 * @param internalId
	 * @return
	 */
	/**
	 * This json had informations of Product
	 * Requisição GET
	 * 
	 * Example: https://www.walmart.com.mx/WebControls/hlGetProductDetail.ashx?upc=00880608419985
	 * Parser: http://json.parser.online.fr/
	 * 
	 * @param internalId
	 * @return
	 */
	private JsonObject crawlJsonOfProduct(String internalId){
		JsonObject product = new JsonObject();
		String url = "https://www.walmart.com.mx/WebControls/hlGetProductDetail.ashx?upc=" + internalId;
		
		String detail = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, cookies);
		
		if(detail.contains("Info =")){
			int x = detail.indexOf("Info =")+6;
			int y = detail.indexOf("};", x)+1;
			
			product = new Gson().fromJson(detail.substring(x, y), JsonObject.class);
		}
		
		return product;
	}

}
