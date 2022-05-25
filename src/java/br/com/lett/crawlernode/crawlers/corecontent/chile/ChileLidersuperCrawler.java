package br.com.lett.crawlernode.crawlers.corecontent.chile;


import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.prices.Prices;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import javax.print.Doc;
import java.util.*;

public class ChileLidersuperCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.lider.cl/supermercado/";
   private static final String SELLER_NAME_LOWER = "lider";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString());

   public ChileLidersuperCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.JSOUP);
      super.config.setParser(br.com.lett.crawlernode.core.models.Parser.HTML);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   protected Response fetchResponse() {

      Map<String, String> headers = new HashMap<>();
      headers.put("authority", "www.lider.cl");
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
      Request request = Request.RequestBuilder.create().setUrl(session.getOriginalURL())
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY))
         .setSendUserAgent(true)
         .mustSendContentEncoding(true)
         .build();

      Response response = CrawlerUtils.retryRequest(request, session, dataFetcher, true);

      return response;
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, "span[itemprop=productID]", true);
         String name = scrapName(doc);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb a > span");
         List<String> images = crawlImages(internalId);
         String primaryImage = !images.isEmpty() ? images.get(0) : null;
         List<String> secondaryImages = crawlSecondaryImages(images);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#product-features"));
         boolean available = isAvailable(doc) == true;
         Offers offers = available ? scrapOffers(doc) : new Offers();
         JSONObject jsonEan = selectJsonFromHtml(doc, "script[type=\"application/ld+json\"]");
         List<String> eans = scrapEans(jsonEan);

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
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

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_NAME_LOWER)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());


      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#productPrice p[itemprop=\"highPrice\"]", null, false, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#productPrice .price", null, false, ',', session);

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


      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }
   private List<String> scrapEans(JSONObject jsonEan) {
      List<String> eans = new ArrayList<>();

      if (jsonEan.has("gtin13")) {
         eans.add(jsonEan.getString("gtin13"));
      }

      return eans;
   }

   private JSONObject selectJsonFromHtml(Document doc, String cssSelector) {
      Element element = doc.selectFirst(cssSelector);
      JSONObject json = new JSONObject();

      if (element != null) {
         String strJson = sanitizeStringJson(element.html());
         json = CrawlerUtils.stringToJson(strJson);
      }

      return json;
   }

   private String sanitizeStringJson(String text) {
      String result = null;
      String substring = null;

      if (text.contains("/*") && text.contains("*/")) {
         substring = text.substring(text.indexOf("/*"), text.indexOf("*/") + 2);
         result = text.replace(substring, "");
      }

      return result;
   }

   private boolean isProductPage(Document doc) {
      return doc.select(".product-info") != null && doc.selectFirst(".product-info .no-available") == null;
   }

   private String scrapName(Document doc) {
      StringBuilder name = new StringBuilder();

      Elements names = doc.select(".product-info h1 span, .product-profile h1 span");
      for (Element e : names) {
         name.append(e.ownText().trim()).append(" ");
      }

      return name.toString().trim();
   }

   private List<String> crawlImages(String id) {
      List<String> images = new ArrayList<>();

      Request request = RequestBuilder.create()
         .setUrl("https://wlmstatic.lider.cl/contentassets/galleries/" + id + ".xml")
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY))
         .setCookies(cookies)
         .build();
      Document docXml = Jsoup.parse(this.dataFetcher.get(session, request).getBody(), "", Parser.xmlParser());

      Elements items = docXml.getElementsByTag("image");
      for (Element e : items) {
         String image = e.text();

         if (image.contains("file:/")) {

            String format = "";

            if (image.contains(".jpg")) {
               format = "=format[jpg]";
            }

            images.add("http://images.lider.cl/wmtcl?source=url[" + image + "]&sink" + format);
         }
      }

      return images;
   }

   private List<String> crawlSecondaryImages(List<String> images) {
      List<String> secondaryImages2 = new ArrayList<>();

      if (images.size() > 1) {
         JSONArray imagesArray = new JSONArray();

         for (int i = 1; i < images.size(); i++) {
            imagesArray.put(images.get(i));
            secondaryImages2.add(images.get(i));
         }
      }

      return secondaryImages2;
   }

   private Boolean isAvailable(Document doc) {
      String noStock = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "p#pdp-no-stock", "class");

      if (noStock.equals("agotado")) {
         return false;
      }
      return true;
   }
}
