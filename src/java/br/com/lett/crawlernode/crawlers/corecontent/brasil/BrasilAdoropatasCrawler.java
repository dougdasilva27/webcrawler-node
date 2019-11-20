package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.Offers;
import models.prices.Prices;

public class BrasilAdoropatasCrawler extends Crawler {

  private static final String HOME_PAGE = "www.adoropatas.com.br";
  
  public BrasilAdoropatasCrawler(Session session) {
    super(session);
  }
  
  @Override
  protected Object fetch() {

	// Colocando User-Agent do mozila para não receber imagens do tipo webp
    Map<String, String> headers = new HashMap<>();
    headers.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:47.0) Gecko/20100101 Firefox/47.0");

    Request request = RequestBuilder.create()
            .setUrl(session.getOriginalURL())
            .setCookies(cookies)
            .build();

    return Jsoup.parse(this.dataFetcher.get(session, request).getBody());
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
    
    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
      
      JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"text/javascript\"]", "Product.Config(", ");", false, true);
      JSONObject childProducts = JSONUtils.getJSONValue(json, "childProducts");
      Map<String, String> variations = reagroupVariationArray(extractVariationArray(json));

      String internalId = scrapInternalId(doc);
      String internalPid = scrapInternalPid(doc);
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name > h1", true);
      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".data > .price", null, true, ',', session);
      Prices prices = scrapPrices(doc, price);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs > ul > li:not(:last-child)", true);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#image-main", 
    		  Arrays.asList("src"), "https", HOME_PAGE);
      String secondaryImages = scrapSecondaryImages(doc, primaryImage);
      String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".short-description", "#collateral-tabs > [class *= tab]:not(:last-child):not(:nth-last-child(2))"));
      Integer stock = null;
      boolean available = doc.selectFirst(".extra-info .availability.out-of-stock") == null;
      Marketplace marketplace = null;
      Offers offers = null;
          
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
          .setOffers(offers)
          .build();
      
      if(variations.size() > 0) {
    	  for(String key : variations.keySet()) {    			  
			  Product clone = product.clone();
			  
			  clone.setInternalId(key);
			  clone.setName(product.getName() + variations.get(key));

			  Float skuPrice = null;
			  if(childProducts.has(key) && childProducts.get(key) instanceof JSONObject) {
				  skuPrice = JSONUtils.getFloatValueFromJSON(childProducts.getJSONObject(key), "finalPrice", true);
				  clone.setPrice(skuPrice);
			  }
			  
			  clone.setPrices(scrapVariationPrices(skuPrice));
			  
			  products.add(clone);
    	  }
      } else {
    	  products.add(product);
      }
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }
    
    return products;
  }
  
  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".catalog-product-view") != null;
  }
  
  private JSONArray extractVariationArray(JSONObject json) {
	  JSONArray variationArray = new JSONArray();
	  
	  if(json.has("attributes") && json.get("attributes") instanceof JSONObject) {
    	  
    	  JSONObject subJson = json.getJSONObject("attributes");
    	  for(String key : subJson.keySet()) {
    		  if(subJson.get(key) instanceof JSONObject) {
    			  JSONObject attrJson = subJson.getJSONObject(key);
    			  
    			  if(attrJson.has("options") && attrJson.get("options") instanceof JSONArray) {
    				  
    				  JSONArray optionsArr = attrJson.getJSONArray("options");
    				  for(Object obj : optionsArr) {
    					  variationArray.put(obj);  
    				  }
    			  }
    		  }
    	  }
      }
	  
	  return variationArray;
  }
  
  private Map<String, String> reagroupVariationArray(JSONArray arr) {
	  Map<String, String> idNameMap = new HashMap<>();
	  
	  for(Object obj : arr) {
		  if(obj instanceof JSONObject) {
			  JSONObject product = (JSONObject) obj;
			  
			  if(product.has("products") && product.get("products") instanceof JSONArray) {
				  JSONArray skus = product.getJSONArray("products");
				  
				  for(Object o : skus) {
					  if(o instanceof String) {
						  if(!idNameMap.containsKey((String) o)) {							  
							  idNameMap.put((String) o, " -");
						  }
						  
						  if(product.has("label") && product.get("label") instanceof String) {
							  idNameMap.put((String) o, idNameMap.get((String) o) + " " + product.getString("label"));
						  }
					  }
				  }
			  }
		  }
	  }
	  
	  return idNameMap;
  }
  
  private String scrapInternalId(Document doc) {
	  String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-shop > .ref", true);
	  
	  if(internalId != null && internalId.startsWith("Ref.:")) {
		  internalId = internalId.substring("Ref.:".length()).trim();
	  }
	  
	  return internalId;
  }
  
  private String scrapInternalPid(Document doc) {
	  String internalPid = null;
	  Element pidElement = doc.selectFirst("[id*=product-price-]");
	  
	  if(pidElement != null) {
		  
		  String pidElementId = pidElement.id();
		  if(pidElementId.startsWith("product-price-")) {
			  internalPid = pidElementId.replace("product-price-", "");
		  }
	  }
	  
	  return internalPid;
  }
  
  private String scrapSecondaryImages(Document doc, String primaryImage) {
	String secondaryImages = null;
	  
    JSONArray secondaryImagesArray = new JSONArray();

    Elements images = doc.select(".product-image-gallery > .gallery-image:not(#image-main)");
    for (Element e : images) {
      String image = CrawlerUtils.sanitizeUrl(e, Arrays.asList("data-zoom-image", "src"), "https", HOME_PAGE);

      if ((primaryImage == null || !primaryImage.equals(image)) && image != null && !primaryImage.contains(CommonMethods.getLast(image.split("/")))) {
        secondaryImagesArray.put(image);
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }
  
  private Prices scrapPrices(Document doc, Float price) {
	  Prices prices = new Prices();
	  
	  if(price != null) {
		  prices.setPriceFrom(CrawlerUtils.scrapDoublePriceFromHtml(doc, ".regular-price > .price-desconto", null, true, ',', session));
		  
		  Map<Integer, Float> installmentPriceMap = new TreeMap<>();
	      installmentPriceMap.put(1, price);
		  
		  Pair<Integer, Float> installment = CrawlerUtils.crawlSimpleInstallment(".valor_dividido", doc, false, "de", "", true, '.');
		  
		  if(!installment.isAnyValueNull()) {
			  installmentPriceMap.put(installment.getFirst(), installment.getSecond());
		  }
		  
		  prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
		  prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
		  prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
	  }
	  
	  return prices;
  }
  
  private Prices scrapVariationPrices(Float price) {
	  Prices prices = new Prices();
	  
	  if(price != null) {
		  prices.setBankTicketPrice(MathUtils.normalizeTwoDecimalPlaces(price * 0.95f));
		  
		  Map<Integer, Float> installmentPriceMap = new TreeMap<>();
	      installmentPriceMap.put(1, price);
		  
		  prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
		  prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
		  prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
	  }
	  
	  return prices;
  }
}
