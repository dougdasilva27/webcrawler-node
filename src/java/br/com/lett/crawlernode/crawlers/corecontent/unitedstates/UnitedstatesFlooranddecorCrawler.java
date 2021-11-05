package br.com.lett.crawlernode.crawlers.corecontent.unitedstates;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.*;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnitedstatesFlooranddecorCrawler extends Crawler {

   protected String storeId = getStoreId();
   private static final Set<String> cards = Sets.newHashSet(Card.DINERS.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.ELO.toString());
   private static final String MAIN_SELLER_NAME = "floor and decor";

   public UnitedstatesFlooranddecorCrawler(Session session) {
      super(session);
      this.config.setParser(Parser.HTML);
      this.config.setFetcher(FetchMode.FETCHER);
   }

   protected String getStoreId() {
      return session.getOptions().optString("StoreID");
   }

   @Override
   public void handleCookiesBeforeFetch() {
      BasicClientCookie cookie = new BasicClientCookie("StoreID", storeId);
      cookie.setDomain("www.flooranddecor.com");
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (doc.selectFirst("div#product-content") != null) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script#productData", "", ";", false, true);
         String internalPid = scrapInternalPid(doc);
         String name = json.optString("name");
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "ol.breadcrumb li a", true);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList("div.product-tabs"));

         Elements variations = doc.select("div.product-variations li.attribute ul li");

         if (variations.isEmpty()) {
            variations = doc.select("div#product-content");
         }

         Element nextVariation = null;
         for (int i = 0; i < variations.size(); i++) {
            String internalId = json.optString("sku");
            String productUrl = JSONUtils.getValueRecursive(json, "offers.url", String.class);
            List<String> images = CrawlerUtils.scrapImagesListFromJSONArray(json.optJSONArray("image"), null, null, "https", "i8.amplience.net", session);
            String primaryImage = !images.isEmpty() ? images.remove(0) : null;

            boolean available = JSONUtils.getValueRecursive(json, "offers.availability", String.class).contains("InStock");
            Offers offers = available ? scrapOffers(json) : new Offers();

            // Creating the product
            Product product = ProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setCategory1(categories.getCategory(0))
               .setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2))
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(images)
               .setDescription(description)
               .setOffers(offers)
               .build();
            products.add(product);

            if(i + 1 < variations.size()){
               nextVariation = variations.get(i + 1);
               String url = CrawlerUtils.scrapStringSimpleInfoByAttribute(nextVariation, "a", "href");
               json = fetchNextVariationJson(url);
            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page:   " + this.session.getOriginalURL());
      }

      return products;
   }

   protected JSONObject fetchNextVariationJson(String productUrl) {
      Request request = Request.RequestBuilder.create()
         .setUrl(productUrl)
         .setCookies(cookies)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_US_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY
         ))
         .build();

      Response response = this.dataFetcher.get(session, request);
      Document doc = Jsoup.parse(response.getBody());
      return CrawlerUtils.selectJsonFromHtml(doc, "script#productData", "", ";", false, true);
   }

   protected String scrapInternalPid(Document doc){
      String internalPid = "";
      Element script = doc.selectFirst("div.primary-content script");

      if(script != null){
         Pattern pattern = Pattern.compile("id: '(.*?)'", Pattern.MULTILINE);
         Matcher matcher = pattern.matcher(script.toString());

         while (matcher.find()) {
            internalPid = matcher.group(1);
         }
      }

      return internalPid;
   }

   protected Offers scrapOffers(JSONObject json) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(json);

      if (pricing != null) {
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(MAIN_SELLER_NAME)
            .setSellersPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .build());
      }

      return offers;
   }

   private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {
      String priceStr = JSONUtils.getValueRecursive(json, "offers.price", String.class);
      Double spotlightPrice = MathUtils.parseDoubleWithDot(priceStr);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(BankSlip.BankSlipBuilder.create().setFinalPrice(spotlightPrice).setOnPageDiscount(0d).build())
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      for (String brand : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(brand)
            .setIsShopCard(false)
            .setInstallments(installments)
            .build());
      }

      return creditCards;
   }
}
