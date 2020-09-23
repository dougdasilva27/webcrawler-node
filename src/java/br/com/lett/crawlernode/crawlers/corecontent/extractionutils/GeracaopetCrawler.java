package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing.PricingBuilder;

public class GeracaopetCrawler extends Crawler {

   private static final String SELLER_NAME = "PETLAND";

   protected String cep;
   protected String storeId;

   public GeracaopetCrawler(Session session, String cep, String storeId) {
      super(session);
      this.cep = cep;
      this.storeId = storeId;
      super.config.setMustSendRatingToKinesis(true);
   }

   @Override
   public void handleCookiesBeforeFetch() {
      BasicClientCookie cookie = new BasicClientCookie("zipcode", cep);
      cookie.setDomain(".www.geracaopet.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      if (doc.selectFirst("#__NEXT_DATA__") != null) {
         JSONObject jsonObject = JSONUtils.stringToJson(doc.selectFirst("#__NEXT_DATA__").data());
         Logging
               .printLogDebug(logger, session, "Product page identified: " + session.getOriginalURL());
         JSONObject props = JSONUtils.getJSONValue(jsonObject, "props");
         JSONObject pageProps = JSONUtils.getJSONValue(props, "pageProps");
         JSONObject content = JSONUtils.getJSONValue(pageProps, "content");
         JSONObject data = JSONUtils.getJSONValue(content, "data");

         String internalPid = data.optString("sku");
         String description = data.optString("description");
         List<String> images = CrawlerUtils.scrapImagesListFromJSONArray(data.optJSONArray("imageGallery"), "file", null, "https", "www.geracaopet.com.br", session);
         String primaryImage = images.isEmpty() ? null : images.remove(0);
         String secondaryImages = images.isEmpty() ? null : CommonMethods.listToJSONArray(images).toString();

         JSONArray children = JSONUtils.getJSONArrayValue(data, "children");
         JSONObject pricesJson = fetchPrices(internalPid);

         children.forEach(obj -> {
            JSONObject skuJson = (JSONObject) obj;

            String name = skuJson.optString("name", null);
            String internalId = skuJson.optString("sku", null);
            List<String> eans = Arrays.asList(internalId);
            boolean available = fetchAvaiability(internalId);

            try {
               Offers offers = available ? scrapOffers(pricesJson, internalId) : new Offers();

               Product product = ProductBuilder.create()
                     .setUrl(session.getOriginalURL())
                     .setInternalId(internalId)
                     .setInternalPid(internalPid)
                     .setOffers(offers)
                     .setName(name)
                     .setCategory1(null)
                     .setCategory2(null)
                     .setCategory3(null)
                     .setPrimaryImage(primaryImage)
                     .setSecondaryImages(secondaryImages)
                     .setDescription(description)
                     .setEans(eans)
                     .setRatingReviews(new RatingsReviews())
                     .build();

               products.add(product);
            } catch (Exception ex) {
               throw new RuntimeException(ex);
            }
         });
      } else {
         products.addAll(new GeracaopetOldCrawler(session, cep, logger, dataFetcher).extractInformation(doc));
      }

      return products;
   }

   private JSONObject fetchPrices(String sku) {
      Request req = RequestBuilder.create()
            .setUrl("https://api.geracaopet.com.br/api/V2/catalogs/products/prices?"
                  + "storeId=" + storeId + "&sku=" + sku)
            .setCookies(cookies)
            .build();

      return JSONUtils.stringToJson(this.dataFetcher.get(session, req).getBody());
   }

   private boolean fetchAvaiability(String sku) {
      Request req = RequestBuilder.create()
            .setUrl("https://api.geracaopet.com.br/api/V2/catalogs/products/delivery-stocks"
                  + "?postalCode=" + this.cep + "&storeId=" + storeId + "&sku=" + sku)
            .setCookies(cookies)
            .build();

      JSONObject avJson = JSONUtils.stringToJson(this.dataFetcher.get(session, req).getBody());

      JSONObject data = avJson.optJSONObject("data");
      boolean delivery = data.optBoolean("deliveryAvailable");
      boolean expressDeliveryAvailable = data.optBoolean("expressDeliveryAvailable");
      boolean pickupAvailable = data.optBoolean("pickupAvailable");

      return delivery || expressDeliveryAvailable || pickupAvailable;
   }

   private Offers scrapOffers(JSONObject json, String internalId) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();

      JSONArray skus = json.optJSONArray("data");

      for (Object obj : skus) {
         JSONObject skuJson = (JSONObject) obj;

         String sku = skuJson.optString("sku", "");

         if (sku.equalsIgnoreCase(internalId)) {
            Double specialPrice = skuJson.optDouble("specialPrice", 0d);
            Double price = skuJson.optDouble("price", 0d);

            offers.add(OfferBuilder.create()
                  .setPricing(PricingBuilder.create()
                        .setPriceFrom(specialPrice > 0d ? price : null)
                        .setSpotlightPrice(specialPrice > 0d ? specialPrice : price)
                        .setBankSlip(BankSlipBuilder
                              .create()
                              .setFinalPrice(specialPrice > 0d ? specialPrice : price)
                              .build())
                        .setCreditCards(new CreditCards(extCreditCards(specialPrice > 0d ? specialPrice : price)))
                        .build())
                  .setIsMainRetailer(true)
                  .setSellerFullName(SELLER_NAME)
                  .setIsBuybox(false)
                  .setUseSlugNameAsInternalSellerId(true)
                  .build());
         }
      }

      return offers;
   }

   private List<CreditCard> extCreditCards(Double price) {
      return Stream.of(Card.VISA, Card.MASTERCARD, Card.ELO)
            .map(card -> {
               try {
                  return CreditCard.CreditCardBuilder.create()
                        .setIsShopCard(false)
                        .setBrand(card.toString())
                        .setInstallments(new Installments(
                              Collections.singleton(InstallmentBuilder
                                    .create()
                                    .setInstallmentNumber(1)
                                    .setInstallmentPrice(price)
                                    .setFinalPrice(price)
                                    .build())
                        ))
                        .build();
               } catch (MalformedPricingException e) {
                  throw new RuntimeException(e);
               }
            })
            .collect(Collectors.toList());
   }
}