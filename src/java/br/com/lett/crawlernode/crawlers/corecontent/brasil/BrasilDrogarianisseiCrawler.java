package br.com.lett.crawlernode.crawlers.corecontent.brasil;

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
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;

public class BrasilDrogarianisseiCrawler extends Crawler {
   private static final String HOME_PAGE = "https://www.farmaciasnissei.com.br/";

   public BrasilDrogarianisseiCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.JSOUP);
      super.config.setParser(Parser.HTML);

   }

//   @Override
//   public void handleCookiesBeforeFetch() {
//
//      Map<String, String> headers = new HashMap<>();
//      headers.put("authority", "www.farmaciasnissei.com.br");
//      headers.put("origin", "https://www.farmaciasnissei.com.br");
//      headers.put("referer", session.getOriginalURL());
//      headers.put("x-requested-with", "XMLHttpRequest");
//
//      Request request = Request.RequestBuilder.create()
//         .setUrl(session.getOriginalURL())
//         .setProxyservice(List.of(ProxyCollection.BUY_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_BR))
//         .setSendUserAgent(false)
//         .setHeaders(headers)
//         .build();
//
//      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(this.dataFetcher, new JsoupDataFetcher(), new FetcherDataFetcher()), session, "get");
//
//     this.cookies = response.getCookies();
//
//   }

   @Override
   protected Response fetchResponse() {
      Map<String, String> headers = new HashMap<>();
      headers.put("authority", "www.farmaciasnissei.com.br");
      headers.put("origin", "https://www.farmaciasnissei.com.br");
      headers.put("referer", session.getOriginalURL());
      headers.put("x-requested-with", "XMLHttpRequest");

      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setProxyservice(List.of(ProxyCollection.BUY_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_BR))
         .setSendUserAgent(false)
         .setCookies(cookies)
         .setHeaders(headers)
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(this.dataFetcher, new ApacheDataFetcher(), new FetcherDataFetcher()), session, "get");

      this.cookies = response.getCookies();

      return response;

      }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "div[data-produto_id]", "data-produto_id");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".mt-3 h4", false);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".small a", true);
         String primaryImage = fixUrlImage(doc, internalId);
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".dots-preview .swiper-slide img", Collections.singletonList("src"), "https", "www.farmaciasnissei.com.br", primaryImage);
         String description = CrawlerUtils.scrapElementsDescription(doc, List.of(".card #tabCollapse-descricao"));
         JSONObject json = accesAPIOffers(internalId);
         Offers offers = scrapOffers(json);
         RatingsReviews ratingsReviews = getRatingsReviews(doc);
         List<String> eans = scrapEan(doc);

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setOffers(offers)
            .setEans(eans)
            .setRatingReviews(ratingsReviews)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private JSONObject accesAPIOffers(String internalId) {
      JSONObject jsonObject = new JSONObject();
      String token = "";
      String url = "https://www.farmaciasnissei.com.br/pegar/preco";

      String cookies = CommonMethods.cookiesToString(this.cookies);

      token = CommonMethods.substring(cookies, "=", ";", true);

      Map<String, String> headers = new HashMap<>();
      headers.put("cookie", cookies);
      headers.put("content-type", "application/x-www-form-urlencoded; charset=UTF-8");
      headers.put("referer", session.getOriginalURL());
      headers.put("authority", "www.farmaciasnissei.com.br");
      headers.put("origin", "https://www.farmaciasnissei.com.br");
      headers.put("x-requested-with", "XMLHttpRequest");

      String payload = "csrfmiddlewaretoken=" + token + "&produtos_ids%5B%5D=" + internalId;

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(List.of(ProxyCollection.BUY_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_BR))
         .setSendUserAgent(false)
         .setPayload(payload)
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(this.dataFetcher, new ApacheDataFetcher(), new FetcherDataFetcher()), session, "post");

      JSONObject json = CrawlerUtils.stringToJson(response.getBody());

      JSONObject precos = json != null ? json.optJSONObject("precos") : null;

      if (precos != null && !precos.isEmpty()) {
         JSONObject dataProduct = precos.optJSONObject(internalId);
         return dataProduct != null ? dataProduct.optJSONObject("publico") : jsonObject;

      }

      return jsonObject;
   }

   private List<String> scrapEan(Document doc) {
      List<String> ean = new ArrayList<>();
      String productInfo = CrawlerUtils.scrapStringSimpleInfo(doc, "div .row div .mt-1", true);
      if (productInfo != null) {
         String[] split = productInfo.split("EAN:");
         if (split.length > 1) {
            ean.add(split[1].trim());
         }
      }

      return ean;
   }

   private boolean isProductPage(Document doc) {
      return !doc.select("div[data-produto_id]").isEmpty();
   }

   private String fixUrlImage(Document doc, String internalId) {
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".swiper-slide img", Collections.singletonList("src"), "https:", "www.farmaciasnissei.com.br");

      if (primaryImage.contains("caixa-nissei")) {
         return primaryImage.replace("caixa-nissei", internalId);

      }
      return primaryImage;
   }


   private Offers scrapOffers(JSONObject jsonInfo) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      if (jsonInfo != null && !jsonInfo.isEmpty()) {
         Pricing pricing = scrapPricing(jsonInfo);
         List<String> sales = scrapSales(jsonInfo);

         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName("Drogaria Nissei")
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(sales)
            .build());
      }
      return offers;

   }

   private List<String> scrapSales(JSONObject jsonInfo) {
      List<String> sales = new ArrayList<>();

      String firstSales = jsonInfo.optString("per_desc");

      if (firstSales != null && !firstSales.isEmpty()) {
         sales.add(firstSales);
      }

      return sales;
   }

   private Pricing scrapPricing(JSONObject jsonInfo) throws MalformedPricingException {
      Double priceFrom = !scrapSales(jsonInfo).isEmpty() ? jsonInfo.optDouble("valor_ini") : null;
      Double spotlightPrice = jsonInfo.optDouble("valor_fim");
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

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


   /*
In this store, grades are given with a double value, e.g: 4.5 instead of 5 or 4.
Therefore, the crawler structure, by accepting only integer values, which is common on most sites, will not be captured the advanced rating.
 */
   private RatingsReviews getRatingsReviews(Document doc) {
      RatingsReviews ratingsReviews = new RatingsReviews();
      ratingsReviews.setDate(session.getDate());

      Integer reviews = CrawlerUtils.scrapIntegerFromHtml(doc, ".text-muted.font-xl", true, 0);
      ratingsReviews.setTotalWrittenReviews(reviews);
      ratingsReviews.setTotalRating(reviews);
      ratingsReviews.setAverageOverallRating(CrawlerUtils.scrapDoublePriceFromHtml(doc, ".avaliacao-produto .rating-produto", null, true, ',', session));

      return ratingsReviews;

   }


}
