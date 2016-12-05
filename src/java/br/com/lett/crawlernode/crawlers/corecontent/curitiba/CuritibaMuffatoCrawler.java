package br.com.lett.crawlernode.crawlers.corecontent.curitiba;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
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
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;

/************************************************************************************************************************************************************************************
 * Crawling notes (11/07/2016):
 * 
 * 1) For this crawler, we have one url per each sku. There is no page is more than one sku in it.
 * 
 * 2) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 3) The sku page identification is done simply looking the URL format combined with some html element.
 * 
 * 4) Availability is crawled from the sku json extracted from a script in the html.
 * 
 * 5) InternalPid is equals internalId for this market.
 * 
 * 6) We have one method for each type of information for a sku (please carry on with this pattern).
 * 
 * Examples:
 * ex1 (available): http://delivery.supermuffato.com.br/leite-em-po-nestle-nan-soy-400g-97756/p?sc=10
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class CuritibaMuffatoCrawler extends Crawler {

	private final String HOME_PAGE = "http://delivery.supermuffato.com.br/";
	
	public CuritibaMuffatoCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}

	@Override
	public String handleURLBeforeFetch(String curURL) {

		if(curURL.endsWith("/p")) {

			try {
				String url = curURL;
				List<NameValuePair> paramsOriginal = URLEncodedUtils.parse(new URI(url), "UTF-8");
				List<NameValuePair> paramsNew = new ArrayList<>();

				for (NameValuePair param : paramsOriginal) {
					if (!param.getName().equals("sc")) {
						paramsNew.add(param);
					}
				}

				paramsNew.add(new BasicNameValuePair("sc", "10"));
				URIBuilder builder = new URIBuilder(curURL.split("\\?")[0]);

				builder.clearParameters();
				builder.setParameters(paramsNew);

				curURL = builder.build().toString();

				return curURL;

			} catch (URISyntaxException e) {
				return curURL;
			}
		}

		return curURL;

	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if( isProductPage(this.session.getOriginalURL(), doc) ) {
			Logging.printLogDebug(logger, "Product page identified: " + this.session.getOriginalURL());

			// InternalId
			String internalId = crawlInternalId(doc);

			// InternalPid
			String internalPid = crawlInternalPid(doc);

			// Name
			String name = crawlName(doc);

			// Price
			Float price = crawlMainPagePrice(doc);

			// Categorias
			ArrayList<String> categories = crawlCategories(doc);
			String category1 = getCategory(categories, 0);
			String category2 = getCategory(categories, 1);
			String category3 = getCategory(categories, 2);
			
			// Sku json from script
			JSONObject skuJson = crawlSkuJson(doc);
			
			if (skuJson == null) {
				Logging.printLogError(logger, "The SKU json information used to crawl images is null!");
			}
			
			// Availability
			boolean available = crawlAvailability(skuJson);
			
			// Primary image
			String primaryImage = crawlPrimaryImage(skuJson);
			
			// Imagens
			String secondaryImages = crawlSecondaryImages(doc);

			// Descrição
			String description = crawlDescription(doc);

			// Stock
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = new JSONArray();
			
			// Prices
			Prices prices = crawlPrices(doc, price);
			
			// create the product
			Product product = new Product();
			product.setUrl(session.getOriginalURL());
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
			Logging.printLogDebug(logger, session, "Not a product page.");
		}
		
		return products;
	}
	
	

	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url, Document document) {
		Element prdElement = document.select(".container.prd-info-container").first();
		if( url.startsWith("http://delivery.supermuffato.com.br/") && url.split("\\?")[0].endsWith("/p") && prdElement != null) return true;
		return false;
	}


	/*******************
	 * General methods *
	 *******************/

	private String crawlInternalId(Document document) {
		String internalId = null;
		Element elementInternalID = document.select(".prd-references .prd-code .skuReference").first();
		if(elementInternalID != null) {
			internalId = elementInternalID.text(); 
		}

		return internalId;
	}

	private String crawlInternalPid(Document document) {
		String internalPid = null;
		Element elementInternalID = document.select(".prd-references .prd-code .skuReference").first();
		if(elementInternalID != null) {
			internalPid = elementInternalID.text(); 
		}

		return internalPid;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select(".fn.productName").first();

		if (nameElement != null) {
			name = nameElement.text().trim();
		}

		return name;
	}

	private Float crawlMainPagePrice(Document document) {
		Float price = null;
		Element elementPrice = document.select(".plugin-preco .preco-a-vista .skuPrice").first();
		if(elementPrice != null) {
			price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
		}

		return price;
	}

	private boolean crawlAvailability(JSONObject skuJson) {
		if (skuJson != null && skuJson.has("available")) {
			return skuJson.getBoolean("available");
		}
		return false;
	}

	private String crawlPrimaryImage(JSONObject skuJson) {
		String primaryImage = null;
		
		if (skuJson != null && skuJson.has("skus")) {
			JSONArray skus = skuJson.getJSONArray("skus");
			if (skus.length() > 0) {
				JSONObject sku = skus.getJSONObject(0);
				if (sku.has("image")) {
					String image = sku.getString("image");
					primaryImage = image.replace("-400-400", "-1000-1000");
				}
			}
		}
		
		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(".breadcrumb-holder .container .row .bread-crumb ul li a");

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
		Element elementDescription = document.select("#prd-description").first();
		if(elementDescription != null) {
			description = description + elementDescription.html();
		}
		
		return description;
	}

	/**
	 * No bank slip payment method in this ecommerce.
	 * 
	 * @param doc
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(Document doc, Float price) {
		Prices prices = new Prices();
		
		if(price != null){
			Map<Integer,Float> installmentPriceMap = new HashMap<>();	
			installmentPriceMap.put(1, price);
	
			Element installmentElement = doc.select(".skuBestInstallmentNumber").first();
			
			if(installmentElement != null) {
				Integer installment = Integer.parseInt(installmentElement.text());
				
				Element valueElement = doc.select(".skuBestInstallmentValue").first();
				
				if(valueElement != null) {
					Float value = MathCommonsMethods.parseFloat(valueElement.text());
					
					installmentPriceMap.put(installment, value);
				}
			}
			
			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.DISCOVER.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AURA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);
		}
		
		return prices;
	}
	
	/**
	 * Get the script having a json variable with the image in it
	 * @return
	 */
	private JSONObject crawlSkuJson(Document document) {
		Elements scriptTags = document.getElementsByTag("script");
		JSONObject skuJson = null;
		
		for (Element tag : scriptTags){                
			for (DataNode node : tag.dataNodes()) {
				if(tag.html().trim().startsWith("var skuJson_0 = ")) {

					skuJson = new JSONObject
							(
							node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1] +
							node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1].split(Pattern.quote("}]};"))[0]
							);

				}
			}        
		}
		
		return skuJson;
	}

}
