package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
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

public class BrasilVianimalCrawler extends Crawler {
   
   public BrasilVianimalCrawler(Session session) {
     super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
     super.extractInformation(doc);
     List<Product> products = new ArrayList<>();
     
     if (isProductPage(doc)) {
       Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
       
       String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-essential [name=product]", "value");
       String internalPid = internalId;
       String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name h1", true);
       Float price = scrapPrice(doc);
       Prices prices = scrapPrices(doc, price);
       CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs > ul > li:not(:last-child)", true);
       String primaryImage = scrapPrimaryImage(doc);
       String secondaryImages = scrapSecondaryImages(doc, primaryImage);
       String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".short-description",".box-description", ".box-additional"));
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
      Element availabilityElement = doc.selectFirst(".availability");
      
      return availabilityElement != null && !availabilityElement.hasClass("out-of-stock");
   }
   
   private Float scrapPrice(Document doc) {
      Element promotionPrice = doc.selectFirst(".price-box .old-price");
      
      if(promotionPrice != null) {
         return CrawlerUtils.scrapFloatPriceFromHtml(doc, ".price-box .special-price .price", null, false, ',', session);
      } 
      
      return CrawlerUtils.scrapFloatPriceFromHtml(doc, ".price-box .regular-price .price", null, false, ',', session);
   }
   
   private String extractImage(String text, String initText, String endText) {
      String image = CrawlerUtils.extractSpecificStringFromScript(
            text, initText, false, endText, false);
      
      if(image.startsWith("'")) {
         image = image.substring(1);
      }
      
      if(image.endsWith("'")) {
         image = image.substring(0, image.length() - 1);
      }
      
      return image;
   }
   
   private String scrapPrimaryImage(Document doc) {
      String primaryImage = null;
      Element imageElement = doc.selectFirst(".product-img-box script:not(:first-child)");
      
     if(imageElement != null && !imageElement.outerHtml().isEmpty()) {
         primaryImage = extractImage(imageElement.outerHtml(), "ig_lightbox_img_sequence.push(", ");");
      }
      
      return primaryImage;
   }
   
   private String scrapSecondaryImages(Document doc, String primaryImage) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();
      Element imagesElement = doc.selectFirst(".product-img-box script:not(:first-child)");
      
      if(imagesElement != null && !imagesElement.outerHtml().isEmpty()) {
         String scriptText = imagesElement.outerHtml();
         String image = null;
         
         while(scriptText.contains("ig_lightbox_img_sequence.push(")) {
            image = extractImage(scriptText, "ig_lightbox_img_sequence.push(", ");");
            
            scriptText = scriptText.substring(
                  scriptText.indexOf("ig_lightbox_img_sequence.push(") + "ig_lightbox_img_sequence.push(".length());
         
            if(!image.equals(primaryImage)) {
               secondaryImagesArray.put(image);
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
       prices.setPriceFrom(CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-box .old-price .price", null, false, ',', session));
        
       Map<Integer, Float> installmentPriceMap = new TreeMap<>();
       installmentPriceMap.put(1, price);
       
       //Pair<Integer, Float> installment = CrawlerUtils.crawlSimpleInstallment(".parcel-price .cash-payment span", doc, true, "x de", "", true);
       
       //if (!installment.isAnyValueNull()) {
       //  installmentPriceMap.put(installment.getFirst(), installment.getSecond());
       //}
       
       List<Card> cards = Arrays.asList(Card.MASTERCARD, Card.VISA, Card.AMEX, Card.ELO, Card.HIPERCARD, Card.DINERS);
       for(Card card  : cards) {
          prices.insertCardInstallment(card.toString(), installmentPriceMap);
       }
     }
     
     return prices;
   }
}
