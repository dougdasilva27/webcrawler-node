package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Parser;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.BankSlip;
import models.pricing.CreditCards;
import models.pricing.Pricing;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;

public class CarrefourCrawler extends Crawler {

   private static final List<String> SELLERS = Collections.singletonList("Carrefour");
   public static final String HOME_PAGE = "https://mercado.carrefour.com.br/";
   protected final String homePage = getHomePage();
   protected final List<String> mainSellersNames = getMainSellersNames();
   protected String storeCard = null;

   public CarrefourCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
      super.config.setParser(Parser.HTML);
   }

   protected String getHomePage() {
      return "https://www.carrefour.com.br/";
   }

   private String getRegionId() {
      return session.getOptions().optString("regionId");
   }

   protected String getLocationToken() {
      return session.getOptions().optString("vtex_segment");
   }

   protected String getCep() {
      return this.session.getOptions().optString("cep");
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   protected Response fetchResponse() {
      return fetchPage(session.getOriginalURL());
   }

   protected Response fetchPage(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "*/*");
      headers.put("authority", "mercado.carrefour.com.br");
      headers.put("referer", "https://mercado.carrefour.com.br/");

      Request request = RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setSendUserAgent(false)
         .setCookies(this.cookies)
         .mustSendContentEncoding(false)
         .setFetcheroptions(
            FetcherOptionsBuilder.create()
               .mustUseMovingAverage(false)
               .mustRetrieveStatistics(true)
               .build())
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.LUMINATI_SERVER_BR)
         )
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new FetcherDataFetcher(), new JsoupDataFetcher(), new ApacheDataFetcher()), session, "get");
      return response;
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      JSONObject contextJson = scrapContextJson(doc);

      String internalPid = contextJson.optString("sku");

      if (internalPid != null) {
         JSONObject productJson = crawlProductApi(internalPid);

         String url = JSONUtils.getValueRecursive(contextJson, "offers.url", ".", String.class, null);
         String internalId = productJson.optString("id");
         String name = productJson.optString("name");
         CategoryCollection categories = scrapCategories(productJson);
         List<String> images = CrawlerUtils.scrapImagesListFromJSONArray(productJson.optJSONArray("image"), "url", null, "https", "carrefourbrfood.vtexassets.com", session);
         String primaryImage = !images.isEmpty() ? images.remove(0) : null;
         List<String> secondaryImages = images;
         String description = productJson.optString("description");
         Offers offers = scrapOffer(productJson);

         Product product = ProductBuilder.create()
            .setUrl(url)
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setOffers(offers)
            .setDescription(description)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
      }

      return products;
   }

   private CategoryCollection scrapCategories(JSONObject productJson) {
      CategoryCollection categories = new CategoryCollection();

      JSONArray categoriesListJson = JSONUtils.getValueRecursive(productJson, "breadcrumbList.itemListElement", ".", JSONArray.class, new JSONArray());

      List<String> categoriesListString = JSONUtils.jsonArrayToStringList(categoriesListJson, "name", ".");
      if (categoriesListString.size() > 0) {
         categoriesListString.remove(categoriesListString.size() - 1);
      }

      categories.addAll(categoriesListString);
      return categories;
   }

   private JSONObject scrapContextJson(Document doc) {
      String contextJSONString = doc.selectFirst("script:containsData(Product)").data();
      return CrawlerUtils.stringToJSONObject(contextJSONString);
   }

   private JSONObject crawlProductApi(String internalPid) {
      String regionId = getRegionId();
      String body = fetchPage("https://mercado.carrefour.com.br/api/graphql?operationName=BrowserProductQuery&variables={\"locator\":[{\"key\":\"id\",\"value\":\"" + internalPid + "\"},{\"key\":\"channel\",\"value\":\"{\\\"salesChannel\\\":\\\"2\\\",\\\"regionId\\\":\\\"" + regionId + "\\\"}\"},{\"key\":\"locale\",\"value\":\"pt-BR\"}]}").getBody();
      JSONObject api = CrawlerUtils.stringToJSONObject(body);

      return JSONUtils.getValueRecursive(api, "data.product", JSONObject.class);
   }

   private Offers scrapOffer(JSONObject productJson) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      JSONArray sellers = productJson.optJSONArray("sellers");
      if (sellers != null) {
         int position = 1;
         for (Object o : sellers) {
            JSONObject offerJson = o instanceof JSONObject ? (JSONObject) o
               : new JSONObject();
            JSONObject commertialOffer = offerJson.optJSONObject("commertialOffer");
            String sellerFullName = "Carrefour";
            boolean isDefaultSeller = offerJson.optBoolean("sellerDefault", true);

            if (commertialOffer != null && sellerFullName != null) {
               Integer stock = commertialOffer.optInt("AvailableQuantity");

               if (stock > 0) {
                  boolean isBuyBox = sellers.length() > 1;
                  boolean isMainRetailer = isMainRetailer(sellerFullName);

                  Pricing pricing = scrapPricing(productJson);
                  List<String> sales = isDefaultSeller ? scrapSales(pricing) : new ArrayList<>();

                  offers.add(Offer.OfferBuilder.create()
                     .setUseSlugNameAsInternalSellerId(true)
                     .setSellerFullName(sellerFullName)
                     .setMainPagePosition(position)
                     .setIsBuybox(isBuyBox)
                     .setIsMainRetailer(isMainRetailer)
                     .setPricing(pricing)
                     .setSales(sales)
                     .build());

                  position++;
               }
            }
         }
      }

      return offers;
   }

   private Pricing scrapPricing(JSONObject productJson) throws MalformedPricingException {
      JSONObject offersJson = productJson.optJSONObject("offers");

      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(offersJson, "lowPrice", false);
      Double priceFrom = JSONUtils.getValueRecursive(productJson, "offers.offers.0.listPrice", ".", Double.class, null);

      if (productJson.optString("measurementUnit").equals("kg")) {
         Double unitMultiplier = productJson.optDouble("unitMultiplier");
         spotlightPrice = Math.floor((spotlightPrice * unitMultiplier) * 100) / 100.0;
         if (priceFrom != null) priceFrom = Math.floor((priceFrom * unitMultiplier) * 100) / 100.0;
      }

      CreditCards creditCards = new CreditCards();
      BankSlip bankSlip = scrapBankSlip(spotlightPrice);

      if (spotlightPrice != null && spotlightPrice.equals(priceFrom)) {
         priceFrom = null;
      }

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();

   }

   protected boolean isMainRetailer(String sellerName) {
      return SELLERS.stream().anyMatch(seller -> seller.toLowerCase().startsWith(sellerName.toLowerCase()));
   }

   protected List<String> getMainSellersNames() {
      return SELLERS;
   }

   protected BankSlip scrapBankSlip(Double spotlightPrice) throws MalformedPricingException {
      return BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();
   }

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();
      if (pricing != null) sales.add(CrawlerUtils.calculateSales(pricing));
      return sales;
   }
}
