package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MexicoSearsCrawler extends Crawler {

   public MexicoSearsCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.JSON);
      super.config.setFetcher(FetchMode.APACHE);
   }

   final private String SELLER_FULLNAME = "sears";

   @Override
   protected Response fetchResponse() {
      String url = "https://seapi.sears.com.mx/app/v1/product/" + getInternalIdFromOriginalUrl();

      Map<String, String> headers = new HashMap<>();
      headers.put("Host", "seapi.sears.com.mx");
      headers.put("Accept", "*/*");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();

      return CrawlerUtils.retryRequest(request, session, this.dataFetcher, true);
   }

   private String getInternalIdFromOriginalUrl() {
      String originalUrl = this.session.getOriginalURL();

      String regex = "\\/([0-9]+).*$";
      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      Matcher matcher = pattern.matcher(originalUrl);

      if (matcher.find()) {
         return matcher.group(1);
      }

      return null;
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {

      List<Product> products = new ArrayList<>();

      JSONObject productJson = json.optJSONObject("data");

      if (!productJson.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = productJson.optString("id");
         String internalPid = productJson.optString("sku");
         String name = productJson.optString("title");
         String description = productJson.optString("description");
         List<String> images = JSONUtils.jsonArrayToStringList(productJson.optJSONArray("images"), "url");
         String primaryImage = images.remove(0);
         List<String> secondaryImages = images;
         CategoryCollection categories = crawlCategories(productJson);
         List<String> eans = List.of(productJson.optString("ean"));
         boolean isAvailable = productJson.optInt("stock", 0) > 0;
         Offers offers = isAvailable ? scrapOffers(productJson) : null;

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setCategories(categories)
            .setDescription(description)
            .setOffers(offers)
            .setEans(eans)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private CategoryCollection crawlCategories(JSONObject productJson) {
      CategoryCollection categories = new CategoryCollection();
      categories.addAll(JSONUtils.jsonArrayToStringList(productJson.optJSONArray("categories"), "name"));

      return categories;
   }

   private Offers scrapOffers(JSONObject productJson) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productJson);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULLNAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());


      return offers;
   }

   private Pricing scrapPricing(JSONObject productJson) throws MalformedPricingException {

      Double priceFrom = CrawlerUtils.getDoubleValueFromJSON(productJson, "price", true, false);
      Double spotlightPrice = CrawlerUtils.getDoubleValueFromJSON(productJson, "sale_price", true, false);

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

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
