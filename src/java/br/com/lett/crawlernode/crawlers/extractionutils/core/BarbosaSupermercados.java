package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
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
import models.pricing.BankSlip;
import models.pricing.CreditCards;
import models.pricing.Pricing;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BarbosaSupermercados extends Crawler {
   private final String HOME_PAGE = "https://www.barbosasupermercados.com.br/";
   private final String SELLER_FULL_NAME = "Barbosa Supermercados";
   private Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString());

   public BarbosaSupermercados(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
      super.config.setParser(Parser.JSON);
   }

   private final String STORE_ID = session.getOptions().optString("storeId");
   private final JSONObject scOptions = session.getOptions().optJSONObject("scraperClass");

   @Override
   protected Response fetchResponse() {
      String url = session.getOriginalURL();
      if (!url.contains("#product=")) {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
         return null;
      }

      String productUri = CommonMethods.getLast(url.split("#product="));

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      headers.put("Connection", "keep-alive");
      headers.put("referer", "https://www.barbosasupermercados.com.br/");

      String appSecret = scOptions.optString("app_secret");
      String appKey = scOptions.optString("app_key");

      String api = "https://bsm.applayos.com:6033/api/ecom/enav/verproduto";
      String payload = "{\"uri\":\"" + productUri + "\",\"session\":{\"loja\":{\"id\":\"" + STORE_ID + "\"}},\"app_key\":\"" + appKey + "\",\"app_secret\":\"" + appSecret + "\"}";

      Request request = Request.RequestBuilder.create()
         .setUrl(api)
         .setHeaders(headers)
         .setPayload(payload)
         .mustSendContentEncoding(false)
         .build();

      int tries = 0;
      Response response = null;

      do {
         try {
            response = new FetcherDataFetcher().post(session, request);
         } catch (Exception e) {
            Logging.printLogError(logger, CommonMethods.getStackTrace(e));
         }
         tries++;
      } while (tries < 3 || !Objects.requireNonNull(response).isSuccess());

      return response;
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      List<Product> products = new ArrayList<>();

      JSONObject productJson = json.optJSONObject("data");

      if (productJson != null) {
         String name = productJson.optString("descricao");
         String internalId = productJson.optString("sku");
         String internalPid = productJson.optString("_id");
         List<String> images = scrapImages(productJson);
         String primaryImage = images != null && !images.isEmpty() ? images.remove(0) : null;
         String description = productJson.optString("obs");
         boolean available = true;
         Offers offers = available ? scrapOffers(productJson) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setCategory1(productJson.optString("departamento"))
            .setCategory2(productJson.optString("categoria"))
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private List<String> scrapImages(JSONObject productJson) {
      JSONArray files = productJson.optJSONArray("files2");
      return IntStream.range(0, files.length()).mapToObj(i -> files.getJSONObject(i).optString("url")).collect(Collectors.toList());
   }

   private Offers scrapOffers(JSONObject product) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(product);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

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

   private Pricing scrapPricing(JSONObject product) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(product, "por", false);
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(product, "de", false);

      if (spotlightPrice.equals(priceFrom)) {
         priceFrom = null;
      }

      CreditCards creditCards = CrawlerUtils.scrapCreditCards(spotlightPrice, cards);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }
}
