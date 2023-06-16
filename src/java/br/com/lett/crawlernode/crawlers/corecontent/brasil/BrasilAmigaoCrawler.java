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
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class BrasilAmigaoCrawler extends Crawler {
   public BrasilAmigaoCrawler(Session session) {
      super(session);
   }

   public Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.ALELO.toString(), Card.DINERS.toString(), Card.AURA.toString(), Card.ELO.toString(),
      Card.COBAL.toString());

   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String productName = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-attr-title .base", false);
         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=\"product\"]", "value");
         String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, ".product.attribute.sku > div", false);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".item.product");
         String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".pricing", "#description > div"));


         String primaryImage = treatedImage(CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".gallery-placeholder__image", "src"));
         List<String> productSecondaryImages = scrapImages(doc);


         boolean available = doc.selectFirst(".stock") != null;
         Offers offers = available ? scrapOffer(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(productName)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(productSecondaryImages)
            .setCategories(categories)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("div.product-info-basic") != null;
   }

   public static String treatedImage(String s) {
      String[] parts = s.split("\\?", 2);
      return parts[0] + "?";
   }

   public static List<String> treatedImage(List<String> urls) {
      List<String> processedUrls = new ArrayList<>();

      for (String url : urls) {
         processedUrls.add(treatedImage(url));
      }

      return processedUrls;
   }

   private Offers scrapOffer(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName("amigao")
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());

      return offers;

   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      // Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-box.price-final_price", null, false, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-wrapper .price", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".old-price .price-final_price .price", null, false, ',', session);

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();

   }

   private CreditCards scrapCreditCards(Double price) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(price)
         .setFinalPrice(price)
         .build());

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private JSONObject getDataJsonFromHtml(Elements imagesElement, String keyword) {
      JSONObject json = new JSONObject();

      for (Element el : imagesElement) {
         if (el.html().contains(keyword)) {
            json = JSONUtils.stringToJson(el.html());
            break;
         }
      }

      return json;
   }
   private List<String> scrapImages(Document doc) {
      List<String> imagesList = new ArrayList<>();

      Elements imagesElement = doc.select("script[type=text/x-magento-init]");

      if (!imagesElement.isEmpty()) {
         JSONObject imagesJson = getDataJsonFromHtml(imagesElement, "mage/gallery/gallery");
         JSONArray imagesArray = JSONUtils.getValueRecursive(imagesJson, "[data-gallery-role=gallery-placeholder].mage/gallery/gallery.data", JSONArray.class);

         if (imagesArray != null) {
            for (Object e : imagesArray) {
               if (e instanceof JSONObject) {
                  JSONObject item = (JSONObject) e;
                  if (!item.optBoolean("isMain", false) && !item.optBoolean("isTrue", false)) {
                     imagesList.add(item.optString("full"));
                  }
               }
            }
         }
      }

      return imagesList;
   }
}
