package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SaopauloDrogaraiaCrawler extends Crawler {


   private static final String SELLER_NAME_LOWER = "Droga Raia";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public SaopauloDrogaraiaCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      String HOME_PAGE = "http://www.drogaraia.com.br/";
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (doc.selectFirst(".product-view") != null) {
         Logging.printLogDebug(
            logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, "tbody .data", true);
         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".page-header-container .header-minicart .novarnish", "data-productid");
         String name = getName(doc);
         List<String> categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs ul li:not(.home):not(.product) a");
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".product-description"));
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-image-gallery img", Arrays.asList("data-zoom-image"), "https://", "www.drogaraia.com.br/");
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".product-image-gallery img", Arrays.asList("data-zoom-image"), "https://", "www.drogaraia.com.br/", primaryImage);
         String ean = scrapEan(doc);
         RatingsReviews ratingReviews = crawRating(doc, internalId);
         boolean available = doc.selectFirst(".product-shop.boxPBM .add-to-cart") != null;
         Offers offers = available ? scrapOffers(doc) : new Offers();

         List<String> eans = new ArrayList<>();
         eans.add(ean);

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setRatingReviews(ratingReviews)
            .setOffers(offers)
            .setEans(eans)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }


   /* Brief explanation of the function
   The number of units and the size of the product must be captured in the crawler (ex: 15ml);

    1 - in some cases the number of units is already in the title but the size (15 ml) is only in the subtitle
    -> in this case, the crawler checks to make a split in "-" and checks if the first index is already in the name;
    2 - in other cases, only the quantity is given and nothing should be added to the name
    -> in this case the crawler checks through the regex if it is stated in the number of units in the name
    3 - In another case, the name does not contain anything of quantity
    -> in this case, the crawler adds the entire subtitle to the name;
    4 - In the last case, when the product is unavailable, the name is elsewhere;
    -> which is the last else, taking only the title name;

    follow the examples:

    https://www.drogaraia.com.br/pampers-premium-care-tamanho-grande-com-68-tiras.html, in this case, the crawler checks whether the quantity is listed in the name;
    https://www.drogaraia.com.br/pantene-ampola-de-tratamento-gold-com-3-unidades-15-ml-cada.html, in this case, the crawler splits the "-" and checks whether the first part repeats in the name;
    https://www.drogaraia.com.br/always-absorvente-externo-active-com-abas-leve-32-com-preco-especial.html, in this case the crawler the crawler adds the entire subtitle to the name
     */

   private String getName(Document doc) {

      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name h1 span", false);
      String quantity = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-attributes .quantidade.show-hover", true);
      if (name != null && quantity != null) {

         if (quantity.contains("-")) {
            String[] quantitySplit = quantity.split(" -");
            String quantityCompare = quantitySplit[0];

            if (name.contains(quantityCompare)) {
               return quantitySplit.length > 0 ? name + " " + quantitySplit[1] : null;

            } else {
               return name + " " + quantity;
            }

         }
         Pattern r = Pattern.compile("[0-9]+");
         Matcher m = r.matcher(quantity);
         if (m.find()) {
            if (name.contains(m.group(0))) {
               return name;

            } else {
               return name + " " + quantity;
            }
         }

      } else {
         String nameWithStore = CrawlerUtils.scrapStringSimpleInfo(doc, "head title", true);
         if (nameWithStore != null) {
            name = nameWithStore.split("\\|")[0];
         }
         return name;
      }
      return name;
   }

   private String scrapEan(Element e) {
      String ean = null;
      Elements trElements = e.select(".farmaBox .data-table tr");

      if (trElements != null && !trElements.isEmpty()) {
         for (Element tr : trElements) {
            if (tr.text().contains("EAN")) {
               Element td = tr.selectFirst("td");
               ean = td != null ? td.text().trim() : null;
            }
         }
      }

      return ean;
   }

   private List<String> scrapSales(Pricing pricing, Document doc) {
      List<String> sales = new ArrayList<>();
      String sale = CrawlerUtils.calculateSales(pricing);
      if (sale != null) {
         sales.add(sale);
      }

      sales.add(scrapPromotion(doc));

      return sales;
   }

   private String scrapPromotion(Document doc) {
      StringBuilder stringBuilder = new StringBuilder();
      String qty = CrawlerUtils.scrapStringSimpleInfo(doc, ".product_label .qty", true);
      String price = CrawlerUtils.scrapStringSimpleInfo(doc, ".product_label .price span", false);

      if (qty != null && price != null) {
         stringBuilder.append(qty + " ");
         stringBuilder.append(price);
      }

      return stringBuilder.toString();
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing, doc);

      String sellerName = CrawlerUtils.scrapStringSimpleInfo(doc, ".sold-and-delivered a", false);
      boolean isMainSeller = sellerName != null && sellerName.equals(SELLER_NAME_LOWER);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(sellerName)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(isMainSeller)
         .setPricing(pricing)
         .setSales(sales)
         .build());


      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-info .old-price .price .cifra", null, false, ',', session);
      Double spotlightPrice = getPrice(doc);
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

   private Double getPrice(Document doc) {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-info .special-price .price span:nth-child(2)", null, false, ',', session);
      if (spotlightPrice == null) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-info .regular-price span:nth-child(2) ", null, true, ',', session);
      }

      return spotlightPrice;
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


   private RatingsReviews crawRating(Document doc, String internalId) {
      TrustvoxRatingCrawler trustVox = new TrustvoxRatingCrawler(session, "71450", logger);
      RatingsReviews ratingsReviews = trustVox.extractRatingAndReviews(internalId, doc, dataFetcher);
      if (ratingsReviews.getTotalReviews() == 0) {
         ratingsReviews = scrapAlternativeRating(internalId);
      }

      return ratingsReviews;
   }

   private JSONObject alternativeRatingFetch(String internalId) {

      String urlApi = "https://trustvox.com.br/widget/root?&code=" + internalId + "&store_id=71450&product_extra_attributes[group]=";

      Map<String,String> headers = new HashMap<>();
      headers.put("Accept","application/vnd.trustvox-v2+json");
      headers.put("Referer","https://www.drogaraia.com.br/");
      headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.182 Safari/537.36");

      Request request = RequestBuilder.create().setHeaders(headers).setUrl(urlApi).build();
      String jsonString = this.dataFetcher.get(session, request).getBody();

      return JSONUtils.stringToJson(jsonString);
   }

   private RatingsReviews scrapAlternativeRating(String internalId) {

      RatingsReviews ratingsReviews = new RatingsReviews();

      JSONObject jsonRating = alternativeRatingFetch(internalId);
      JSONObject storeRate = JSONUtils.getJSONValue(jsonRating,"store_raate");

      if (storeRate != null && !storeRate.isEmpty()) {

         double avgReviews = JSONUtils.getDoubleValueFromJSON(storeRate, "average", true);
         int totalRating = JSONUtils.getIntegerValueFromJSON(storeRate, "count", 0);

         ratingsReviews.setAverageOverallRating(MathUtils.normalizeTwoDecimalPlaces(avgReviews));
         ratingsReviews.setTotalRating(totalRating);
      }
      return ratingsReviews;
   }
}

