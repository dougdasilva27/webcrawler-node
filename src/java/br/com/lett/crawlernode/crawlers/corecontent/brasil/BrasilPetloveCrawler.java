package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * date: 27/03/2018
 *
 * @author gabriel
 */

public class BrasilPetloveCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.petlove.com.br";
   private static final String SELLER_FULL_NAME = "petlove";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilPetloveCrawler(Session session) {
      super(session);
   }

   @Override
   protected Object fetch() {
      List<String> proxies = List.of(ProxyCollection.BUY_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_ANY_HAPROXY);
      int attempt = 0;
      boolean succes = false;
      Document doc = new Document("");
      do {
         try {
            webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), proxies.get(attempt), session);
            if (webdriver != null) {
               doc = Jsoup.parse(webdriver.getCurrentPageSource());
               succes = !doc.select("section.product.flex").isEmpty();
               webdriver.terminate();
            }
         } catch (Exception e) {
            Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));
            Logging.printLogWarn(logger, "Page not captured");
         }
      } while (!succes && attempt++ <= (proxies.size() - 1));

      return doc;
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String nuxtData = doc.selectFirst("script:containsData(window.__NUXT__)").data();
         String nuxtDataSanitized = CommonMethods.getLast(nuxtData.split("}\\(\"\",true,void 0,\"id\",.{2,20}\"")).replace("));", "");
         JSONObject productJson = CrawlerUtils.stringToJSONObject(doc.selectFirst("script:containsData(\"@type\":\"Product\")").data());
         String internalPid = productJson.optString("sku");
         CategoryCollection categories = crawlCategories(doc);
         String description = crawlDescription(doc);
         RatingsReviews ratingReviews = crawlRating(productJson);

         JSONArray variantOffers = JSONUtils.getValueRecursive(productJson, "offers.offers", ".", JSONArray.class, new JSONArray());
         List<String> variantSkus = JSONUtils.jsonArrayToStringList(variantOffers, "sku");
         variantSkus.removeIf(sku -> !nuxtDataSanitized.matches(".*\\d{2,20},\"" + sku + "\".*"));
         List<String> variantGrammature = doc.select(".variant-list-modal .font-bold").eachText();
         Map<String, String> variantMap = IntStream.range(0, variantSkus.size()).boxed().collect(Collectors.toMap(variantSkus::get, variantGrammature::get));

         for (Object obj : variantOffers) {
            if (obj instanceof JSONObject) {
               JSONObject jsonSku = (JSONObject) obj;

               String url = HOME_PAGE + jsonSku.optString("url");
               String internalId = JSONUtils.getStringValue(jsonSku, "sku");
               String name = productJson.optString("name") + " " + variantMap.get(internalId);
               String primaryImage = crawlPrimaryImage(internalId, nuxtDataSanitized);
               List<String> secondaryImages = crawlSecondaryImages(internalId, nuxtDataSanitized, primaryImage);
               boolean available = jsonSku.optString("availability") != null && jsonSku.optString("availability").equals("https://schema.org/InStock");
               Offers offers = available ? scrapOffers(jsonSku, doc, url) : new Offers();

               // Creating the product
               Product product = ProductBuilder.create()
                  .setUrl(url)
                  .setRatingReviews(ratingReviews)
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setCategory1(categories.getCategory(0))
                  .setCategory2(categories.getCategory(1))
                  .setCategory3(categories.getCategory(2))
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
                  .setDescription(description)
                  .setOffers(offers)
                  .build();

               products.add(product);
            }
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".product__content") != null;
   }

   private RatingsReviews crawlRating(JSONObject productJson) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = JSONUtils.getValueRecursive(productJson, "aggregateRating.reviewCount", ".", Integer.class, null);
      Integer totalWrittenReviews = JSONUtils.getValueRecursive(productJson, "review", ".", JSONArray.class, new JSONArray()).length();
      Double avgRating = JSONUtils.getDoubleValueFromJSON(productJson.optJSONObject("aggregateRating"), "ratingValue", true);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalWrittenReviews);
      ratingReviews.setAverageOverallRating(avgRating);

      return ratingReviews;
   }

   private String crawlPrimaryImage(String internalId, String nuxtDataSanitized) {

      String regex = "(https:\\\\u002F\\\\u002Fwww.petlove.com.br\\\\u002Fimages\\\\u002Fproducts\\\\u002F\\d{2,10}\\\\u002Fproduct\\\\u002F.{2,220}" + internalId + ".{2,50})\",";
      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      Matcher matcher = pattern.matcher(nuxtDataSanitized);

      if (matcher.find()) {
         return matcher.group(1).replace("\\u002F", "/");
      }

      String regexSingleImage = "(https:\\\\u002F\\\\u002Fwww.petlove.com.br\\\\u002Fimages\\\\u002Fproducts\\\\u002F\\d{2,10}\\\\u002Fproduct\\\\u002F.{2,220})\",";
      Pattern patternSingleImage = Pattern.compile(regexSingleImage, Pattern.MULTILINE);
      Matcher matcherSingleImage = patternSingleImage.matcher(nuxtDataSanitized);

      return matcherSingleImage.find() ? matcherSingleImage.group(1).replace("\\u002F", "/") : null;
   }

   private List<String> crawlSecondaryImages(String internalId, String nuxtDataSanitized, String primaryImage) {
      List<String> secondaryImages = new ArrayList<>();

      String regex = "(https:\\\\u002F\\\\u002Fwww.petlove.com.br\\\\u002Fimages\\\\u002Fproducts\\\\u002F\\d{2,10}\\\\u002Fproduct\\\\u002F.{2,220}" + internalId + ".{2,50})\",";
      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      Matcher matcher = pattern.matcher(nuxtDataSanitized);
      while (matcher.find()) {
         String secondaryImage = matcher.group(1).replace("\\u002F", "/");
         if (!secondaryImage.equals(primaryImage)) {
            secondaryImages.add(secondaryImage);
         }
      }

      if (secondaryImages.isEmpty()) {
         String regexSingleImage = "(https:\\\\u002F\\\\u002Fwww.petlove.com.br\\\\u002Fimages\\\\u002Fproducts\\\\u002F\\d{2,10}\\\\u002Fproduct\\\\u002F.{2,220})\",";
         Pattern patternSingleImage = Pattern.compile(regexSingleImage, Pattern.MULTILINE);
         Matcher matcherSingleImage = patternSingleImage.matcher(nuxtDataSanitized);

         while (matcherSingleImage.find()) {
            String secondaryImage = matcherSingleImage.group(1).replace("\\u002F", "/");
            if (!secondaryImage.equals(primaryImage)) {
               secondaryImages.add(secondaryImage);
            }
         }
      }

      return secondaryImages;
   }

   private CategoryCollection crawlCategories(Document document) {
      CategoryCollection categories = new CategoryCollection();
      Elements elementCategories = document.select(".product__breadcrumbs.row > ul > li > a");

      for (Element e : elementCategories) {
         String cat = e.ownText().trim();

         if (!cat.isEmpty()) {
            categories.add(cat);
         }
      }

      return categories;
   }


   private String crawlDescription(Document doc) {
      StringBuilder description = new StringBuilder();

      Element elementInfoDescription = doc.selectFirst("section .card-accordion__summary:contains(Informações)");

      if (elementInfoDescription != null) {
         description.append(elementInfoDescription.html());
      }

      Element elementSpecificationDescription = doc.selectFirst("section .card-accordion__summary:contains(Especificações)");

      if (elementSpecificationDescription != null) {
         description.append(elementSpecificationDescription.html());
      }

      return description.toString();
   }


   private Offers scrapOffers(JSONObject skuJson, Document doc, String urlVariant) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      List<String> sales = scrapSales(doc);
      Pricing pricing = scrapPricing(skuJson, urlVariant);

      offers.add(OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private List<String> scrapSales(Document doc) {
      List<String> sales = new ArrayList<>();

      Elements discounts = doc.select(".badge__text:contains(%)");

      for (Element discount : discounts) {
         if (discount != null && !discount.text().isEmpty()) {
            sales.add(discount.text());
         }

      }

      return sales;
   }

   private Double getPriceFrom(String url) {
      Response response = dataFetcher.get(session, Request.RequestBuilder.create().setCookies(cookies).setUrl(url).build());

      Document variantDoc = Jsoup.parse(response.getBody());
      Double priceFrom = null;

      if (variantDoc.select(".font-body.font-medium.color-neutral").size() > 1) {
         priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(variantDoc, ".font-body.font-medium.color-neutral", null, false, ',', session);
      }

      return priceFrom;
   }

   private Pricing scrapPricing(JSONObject skuJson, String urlVariant) throws MalformedPricingException {
      Double priceFrom = getPriceFrom(urlVariant);
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(skuJson, "price", true);

      if (priceFrom != null && priceFrom.equals(spotlightPrice)) {
         priceFrom = null;
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      installments.add(InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      for (String card : cards) {
         creditCards.add(CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }
}
