package br.com.lett.crawlernode.crawlers.corecontent.brasil;

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
import models.Marketplace;
import models.prices.Prices;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;

public class BrasilOnagapetshopCrawler extends Crawler {
  
  public BrasilOnagapetshopCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
    
    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
      
      JSONArray jsonArr = extractPageJSON(doc);

      for(int i = 0; i < jsonArr.length(); i++) {
        JSONObject skuJson = jsonArr.getJSONObject(i);
        
        String internalId = skuJson.has("sku") && !skuJson.isNull("sku") ? skuJson.get("sku").toString() : null;
        String internalPid = skuJson.has("product_id") && !skuJson.isNull("product_id") ? skuJson.get("product_id").toString() : null;
        String name = buildVariationName(CrawlerUtils.scrapStringSimpleInfo(doc, ".title h1", true), skuJson);
        Float price = JSONUtils.getFloatValueFromJSON(skuJson, "price_number", true);
        Prices prices = scrapPrices(doc, skuJson, price);
        CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "#breadcrumb > a.crumb", true);
        String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, 
            "#product-slider-container .product-slide > a", Arrays.asList("href"), "https:", "d26lpennugtm8s.cloudfront.net");
        String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, 
            "#product-slider > .product-slide > a", Arrays.asList("href"), "https:", "d26lpennugtm8s.cloudfront.net", primaryImage);
        String description = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-description-wrap", false);
          Integer stock = skuJson.has("stock") && skuJson.get("stock") instanceof Integer ? skuJson.getInt("stock") : null;
          boolean available = (skuJson.has("available") && skuJson.get("available") instanceof Boolean) && skuJson.getBoolean("available");
          Marketplace marketplace = null;
        String ean = internalId;
        
        List<String> eans = new ArrayList<>();
        eans.add(ean);
            
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
            .setEans(eans)
            .build();
  
          products.add(product);
      }
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }
    
    return products;
  }
  
  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".page-product") != null;
  }
  
  private JSONArray extractPageJSON(Document doc) {
    String arrString = "[]";
    Element jsonElement = doc.selectFirst("#single-product .productContainer");
    
    if(jsonElement != null && jsonElement.hasAttr("data-variants")) {
      arrString = jsonElement.attr("data-variants");
    }
    
    return CrawlerUtils.stringToJsonArray(arrString);
  }
  
  private String buildVariationName(String name, JSONObject json) {
    String variationName = name;
    
    boolean hasVariation = false;
    
    if(json.has("option0") && json.get("option0") instanceof String) {
      variationName = variationName + " - " + json.getString("option0");
      hasVariation = true;
    }
    
    if(json.has("option1") && json.get("option1") instanceof String) {
      variationName = hasVariation 
          ? variationName + ", " + json.getString("option1") 
          : variationName + " - " + json.getString("option1");
          
      hasVariation = true;
    }
    
    if(json.has("option2") && json.get("option2") instanceof String) {
      variationName = hasVariation 
          ? variationName + ", " + json.getString("option2") 
          : variationName + " - " + json.getString("option2");
    }
    
    return variationName;
  }
  
  private Prices scrapPrices(Document doc, JSONObject skuJson, Float price) {
    Prices prices = new Prices();
    
    if(price != null) {
      prices.setBankTicketPrice(price);
      
      Float priceFrom = CrawlerUtils.scrapFloatPriceFromHtml(doc, "#compare_price_display", null, false, ',', session);
      if(priceFrom != null) {
        Double priceFromDouble = MathUtils.normalizeTwoDecimalPlaces(priceFrom.doubleValue());
        prices.setPriceFrom(priceFromDouble);
      }
      
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      
      if(skuJson.has("installments_data") && !skuJson.isNull("installments_data") && skuJson.get("installments_data") instanceof String) {
        String installmentsJsonString = skuJson.getString("installments_data");
        JSONObject installmentsJson = JSONUtils.stringToJson(installmentsJsonString);
        
        installmentsJson = JSONUtils.getJSONValue(installmentsJson, "Pagseguro");
        
        for(String key : installmentsJson.keySet()) {
          Integer first = MathUtils.parseInt(key);
          JSONObject installmentJson = installmentsJson.get(key) instanceof JSONObject ? installmentsJson.getJSONObject(key) : new JSONObject();
          Float second = JSONUtils.getFloatValueFromJSON(installmentJson, "installment_value", true);
          
          if(first != null && second != null) {
            installmentPriceMap.put(first, second);
          }
        }
      }
      
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPER.toString(), installmentPriceMap);
    }
    
    return prices;
  }
}
