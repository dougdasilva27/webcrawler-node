package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchUtilities;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.exceptions.MalformedUrlException;
import br.com.lett.crawlernode.util.*;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
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
      super.config.setParser(Parser.HTML);
   }

   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.HIPERCARD.toString());
   private final String sellerName = getSellerName();

   protected String getSellerName() {
      return session.getOptions().optString("Seller");
   }

   private final String homePage = getHomePage();

   protected String getHomePage() {
      return session.getOptions().optString("HomePage");
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(homePage));
   }

   protected boolean acceptCatalog = isAcceptCatalog();

   private boolean isAcceptCatalog() {
      return session.getOptions().optBoolean("accept_catalog", true);
   }

   private boolean isOwnProduct() {
      Pattern pattern = Pattern.compile("/p/");
      Matcher matcher = pattern.matcher(session.getOriginalURL());
      if (matcher.find()) {
         Logging.printLogDebug(logger, session, "Is a own product " + this.session.getOriginalURL());
         return false;
      } else {
         Logging.printLogDebug(logger, session, "Is not a own product " + this.session.getOriginalURL());
         return true;
      }
   }
   protected Document fetchDoc(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.USER_AGENT, FetchUtilities.randUserAgent());
      if (acceptCatalog || isOwnProduct()) {
         Request request = Request.RequestBuilder.create()
            .setUrl(url)
            .setCookies(cookies)
            .setHeaders(headers)
            .build();
         Response response = this.dataFetcher.get(session, request);

         return Jsoup.parse(response.getBody());
      }else{
         return new Document("");
      }
   }

   @Override
   protected Response fetchResponse() {
      String href = session.getOriginalURL().toLowerCase();
      if(!href.startsWith(homePage)) {
         throw new MalformedUrlException("URL não corresponde ao market");
      }
      return super.fetchResponse();
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      doc = fetchDoc(session.getOriginalURL());
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         Elements variants = doc.select(".ui-pdp-thumbnail.ui-pdp-variations--thumbnail");
         if (variants.size() > 0) {
            for (Element variant : variants) {
               String variantUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(variant, ".ui-pdp-thumbnail.ui-pdp-variations--thumbnail", "href");
               variantUrl = homePage + variantUrl;
               if (!mustAddProduct(variantUrl, products)) {
                  continue;
               }
               Document variantDoc = fetchDoc(variantUrl);
               Product p = addProduct(variantDoc, variantUrl);
               products.add(p);
            }
         } else {
            Product p = addProduct(doc, session.getOriginalURL());
            products.add(p);
         }


      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean mustAddProduct(String url, List<Product> products) {
      for (Product product : products) {
         if (product.getUrl().equals(url)) {
            return false;
         }
      }

      return true;
   }

   private boolean isProductPage(Document doc) {
      return !doc.select("h1.ui-pdp-title").isEmpty();
   }

   private Product addProduct(Document doc, String url) throws MalformedProductException, MalformedPricingException, OfferException {
      JSONObject initialState = selectJsonFromHtml(doc);
      JSONObject schema = initialState != null ? JSONUtils.getValueRecursive(initialState, "schema.0", JSONObject.class) : null;
      String internalPid = schema.optString("productID");
      String internalId;

      Element variationElement = doc.selectFirst("input[name='variation']");
      if (variationElement != null) {
         internalId = internalPid + '_' + variationElement.attr("value");
      } else {
         internalId = schema.optString("sku");
      }
      String name = scrapName(doc);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".andes-breadcrumb__item a");
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".ui-pdp-description", ".ui-pdp-specs"));
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "figure.ui-pdp-gallery__figure img", Arrays.asList("data-zoom", "src"), "https:", "http2.mlstatic.com");
      List<String> secondaryImages = crawlImages(primaryImage, doc);

      boolean availableToBuy = isAvailable(doc);
      Offers offers = availableToBuy ? scrapOffers(doc) : new Offers();

      Product product = ProductBuilder.create()
         .setUrl(url)
         .setInternalId(internalId)
         .setInternalPid(internalPid)
         .setName(name)
         .setDescription(description)
         .setCategories(categories)
         .setPrimaryImage(primaryImage)
         .setSecondaryImages(secondaryImages)
         .setOffers(offers)
         .build();
      return product;
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

      Elements variationsElements = doc.select(".ui-pdp-variations__selected-label span");

      if (!variationsElements.isEmpty()) {

         for (Element e : variationsElements) {
            String colorOrVolts = e.ownText().trim();
            if (!productName.contains(colorOrVolts)) {
               name.append(" ").append(e.ownText().trim());
            }
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
      String adStatus = CrawlerUtils.scrapStringSimpleInfo(doc, ".andes-message__text.andes-message__text--warning", true);
      if (adStatus != null && adStatus.contains("Anúncio pausado")) {
         return false;
      }
      return !doc.select(".ui-pdp-price").isEmpty();

   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = new ArrayList<>();

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(sellerName)
         .setMainPagePosition(1)
         .setIsMainRetailer(true)
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
      Integer priceFraction = CrawlerUtils.scrapIntegerFromHtml(doc, ".andes-money-amount.ui-pdp-price__part.ui-pdp-price__original-value.andes-money-amount--previous  .andes-money-amount__fraction", false, 0);
      Integer priceCents = CrawlerUtils.scrapIntegerFromHtml(doc, ".andes-money-amount.ui-pdp-price__part.ui-pdp-price__original-value.andes-money-amount--previous .andes-money-amount__cents ", false, 0);

      if (priceFraction == 0){
         return null;
      }

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
