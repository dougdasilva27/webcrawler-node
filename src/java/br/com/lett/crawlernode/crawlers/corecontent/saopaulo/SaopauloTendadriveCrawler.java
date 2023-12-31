package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Date: 04/09/17
 *
 * @author gabriel
 */
public class SaopauloTendadriveCrawler extends Crawler {

   private static final String HOME_PAGE = "http://www.tendaatacado.com.br/";
   private final String storeId = getStoreId();

   public String getStoreId() {
      return session.getOptions().optString("storeId");
   }

   public SaopauloTendadriveCrawler(Session session) {
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
      JSONObject jsonObject = JSONUtils.stringToJson(doc.selectFirst("#__NEXT_DATA__").data());
      JSONObject skuJson = (JSONObject) jsonObject.optQuery("/props/pageProps/product");

      if (skuJson != null && !skuJson.isEmpty()) {

         String description = scrapDescription(skuJson);

         String internalId = skuJson.optString("sku");
         String internalPid = skuJson.optString("id");
         String name = skuJson.optString("name");
         List<String> images = scrapImages(skuJson);
         String primaryImage = !images.isEmpty() ? images.remove(0) : null;
         Offers offers = scrapOffers(skuJson);
         Integer stock = getStockFromStoreSpecific(skuJson);

         Product product =
            ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setOffers(offers)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(images)
               .setDescription(description)
               .setStock(stock)
               .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private String scrapDescription(JSONObject skuJson) {
      StringBuilder description = new StringBuilder();

      description.append(skuJson.optString("description"));

      JSONArray specs = skuJson.optJSONArray("specifications");

      if (specs != null) {
         description.append("<table id=\"specs\">");

         for (Object obj : specs) {
            JSONObject spec = (JSONObject) obj;

            description.append("<tr>");
            description.append("<td> " + spec.optString("name") + "</td>");
            description.append("<td> " + spec.optString("value") + "</td>");
            description.append("</tr>");
         }

         description.append("</table>");
      }


      return description.toString();
   }

   private Offers scrapOffers(JSONObject skuJson)
      throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Double price = skuJson.optDouble("price");
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      installments.add(InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(price)
         .build());

      creditCards.add(CreditCardBuilder.create()
         .setIsShopCard(false)
         .setBrand(Card.VISA.toString())
         .setInstallments(installments)
         .build());

      offers.add(
         OfferBuilder.create()
            .setSellerFullName("Tenda")
            .setIsBuybox(false)
            .setPricing(
               Pricing.PricingBuilder.create()
                  .setSpotlightPrice(price)
                  .setBankSlip(BankSlipBuilder.create().setFinalPrice(price).build())
                  .setCreditCards(creditCards)
                  .build())
            .setIsMainRetailer(true)
            .setUseSlugNameAsInternalSellerId(true)
            .build());

      return offers;
   }

   private List<String> scrapImages(JSONObject skuJson) {
      JSONArray photos = skuJson.optJSONArray("photos");
      List<String> images = new ArrayList<>();
      for (Object obj : photos) {
         if (obj instanceof JSONObject) {
            JSONObject json = (JSONObject) obj;
            images.add(json.optString("url", null));
         }
      }
      return images;
   }

   private Integer getStockFromStoreSpecific(JSONObject skuJson){
      int stock = 0;
      JSONArray inventory = skuJson.optJSONArray("inventory");
      for (Object o : inventory){
         if (o instanceof JSONObject){
            JSONObject idsStore = (JSONObject) o;
            String branchId = idsStore.optString("branchId");
            if (branchId.equals(storeId)){
               stock = idsStore.optInt("totalAvailable");
               break;
            }
         }
      }

      return stock;
   }
}
