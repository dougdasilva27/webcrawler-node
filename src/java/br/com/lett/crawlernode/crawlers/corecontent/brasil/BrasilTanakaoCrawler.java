package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.Pair;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
         Logging.printLogDebug(
            logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONObject billingInfoJson =
            CrawlerUtils.selectJsonFromHtml(
               doc, "script[type=\"text/javascript\"]", "Product.Config(", ");", false, true);
         JSONObject variationsInfoJson =
            CrawlerUtils.selectJsonFromHtml(
               doc, " script[type=\"text/javascript\"]", "AmConfigurableData(", ");", false, true);

         String internalId =
            CrawlerUtils.scrapStringSimpleInfoByAttribute(
               doc, ".product-view [name=\"product\"]", "value");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name > h1", true);
         Offers offers = doc.selectFirst(".availability.in-stock") != null ? scrapOffers(doc) : null;
         CategoryCollection categories =
            CrawlerUtils.crawlCategories(doc, ".inner-breadcrumbs div ul li a", true);
         String primaryImage =
            CrawlerUtils.scrapSimplePrimaryImage(
               doc, ".product-image-gallery .gallery-image", Arrays.asList("data-zoom-image", "src"), "https", HOME_PAGE);
         List<String> secondaryImages = scrapSecondaryImages(doc);
         String description =
            CrawlerUtils.scrapElementsDescription(
               doc,
               Arrays.asList(
                  ".short-description", ".product-collateral .box-collateral:not(.box-reviews)"));

         if (billingInfoJson != null && !billingInfoJson.keySet().isEmpty()) {
            String idAttr = getAttribute(doc);

            if (idAttr != null) {
               billingInfoJson =
                  billingInfoJson.has("attributes")
                     && billingInfoJson.get("attributes") instanceof JSONObject
                     ? billingInfoJson.getJSONObject("attributes")
                     : new JSONObject();
               billingInfoJson =
                  billingInfoJson.has(idAttr) && billingInfoJson.get(idAttr) instanceof JSONObject
                     ? billingInfoJson.getJSONObject(idAttr)
                     : new JSONObject();

               JSONArray variations =
                  billingInfoJson.has("options") && billingInfoJson.get("options") instanceof JSONArray
                     ? billingInfoJson.getJSONArray("options")
                     : new JSONArray();

               for (Object o : variations) {
                  if (o instanceof JSONObject) {
                     JSONObject variationJson = (JSONObject) o;
                     String id = variationJson.optString("id", null);
                     JSONObject variationInfoJson =
                        variationsInfoJson.has(id) && variationsInfoJson.get(id) instanceof JSONObject
                           ? variationsInfoJson.getJSONObject(id)
                           : new JSONObject();

                     String nameVariation = variationInfoJson.optString("name");
                     Offers offersVariation = variationInfoJson.optString("price_html", null) != null ? scrapVariationPrices(
                        Jsoup.parse(variationInfoJson.optString("price_html")), offers.clone(), variationInfoJson.getDouble("price")) : new Offers();

                     if (variationJson.has("products")
                        && variationJson.get("products") instanceof JSONArray) {
                        for (Object obj : variationJson.getJSONArray("products")) {
                           if (obj instanceof String) {
                              ProductBuilder product =
                                 ProductBuilder.create()
                                    .setUrl(session.getOriginalURL())
                                    .setInternalId(obj.toString())
                                    .setInternalPid(internalId)
                                    .setName(nameVariation)
                                    .setOffers(offers)
                                    .setCategory1(categories.getCategory(0))
                                    .setCategory2(categories.getCategory(1))
                                    .setCategory3(categories.getCategory(2))
                                    .setPrimaryImage(primaryImage)
                                    .setSecondaryImages(secondaryImages)
                                    .setDescription(description)
                                    .setOffers(offersVariation);

                              products.add(product.build());
                           }
                        }
                     }
                  }
               }
            }
         } else {
            ProductBuilder product =
               ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalId)
                  .setName(name)
                  .setOffers(offers)
                  .setCategory1(categories.getCategory(0))
                  .setCategory2(categories.getCategory(1))
                  .setCategory3(categories.getCategory(2))
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
                  .setDescription(description);

            products.add(product.build());
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
      String attribute =
         CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "[id^=\"attribute\"]", "id");

      if (attribute != null && !attribute.isEmpty()) {
         attribute = attribute.replace("attribute", "");
      }

      return attribute;
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();

      Double price =
         CrawlerUtils.scrapDoublePriceFromHtml(
            doc, "[id*=product-price]", null, false, ',', session);

      Double priceFrom =
         CrawlerUtils.scrapDoublePriceFromHtml(
            doc, ".product-shop-stock-price .old-price .price", null, true, ',', session);

      offers.add(
         OfferBuilder.create()
            .setIsBuybox(false)
            .setPricing(getPricing(price, priceFrom, doc))
            .setSellerFullName("Tanakao")
            .setIsMainRetailer(true)
            .setUseSlugNameAsInternalSellerId(true)
            .build());

      return offers;
   }

   private Pricing getPricing(Double price, Double priceFrom, Document doc)
      throws MalformedPricingException {

      CreditCards creditCards =
         new CreditCards(
            Stream.of(
               Card.VISA,
               Card.MASTERCARD,
               Card.ELO,
               Card.HIPERCARD,
               Card.AMEX,
               Card.HIPER,
               Card.DINERS)
               .map(
                  card -> {
                     Pair<Integer, Float> installmentPair =
                        CrawlerUtils.crawlSimpleInstallment(
                           ".product-view .plots", doc, false, "x");

                     Set<Installment> installments = new HashSet<>();

                     try {
                        installments.add(
                           InstallmentBuilder.create()
                              .setInstallmentPrice(price)
                              .setInstallmentNumber(1)
                              .setFinalPrice(price)
                              .build());

                        if (!installmentPair.isAnyValueNull()) {

                           installments.add(
                              InstallmentBuilder.create()
                                 .setInstallmentPrice(installmentPair.getSecond().doubleValue())
                                 .setInstallmentNumber(installmentPair.getFirst())
                                 .setFinalPrice(
                                    installmentPair.getSecond().doubleValue()
                                       * installmentPair.getFirst())
                                 .build());
                        }

                        return CreditCardBuilder.create()
                           .setBrand(card.toString())
                           .setIsShopCard(false)
                           .setInstallments(new Installments(installments))
                           .build();

                     } catch (MalformedPricingException e) {
                        throw new RuntimeException(e);
                     }
                  })
               .collect(Collectors.toList()));

      return PricingBuilder.create()
         .setSpotlightPrice(price)
         .setPriceFrom(priceFrom)
         .setBankSlip(BankSlipBuilder.create().setFinalPrice(price).build())
         .setCreditCards(creditCards)
         .build();
   }

   private Offers scrapVariationPrices(Document doc, Offers offers, Double price)
      throws OfferException {
      List<Offer> offerList =
         offers.getOffersList().stream()
            .map(
               offer -> {
                  Double priceFrom =
                     CrawlerUtils.scrapDoublePriceFromHtml(
                        doc, ".old-price .price", null, true, ',', session);
                  try {
                     offer.setPricing(getPricing(price, priceFrom, doc));
                     return offer;
                  } catch (MalformedPricingException e) {
                     throw new RuntimeException(e);
                  }
               })
            .collect(Collectors.toList());

      return new Offers(offerList);
   }

   private List<String> scrapSecondaryImages(Document doc){
      List<String> secondaryImages = new ArrayList<>();

      Elements images = doc.select(".product-image-thumbs li a img");

      for(Element e: images){
         if(images.indexOf(e) != 0){
            secondaryImages.add(CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, null, "src"));
         }
      }

      return secondaryImages;
   }
}
