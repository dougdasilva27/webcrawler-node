package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
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
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.apache.commons.lang.WordUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilMateusmaisCrawler extends Crawler {
   private static final String SELLER_NAME_LOWER = "mateusmais";
   private static final Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.DINERS.toString(), Card.AMEX.toString(), Card.ELO.toString());
   public BrasilMateusmaisCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      String idUrl = getUrlid();
      JSONObject productList = getProduct(idUrl);
      List<Product> products = new ArrayList<>();

      if (productList != null && !productList.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = productList.optString("sku");
         String name = productList.optString("name") + " "+ productList.optString("measure") + productList.optString("measure_type");;
         String description = productList.optString("description");
         String primaryImage = productList.optString("image");
         List<String> eans = Collections.singletonList(productList.optString("barcode"));
         CategoryCollection categories = getCategory(productList);
         String brand = productList.optString("brand");
         boolean available = productList.optBoolean("available");
         Offers offers = available ? scrapOffers(productList) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(brand + " " + name)
            .setPrimaryImage(primaryImage)
            .setCategories(categories)
            .setEans(eans)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);


      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private CategoryCollection getCategory(JSONObject productList) {
      CategoryCollection categories = new CategoryCollection();
      String objCategory = productList.optString("category");
      if (objCategory != null && !objCategory.isEmpty()) {
         String category = WordUtils.capitalize(objCategory);
         categories.add(category);
      }
      return categories;
   }
   private String getUrlid() {
      String id = null;

      String regex = "9/(.*)";
      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      final Matcher matcher = pattern.matcher(session.getOriginalURL());

      if (matcher.find()) {
         id = matcher.group(1);
      }
      return id;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".proBoxPrimaryInner") != null;
   }

   private Offers scrapOffers(JSONObject productList) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productList);

      if (pricing != null) {
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_NAME_LOWER)
            .setSellersPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .build());
      }

      return offers;
   }

   private Pricing scrapPricing(JSONObject productList) throws MalformedPricingException {

      Double priceFrom = productList.optDouble("price");
      Double spotlightPrice = productList.optDouble("low_price");

      if (spotlightPrice.isNaN() ){
         spotlightPrice = priceFrom;
      }

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = CrawlerUtils.scrapCreditCards(spotlightPrice, cards);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private JSONObject getProduct(String internalId) {
      String url = "https://app.mateusmais.com.br/market/2857c51e-ffc9-4365-b39a-0156cfc032b9/product/" + internalId;

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .mustSendContentEncoding(true)
         .build();
      Response response = this.dataFetcher.get(session, request);
      if (response != null) {
         return CrawlerUtils.stringToJson(response.getBody());
      }
      return null;

   }
}
