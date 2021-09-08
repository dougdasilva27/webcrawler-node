package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
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
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.*;
import models.pricing.Pricing.PricingBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;

public class BrasilRiachueloCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.riachuelo.com.br/";
   private static final String SELLER_FULL_NAME = "Riachuelo";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());


   private static final String X_API_KEY = "KhMO3jH1hsjvSRzQXNfForv5FrnfSpX6StdqMmjncjGivPBj3MS4kFzRWn2j7MPn";
   private static final String TOKEN_KEY = "SjqJQstBFWjIqzYzP73umkNHT7RTeWcHanVu1K7mGYHrqIskym+BvChLueA0qnAstBZzgVcwOt/UNlU1wXbhJ7ta6/8esxROylJS6kTk3VEw1l3QBHijzGk/CF8afz1HmOHFFQ4u/+N7+GqJ1Pax8BmrOt3KitkBF47zyxMagTAUruSogIx0A/ib7JtSUvDHLi53MRlODpjG/Pezkm/EhhczAjYk2+3bRWMu0/nk3KknXXoO+SDf826ukLDpkfjwg8OUYOTWdvt5X7WiuspIB2E5ZklYYK8C8hxda3Sy5QaGngElEgzZfZkcC0slJuVMSS3+7F6ysxgKLIX0K1LZPZALGe7BtEsCKMDv9L2LarGUzZkOJT9X6kFa3wsQj3YggZtIGASIznkWWUg0hhrX+FzWsvjwhxvCaX4LYpXQ2byA9lmlliZ1wtf0ZvNmrjc01tzvZHfm67PdqO3VHqK+tEhlVdTZuQlWb4ekExpkyoKpZnkqSVdpQ/LkemnKgzVmah00EvCWOJhFgEzqxxTCRobzBoUKNmj/ZSg51H/3e95+Xxdpf0Y5+TIpuWyq79tY3ZxQcUceF0dQUQlptRIlOjzt9jGHyYrO5El3PwAH1FOvyQialAomF2mjo2ffa73l9d6IN+8H+6s5dVUYsT9FCqeO1RKveZcWQ5TEVe+Y5lw=";

   public BrasilRiachueloCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   protected Document fetch() {
      return Jsoup.parse(fetchPage(session.getOriginalURL(), session));
   }

   public String fetchPage(String url, Session session) {
      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
      headers.put("connection", "keep-alive");
      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.182 Safari/537.36");
      Request request = RequestBuilder.create()
         .setUrl(url)
         .setIgnoreStatusCode(false)
         .mustSendContentEncoding(false)
         .setHeaders(headers)
         .setFetcheroptions(
            FetcherOptionsBuilder.create()
               .mustUseMovingAverage(false)
               .mustRetrieveStatistics(true)
               .setForbiddenCssSelector("#px-captcha")
               .build()
         ).build();

      Response response = this.dataFetcher.get(session, request);

      return response.getBody();
   }

   private JSONObject fetchVariations() {
      String url = "https://api-dc-rchlo-prd.riachuelo.com.br/ecommerce-web-catalog/v2/products/variations?slug=";
      url += session.getOriginalURL().substring(28);

      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "*/*");
      headers.put("accept-encoding", "no");
      headers.put("connection", "keep-alive");
      headers.put("x-api-key", X_API_KEY);
      headers.put("x-app-token", getToken());

      Request request = RequestBuilder.create()
         .setUrl(url)
         .setIgnoreStatusCode(false)
         .mustSendContentEncoding(false)
         .setHeaders(headers)

         .build();
      String jsonStr = this.dataFetcher.get(session, request).getBody();

      return JSONUtils.stringToJson(jsonStr);
   }

   private String getToken() {
      String token = "";
      String url = "https://9hyxh9dsj1.execute-api.us-east-1.amazonaws.com/v1/bf60cb91-a86d-4a68-86eb-46855b4738c8/get-token";

      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "*/*");
      headers.put("connection", "keep-alive");
      headers.put("x-api-key", X_API_KEY);

      JSONObject payload = new JSONObject();
      payload.put("value", TOKEN_KEY);

      Request request = RequestBuilder.create()
         .setUrl(url)
         .setIgnoreStatusCode(false)
         .mustSendContentEncoding(false)
         .setHeaders(headers)
         .setPayload(payload.toString())
         .build();

      JSONObject json = JSONUtils.stringToJson(this.dataFetcher.post(session, request).getBody());

      if (json != null && !json.isEmpty()) {
         token = json.optString("IdToken");
      }

      return token;
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script[id=__NEXT_DATA__]", null, null, false, false);

         if (json != null && !json.isEmpty()) {
            String internalPid = JSONUtils.getValueRecursive(json, "props.pageProps.data.sku", String.class);
            String description = JSONUtils.getValueRecursive(json, "props.pageProps.data.description", String.class);
            String name = JSONUtils.getValueRecursive(json, "props.pageProps.data.name", String.class);
            List<String> images = scrapImages(json);
            String primaryImage = !images.isEmpty() ? images.remove(0) : null;

            JSONObject jsonVariations = fetchVariations();
            JSONObject variations = jsonVariations.optJSONObject("products");

            if (!jsonVariations.isEmpty()) {
               for (String variationName : variations.keySet()) {

                  if (!variationName.equals("default")) {
                     String variationNameProduct = name + (variationName.contains(":RCHLO") ? " - " + variationName.replace(":RCHLO", "") : "");
                     String internalId = JSONUtils.getValueRecursive(variations, variationName + ".sku", String.class);
                     boolean isAvailable = !jsonVariations.optBoolean("soldOut");

                     Offers offers = isAvailable ? scrapVariationOffers(variations, variationName) : new Offers();

                     Product product = ProductBuilder.create()
                        .setUrl(session.getOriginalURL())
                        .setInternalId(internalId)
                        .setInternalPid(internalPid)
                        .setOffers(offers)
                        .setName(variationNameProduct)
                        .setPrimaryImage(primaryImage)
                        .setSecondaryImages(images)
                        .setDescription(description)
                        .build();

                     products.add(product);
                  }
               }
            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("section[aria-label=Detalhes do produto]") != null;
   }

   private List<String> scrapImages(JSONObject json) {
      List<String> variationName = new ArrayList<>();

      JSONArray jsonProducts = JSONUtils.getValueRecursive(json, "props.pageProps.data.media", JSONArray.class);

      if (jsonProducts != null && !jsonProducts.isEmpty()) {
         for (Object img : jsonProducts) {
            JSONObject imgJson = (JSONObject) img;
            variationName.add(imgJson.optString("url"));
         }
      }

      return variationName;
   }

   public String scrapInternalId(JSONObject jsonConfig, String id) {
      JSONObject skuHtml = jsonConfig.optJSONObject("sku-html");

      return skuHtml.getString(id);
   }

   private Offers scrapVariationOffers(JSONObject json, String variationName) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();

      Pricing pricing = scrapPricing(json, variationName);
      String sales = JSONUtils.getValueRecursive(json, variationName + ".price.discount", String.class);

      offers.add(OfferBuilder.create()
         .setSales(Collections.singletonList(sales))
         .setPricing(pricing)
         .setIsMainRetailer(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setIsBuybox(false)
         .setUseSlugNameAsInternalSellerId(true)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject json, String variationName) throws MalformedPricingException {
      Double spotLightPrice = JSONUtils.getValueRecursive(json, variationName + ".price.final", Double.class);
      Double priceFrom = JSONUtils.getValueRecursive(json, variationName + ".price.regular", Double.class);

      if (priceFrom != null && priceFrom.equals(spotLightPrice)) {
         priceFrom = null;
      }

      return PricingBuilder.create()
         .setSpotlightPrice(spotLightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(BankSlipBuilder
            .create()
            .setFinalPrice(spotLightPrice)
            .build())
         .setCreditCards(scrapCreditCards(json, spotLightPrice, variationName))
         .build();
   }

   private CreditCards scrapCreditCards(JSONObject json, Double spotlightPrice, String variationName) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      JSONArray installmentsArr = JSONUtils.getValueRecursive(json, variationName + ".paymentMethod", JSONArray.class);

      if (installmentsArr != null && !installmentsArr.isEmpty()) {
         for (Object o : installmentsArr) {
            JSONObject installmentsJson = (JSONObject) o;

            if (installmentsJson.optString("type").equals("rchloCreditCard")) {
               Installments shopCardInstallments = new Installments();
               shopCardInstallments.add(Installment.InstallmentBuilder.create()
                  .setInstallmentNumber(installmentsJson.optInt("installments"))
                  .setInstallmentPrice(installmentsJson.optDouble("amount"))
                  .build());

               creditCards.add(CreditCard.CreditCardBuilder.create()
                  .setBrand(installmentsJson.optString("label"))
                  .setInstallments(shopCardInstallments)
                  .setIsShopCard(true)
                  .build());
            } else {
               installments.add(
                  Installment.InstallmentBuilder.create()
                     .setInstallmentNumber(installmentsJson.optInt("installments"))
                     .setInstallmentPrice(installmentsJson.optDouble("amount"))
                     .build()
               );
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
}
