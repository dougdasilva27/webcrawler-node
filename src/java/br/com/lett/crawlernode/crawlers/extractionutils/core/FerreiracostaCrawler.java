package br.com.lett.crawlernode.crawlers.extractionutils.core;


import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class FerreiracostaCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "ferreira costa";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());


   public FerreiracostaCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.HTML);
   }

   @Override
   protected Response fetchResponse() {
      String location = session.getOptions().optString("location");
      String region = session.getOptions().optString("region");
      try {
         HttpClient client = HttpClient.newBuilder().build();
         HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(session.getOriginalURL()))
            .header("cookie", "ASP.NET_SessionId=afdhrtmd1chvych4b1y1zqmv;" +
               " osVisitor=b44b973b-4257-4e0b-a2ca-47e37d439251;" +
               " osVisit=f29d817f-51ed-4082-8dd0-7ae9b309c837;" +
               " eco_ce=; ecoFC=FCB02C35F253FCE56D9696DC7757BC6E;" +
               " _gid=GA1.2.1245936171.1631798355;" +
               " _gat_UA-48783684-2=1;" +
               " _gat_gtag_UA_48783684_2=1;" +
               " G_ENABLED_IDPS=google;" +
               " _fbp=fb.1.1631798354801.1640176812;" +
               " _pin_unauth=dWlkPU9XVTBZMlExTlRRdE5UZ3pNeTAwTWprd0xXSTNNMkV0TVRWaE5XVTJZVE0yTkRZMw;" +
               " _hjid=fdb9c70b-3929-430b-8698-0a11e9020213;" +
               " _hjIncludedInSessionSample=1;" +
               " _hjAbsoluteSessionInProgress=0;" +
               "@FC:Ecom:Dropdown:BranchLocation=" + location +
               ";@FC:Ecom:Dropdown:Region=" + region +
               " _ga=GA1.2.1447234012.1631798354;" +
               " _ga_DD7Y69210P=GS1.1.1631798009.3.1.1631798369.0;" +
               " cto_bundle=QnJvIl9STm1tMVN2ZVhydXYxSXhnSGJFbEV1ak1kT1VRYXlGRnIyUldQbm4lMkZhM0hFVWNCYXM4JTJCUDRJUFklMkIzJTJGblglMkJaSCUyQkU5QkUlMkYweWVVNUU5bkYxWkV0bXdzYnZEOWxxV2xEdjFZMDIlMkZvSTVWTnRueGVKZDZxT3dRQ05SbnQlMkJ0cWdvYnZac1pBSSUyRkZtenpzVzA0RFRQSiUyQmZBJTNEJTNE;" +
               " AWSALB=AWQren+oPEAFGXDRiJutL5+sy0hpn5zZBAoiHwI5wCsthQh1UcN4sz5hYfT2hEfrlKuY45Vz5J0qEsHDS9JBbMDsPqDb7l12m63zMvEokIrgKyyLHS5mFcV8YyT+;" +
               " AWSALBCORS=AWQren+oPEAFGXDRiJutL5+sy0hpn5zZBAoiHwI5wCsthQh1UcN4sz5hYfT2hEfrlKuY45Vz5J0qEsHDS9JBbMDsPqDb7l12m63zMvEokIrgKyyLHS5mFcV8YyT+;" +
               " RT=s=1631798388140&r=https%3A%2F%2Fwww.ferreiracosta.com%2FProduto%2F408846%2Flavadora-de-roupa-brastemp-12kg-branca-127v-bwk12abana")
            .build();

         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         return new Response.ResponseBuilder()
            .setBody(response.body())
            .setLastStatusCode(response.statusCode())
            .build();
      } catch (IOException | InterruptedException e) {
         throw new RuntimeException("Failed to load document: " + session.getOriginalURL(), e);
      }
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      JSONObject jsonInfo = CrawlerUtils.selectJsonFromHtml(doc, "#__NEXT_DATA__", "", null, false, false);
      JSONObject jsonProduct = JSONUtils.getValueRecursive(jsonInfo, "props.pageProps.dataProduct", JSONObject.class, new JSONObject());
      if (!jsonProduct.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + session.getOriginalURL());
         JSONObject offersInfo = JSONUtils.getValueRecursive(jsonProduct, "prices.0", JSONObject.class, new JSONObject());
         CategoryCollection categories = getCategories(jsonProduct);
         String name = jsonProduct.optString("description");
         String internalId = jsonProduct.optString("id");
         String description = jsonProduct.optString("detailedDescription") + getSpecs(internalId);
         String primaryImage = getPrimaryImage(jsonProduct);
         List<String> secondaryImages = getSecondaryImages(jsonProduct);
         boolean available = !offersInfo.isNull("priceList");
         Offers offers = available ? scrapOffers(offersInfo) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setOffers(offers)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setCategories(categories)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private String getPrimaryImage(JSONObject jsonProduct) {
      JSONArray mediaLinks = jsonProduct.optJSONArray("mediaLinks");
      for (int i = 0; i < mediaLinks.length(); i++) {
         JSONObject mediaLink = mediaLinks.optJSONObject(i);
         if (mediaLink.optString("linkType").equals("IMAGEM")) {
            return mediaLink.optString("imageUrl");
         }
      }
      return "";
   }

   private List<String> getSecondaryImages(JSONObject jsonProduct) {
      List<String> secondaryImages = new ArrayList<>();
      boolean primaryImage = false;
      JSONArray mediaLinks = jsonProduct.optJSONArray("mediaLinks");
      for (int i = 0; i < mediaLinks.length(); i++) {
         JSONObject mediaLink = mediaLinks.optJSONObject(i);
         if (mediaLink.optString("linkType").equals("IMAGEM")) {
            if (!primaryImage) {
               primaryImage = true;
            } else {
               secondaryImages.add(mediaLink.optString("imageUrl"));
            }
         }
      }
      return secondaryImages;
   }

   private CategoryCollection getCategories(JSONObject jsonProduct) {
      CategoryCollection categories = new CategoryCollection();
      JSONObject jsonCategproes = jsonProduct.optJSONObject("breadCrumbs");
      for (String category : jsonCategproes.keySet()) {
         JSONObject categoryObject = jsonCategproes.optJSONObject(category);
         String name = categoryObject.optString("name");
         categories.add(name);
      }
      return categories;
   }

   private String getSpecs(String internalId) {
      try {
         HttpClient client = HttpClient.newBuilder().build();
         HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create("https://fcxlabs-ecommerce-api.ferreiracosta.com/catalog/v1/products/" + internalId + "/specs"))
            .build();

         HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());

         JSONArray jsonArray = new JSONArray(resp.body());
         StringBuilder description = new StringBuilder();
         for (Object item : jsonArray) {
            JSONObject jsonObject = (JSONObject) item;
            JSONArray specsArray = jsonObject.optJSONArray("specs");
            for (Object spec : specsArray) {
               JSONObject specObject = (JSONObject) spec;
               description.append(specObject.optString("name") + " : " + specObject.optString("value") + "\n");
            }
         }
         return description.toString();
      } catch (IOException | InterruptedException e) {
         throw new RuntimeException("Failed to retrieve specifications for internalId: " + internalId, e);
      }
   }

   private Offers scrapOffers(JSONObject offersInfo) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(offersInfo);
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


   private Pricing scrapPricing(JSONObject offersInfo) throws MalformedPricingException {
      Boolean hasDiscount = !offersInfo.isNull("spotPrice");
      Double spotlightPrice = hasDiscount ? offersInfo.optDouble("spotPrice") : offersInfo.optDouble("priceList");
      if (!hasDiscount && offersInfo.optDouble("priceList") != offersInfo.optDouble("salePrice")) {
         hasDiscount = true;
         spotlightPrice = offersInfo.optDouble("salePrice");
      }
      Double priceFrom = hasDiscount ? offersInfo.optDouble("priceList") : null;
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
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
}
