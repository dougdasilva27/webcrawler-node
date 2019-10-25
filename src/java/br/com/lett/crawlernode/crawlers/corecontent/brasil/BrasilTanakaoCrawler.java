package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.prices.Prices;

public class BrasilTanakaoCrawler extends Crawler {
  
  private static final String HOME_PAGE = "tanakao.com.br";
  
  public BrasilTanakaoCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
    
    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
      
      JSONObject billingInfoJson = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"text/javascript\"]", "Product.Config(", ");", false, true);
      JSONObject variationsInfoJson = CrawlerUtils.selectJsonFromHtml(doc, " script[type=\"text/javascript\"]", "AmConfigurableData(", ");", false, true);

      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-view [name=\"product\"]", "value");
      String internalPid = internalId;
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name > h1", true);
      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, "[id*=product-price]", null, true, ',', session);
      Prices prices = scrapPrices(doc, price);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs > ul > li", true);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "a.rsImg", Arrays.asList("data-rsbigimg","href"), "https", HOME_PAGE);
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "#galeria a", Arrays.asList("href"), "https", HOME_PAGE, primaryImage);
      String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".short-description", ".product-collateral .box-collateral:not(.box-reviews)"));
      boolean available = doc.selectFirst(".availability .in-stock") != null;
          
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
      
      if(billingInfoJson != null && !billingInfoJson.keySet().isEmpty()) {
        String idAttr = getAttribute(doc);
        
        if(idAttr != null) {          
          billingInfoJson = billingInfoJson.has("attributes") && billingInfoJson.get("attributes") instanceof JSONObject 
              ? billingInfoJson.getJSONObject("attributes") 
              : new JSONObject();
          billingInfoJson = billingInfoJson.has(idAttr) && billingInfoJson.get(idAttr) instanceof JSONObject 
              ? billingInfoJson.getJSONObject(idAttr) 
              : new JSONObject();
          
          JSONArray variations = billingInfoJson.has("options") && billingInfoJson.get("options") instanceof JSONArray 
              ? billingInfoJson.getJSONArray("options") 
              : new JSONArray();
              
          for(Object o : variations) {
            if(o instanceof JSONObject) {
              JSONObject variationJson = (JSONObject) o;
              Product clone = product.clone();
              
              String id = variationJson.has("id") && !variationJson.isNull("id") ? variationJson.get("id").toString() : null;
              JSONObject variationInfoJson = variationsInfoJson.has(id) && variationsInfoJson.get(id) instanceof JSONObject 
                  ? variationsInfoJson.getJSONObject(id) 
                  : new JSONObject();
                  
              if(variationInfoJson.has("name") && variationInfoJson.get("name") instanceof String) {
                clone.setName(variationInfoJson.getString("name"));
              }
                  
              if(variationInfoJson.has("not_is_in_stock") && variationInfoJson.get("not_is_in_stock") instanceof Boolean) {
                clone.setAvailable(!variationInfoJson.getBoolean("not_is_in_stock"));
              }
              
              if(variationInfoJson.has("price") && (variationInfoJson.get("price") instanceof Float || variationInfoJson.get("price") instanceof Double)) {
                clone.setPrice(variationInfoJson.getFloat("price"));
              }
              
              if(variationInfoJson.has("price_html") && variationInfoJson.get("price_html") instanceof String) {                
                clone.setPrices(scrapVariationPrices(Jsoup.parse(variationInfoJson.getString("price_html")), clone.getPrice()));
              }
              
              if(variationJson.has("products") && variationJson.get("products") instanceof JSONArray) {
                for(Object obj : variationJson.getJSONArray("products")) {
                  if(obj instanceof String) {
                    Product cloneClone = clone.clone();
                    cloneClone.setInternalId((String) obj);

                    products.add(cloneClone);
                  }
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
  
  private String getAttribute(Document doc) {
    String attribute = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "[id^=\"attribute\"]", "id");
    
    if(attribute != null && !attribute.isEmpty()) {
      attribute = attribute.replace("attribute", "");
    }
    
    return attribute;
  }
  
  private Prices scrapPrices(Document doc, Float price) {
    Prices prices = new Prices();
    
    if(price != null) {
      prices.setBankTicketPrice(price);
      prices.setPriceFrom(CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-shop-stock-price .old-price .price", null, true, ',', session));
      
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPER.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
    }
    
    return prices;
  }

  private Prices scrapVariationPrices(Document doc, Float price) {
    Prices prices = new Prices();
    
    if(price != null) {
      prices.setBankTicketPrice(price);
      prices.setPriceFrom(CrawlerUtils.scrapDoublePriceFromHtml(doc, ".old-price .price", null, true, ',', session));
      
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPER.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
    }
    
    return prices;
  }
}
