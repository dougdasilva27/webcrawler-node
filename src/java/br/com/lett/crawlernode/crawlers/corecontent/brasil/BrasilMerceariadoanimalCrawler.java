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
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

public class BrasilMerceariadoanimalCrawler extends Crawler {
  
  public BrasilMerceariadoanimalCrawler(Session session) {
    super(session);
    super.config.setMustSendRatingToKinesis(true);
  }

  private static final String HOME_PAGE = "https://www.merceariadoanimal.com.br/";

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
        
      String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".VariationProductSKU", true);
      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#productDetailsAddToCartForm > input[name=\"product_id\"]", "value");
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".ProductMain [itemprop=\"name\"]", true) + " - " +
          CrawlerUtils.scrapStringSimpleInfo(doc, ".ProductMain .brand > a", true);
      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".PriceRow [itemprop=\"price\"]", "content", false, '.', session);
      Prices prices = scrapPrices(doc, price);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".Breadcrumb > ul > li[itemprop]", true);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".ProductThumbImage > a", Arrays.asList("href"), "https:", HOME_PAGE);
      String secondaryImages = scrapSecondaryImages(doc, primaryImage);
      String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(
          "#product-tabs ul> li:not(#tab-reviews)", "#product-tabs .tab-content > div:not(#reviews)"));
      Integer stock = null;
      boolean available = doc.selectFirst("#hidden_aviseme") == null;
      Marketplace marketplace = null;
      RatingsReviews ratingReviews = scrapRatingsReviews(doc);
      

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
          .setRatingReviews(ratingReviews)
          .build();
      
      Elements selectVariations = doc.select(".ProductOptionList .VariationSelect option:not([value=\"\"])");
      Elements radioVariations = doc.select(".ProductOptionList .Value > ul > li > label");
      
      if(!selectVariations.isEmpty()) {
        for(Element variation : selectVariations) {
          JSONObject json = getVariationJSON(variation.attr("value"), internalPid);
          
          Product clone = product.clone();
          
          if(json.has("combinationid") && !json.isNull("combinationid")) {
            clone.setInternalId(json.get("combinationid").toString());
          }
          
          clone.setName(product.getName() + ", " + variation.text().replace("(Indisponível)", "").trim());
          
          clone.setPrice(CrawlerUtils.getFloatValueFromJSON(json, "price", false, true));
          clone.setPrices(scrapVariationPrices(prices, json, clone.getPrice()));
         
          if(json.has("instock") && json.get("instock") instanceof Boolean) {
            clone.setAvailable(json.getBoolean("instock"));
          }
          
          products.add(clone);
        }
      } else if(!radioVariations.isEmpty()) {        
        for(Element variation : radioVariations) {
          Element input = variation.selectFirst("input");
          
          if(input != null) {
            JSONObject json = getVariationJSON(input.attr("value"), internalPid);
            
            Product clone = product.clone();
            
            if(json.has("combinationid") && !json.isNull("combinationid")) {
              clone.setInternalId(json.get("combinationid").toString());
            }
            
            clone.setName(product.getName() + ", " + variation.text().replace("(Indisponível)", "").trim());
            
            clone.setPrice(CrawlerUtils.getFloatValueFromJSON(json, "price", false, true));
            clone.setPrices(scrapVariationPrices(prices, json, clone.getPrice()));
           
            if(json.has("instock") && json.get("instock") instanceof Boolean) {
              clone.setAvailable(json.getBoolean("instock"));
            }
            
            products.add(clone);
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
    return doc.selectFirst("#product-main") != null;
  }
  
  private String scrapSecondaryImages(Document doc, String primaryImage) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();
    Elements imagesElement = doc.select(".ProductTinyImageList > ul > li > div > div > a");
    
    for(Element imageElement : imagesElement) {
      if(imageElement.hasAttr("rel")) {
        JSONObject json = JSONUtils.stringToJson(imageElement.attr("rel"));
        
        if(json.has("largeimage") && json.get("largeimage") instanceof String) {
          
          String image = json.getString("largeimage");
          
          if(!image.equals(primaryImage)) {
            secondaryImagesArray.put(image);
          }
        }
      }
    }
    
    if(secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }
    
    return secondaryImages;
  }
  
  private Prices scrapPrices(Document doc, Float price) {
    Prices prices = new Prices();
    
    if(price != null) {
      prices.setBankTicketPrice(CrawlerUtils.scrapFloatPriceFromHtml(doc, ".MsgBoleto strong", null, true, ',', session));
      
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      
      Pair<Integer, Float> installment = CrawlerUtils.crawlSimpleInstallment(".MsgParcelamento", doc, true, "x de", "juros", true);
      if (!installment.isAnyValueNull()) {
        installmentPriceMap.put(installment.getFirst(), installment.getSecond());
      }
      
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
    }
    
    return prices;
  }
  
  private RatingsReviews scrapRatingsReviews(Document doc) {
    RatingsReviews ratingReviews = new RatingsReviews();
    ratingReviews.setDate(session.getDate());
    
    Integer totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtmlAttr(doc, 
        "[itemprop=\"aggregateRating\"] meta[itemprop=\"ratingCount\"]", "content", 0);
    Double avgRating = CrawlerUtils.scrapDoublePriceFromHtml(doc, "[itemprop=\"aggregateRating\"] meta[itemprop=\"ratingValue\"]", "content", false, '.', session);
    Integer totalWrittenReviews = doc.select(".ProductReviewList > li").size();
    
    ratingReviews.setTotalRating(totalNumOfEvaluations);
    ratingReviews.setAverageOverallRating(avgRating);
    ratingReviews.setTotalWrittenReviews(totalWrittenReviews);
    
    return ratingReviews;
  }
  
  private JSONObject getVariationJSON(String variation, String internalPid) {
    JSONObject variationJSON = new JSONObject();
    
    Request request = RequestBuilder.create()
        .setUrl("https://www.merceariadoanimal.com.br/ajax.ecm?w=GetVariationOptions&productId=" + internalPid + "&options=" + variation)
        .build();
    
    variationJSON = JSONUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
    
    return variationJSON;
  }
  
  private Prices scrapVariationPrices(Prices prices, JSONObject json, Float price) {   
    if(price == null) {
      return new Prices();
    }
    
    Prices clone = prices.clone();
    
    if(json.has("checkdescmsg") && json.get("checkdescmsg") instanceof String) {
      clone.setBankTicketPrice(CrawlerUtils.scrapFloatPriceFromString(json.getString("checkdescmsg"), ',', "(", ")", session));
    }
    
    Map<Integer, Float> installmentPriceMap = new TreeMap<>();
    installmentPriceMap.put(1, price);
    
    if(json.has("parcmsg") && json.get("parcmsg") instanceof String) {
      String installmentText = json.getString("parcmsg");
      
      Pair<Integer, Float> installment = CrawlerUtils.crawlSimpleInstallmentFromString(installmentText, "x", "juros", true);
      if (!installment.isAnyValueNull()) {
        installmentPriceMap.put(installment.getFirst(), installment.getSecond());
      }
      
      clone.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      clone.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      clone.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      clone.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
    }
    
    return clone;
  }
}
