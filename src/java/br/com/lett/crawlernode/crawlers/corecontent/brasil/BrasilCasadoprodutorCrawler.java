package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.*;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilCasadoprodutorCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.casadoprodutor.com.br/";
   private static final String FULL_SELLER_NAME = "Casa do produtor";
   private Set<String> cards = Sets.newHashSet(Card.MASTERCARD.toString(), Card.VISA.toString(), Card.ELO.toString(), Card.AMEX.toString());

   public BrasilCasadoprodutorCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=product]", "value");

      if (internalId != null) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, ".product.attribute.sku .value", true);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".page-title span", true);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs ul li:not(:first-child)");
         String description = CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList(".description"));
         List<String> images = crawlImageList(doc);
         String primaryImage = !images.isEmpty() ? images.remove(0) : CrawlerUtils.scrapSimplePrimaryImage(doc, ".gallery-placeholder._block-content-loading img", Collections.singletonList("src"), "https:", "www.casadoprodutor.com.br");

         boolean available = doc.select(".product.alert.stock").isEmpty();
         Offers offers = available ? scrapOffers(doc) : new Offers();

         List<String> eans = scrapEans(doc);

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategories(categories)
            .setDescription(description)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setOffers(offers)
            .setEans(eans)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private List<String> crawlImageList(Document doc) {
      List<String> imagesList = new ArrayList<>();
      Element imageScript = doc.selectFirst("script:containsData(mage/gallery/gallery)");

      if (imageScript != null) {
         JSONObject imageToJson = CrawlerUtils.stringToJson(imageScript.html());
         JSONArray imageArray = JSONUtils.getValueRecursive(imageToJson, "[data-gallery-role=gallery-placeholder].mage/gallery/gallery.data", JSONArray.class, new JSONArray());

         for (Object obj : imageArray) {
            if (obj instanceof JSONObject) {
               String imageUrl = ((JSONObject) obj).optString("img");
               String cleanedUrl = cleanUrlImage(imageUrl);
               if (cleanedUrl != null) {
                  imagesList.add(cleanedUrl);
               }
            }
         }
      }

      return imagesList;
   }

   private String cleanUrlImage(String imageUrl) {
      try {
         URL url = new URL(imageUrl);
         String path = url.getPath();
         return url.getProtocol() + "://" + url.getHost() + path;
      } catch (MalformedURLException e) {
         e.printStackTrace();
      }
      return null;
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(OfferBuilder.create()
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSellerFullName(FULL_SELLER_NAME)
         .setIsMainRetailer(true)
         .setSales(sales)
         .setUseSlugNameAsInternalSellerId(true)
         .build());

      return offers;
   }

   private List<String> scrapEans(Document doc) {
      List<String> eans = new ArrayList<>();
      String ean = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "body[class^=\"catalog-product-view\"]", "class");
      if (ean != null) {
         Pattern pattern = Pattern.compile("product-([0-9]*)-");
         Matcher matcher = pattern.matcher(ean.toString());
         if (matcher.find()) {
            eans.add(matcher.group(1));
         }
      }
      return eans;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "[data-price-type=\"finalPrice\"] .price", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".old-price .price", null, false, ',', session);

      CreditCards creditCards = new CreditCards();
      Installments installments = scrapInstallments(doc, spotlightPrice);

      for (String brand : cards) {
         creditCards.add(CreditCardBuilder.create()
            .setBrand(brand)
            .setIsShopCard(false)
            .setInstallments(installments)
            .build());
      }

      return PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(BankSlipBuilder.create()
            .setFinalPrice(spotlightPrice)
            .build())
         .setCreditCards(creditCards)
         .build();
   }

   private Installments scrapInstallments(Document doc, Double spotlightPrice) throws MalformedPricingException {
      Installments installments = new Installments();

      installments.add(InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      Elements elements = doc.select(".parcelas-list tbody tr");
      for (Element element : elements) {
         Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallmentFromString(element.text(), "x", "", true);
         if (!pair.isAnyValueNull()) {
            installments.add(InstallmentBuilder.create()
               .setInstallmentNumber(pair.getFirst())
               .setInstallmentPrice(MathUtils.normalizeTwoDecimalPlaces(pair.getSecond().doubleValue()))
               .build());
         }
      }

      return installments;
   }
}
