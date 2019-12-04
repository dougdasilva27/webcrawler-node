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
import models.prices.Prices;

public class BrasilAdoropatasCrawler extends Crawler {
  
  public BrasilAdoropatasCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
    
    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
      
      JSONObject variationJson = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"text/x-magento-init\"]", "\"#product_addtocart_form\": ", ",", false, true);
      JSONObject imagesJson = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"text/x-magento-init\"]", "\"[data-gallery-role=gallery-placeholder]\": ", "}", false, true);
      JSONArray imagesArr = scrapImages(imagesJson);
      
      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "[data-product-id]", "data-product-id");
      String internalPid = internalId;
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.page-title", false);
      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, "[data-price-type=finalPrice] .price", null, false, ',', session);
      Prices prices = scrapPrices(doc, price);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs > ul > li:not(:last-child)", true);
      String primaryImage = imagesArr.length() > 0 ? (String) imagesArr.remove(0) : null;
      String secondaryImages = imagesArr.length() > 0 ? imagesArr.toString() : null;
      String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(
          "[itemprop=description]", ".product.info > div > div:not(#tab-label-reviews):not(#reviews):not(#tab-label-questions):not(#questions)"));
      boolean available = scrapAvailability(doc);
          
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
          .build();
      
      if (variationJson != null && !variationJson.keySet().isEmpty()) {
        variationJson = rebuildVariationJson(variationJson);
        
        products.addAll(buildVariationProducts(variationJson, product, imagesArr));
        
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
  
  private List<Product> buildVariationProducts(JSONObject json, Product product, JSONArray imagesArr) {
    List<Product> products = new ArrayList<>();
    
    for(String key : json.keySet()) {
      if(json.get(key) instanceof JSONObject) {
        JSONObject skuJson = json.getJSONObject(key);
        
        Product clone = product.clone();
        
        Float varPrice = JSONUtils.getFloatValueFromJSON(skuJson, "price", true);
        
        String varCode = JSONUtils.getStringValue(skuJson, "code");
        String varLabel = JSONUtils.getStringValue(skuJson, "label");
        
        String varName = varCode != null && varLabel != null ? product.getName() + " - " + varCode + " " + varLabel : product.getName();
        
        JSONArray images = JSONUtils.getJSONArrayValue(skuJson, "images");
        
        clone.setInternalId(key);
        clone.setPrice(varPrice);
        clone.setName(varName);
        clone.setPrices(buildVariationPrices(skuJson, varPrice));
        clone.setPrimaryImage(images.length() > 0 ? (String) images.remove(0) : null);
        
        if(product.getPrimaryImage() != null) {
          images.put(product.getPrimaryImage());
        }
        
        for(Object obj : imagesArr) {
          images.put(obj);
        }
        
        clone.setSecondaryImages(images.length() > 0 ? images.toString() : null);
        
        products.add(clone);
      }
    }
    
    return products;
  }
  
  private JSONObject rebuildVariationJson(JSONObject json) {
    JSONObject variations = new JSONObject();
    
    json = JSONUtils.getJSONValue(json, "configurable");
    json = JSONUtils.getJSONValue(json, "spConfig");
    
    JSONObject attributes = JSONUtils.getJSONValue(json, "attributes");
    for(String key : attributes.keySet()) {      
      if(attributes.get(key) instanceof JSONObject) {
        JSONObject attributesJson = attributes.getJSONObject(key);
        JSONArray options = JSONUtils.getJSONArrayValue(attributesJson, "options");
        
        String code = JSONUtils.getStringValue(attributesJson, "code");
        
        for (Object o : options) {
          if (o instanceof JSONObject) {
            JSONObject variationJson = (JSONObject) o;
            
            String label = JSONUtils.getStringValue(variationJson, "label");
            
            if (variationJson.has("products") && variationJson.get("products") instanceof JSONArray) {
              for (Object obj : variationJson.getJSONArray("products")) {
                if (obj instanceof String) {
                  variations.put((String) obj, new JSONObject().put("label", label).put("code", code));
                }
              }
            }
          }
        }
      }
    }
    
    JSONObject prices = JSONUtils.getJSONValue(json, "optionPrices");
    for(String key : prices.keySet()) {
      if(prices.get(key) instanceof JSONObject) {
        JSONObject pricesJson = prices.getJSONObject(key);
        
        JSONObject priceJson = JSONUtils.getJSONValue(pricesJson, "finalPrice");
        Float price = JSONUtils.getFloatValueFromJSON(priceJson, "amount", true);
        
        JSONObject oldPriceJson = JSONUtils.getJSONValue(pricesJson, "oldPrice");
        Float oldPrice = JSONUtils.getFloatValueFromJSON(oldPriceJson, "amount", true);
        
        if(variations.has(key) && variations.get(key) instanceof JSONObject) {
          variations.getJSONObject(key).put("price", price).put("oldPrice", oldPrice);
        }
      }
    }
    
    JSONObject images = JSONUtils.getJSONValue(json, "images");
    for(String key : images.keySet()) {
      JSONArray variationImagesArray = new JSONArray();
      
      if(images.get(key) instanceof JSONArray) {
        JSONArray imagesArray = images.getJSONArray(key);
        
        for(Object obj : imagesArray) {
          if(obj instanceof JSONObject) {
            JSONObject image = (JSONObject) obj;
            
            String imageUrl = JSONUtils.getStringValue(image, "img");
            if(image.has("isMain") && image.get("isMain") instanceof Boolean) {
              
              if(image.getBoolean("isMain")) {
                JSONArray newArr = new JSONArray().put(imageUrl);
                
                for(Object arrObj : variationImagesArray) {
                  newArr.put(arrObj);
                }
                
                variationImagesArray = newArr;
                
              } else {
                variationImagesArray.put(imageUrl);
              }
            }
          }
        }
      }
      
      if(variations.has(key) && variations.get(key) instanceof JSONObject) {
        variations.getJSONObject(key).put("images", variationImagesArray);
      }
    }
    
    return variations;   
  }
  
  private Prices buildVariationPrices(JSONObject json, Float price) {
    Prices prices = new Prices();
    
    if(price != null) {
      Float oldPrice = JSONUtils.getFloatValueFromJSON(json, "oldPrice", true);
      
      if(oldPrice.floatValue() != price.floatValue()) {
        prices.setPriceFrom(MathUtils.normalizeTwoDecimalPlaces(oldPrice.doubleValue()));
      }
      
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      
      List<Card> cards = Arrays.asList(Card.VISA, Card.MASTERCARD, Card.MAESTRO);
      for(Card card : cards) {
        prices.insertCardInstallment(card.toString(), installmentPriceMap);
      }
    }
    
    return prices;
  }
  
  private boolean scrapAvailability(Document doc) {
    Element e = doc.selectFirst(".stock");
    
    return e != null && e.hasClass("available");
  }
  
  private Prices scrapPrices(Document doc, Float price) {
    Prices prices = new Prices();
    
    if(price != null) {
      
      prices.setPriceFrom(CrawlerUtils.scrapDoublePriceFromHtml(doc, ".old-price .price-wrapper .price", null, false, ',', session));
      
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      
      List<Card> cards = Arrays.asList(Card.VISA, Card.MASTERCARD, Card.MAESTRO);
      for(Card card : cards) {
        prices.insertCardInstallment(card.toString(), installmentPriceMap);
      }
    }
    
    return prices;
  }
  
  private JSONArray scrapImages(JSONObject json) {
    JSONArray secondaryImagesArray = new JSONArray();
    
    if(json.has("mage/gallery/gallery") && json.get("mage/gallery/gallery") instanceof JSONObject) {
      json = json.getJSONObject("mage/gallery/gallery");
      
      JSONArray images = json.has("data") && json.get("data") instanceof JSONArray ? json.getJSONArray("data") : new JSONArray();
      for(Object obj : images) {
        if(obj instanceof JSONObject) {
          JSONObject imageJson = (JSONObject) obj;
          
          if(imageJson.has("full") && imageJson.get("full") instanceof String) {
            secondaryImagesArray.put(imageJson.getString("full"));
          } else if(imageJson.has("img") && imageJson.get("img") instanceof String) {
            secondaryImagesArray.put(imageJson.getString("img"));
          }
        }
      }
    }
    
    return secondaryImagesArray;
  }
}
