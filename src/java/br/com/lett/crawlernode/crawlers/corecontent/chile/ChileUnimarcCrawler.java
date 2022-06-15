package br.com.lett.crawlernode.crawlers.corecontent.chile;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
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
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;


public class ChileUnimarcCrawler extends Crawler {

   public ChileUnimarcCrawler(Session session) {
      super(session);
    config.setFetcher(FetchMode.APACHE);
   }

   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      //super.extractInformation(doc);
      Document doc = getProduct(this.session.getOriginalURL());
      List<Product> products = new ArrayList<>();
      //String script = CrawlerUtils.scrapScriptFromHtml(doc, "#__NEXT_DATA__");
    //  JSONArray dataArray = JSONUtils.stringToJsonArray(script);

      JSONArray dataArray = CrawlerUtils.selectJsonArrayFromHtml(doc, "#__NEXT_DATA__",null,null, false,false,false);
      if (JSONUtils.getValueRecursive(dataArray, "0.isFallback", Boolean.class)) {
         Logging.printLogDebug(logger, session, "Not a product page" + session.getOriginalURL());
         return products;
      }
      JSONObject data = JSONUtils.getValueRecursive(dataArray, "0.props.pageProps", JSONObject.class);

      String internalId = JSONUtils.getValueRecursive(data, "product.data.itemId", String.class);
      String internalPid = JSONUtils.getValueRecursive(data, "product.data.productId", String.class);
      String name = JSONUtils.getValueRecursive(data, "product.data.name", String.class);
      JSONArray images = JSONUtils.getValueRecursive(data, "product.data.images", JSONArray.class);
      String primaryImage = !images.isEmpty() ? (String) images.get(0) : "";
      String description = JSONUtils.getValueRecursive(data, "product.data.description", String.class);
      CategoryCollection categories = scrapCategories(data);
      Integer stock = JSONUtils.getValueRecursive(data, "product.data.sellers.0.availableQuantity", Integer.class);
      List<String> secondaryImages = scrapSecondaryImages(images, primaryImage);
      Offers offers = stock > 0 ? scrapOffers(data) : new Offers();
      Product product = ProductBuilder.create()
         .setUrl(session.getOriginalURL())
         .setInternalId(internalId)
         .setInternalPid(internalPid)
         .setName(name)
         .setOffers(offers)
         .setPrimaryImage(primaryImage)
         .setSecondaryImages(secondaryImages)
         .setDescription(description)
         .setCategories(categories)
         .setStock(stock)
         .build();

      products.add(product);


      return products;
   }

   private boolean isProductPage(Document doc) {


      return true;
   }

   private List<String> scrapSecondaryImages(JSONArray images, String primaryImage) {
      List<String> list = new ArrayList<>();
      for (Integer i = 0; i < images.length(); i++) {
         String image = (String) images.get(i);
         if (!image.equals(primaryImage)) {
            list.add(image);
         }
      }


      return list;
   }

   private CategoryCollection scrapCategories(JSONObject data) {
      JSONArray arr = data.optJSONArray("categories");
      CategoryCollection categories = new CategoryCollection();
      int cont = 0;
      for (Integer i = 0; i < arr.length(); i++) {
         categories.add((String) arr.get(i));
         cont++;
         if (cont == 3) {
            return categories;
         }
      }
      return categories;
   }

   private Offers scrapOffers(JSONObject data) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();

      Pricing pricing = scrapPricing(data);
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSales(sales)
         .setSellerFullName("unimarc")
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject data) throws MalformedPricingException {
      Integer spotlightPriceInt = JSONUtils.getValueRecursive(data, "product.data.sellers.0.price", Integer.class);
      spotlightPriceInt = spotlightPriceInt * 100;
      Integer priceFromInt = JSONUtils.getValueRecursive(data, "product.data.sellers.0.price.listPrice", Integer.class);
      priceFromInt = priceFromInt * 100;
      Double spotlightPrice = spotlightPriceInt / 100.0;
      Double priceFrom = priceFromInt / 100.0;


      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(bankSlip)
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

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AURA.toString(), Card.DINERS.toString(), Card.AMEX.toString());

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private Document getProduct(String url) {

//      Map<String, String> headers = new HashMap<>();
//      headers.put(HttpHeaders.,"application/json");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .mustSendContentEncoding(false)
         .setSendUserAgent(false)
         .setProxyservice(Arrays.asList(ProxyCollection.BUY,ProxyCollection.NETNUT_RESIDENTIAL_BR,ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY))
         .build();

      return CrawlerUtils.retryRequestDocument(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session);
     // Response response = this.dataFetcher.get(session, request);
      // return CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session);

   }

}
