package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BrasilDafitiCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.dafiti.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "dafiti";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilDafitiCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         Elements elementPreName = doc.select("h1.product-name");
         String preName = elementPreName.text().replace("'", "").replace("’", "").trim();
         CategoryCollection categoryCollection = CrawlerUtils.crawlCategories(doc, "ul.breadcrumb2", true);
         Elements elementPrimaryImage = doc.select(".gallery-thumbs ul.carousel-items").select("a");
         String primaryImage = null;
         String secondaryImages = null;
         JSONArray secondaryImagesArray = new JSONArray();

         for (Element e : elementPrimaryImage) {

            if (primaryImage == null) {
               primaryImage = e.attr("data-img-zoom");
            } else {
               secondaryImagesArray.put(e.attr("data-img-zoom"));
            }

         }

         if (secondaryImagesArray.length() > 0) {
            secondaryImages = secondaryImagesArray.toString();
         }

         String description = CrawlerUtils.scrapStringSimpleInfo(doc, ".box-description", false);
         Element elementSku = doc.select("#add-to-cart input[name=p]").first();

         try {
            String sku = elementSku.attr("value");
            String url = "https://www.dafiti.com.br/catalog/detailJson?sku=" + sku + "&_=1439492531368";
            Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
            JSONObject json = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
            JSONArray sizes = json.has("sizes") ? json.getJSONArray("sizes") : new JSONArray();

            for (int i = 0; i < sizes.length(); i++) {

               String internalId = sizes.getJSONObject(i).getString("sku");
               String internalPid = internalId.split("-")[0];
               String name = preName + " (tamanho " + sizes.getJSONObject(i).getString("name") + ")";

               RatingsReviews ratingReviews = scrapRatingReviews(sku);

               Offers offers = scrapOffers(doc, json);

               Product product = ProductBuilder.create()
                  .setUrl(this.session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setCategories(categoryCollection)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
                  .setDescription(description)
                  .setOffers(offers)
                  .setRatingReviews(ratingReviews)
                  .build();

               products.add(product);

            }
         } catch (Exception e1) {
            e1.printStackTrace();
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private Offers scrapOffers(Document doc, JSONObject json) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc, json);

      String sellerName = MAIN_SELLER_NAME_LOWER;
      Element sellerNameElement = doc.select(".product-seller-name-link").first();

      if (sellerNameElement != null) {
         sellerName = sellerNameElement.ownText().toLowerCase();
      }


      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(sellerName)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(sellerName.toLowerCase(Locale.ROOT).equals(MAIN_SELLER_NAME_LOWER))
         .setPricing(pricing)
         .build());

      return offers;

   }

   /***********
    * Product page identification *
    ***********/

   private boolean isProductPage(Document document) {
      return (document.select(".container.product-page").first() != null);
   }

   private Pricing scrapPricing(Document doc, JSONObject skuJson) throws MalformedPricingException {

      Pricing pricing = null;

      Element priceElement = doc.select(".catalog-detail-price-value").first();

      if (priceElement != null) {
         Double spotlightPrice = MathUtils.parseDoubleWithComma(priceElement.ownText());
         Double priceFrom = skuJson.has("specialPrice") ? MathUtils.parseDoubleWithComma(skuJson.optString("specialPrice")) : null;
         CreditCards creditCards = scrapCard(skuJson);

         BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
            .setFinalPrice(spotlightPrice)
            .build();

         pricing = Pricing.PricingBuilder.create()
            .setSpotlightPrice(spotlightPrice)
            .setPriceFrom(priceFrom)
            .setCreditCards(creditCards)
            .setBankSlip(bankSlip)
            .build();
      }

      return pricing;
   }

   private CreditCards scrapCard(JSONObject skuJson) throws MalformedPricingException {

      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      if (skuJson.has("installments")) {
         JSONObject installmentsObj = skuJson.getJSONObject("installments");

         if (installmentsObj.has("count") && installmentsObj.has("value")) {
            String installment = installmentsObj.get("count").toString().replaceAll("[^0-9]", "").trim();
            Double priceInstallment = MathUtils.parseDoubleWithComma(installmentsObj.get("value").toString());

            if (!installment.isEmpty() && priceInstallment != null) {
               installments.add(Installment.InstallmentBuilder.create()
                  .setInstallmentNumber(Integer.parseInt(installment))
                  .setInstallmentPrice(priceInstallment)
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
      }
      return creditCards;
   }

   private RatingsReviews scrapRatingReviews(String sku) {
      String apiURL = "https://trustvox.com.br/widget/root?code=" + sku + "&store_id=113911&url=" + session.getOriginalURL();
      RatingsReviews ratingReviews = new RatingsReviews();
      HttpResponse<String> response;

      try {
         HttpClient client = HttpClient.newBuilder().build();
         HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .header("Accept", "application/vnd.trustvox-v2+json")
            .header("Referer", "https://www.dafiti.com.br/")
            .uri(URI.create(apiURL))
            .build();
         response = client.send(request, HttpResponse.BodyHandlers.ofString());

      } catch (Exception e) {
         throw new RuntimeException("Failed in load document: " + session.getOriginalURL(), e);
      }

      JSONObject json = CrawlerUtils.stringToJson(response.body());
      JSONObject storeRate = json.optJSONObject("rate");

      if (storeRate != null) {
         Double avgRating = MathUtils.parseDoubleWithDot(storeRate.optString("average"));
         int count = storeRate.optInt("count");
         ratingReviews.setAverageOverallRating(avgRating);
         ratingReviews.setTotalRating(count);
         ratingReviews.setTotalWrittenReviews(count);
      }

      return ratingReviews;
   }

}
