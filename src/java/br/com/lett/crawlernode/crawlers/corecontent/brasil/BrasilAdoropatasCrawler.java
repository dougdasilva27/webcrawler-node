package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.Offers;
import models.prices.Prices;

public class BrasilAdoropatasCrawler extends Crawler {
	
  // Dis: https://www.adoropatas.com.br/bravecto-20kg-a-40kg
  // Ind: https://www.adoropatas.com.br/flunixin-5mg-e-20mg
  // Var: https://www.adoropatas.com.br/pa-higienica-cara-de-gato-cores
	  
  private static final String HOME_PAGE = "www.adoropatas.com.br";
  
  public BrasilAdoropatasCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
    
    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
      
      JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"text/javascript\"]", "Product.Config(", ");", false, true);
      JSONObject childProducts = JSONUtils.getJSONValue(json, "childProducts");
      JSONArray variationArray = extractVariationArray(json);

      String internalId = scrapInternalId(doc);
      String internalPid = scrapInternalPid(doc);
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name > h1", true);
      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".regular-price > .price", null, true, ',', session);
      Prices prices = scrapPrices(doc, price);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs > ul > li:not(:last-child)", true);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#image-main", Arrays.asList("src"), "https", HOME_PAGE);
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".product-image-gallery > .gallery-image", Arrays.asList("data-zoom-image", "src"), "https", HOME_PAGE, primaryImage);
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
      
      if(variationArray.length() > 0) {
    	  for(Object obj : variationArray) {
    		  if(obj instanceof JSONObject) {
    			  JSONObject varJson = (JSONObject) obj;
    			  
    			  Product clone = product.clone();
    			  
    			  if(varJson.has("label") && varJson.get("label") instanceof String) {
    				  clone.setName(product.getName() + " - " + varJson.getString("label"));
    			  }
    			  
    			  if(varJson.has("products") && varJson.get("products") instanceof JSONArray) {
    				  for(Object productsObj : varJson.getJSONArray("products")) {
    					  if(productsObj instanceof String) {
    						  Product cloneClone = clone.clone();
    						  String skuId = (String) productsObj;
    						  Float skuPrice = null;
    						  
    						  cloneClone.setInternalId(skuId);
    						  
    						  if(childProducts.has(skuId) && childProducts.get(skuId) instanceof JSONObject) {
    							  skuPrice = JSONUtils.getFloatValueFromJSON(childProducts.getJSONObject(skuId), "finalPrice", true);
    							  cloneClone.setPrice(skuPrice);
    						  }
    						  
    						  cloneClone.setPrices(scrapVariationPrices(skuPrice));
    						  
    						  products.add(cloneClone);
    					  }
    				  }
    			  }
    		  }
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
  
  private Prices scrapPrices(Document doc, Float price) {
	  Prices prices = new Prices();
	  
	  if(price != null) {
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
		  prices.setBankTicketPrice(MathUtils.normalizeTwoDecimalPlacesUp(price * 0.95f));
		  
		  Map<Integer, Float> installmentPriceMap = new TreeMap<>();
	      installmentPriceMap.put(1, price);
		  
		  prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
		  prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
		  prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
	  }
	  
	  return prices;
  }
}
