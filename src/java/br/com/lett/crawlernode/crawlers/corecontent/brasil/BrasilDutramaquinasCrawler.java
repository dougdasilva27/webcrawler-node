package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
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
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class BrasilDutramaquinasCrawler extends Crawler {
   public BrasilDutramaquinasCrawler(Session session) {
      super(session);
   }

   private final String SELLER_FULL_NAME = "Dutra Máquinas";
   private Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.DINERS.toString(), Card.HIPERCARD.toString(), Card.ELO.toString());


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      List<String> variationsUrls = scrapVariations(doc);

      products.add(extractProductInformation(doc));

      for (String variationUrl : variationsUrls) {
         Document variationDoc = fetchDocument(variationUrl);
         Product product = extractProductInformation(variationDoc);
         if (product != null) {
            products.add(product);
         }
      }

      return products;
   }

   private Document fetchDocument(String variationUrl) {
      Request request = Request.RequestBuilder.create()
         .setUrl(variationUrl)
         .build();

      String body = this.dataFetcher.get(session, request).getBody();
      return Jsoup.parse(body);
   }

   private List<String> scrapVariations(Document doc) {
      List<String> variationsUrls = new ArrayList<>();

      Elements variations = doc.select(".variacao .var-text");
      if (variations != null && !variations.isEmpty()) {
         for (Element variation : variations) {
            if (variation.selectFirst(".select-variacao").hasClass("active")) continue;
            try {
               String variationUrl = variation.attr("value");
               String variationTitle = variation.select("span").text();
               variationUrl = variationUrl.replace(variationTitle + "|", "");
               String completeUrl = CrawlerUtils.completeUrl(variationUrl, "https", "www.dutramaquinas.com.br/p/");
               variationsUrls.add(completeUrl);
            } catch (Exception e) {
               Logging.printLogDebug(logger, session, "Failed to capture variations for Dutra Máquinas " + this.session.getOriginalURL());
            }
         }
      }


      return variationsUrls;
   }

   private Product extractProductInformation(Document doc) throws Exception {
      JSONObject jsonProduct = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"application/ld+json\"]", null, null, false, false);

      if (isProductPage(doc) && jsonProduct != null && !jsonProduct.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#ls_id_produto", "value");
         String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, "td.cod-produto:nth-child(4)", true);
         String name = scrapName(doc, jsonProduct);
         List<String> images = CrawlerUtils.scrapSecondaryImages(doc, ".fotos-produto .slider-principal img", List.of("data-original"), "https", "www.dutramaquinas.com.br", null);
         String primaryImage = images.isEmpty() ? null : images.remove(0);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb-lkn a", true);
         String description = CrawlerUtils.scrapElementsDescription(doc, List.of("#descricao"));
         boolean availableToBuy = JSONUtils.getValueRecursive(jsonProduct, "offers.availability", String.class, "").contains("InStock");
         String ean = jsonProduct.optString("gtin13", "");
         Offers offers = availableToBuy ? scrapOffers(doc) : new Offers();
         RatingsReviews ratingsReviews = scrapRatingsReviews(doc, jsonProduct);

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setCategories(categories)
            .setDescription(description)
            .setOffers(offers)
            .setRatingReviews(ratingsReviews)
            .setEans(List.of(ean))
            .build();

         return product;
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return null;
   }

   private String scrapName(Document doc, JSONObject jsonProduct) {
      String brand = jsonProduct.optString("brand");
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "#titulo-produto", false);

      if (brand != null && !brand.isEmpty() && name != null && !name.toLowerCase(Locale.ROOT).contains(brand.toLowerCase(Locale.ROOT))) {
         name = brand.trim() + " " + name.trim();
      }

      return name;
   }

   private RatingsReviews scrapRatingsReviews(Document doc, JSONObject jsonProduct) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      JSONObject aggregateRating = jsonProduct.optJSONObject("aggregateRating");

      if (aggregateRating != null) {
         int totalComments = aggregateRating.optInt("reviewCount", 0);
         double avgRating = aggregateRating.optDouble("ratingValue", 0);

         ratingReviews.setTotalRating(totalComments);
         ratingReviews.setTotalWrittenReviews(totalComments);
         ratingReviews.setAverageOverallRating(avgRating);
      }

      return ratingReviews;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      String sales = CrawlerUtils.calculateSales(pricing);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(Collections.singletonList(sales))
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".valor .preco", null, true, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".valor .valor-de", null, true, ',', session);
      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(BankSlip.BankSlipBuilder.create().setFinalPrice(spotlightPrice).setOnPageDiscount(0d).build())
         .build();
   }

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = scrapInstallments(doc, spotlightPrice);
      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false).build());
      }
      return creditCards;
   }

   private Installments scrapInstallments(Document doc, Double spotlightPrice) throws MalformedPricingException {
      Installments installments = new Installments();
      installments.add(
         Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice).build());

      if (doc.selectFirst(".ou-sem-juros") != null) {
         String installmentString = CrawlerUtils.scrapStringSimpleInfo(doc, ".ou-sem-juros", true);
         if (installmentString != null && installmentString.contains("em")) {
            String installmentSanitized = installmentString.split("em")[1].trim();
            Pair<Integer, Float> installment = CrawlerUtils.crawlSimpleInstallmentFromString(installmentSanitized, "x", "", true);
            if (!installment.isAnyValueNull()) {
               int parcels = installment.getFirst();
               Float price = installment.getSecond();
               installments.add(Installment.InstallmentBuilder.create()
                  .setInstallmentNumber(parcels)
                  .setInstallmentPrice(price.doubleValue())
                  .build());
            }
         }
      }

      return installments;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("#mainCenterProdutos") != null;
   }
}
