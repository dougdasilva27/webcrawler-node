package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
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
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.apache.kafka.common.security.JaasUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;

public class MerqueoCrawler extends Crawler {

   String zoneId = session.getOptions().optString("zoneId");
   private static final String SELLER_FULL_NAME = "Merqueo";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AMEX.toString());


   public MerqueoCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      JSONObject apiJson = scrapApiJson(session.getOriginalURL());
      JSONObject data = new JSONObject();
      JSONObject attributes = new JSONObject();
      JSONArray included = new JSONArray();

      if (apiJson.has("data") && !apiJson.isNull("data")) {
         data = apiJson.optJSONObject("data");
         included = apiJson.optJSONArray("included");
         attributes = data.optJSONObject("attributes");
      }

      if (data.has("id")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = String.valueOf(data.optInt("id"));
         String name = scrapName(attributes);


         String primaryImage = attributes.optString("image_large_url");
         List<String> secondaryImages = included != null ? scrapSecondaryImages(included, primaryImage) : new ArrayList<>();
         String description = attributes.optString("description");
         Integer stock = crawlStock(data);
         Offers offers = JSONUtils.getValueRecursive(data, "attributes.status", Boolean.class) ? crawlOffers(attributes) : new Offers();
         String url = verifyUrl();
         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(url)
            .setInternalId(internalId)
            .setName(name)
            .setOffers(offers)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setStock(stock)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   List<String> scrapSecondaryImages(JSONArray data, String primaryImage) {
      List<String> list = new ArrayList<>();
      for (Object o : data) {
         JSONObject obj = (JSONObject) o;
         String type = obj.optString("type");
         if (type != null && type.equals("images")) {
            String image = JSONUtils.getValueRecursive(obj, "attributes.image_large_url", String.class);
            if (image != null && !image.equals(primaryImage)) {
               list.add(image);
            }
         }
      }

      return list;
   }

   private String verifyUrl() {
      String country = session.getOptions().optString("country");
      if (country != null && country.equals("br")) {
         return formatUrl();
      }
      return session.getOriginalURL();
   }

   private String formatUrl() {
      return session.getOriginalURL().replace("https://merqueo.com/sao-paulo/", "https://merqueo.com.br/" + session.getOptions().optString("city") + "/" + session.getOptions().optString("locate") + "/");
   }

   private String scrapName(JSONObject attributes) {
      StringBuilder slugString = new StringBuilder();

      String slug = attributes.optString("slug");
      if (!slug.isEmpty()) {
         String[] slugSplit = slug.split("-");
         for (int i = 0; i < slugSplit.length; i++) {
            slugString.append(slugSplit[i].substring(0, 1).toUpperCase()).append(slugSplit[i].substring(1));
            slugString.append(" ");
         }
      }
      return slugString.toString();
   }


   private JSONObject scrapApiJson(String originalURL) {
      List<String> slugs = scrapSlugs(originalURL);

      StringBuilder apiUrl = new StringBuilder();
      apiUrl.append("https://merqueo.com/api/3.1/stores/281/");

      if (slugs.size() == 3) {
         apiUrl.append("department/").append(slugs.get(0));
         apiUrl.append("/shelf/").append(slugs.get(1));
         apiUrl.append("/products/").append(slugs.get(2));
      }

      apiUrl.append("?zoneId=");
      apiUrl.append(zoneId);

      Request request = Request.RequestBuilder
         .create()
         .setUrl(apiUrl.toString())
         .setProxyservice(Arrays.asList(ProxyCollection.BUY, ProxyCollection.NETNUT_RESIDENTIAL_BR, ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY))
         .mustSendContentEncoding(false)
         .build();

      return CrawlerUtils.stringToJson(CrawlerUtils.retryRequestString(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session));
   }

   /*
    * Url exemple:
    * https://merqueo.com/bogota/aseo-del-hogar/detergentes/ariel-concentrado-doble-poder-detergente-la
    * -quido-2-lt
    */
   private List<String> scrapSlugs(String originalURL) {
      List<String> slugs = new ArrayList<>();
      String slugString = CommonMethods.getLast(originalURL.split("sao-paulo/"));
      String[] slug = slugString.contains("/") ? slugString.split("/") : null;

      if (slug != null) {
         Collections.addAll(slugs, slug);
      }
      return slugs;
   }


   private Integer crawlStock(JSONObject data) {
      String quantity = JSONUtils.getValueRecursive(data, "attributes.status", String.class);
      if (quantity != null) {
         return Integer.parseInt(quantity);
      }
      return 0;
   }

   private Offers crawlOffers(JSONObject attributes) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();

      Pricing pricing = crawlpricing(attributes);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());

      return offers;
   }

   private Pricing crawlpricing(JSONObject attributes) throws MalformedPricingException {

      Double spotLightprice = attributes.optDouble("special_price", 0);
      spotLightprice = spotLightprice == 0d ? null : spotLightprice;
      Double priceFrom = attributes.optDouble("price", 0);
      priceFrom = priceFrom == 0d ? null : priceFrom;

      if (spotLightprice == null && priceFrom != null) {
         spotLightprice = priceFrom;
         priceFrom = null;
      }
      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotLightprice)
         .build());


      CreditCards creditCards = new CreditCards();
      for (String s : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setInstallments(installments)
            .setBrand(s)
            .setIsShopCard(false)
            .build());
      }


      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotLightprice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(BankSlip.BankSlipBuilder.create()
            .setFinalPrice(spotLightprice)
            .build())
         .build();

   }


}
