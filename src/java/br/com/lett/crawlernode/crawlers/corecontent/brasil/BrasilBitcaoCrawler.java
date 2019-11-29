package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import models.AdvancedRatingReview;
import models.RatingsReviews;
import models.prices.Prices;

public class BrasilBitcaoCrawler extends Crawler {
  
  private static final String HOME_PAGE = "www.bitcao.com.br";
  
  public BrasilBitcaoCrawler(Session session) {
    super(session);
    super.config.setMustSendRatingToKinesis(true);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
    
    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
      
      JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"text/javascript\"]", "Product.Config(", ");", false, true);
      JSONArray variationArray = extractVariationArray(json);
      
      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "[name=product]", "value");
      String internalPid = internalId;
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name > h1", true);
      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, "#product-price-" + internalId + " .customize-price", null, true, ',', session);
      Prices prices = scrapPrices(doc, price);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs > ul >li:not(.product)", true);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-image .product-image-gallery > a", Arrays.asList("href"), "https", HOME_PAGE);
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".product-image-thumbs > li > a", Arrays.asList("href"), "https", HOME_PAGE, primaryImage);
      String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".short-description", "#collateral-tabs"));
      RatingsReviews ratingsReviews = scrapRatingReviews(doc);
      
      Element availabilityElement = doc.selectFirst(".availability");
      boolean available = availabilityElement != null && availabilityElement.hasClass("in-stock");
          
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
          .setRatingReviews(ratingsReviews)
          .build();
      
      if(variationArray.length() > 0) {
        products.addAll(extractVariations(variationArray, product));
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
  
  private List<Product> extractVariations(JSONArray variations, Product product) {
    List<Product> products = new ArrayList<>();
    Map<String, String> idNameMap = new HashMap<>();
    Map<String, Float> idPriceMap = new HashMap<>();
    Map<String, Prices> idPricesMap = new HashMap<>();
    
    for(Object obj : variations) {      
      if(obj instanceof JSONObject) {
        JSONObject productJson = (JSONObject) obj;
        
        if(productJson.has("products") && productJson.get("products") instanceof JSONArray) {
          JSONArray skus = productJson.getJSONArray("products");
          
          Float price = JSONUtils.getFloatValueFromJSON(productJson, "price", true);
          if(price != null) {
            price = price + product.getPrice();
          }
          
          Float oldPrice = JSONUtils.getFloatValueFromJSON(productJson, "oldPrice", true);
          Prices prices = scrapVariationPrices(product.getPrices(), oldPrice, price);
          
          for(Object o : skus) {
            if(o instanceof String) {
              if(!idNameMap.containsKey((String) o)) {                
                idNameMap.put((String) o, " -");
              }
              
              if(productJson.has("label") && productJson.get("label") instanceof String) {
                idNameMap.put((String) o, idNameMap.get((String) o) + " " + productJson.getString("label"));
              }
              
              if(price != null) {
                idPriceMap.put((String) o, price);
              }
              
              idPricesMap.put((String) o, prices);
            }
          }
        }
      }
    }
    
    for(String key : idNameMap.keySet()) {
      Product clone = product.clone();
      
      clone.setInternalId(key);
      clone.setName(product.getName() + idNameMap.get(key));
      
      if(idPriceMap.containsKey(key)) {
        clone.setPrice(idPriceMap.get(key));
      }
      
      if(idPricesMap.containsKey(key)) {
        clone.setPrices(idPricesMap.get(key));
      }
      
      products.add(clone);
    }
    
    return products;
  }
  
  
  private Prices scrapPrices(Document doc, Float price) {
    Prices prices = new Prices();
    
    if(price != null) {
      prices.setPriceFrom(CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-essential .price-box [id*=old-price-] .customize-price", null, true, ',', session));
      
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      
      List<Card> marketCards = Arrays.asList(Card.VISA, Card.MASTERCARD, Card.ELO, Card.DINERS, Card.AMEX);
      for(Card c : marketCards) {
        prices.insertCardInstallment(c.toString(), installmentPriceMap);
      }
    }
    
    return prices;
  }
  
  private Prices scrapVariationPrices(Prices prices, Float oldPrice, Float price) {
    Prices variationPrices = new Prices();
    
    if(price != null) {
      if(oldPrice != null && prices.getPriceFrom() != null) {
        variationPrices.setPriceFrom(prices.getPriceFrom() + oldPrice);
      }
      
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      
      List<Card> marketCards = Arrays.asList(Card.VISA, Card.MASTERCARD, Card.ELO, Card.DINERS, Card.AMEX);
      for(Card c : marketCards) {
        variationPrices.insertCardInstallment(c.toString(), installmentPriceMap);
      }
    }
    
    return variationPrices;
  }
  
  private RatingsReviews scrapRatingReviews(Document doc) {
    RatingsReviews ratingReviews = new RatingsReviews();
    ratingReviews.setDate(session.getDate());
    
    Integer totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtml(doc, ".ratings .rating-links", null, "coment", false, false, 0);
    Integer totalWrittenReviews = totalNumOfEvaluations;
    AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(doc);
    Double avgRating = computeAvgRating(advancedRatingReview);
    
    ratingReviews.setTotalRating(totalNumOfEvaluations);
    ratingReviews.setAverageOverallRating(avgRating);
    ratingReviews.setTotalWrittenReviews(totalWrittenReviews);
    ratingReviews.setAdvancedRatingReview(advancedRatingReview);
    
    return ratingReviews;
  }
  
  private Double computeAvgRating(AdvancedRatingReview advancedRatingReview) {
    
    int total5 = advancedRatingReview.getTotalStar5();
    int total4 = advancedRatingReview.getTotalStar4();
    int total3 = advancedRatingReview.getTotalStar3();
    int total2 = advancedRatingReview.getTotalStar2();
    int total1 = advancedRatingReview.getTotalStar1();
    
    int totalTotal = total5 + total4 + total3 + total2 + total1;
    
    if(totalTotal == 0) {
      return 0.0;
    }
    
    return (total5 * 5.0 + total4 * 4.0 + total3 * 3.0 + total2 * 2.0 + total1 * 1.0) / totalTotal;
  }
  
  private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
    Integer star1 = 0;
    Integer star2 = 0;
    Integer star3 = 0;
    Integer star4 = 0;
    Integer star5 = 0;
    
    Elements reviews = doc.select("#customer-reviews .rating");
    
    for(Element review : reviews) {
      if(review.hasAttr("style")) {
        String width = review.attr("style").replaceAll("[^0-9]+", "");
        
        if(!width.isEmpty()) {
          Integer val = Integer.parseInt(width);
          Integer rating = 5 * val / 100;

          switch(rating) {
            case 1: star1 += 1; 
            break;
            case 2: star2 += 1; 
            break;
            case 3: star3 += 1; 
            break;
            case 4: star4 += 1; 
            break;
            case 5: star5 += 1; 
            break;
          }
        }
      }
    }
    
    return new AdvancedRatingReview.Builder()
        .totalStar1(star1)
        .totalStar2(star2)
        .totalStar3(star3)
        .totalStar4(star4)
        .totalStar5(star5)
        .build();
  }
}
