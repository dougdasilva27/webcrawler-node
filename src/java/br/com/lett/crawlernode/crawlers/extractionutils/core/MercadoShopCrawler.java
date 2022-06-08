package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchUtilities;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.*;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MercadoShopCrawler extends Crawler {

   public MercadoShopCrawler(Session session) {
      super(session);
   }

   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.HIPERCARD.toString());

   private final String sellerName = getSellerName();
   private final String homePage = getHomePage();

   protected String getSellerName() {
      return session.getOptions().optString("Seller");
   }

   protected String getHomePage() {
      return session.getOptions().optString("HomePage");
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(homePage));
   }

   @Override
   protected Object fetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.USER_AGENT, FetchUtilities.randUserAgent());

      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setCookies(cookies)
         .setHeaders(headers)
         .build();

      return Jsoup.parse(this.dataFetcher.get(session, request).getBody());
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONObject initialState = selectJsonFromHtml(doc);
         JSONObject schema = initialState != null ? JSONUtils.getValueRecursive(initialState, "schema.0", JSONObject.class) : null;
         if (schema != null) {
            String internalPid = schema.optString("productID");
            String internalId;
            Element variationElement = doc.selectFirst("input[name='variation']");
            if (variationElement != null && (!doc.select(".ui-pdp-variations .ui-pdp-variations__picker:not(.ui-pdp-variations__picker-single) a").isEmpty() || !doc.select(".andes-dropdown__popover ul li").isEmpty())) {
               internalId = internalPid + '_' + variationElement.attr("value");
            } else {
               internalId = schema.optString("sku");
            }

            String name = scrapName(doc);
            CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".andes-breadcrumb__item a");
            String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "figure.ui-pdp-gallery__figure img", Arrays.asList("data-zoom", "src"), "https:", "http2.mlstatic.com");
            List<String> secondaryImages = crawlImages(primaryImage, doc);
            String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".ui-pdp-description", ".ui-pdp-specs"));

            boolean availableToBuy = isAvailable(doc);
            //Offers offers = availableToBuy ? scrapOffers(doc) : new Offers();

            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setDescription(description)
               .setCategories(categories)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               //.setOffers(offers)
               .build();
            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return !doc.select("h1.ui-pdp-title").isEmpty();
   }

   public JSONObject selectJsonFromHtml(Document doc) {

      if (doc == null)
         throw new IllegalArgumentException("Argument doc cannot be null");
      String token = "window.__PRELOADED_STATE__";
      JSONObject object = null;
      Elements scripts = doc.select("script");

      for (Element e : scripts) {
         String script = e.html();

         if (script.contains(token)) {
            String stringToConvertInJson;
            if (script.contains("shopModel")) {
               stringToConvertInJson = getObject(script);
               if (!stringToConvertInJson.isEmpty()) {
                  object = CrawlerUtils.stringToJson(stringToConvertInJson);
               }
            } else {
               stringToConvertInJson = getObjectSecondOption(script);
               object = CrawlerUtils.stringToJson(stringToConvertInJson);
            }
            break;
         }
      }

      return object;
   }

   private String getObjectSecondOption(String script) {
      String json = null;
      Pattern pattern = Pattern.compile("initialState\":(.*)?,\"csrfToken\"");
      Matcher matcher = pattern.matcher(script);
      if (matcher.find()) {
         json = matcher.group(1);
      }
      return json;
   }

   private String getObject(String script) {
      String json = null;
      Pattern pattern = Pattern.compile("initialState\":(.*)?,\"site\"");
      Matcher matcher = pattern.matcher(script);
      if (matcher.find()) {
         json = matcher.group(1);
      }
      return json;
   }

   private String scrapName(Document doc) {
      String productName = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.ui-pdp-title", true);
      StringBuilder name = new StringBuilder();
      name.append(productName);

      Elements variationsElements = doc.select(".ui-pdp-variations__selected-label");

      if (!variationsElements.isEmpty()) {

         for (Element e : variationsElements) {
            name.append(" ").append(e.ownText().trim());
         }

      } else if (!doc.select(".andes-dropdown__popover ul li").isEmpty()) {

         name.append(" ").append(CrawlerUtils.scrapStringSimpleInfo(doc, ".ui-pdp-dropdown-selector__item--label", true));

      }
      return name.toString();
   }

   private List<String> crawlImages(String primaryImage, Document doc) {
      List<String> images = CrawlerUtils.scrapSecondaryImages(doc, "figure.ui-pdp-gallery__figure img", Arrays.asList("data-zoom", "src"), "htps", "http2.mlstatic.com", primaryImage);
      List<String> secondaryImages = new ArrayList<>();
      if (!images.isEmpty()) {
         for (String secondaryImage : images) {
            secondaryImages.add(secondaryImage.replace(".webp", ".jpg"));
         }
      }
      return secondaryImages;
   }

   private boolean isAvailable(Document doc) {
      return !doc.select(".ui-pdp-stock-information__title").isEmpty();
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = new ArrayList<>();

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(sellerName)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = scrapPriceFrom(doc);
      Double spotlightPrice = scrapSpotlightPrice(doc);

      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);
      BankSlip bankTicket = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bankTicket)
         .build();
   }

   private Double scrapSpotlightPrice(Element doc) {
      Integer priceFraction = CrawlerUtils.scrapIntegerFromHtml(doc, ".ui-pdp-price__second-line .andes-money-amount__fraction", false, 0);
      Integer priceCents = CrawlerUtils.scrapIntegerFromHtml(doc, ".ui-pdp-price__second-line .andes-money-amount__cents", false, 0);

      return priceFraction + (double) priceCents / 100;
   }
   private Double scrapPriceFrom(Element doc) {
      Integer priceFraction = CrawlerUtils.scrapIntegerFromHtml(doc, ".ui-pdp-price__second-line .andes-money-amount__fraction", false, 0);
      Integer priceCents = CrawlerUtils.scrapIntegerFromHtml(doc, ".ui-pdp-price__second-line .andes-money-amount__cents", false, 0);

      return priceFraction + (double) priceCents / 100;
   }

   private CreditCards scrapCreditCards(Element doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = scrapInstallments(doc);
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

   public Installments scrapInstallments(Element doc, String selector) throws MalformedPricingException {
      Installments installments = new Installments();

      Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(selector, doc, false);
      if (!pair.isAnyValueNull()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(pair.getFirst())
            .setInstallmentPrice(MathUtils.normalizeTwoDecimalPlaces(((Float) pair.getSecond()).doubleValue()))
            .build());
      }

      return installments;
   }
   public Installments scrapInstallments(Element doc) throws MalformedPricingException {

      Installments installments = scrapInstallments(doc, ".ui-pdp-payment--md .ui-pdp-media__title");

      if (installments == null || installments.getInstallments().isEmpty()) {
         return scrapInstallmentsV2(doc);
      }
      return installments;
   }

   public Installments scrapInstallmentsV2(Element doc) throws MalformedPricingException {

      return scrapInstallments(doc, ".ui-pdp-container__row--payment-summary .ui-pdp-media__title");
   }

}
