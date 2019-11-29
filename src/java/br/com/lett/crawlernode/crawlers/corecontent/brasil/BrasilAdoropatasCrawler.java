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
import br.com.lett.crawlernode.util.Logging;
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
      
      JSONObject imagesJson = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"text/x-magento-init\"]", "\"[data-gallery-role=gallery-placeholder]\": ", "}", false, true);
      JSONArray imagesArr = scrapImages(imagesJson);
          
      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "[data-product-id]", "data-product-id");
      String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, "[itemprop=sku]", true);
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
      
      products.add(product);
      
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }
    
    return products;
  }
  
  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".catalog-product-view") != null;
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
