package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.util.*;
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

import java.util.*;

/**
 * date: 27/03/2018
 *
 * @author gabriel
 */

public class BrasilCentauroCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "centauro";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());


   //This token is hardcoded because contains information about location and store id.
   private static final String BEARER_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1bmlxdWVfbmFtZSI6ImZyb250LWVuZCBjZW50YXVybyIsIm5iZiI6MTU4OTkxOTgxMywiZXhwIjoxOTA1NDUyNjEzLCJpYXQiOjE1ODk5MTk4MTN9.YeCTBYcWozaQb4MnILtfeKTeyCwApNgLSOfGeVVM8D0";

   public BrasilCentauroCrawler(Session session) {
      super(session);
   }

   private JSONObject scrapInfoFromAPI() {
      String url = "https://apigateway.centauro.com.br/ecommerce/v4.3/produtos?codigoModelo=/";
      String slug = CommonMethods.getLast(session.getOriginalURL().split("/"));

      if (slug.contains("?")) {
         url += slug.split("\\?")[0];
      } else {
         url += slug;
      }

      Map<String, String> headers = new HashMap<>();
      headers.put("authorization", "Bearer " + BEARER_TOKEN);

      Request request = Request.RequestBuilder.create()
         .setHeaders(headers)
         .setUrl(url)
         .build();
      Response response = this.dataFetcher.get(session, request);

      return CrawlerUtils.stringToJson(response.getBody());
   }



   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      JSONObject json  = scrapInfoFromAPI();

      if (json.has("informacoes")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONObject info = json.optJSONObject("informacoes");
         JSONArray preco = json.optJSONArray("precos");
         JSONArray imagens = json.optJSONArray("imagens");
         JSONObject disponibilidade = json.optJSONObject("disponibilidade");
         JSONArray cores = disponibilidade.optJSONArray("cores");


         String internalPid = info.optString("codigo");
         String description = info.optString("descricaoSEO");



         for (int i = 0; i < cores.length(); i++) {
            JSONObject skuObject = cores.optJSONObject(i);
            JSONObject price = preco.optJSONObject(i);
            JSONObject imageObject = imagens.optJSONObject(i);

            String name = info.optString("tituloSEO")+ " " + skuObject.optString("nomeCor");
            String internalId = skuObject.optString("sku");
            String primaryImage = scrapPrimaryImage(imageObject);
            List<String> secondaryImage = scrapSecondaryImage(imageObject);
            Offers offers = price != null ? scrapOffer(price): new Offers();
            RatingsReviews rating = scrapRating(internalId);

            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImage)
               .setDescription(description)
               .setRatingReviews(rating)
               .setOffers(offers)
               .build();

            products.add(product);

         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private String scrapPrimaryImage (JSONObject images){
      String primaryImage = "";


      if (images != null) {

         for (Object imagesObject : images.optJSONArray("urls")) {
            JSONObject urls = (JSONObject) imagesObject;

            if (urls.optBoolean("principal") && urls.optString("resolucao").equals("1300x1300")) {
               primaryImage = urls.optString("url");
            }
         }
      }

      return primaryImage;
   }


   private List<String> scrapSecondaryImage (JSONObject images){
      List<String> secondaryImage = new ArrayList<>();

      if (images != null) {

         for (Object imagesObject : images.optJSONArray("urls")) {
            JSONObject urls = (JSONObject) imagesObject;

            if (!urls.optBoolean("principal") && urls.optString("resolucao").equals("1300x1300")) {
               secondaryImage.add(urls.optString("url"));
            }
         }
      }


      return secondaryImage;
   }

   protected CategoryCollection scrapCategories(JSONObject json){
      CategoryCollection categories = new CategoryCollection();

      categories.add(JSONUtils.getValueRecursive(json, "informacoes.grupo", String.class));
      categories.add(JSONUtils.getValueRecursive(json, "informacoes.subGrupo", String.class));
      categories.add(JSONUtils.getValueRecursive(json, "informacoes.genero", String.class));

      return categories;
   }

   protected CategoryCollection scrapImages(JSONObject json){
      CategoryCollection categories = new CategoryCollection();

      categories.add(JSONUtils.getValueRecursive(json, "informacoes.grupo", String.class));
      categories.add(JSONUtils.getValueRecursive(json, "informacoes.subGrupo", String.class));
      categories.add(JSONUtils.getValueRecursive(json, "informacoes.genero", String.class));

      return categories;
   }


   private Offers scrapOffer(JSONObject price) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(price);
      List<String> sales = new ArrayList<>();

      offers.add(Offer.OfferBuilder.create()
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


   private Pricing scrapPricing(JSONObject price) throws MalformedPricingException {
      Double spotlightPrice = MathUtils.parseDoubleWithComma(price.optString("valor"));
      Double priceFrom =      MathUtils.parseDoubleWithComma(price.optString("valorDe"));

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }


   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      if (installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
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

   private RatingsReviews scrapRating (String internalId){
      String apiURL = "https://trustvox.com.br/widget/root?code="+ internalId+"&store_id=75856&url=" + session.getOriginalURL();

      Request request = Request.RequestBuilder.create().setUrl(apiURL).build();
      String response = this.dataFetcher.get(session,request).getBody();

      RatingsReviews ratingReviews = new RatingsReviews();

      JSONObject json = CrawlerUtils.stringToJson(response);
      JSONObject storeRate = json.optJSONObject("store_rate");

      if(storeRate != null) {
         Double avgRating = MathUtils.parseDoubleWithDot(storeRate.optString("average"));
         int cout = storeRate.optInt("count");
         ratingReviews.setAverageOverallRating(avgRating);
         ratingReviews.setTotalRating(cout);
      }

      return ratingReviews;
   }


}
