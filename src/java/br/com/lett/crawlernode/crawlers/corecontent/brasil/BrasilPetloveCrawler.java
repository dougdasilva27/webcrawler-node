package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
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
import java.util.Set;

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
      return fetchDocumentVariation(session.getOriginalURL());
   }

   private Document fetchDocumentVariation(String url) {
      List<String> proxies = List.of(ProxyCollection.BUY_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_ANY_HAPROXY);
      int attempt = 0;
      boolean succes = false;
      Document doc = new Document("");
      do {
         try {
            webdriver = DynamicDataFetcher.fetchPageWebdriver(url, proxies.get(attempt), session);
            if (webdriver != null) {
               doc = Jsoup.parse(webdriver.getCurrentPageSource());
               succes = !doc.select("section.product.flex").isEmpty();
            }
         } catch (Exception e) {
            Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));
            Logging.printLogWarn(logger, "Page not captured");
         } finally {
            if (webdriver != null) {
               webdriver.terminate();
            }
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

         JSONObject productJson = CrawlerUtils.stringToJSONObject(doc.selectFirst("script:containsData(\"@type\":\"Product\")").data());

         JSONArray variantOffers = JSONUtils.getValueRecursive(productJson, "offers.offers", ".", JSONArray.class, new JSONArray());
         String internalPid = productJson.optString("sku");
         String description = crawlDescription(doc);
         CategoryCollection categories = crawlCategories(doc);
         RatingsReviews ratingReviews = crawlRating(productJson);

         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product__header h1", true);

         for (Object obj : variantOffers) {
            JSONObject jsonSku = (JSONObject) obj;
            String productUrl = HOME_PAGE + jsonSku.optString("url");
            Document docVariant = fetchDocumentVariation(productUrl);

            if (docVariant.select("div.alert__text").isEmpty()) {
               String internalId = JSONUtils.getStringValue(jsonSku, "sku");
               String variantName = scrapName(docVariant, name);
               List<String> images = scrapImages(docVariant);
               String primaryImage = !images.isEmpty() ? images.remove(0) : null;

               boolean isAvailable = docVariant.select(".my-8 .button.button--secondary").isEmpty();
               Offers offers = isAvailable ? scrapOffers(docVariant) : new Offers();

               Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setName(variantName)
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(images)
                  .setDescription(description)
                  .setCategories(categories)
                  .setRatingReviews(ratingReviews)
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

   private String scrapName(Document docVariant, String name) {
      String nameVariant = CrawlerUtils.scrapStringSimpleInfo(docVariant, ".variant-list .variant-selector__card--selected .font-bold", true);
      if (nameVariant != null && !name.contains(nameVariant)) {
         return name + " - " + nameVariant;
      }
      return name;
   }

   private List<String> scrapImages(Document docVariant) {
      List<String> imageList = new ArrayList<>();
      Elements images = docVariant.select("[datatest-id=\"mini-img\"] img");
      for (Element image : images) {
         String img = CrawlerUtils.scrapStringSimpleInfoByAttribute(image, "img", "src");
         img = img.replaceAll("mini", "hd_no_extent");
         imageList.add(img);
      }
      return imageList;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("section.product.flex") != null;
   }

   private RatingsReviews crawlRating(JSONObject productJson) {
      JSONArray review = productJson.optJSONArray("review");
      RatingsReviews ratingReviews = new RatingsReviews();
      if (review.length() > 0) {
         ratingReviews.setDate(session.getDate());

         Integer totalNumOfEvaluations = JSONUtils.getValueRecursive(productJson, "aggregateRating.reviewCount", ".", Integer.class, null);
         Integer totalWrittenReviews = JSONUtils.getValueRecursive(productJson, "review", ".", JSONArray.class, new JSONArray()).length();
         Double avgRating = JSONUtils.getDoubleValueFromJSON(productJson.optJSONObject("aggregateRating"), "ratingValue", true);

         ratingReviews.setTotalRating(totalNumOfEvaluations);
         ratingReviews.setTotalWrittenReviews(totalWrittenReviews);
         ratingReviews.setAverageOverallRating(avgRating);
      }

      return ratingReviews;
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

   private Offers scrapOffers(Document docVariant) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      List<String> sales = scrapSales(docVariant);
      Pricing pricing = scrapPricing(docVariant);

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

   private Pricing scrapPricing(Document docVariant) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(docVariant, ".product__call-to-action-wrapper .mb-2 .font-body", null, true, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(docVariant, ".product__call-to-action-wrapper .font-title-xs", null, true, ',', session);

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
