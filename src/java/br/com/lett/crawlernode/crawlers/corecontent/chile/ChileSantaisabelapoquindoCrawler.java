package br.com.lett.crawlernode.crawlers.corecontent.chile;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
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
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChileSantaisabelapoquindoCrawler extends Crawler {

   public ChileSantaisabelapoquindoCrawler(Session session) {
      super(session);
   }

   private static final String SELLER_FULL_NAME = "Santa Isabel";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());


   protected Document fetchDoc() {
      BasicClientCookie cookie = new BasicClientCookie("seller", "apoquindo");
      this.cookies.add(cookie);
      BasicClientCookie cookie2 = new BasicClientCookie("store-name", "Santa%20Isabel%20Apoquindo");
      this.cookies.add(cookie2);

      Request request = new Request.RequestBuilder()
         .setUrl(session.getOriginalURL())
         .setCookies(cookies)
         .setProxyservice(List.of(
            ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR
         ))
         .build();
      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new FetcherDataFetcher(), this.dataFetcher, new JsoupDataFetcher(), new ApacheDataFetcher()), session, "get");
      return Jsoup.parse(response.getBody());
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      doc = fetchDoc();


      List<Product> products = new ArrayList<>();

      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
      if (doc != null) {
         JSONObject json = scrapJSOMFromHTML(doc);

         if (!json.isEmpty()) {

            String internalId = json.optString("sku");
            String internalPid = internalId;
            String name = scrapNameWithBrand(json);
            List<String> images = getImages(doc);
            String primaryImage = !images.isEmpty() ? images.remove(0) : json.optString("image");
            String description = json.optString("description");
            Integer stock = null;
            boolean availableToBuy = JSONUtils.getValueRecursive(json, "offers.availability", String.class).contains("InStock");
            Offers offers = availableToBuy ? scrapOffer(json, doc) : new Offers();

            // Creating the product
            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(images)
               .setDescription(description)
               .setStock(stock)
               .setOffers(offers)
               .build();

            products.add(product);


         } else {
            Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
         }
      }
      return products;
   }

   private String scrapNameWithBrand(JSONObject json) {
      String name = json.optString("name");

      JSONObject o = json.optJSONObject("brand");
      if (o != null) {
         String brand = o.optString("name");

         if (name != null && brand != null) {
            return name + " - " + brand;
         }
      }
      return name;
   }

   private String selectJsonFromHtml(Document doc, String cssElement, String token, String finalIndex) throws JSONException, ArrayIndexOutOfBoundsException, IllegalArgumentException {

      String object = null;
      Elements scripts = doc.select(cssElement);

      for (Element e : scripts) {
         String script = e.html();

         if (script.contains(token) && (finalIndex == null || script.contains(finalIndex))) {
            object = script.replace(token, "").replace("\\", "");
            break;
         }
      }

      return object;
   }

   private List<String> getImages(Document doc) {
      List<String> imageList = new ArrayList<>();
      String infoProduct = selectJsonFromHtml(doc, "script", "window.__renderData = ", ";");

      if (infoProduct != null) {
         Pattern pattern = Pattern.compile("imageUrl\":\"(.+?)\",\"imageTag");
         Matcher matcher = pattern.matcher(infoProduct);
         while (matcher.find()) {
            imageList.add(matcher.group(1));
         }
      }
      return imageList;
   }

   private JSONObject getPriceDoc(Document doc) {
      Element script = doc.selectFirst("script:containsData(window)");
      String varString = script.toString();
      String sanitizedString = varString.replace("\\", "");
      String extractedString = CommonMethods.substring(sanitizedString, "sellers\":[", "]}]", true);
      return JSONUtils.stringToJson(extractedString);
   }

   private JSONObject scrapJSOMFromHTML(Document doc) {
      JSONObject json = new JSONObject();
      Elements scripts = doc.select("script[type=\"application/ld+json\"]");
      for (Element s : scripts) {
         String script = s.html();
         if (script.contains("Product")) {
            json = CrawlerUtils.stringToJson(script);
         }
      }

      return json;
   }

   private Offers scrapOffer(JSONObject json, Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      JSONObject price = getPriceDoc(doc);
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


   private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {

      Double rawPrice = 1.0 * JSONUtils.getValueRecursive(json, "commertialOffer.PriceWithoutDiscount", Integer.class, 0);
      rawPrice = rawPrice == 0 ? null : rawPrice;
      Double spotlightPrice = 1.0 * JSONUtils.getValueRecursive(json, "commertialOffer.Price", Integer.class, 0);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(rawPrice)
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

}
