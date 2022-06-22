package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class BrasilPalacioDasFerramentas extends Crawler {
   private static String SELLER_FULL_NAME = "Palácio das ferramentas";
   private static String HOST = "www.palaciodasferramentas.com.br/";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.ELO.toString(), Card.DINERS.toString());
   private Integer pageRating = 1;
   private Integer star1 = 0;
   private Integer star2 = 0;
   private Integer star3 = 0;
   private Integer star4 = 0;
   private Integer star5 = 0;

   public BrasilPalacioDasFerramentas(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      Product product = null;
      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String name;
         String internalId = scrapInternalId(doc);
         String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, "[itemprop=\"sku\"]", false);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "img#mainImage", Arrays.asList("data-big"), "https", HOST);
         List<String> images = CrawlerUtils.scrapSecondaryImages(doc, "ul#productImages img", Arrays.asList("data-big"), "https", HOST, primaryImage);
         String description = CrawlerUtils.scrapStringSimpleInfo(doc, "div.descricao", false);
         Boolean available = isAvailable(doc);
         Offers offers = available != null && available ? scrapOffers(doc) : new Offers(); // I did not found any product having price or avaibility differente because volts or model
         RatingsReviews ratings = crawlRating(doc, internalId);

         Elements variations = getVariations(doc);
         if (!variations.isEmpty()) {
            for (Element variation : variations) {
               String voltsOrModel = scrapVoltsOrModels(variation);
               name = scrapName(doc, voltsOrModel);
               internalId += voltsOrModel;
               product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(images)
                  .setDescription(description)
                  .setRatingReviews(ratings)
                  .setOffers(offers)
                  .build();

               products.add(product);
            }
         } else {
            name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1[itemprop=\"name\"]", false);
            product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(images)
               .setDescription(description)
               .setRatingReviews(ratings)
               .setOffers(offers)
               .build();

            products.add(product);

         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("li.product") != null;
   }

   private String scrapName(Document doc, String voltsOrModel) {
      StringBuilder stringBuilder = new StringBuilder();
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1[itemprop=\"name\"]", false);

      if (name != null) {
         stringBuilder.append(name);
         if (voltsOrModel != null && !name.contains(voltsOrModel)) {
            stringBuilder.append(" - ").append(voltsOrModel);
         }
      }

      return stringBuilder.toString();

   }

   private String scrapInternalId(Document doc) {
      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "button#Buy", "data-item");

      if (internalId == null) {
         internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".comentar button", "data-url");

         internalId = internalId != null ? internalId.replaceAll("[^0-9]", "") : null;

      }
      return internalId;
   }

   private String scrapVoltsOrModels(Element variation) {
      String voltsOrModel = CrawlerUtils.scrapStringSimpleInfo(variation, null, true);

      if (voltsOrModel != null) {
         voltsOrModel = voltsOrModel.replace("indisponível","");

      }

      return voltsOrModel;
   }

   private Boolean isAvailable(Document doc) {
      return doc.select(".aviseme").isEmpty();
   }

   private Elements getVariations(Document doc) {
      Elements variations = doc.select(".voltagem option:not(:first-child)");
      if (variations.isEmpty()) {
         variations = doc.select(".modelo option:not(:first-child)");
      }

      return variations;

   }

   private Offers scrapOffers(Document doc) {
      Offers offers = new Offers();
      try {
         Pricing pricing = scrapPricing(doc);
         List<String> sales = scrapSales(pricing);

         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_FULL_NAME)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(sales)
            .build());

      } catch (Exception e) {
         Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
      }
      return offers;

   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = convertPrice(doc, "[itemprop=\"offers\"] li.de strong", null);
      Double spotlightPrice = convertPrice(doc, "[itemprop=\"offers\"] li.por strong", null);
      Double priceBankSlip = CrawlerUtils.scrapDoublePriceFromHtml(doc, "li.price [itemprop=\"price\"]", "content", false, '.', session);

      if (Objects.equals(priceFrom, spotlightPrice)) priceFrom = null;

      CreditCards creditCards = scrapCreditCards(doc);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(priceBankSlip)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private CreditCards scrapCreditCards(Document doc) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Elements installmentsInfo = doc.select("li.parcelamento ul li");
      Installments installments = new Installments();

      for (Element installment : installmentsInfo) {
         String installmentNumber = CrawlerUtils.scrapStringSimpleInfo(installment, "li strong:nth-of-type(1)", false);

         if (installmentNumber != null) {
            installmentNumber = installmentNumber.replace("x", "");

            installments.add(Installment.InstallmentBuilder.create()
               .setInstallmentNumber(Integer.parseInt(installmentNumber))
               .setInstallmentPrice(convertPrice(null, "li strong:nth-of-type(2)", installment))
               .build());
         }
      }

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private RatingsReviews crawlRating(Document doc, String internalId) {
      RatingsReviews ratingsReviews = new RatingsReviews();
      Number avgReviews = CrawlerUtils.scrapIntegerFromHtmlAttr(doc, "div.rating [itemprop=\"ratingValue\"]", "content", 0);
      Integer totalReviews = CrawlerUtils.scrapIntegerFromHtmlAttr(doc, "[itemprop=\"reviewCount\"]", "content", 0);

      Document ratingResponse = fetchRatings(internalId);
      Elements comments = ratingResponse.select("body > li");
      Integer currentRating = 0;

      while (comments != null && !comments.isEmpty() && currentRating < totalReviews) {
         for (Element comment : comments) {
            scrapAdvancedRatingReview(comment);
            currentRating++;
         }
         pageRating++;
         ratingResponse = fetchRatings(internalId);
         comments = ratingResponse.select("body > li");
      }

      AdvancedRatingReview advancedRatingReview = new AdvancedRatingReview.Builder()
         .totalStar1(star1)
         .totalStar2(star2)
         .totalStar3(star3)
         .totalStar4(star4)
         .totalStar5(star5)
         .build();

      ratingsReviews.setTotalRating(totalReviews);
      ratingsReviews.setTotalWrittenReviews(totalReviews);
      ratingsReviews.setAverageOverallRating(Objects.isNull(avgReviews) ? null : avgReviews.doubleValue());
      ratingsReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingsReviews;
   }

   private void scrapAdvancedRatingReview(Element doc) {
      Integer star = CrawlerUtils.scrapIntegerFromHtml(doc, "li.rating option[selected=\"selected\"]", false, 0);

      if (star != null) {
         switch (star) {
            case 1:
               star1++;
               break;
            case 2:
               star2++;
               break;
            case 3:
               star3++;
               break;
            case 4:
               star4++;
               break;
            case 5:
               star5++;
               break;
         }
      }
   }

   protected Document fetchRatings(String internalId) {
      Map<String, String> headers = new HashMap<>();
      headers.put("Accept", "*/*");
      headers.put("Accept-Encoding", "gzip, deflate, br");
      headers.put("Connection", "keep-alive");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.palaciodasferramentas.com.br/load/comentarios/" + internalId + "?page=" + pageRating)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.BUY_HAPROXY))
         .build();

      Response response = this.dataFetcher.get(session, request);

      return Jsoup.parse(response.getBody());
   }

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();

      String saleDiscount = CrawlerUtils.calculateSales(pricing);

      if (saleDiscount != null) {
         sales.add(saleDiscount);
      }

      return sales;
   }

   private Double convertPrice(Document doc, String css, Element e) {
      String price = null;

      if (doc != null) {
         price = CrawlerUtils.scrapStringSimpleInfo(doc, css, false);
      } else if (e != null) {
         price = CrawlerUtils.scrapStringSimpleInfo(e, css, false);
      }

      if (price != null) {
         price = price.replace("R$", "").trim();

         return MathUtils.parseDoubleWithComma(price);
      }
      return null;
   }
}

