package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BrasilQualidocCrawler extends Crawler {

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilQualidocCrawler(Session session) {
      super(session);
   }

   @Override
   protected Object fetch() {
      Map<String, String> headers = new HashMap<>();

      String path = session.getOriginalURL().replace("https://www.qualidoc.com.br/", "");
      path = URLEncoder.encode(path, StandardCharsets.UTF_8);

      String url = "https://www.qualidoc.com.br/ccstoreui/v1/pages/" + path + "?dataOnly=false&cacheableDataOnly=true&productTypesRequired=false";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setHeaders(headers)
         .build();

      Response response = this.dataFetcher.get(session, request);

      return JSONUtils.stringToJson(response.getBody());
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();

      JSONObject jsonProduct = JSONUtils.getValueRecursive(json, "data.page.product", JSONObject.class);

      if (jsonProduct.has("id")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = jsonProduct.optString("id");
         String name = jsonProduct.optString("displayName");
         CategoryCollection categories = scrapCategories(jsonProduct);
         List<String> images = scrapImages(jsonProduct);
         String primaryImage = !images.isEmpty() ? images.remove(0) : "";
         String description = scrapDescription(jsonProduct);

         JSONObject jsonOffers = fetchOffers();
         boolean availableToBuy = jsonOffers.optString("availability").contains("InStock");
         Offers offers = availableToBuy ? scrapOffer(jsonProduct, jsonOffers) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   protected CategoryCollection scrapCategories(JSONObject json) {
      CategoryCollection categories = new CategoryCollection();

      return categories;
   }

   protected List<String> scrapImages(JSONObject json) {
      List<String> imgs = new ArrayList<>();

      JSONArray arrayImages = json.optJSONArray("fullImageURLs");
      arrayImages.forEach(x -> imgs.add("https://www.qualidoc.com.br" + x.toString()));

      return imgs;
   }

   protected String scrapDescription(JSONObject json) {
      StringBuilder description = new StringBuilder();

      description.append("Indicação:\n");
      description.append(json.optString("x_indicacao") + "\n");
      description.append("Modo de Usar:\n");
      description.append(json.optString("x_comoUsar"));

      return description.toString();
   }

   protected JSONObject fetchOffers() {
      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setCookies(cookies)
         .build();

      Response response = this.dataFetcher.get(session, request);
      Document doc = Jsoup.parse(response.getBody());
      JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script#CC-schema-org-server", null, null, false, false);

      //The first json object in the array corresponds to the main offer
      return json.optJSONArray("offers").getJSONObject(0);
   }

   private Offers scrapOffer(JSONObject json, JSONObject jsonOffers) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();

      Pricing pricing = scrapPricing(json, jsonOffers);

      if(!json.optString("x_valorDeCashback").equals("")) {
         sales.add(json.optString("x_valorDeCashback") + " on cashback");
      }
      if(pricing.getPriceFrom() != null) {
         sales.add(CrawlerUtils.calculateSales(pricing));
      }

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName("qualidoc")
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private Pricing scrapPricing(JSONObject json, JSONObject jsonOffers) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(jsonOffers, "salePrice", false);
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(jsonOffers, "price", false);

      if(spotlightPrice == null){
         spotlightPrice = priceFrom;
         priceFrom = null;
      }

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

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private String scrapSales(JSONObject json){
      //Cashback sale
      String sale = json.optString("x_valorDeCashback") + " on cashback";

      return sale;
   }

}
