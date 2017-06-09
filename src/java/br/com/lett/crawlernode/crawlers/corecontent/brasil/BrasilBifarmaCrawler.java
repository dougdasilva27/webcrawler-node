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
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;
import models.Marketplace;
import models.Prices;

public class BrasilBifarmaCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.bifarma.com.br/";

	public BrasilBifarmaCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<>();

		if (isProductPage(doc)) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
			
			JSONObject productInfo = crawlProductInfo(doc);
			
			String internalId = crawlInternalId(productInfo);
			String internalPid = crawlInternalPid(productInfo);
			String name = crawlName(productInfo);
			Float price = crawlPrice(doc);
			Prices prices = crawlPrices(price, productInfo);
			boolean available = crawlAvailability(productInfo);
			CategoryCollection categories = crawlCategories(productInfo);
			String primaryImage = crawlPrimaryImage(doc);
			String secondaryImages = crawlSecondaryImages(doc);
			String description = crawlDescription(doc);
			Integer stock = null;
			Marketplace marketplace = crawlMarketplace();
			
			// Creating the product
			Product product = ProductBuilder.create()
					.setUrl(session.getOriginalURL())
					.setInternalId(internalId)
					.setInternalPid(internalPid)
					.setName(name).setPrice(price)
					.setPrices(prices)
					.setAvailable(available)
					.setCategory1(categories.getCategory(0))
					.setCategory2(categories.getCategory(1))
					.setCategory3(categories.getCategory(2))
					.setPrimaryImage(primaryImage)
					.setSecondaryImages(secondaryImages)
					.setDescription(description).setStock(stock)
					.setMarketplace(marketplace)
					.build();

			products.add(product);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return products;

	}

	private boolean isProductPage(Document doc) {
		if (doc.select(".product_body").first() != null) {
			return true;
		}
		return false;
	}

	private String crawlInternalId(JSONObject info) {
		String internalId = null;

		if(info.has("skus")) {
			JSONArray skus = info.getJSONArray("skus");
			
			if(skus.length() > 0) {
				JSONObject sku = skus.getJSONObject(0);
				
				if(sku.has("sku")) {
					internalId = sku.getString("sku");
				}
			}
		}

		return internalId;
	}

	private String crawlInternalPid(JSONObject info) {
		String internalPid = null;
		
		if(info.has("id")) {
			internalPid = info.getString("id");
		}
		
		return internalPid;
	}

	private String crawlName(JSONObject info) {
		String name = null;

		if (info.has("name")) {
			name = info.getString("name");
		}

		return name;
	}

	private Float crawlPrice(Document document) {
		Float price = null;
		Element salePriceElement = document.select(".product_current_price strong").first();
		
		if(salePriceElement != null) {
			String priceText = salePriceElement.ownText().trim();
			
			if(!priceText.isEmpty()) {
				price = MathCommonsMethods.parseFloat(priceText);
			}
		}

		return price;
	}

	private boolean crawlAvailability(JSONObject info) {
		boolean available = false;	

		if(info.has("status")) {
			String status = info.getString("status");
			
			if(status.equals("available")) {
				available = true;
			}
		}

		return available;
	}

	private Marketplace crawlMarketplace() {
		return new Marketplace();
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select(".slider-product .slide_image img").first();

		if (primaryImageElement != null) {
			primaryImage = primaryImageElement.attr("src").trim();
			
			if(!primaryImage.contains("bifarma")) {
				primaryImage = HOME_PAGE + primaryImage;
			}
			
			if(primaryImage.contains("SEM_IMAGEM")) {
				primaryImage = null;
			}
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select(".slider-thumbs .thumb > img");

		for (int i = 1; i < imagesElement.size(); i++) { // first index is the primary image
			String image = imagesElement.get(i).attr("src").trim().replace("_mini", "");
			
			if(!image.contains("bifarma")) {
				image = HOME_PAGE + image;
			}
			
			secondaryImagesArray.put(image);
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private CategoryCollection crawlCategories(JSONObject info) {
		CategoryCollection categories = new CategoryCollection();

		if(info.has("categories")) {
			JSONArray categoriesJson = info.getJSONArray("categories");
			
			for (int i = 0; i < categoriesJson.length(); i++) { 
				JSONObject categorieJson = categoriesJson.getJSONObject(i);
				
				if(categorieJson.has("name")) {
					categories.add(categorieJson.getString("name"));
				}
			}
		}
		

		return categories;
	}

	private String crawlDescription(Document document) {
		StringBuilder description = new StringBuilder();
		Element descriptionElement = document.select(".accordion-section").first();

		if (descriptionElement != null) {
			description.append(descriptionElement.html());
		}

		return description.toString();
	}

	private Prices crawlPrices(Float price, JSONObject info) {
		Prices prices = new Prices();

		if (price != null) {

			Map<Integer, Float> installmentPriceMap = new HashMap<>();
			
			prices.setBankTicketPrice(price);
			installmentPriceMap.put(1, price);
			
			if(info.has("installment")) {
				JSONObject installment = info.getJSONObject("installment");
				
				if(installment.has("price") && installment.has("count")) {
					Double priceInstallment = installment.getDouble("price");
					Integer installmentCount = installment.getInt("count");
					
					if(installmentCount > 0) {
						installmentPriceMap.put(installmentCount, priceInstallment.floatValue());
					}
				}
			}

			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
		}

		return prices;
	}
	
	/**
	 * Return a json like this:
	 * "{\"product\": {\n" +
		"\t        \"id\": \"7748\",\n" +
		"\t        \"name\": \"Mucilon arroz/aveia 400gr neste\",\n" +
		"\t        \n" +
		"\t        \"url\": \"/produto/mucilon-arroz-aveia-400gr-neste-7748\",\n" +
		"\t        \"images\": {\n" +
		"\t            \"235x235\": \"/fotos/PRODUTO_SEM_IMAGEM_mini.png\"\n" +
		"\t        },\n" +
		"\t        \"status\": \"available\",\n" +
		"\t\t    \n" +
		"\t        \"price\": 12.50,\n" +
		"\t        \"categories\": [{\"name\":\"Mamãe e Bebê\",\"id\":\"8\"},{\"name\":\"Alimentos\",\"id\":\"89\",\"parents\":[\"8\"]},],\n" +
		"\t        \"installment\": {\n" +
		"\t            \"count\": 0,\n" +
		"\t            \"price\": 0.00\n" +
		"\t        },\n" +
		"\t        \n" +
		"\t        \n" +
		"\t        \n" +
		"\t        \t\"skus\": [ {\n" +
		"\t        \t\t\t\"sku\":  \"280263\"\n" +
		"\t        \t}],\n" +
		"\t        \n" +
		"\t        \"details\": {},\n" +
		"\t \t\t\"published\": \"2017-01-24\"\n" +
		"\t    }}";
	 *
	 * @param doc
	 * @return
	 */
	private JSONObject crawlProductInfo(Document doc) {
		JSONObject info = new JSONObject();
		
		Elements scripts = doc.select("script[type=\"text/javascript\"]");
		
		for(Element e : scripts) {
			String text = e.outerHtml();
			
			String varChaordic = "chaordicProduct=";
			
			if(text.contains(varChaordic)) {
				int x = text.indexOf(varChaordic) + varChaordic.length();
				int y = text.indexOf(";", x);
				
				String json = text.substring(x, y).trim();
				
				if(json.startsWith("{") && json.endsWith("}")) {
					JSONObject product = new JSONObject(json);
					
					if(product.has("product")) {
						info = product.getJSONObject("product");
					}
				}
				
				break;
			}
		}
		
		return info;
	}
}