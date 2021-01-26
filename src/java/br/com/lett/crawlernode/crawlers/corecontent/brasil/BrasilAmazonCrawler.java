package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.AmazonScraperUtils;
import br.com.lett.crawlernode.util.*;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

/**
 * Date: 15/11/2017
 *
 * @author Gabriel Dornelas
 *
 */
public class BrasilAmazonCrawler extends Crawler {

   private static final String HOST = "www.amazon.com.br";
   private static final String HOME_PAGE = "https://" + HOST;
   private static final String SELLER_NAME = "amazon.com.br";
   private static final String SELLER_NAME_2 = "amazon.com";
   private static final String SELLER_NAME_3 = "Amazon";


   private static final String IMAGES_HOST = "images-na.ssl-images-amazon.com";
   private static final String IMAGES_PROTOCOL = "https";

   protected Set<String> cards = Sets.newHashSet(Card.DINERS.toString(), Card.VISA.toString(),
      Card.MASTERCARD.toString(), Card.ELO.toString());

   public BrasilAmazonCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   private AmazonScraperUtils amazonScraperUtils = new AmazonScraperUtils(logger, session);

   @Override
   public void handleCookiesBeforeFetch() {
      this.cookies = amazonScraperUtils.handleCookiesBeforeFetch(HOME_PAGE, cookies, dataFetcher);
   }

   @Override
   protected Document fetch() {
      return amazonScraperUtils.fetchProductPage(cookies, dataFetcher);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = crawlInternalId(doc);
         String internalPid = internalId;
         String name = crawlName(doc);
         CategoryCollection categories = crawlCategories(doc);

         JSONArray images = this.amazonScraperUtils.scrapImagesJSONArray(doc);
         String primaryImage = this.amazonScraperUtils.scrapPrimaryImage(images, doc, IMAGES_PROTOCOL, IMAGES_HOST);
         String secondaryImages = this.amazonScraperUtils.scrapSecondaryImages(images, IMAGES_PROTOCOL, IMAGES_HOST);

         String description = crawlDescription(doc);
         Integer stock = null;

         Offer mainPageOffer = scrapMainPageOffer(doc);
         List<Document> docOffers = fetchDocumentsOffers(doc, internalId);
         Offers offers = scrapOffers(doc ,docOffers, mainPageOffer);

         String ean = crawlEan(doc);

         RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
         ratingReviewsCollection.addRatingReviews(crawlRating(doc, internalId));
         RatingsReviews ratingReviews = ratingReviewsCollection.getRatingReviews(internalId);

         List<String> eans = new ArrayList<>();
         eans.add(ean);

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setStock(stock)
            .setEans(eans)
            .setRatingReviews(ratingReviews)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private Offer scrapMainPageOffer(Document doc) throws OfferException, MalformedPricingException {
      String seller = CrawlerUtils.scrapStringSimpleInfo(doc, "#tabular-buybox-truncate-1 .a-truncate-full .tabular-buybox-text a", false);
      if (seller == null){
         seller = CrawlerUtils.scrapStringSimpleInfo(doc, "#tabular-buybox-truncate-1 .a-truncate-full .tabular-buybox-text", false);
      }
      String sellerUrl = CrawlerUtils.scrapUrl(doc, "#tabular-buybox-truncate-1 .a-truncate-full .tabular-buybox-text a", "href", "https", HOST);
      String sellerId = scrapSellerIdByUrl(sellerUrl);

      if (seller == null) {
         seller = CrawlerUtils.scrapStringSimpleInfo(doc, "#merchant-info", false);

         if (seller != null && seller.contains("vendido por")) {
            seller = CommonMethods.getLast(seller.split("vendido por")).trim();

            if (seller.endsWith(".")) {
               seller = seller.substring(0, seller.length() - 1);
            }
         }
      }

      if (seller != null && !seller.isEmpty()) {
         boolean isMainRetailer = seller.equalsIgnoreCase(SELLER_NAME) || seller.equalsIgnoreCase(SELLER_NAME_2) || seller.equalsIgnoreCase(SELLER_NAME_3);
         Pricing pricing = scrapMainPagePricing(doc);
         if (sellerId == null) {
            sellerId = CommonMethods.toSlug(seller);
         }

         return OfferBuilder.create()
            .setInternalSellerId(sellerId)
            .setSellerFullName(seller)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(isMainRetailer)
            .setPricing(pricing)
            .build();
      }

      return null;
   }

   private String scrapSellerIdByUrl(String sellerUrl) {
      String sellerId = null;

      if (sellerUrl != null) {
         for (String parameter : sellerUrl.split("&")) {
            if (parameter.startsWith("seller=")) {
               sellerId = parameter.split("=")[1];
               break;
            }
         }
      }

      return sellerId;
   }

   private Pricing scrapMainPagePricing(Element doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#priceblock_ourprice", null, true, ',', session);

      if (spotlightPrice == null) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#priceblock_dealprice, #priceblock_saleprice, #unifiedPrice_feature_div #conditionalPrice .a-color-price", null, false, ',', session);

         if (spotlightPrice == null) {
            spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#soldByThirdParty span", null, false, ',', session);
         }
      }

      CreditCards creditCards = scrapCreditCardsFromSellersPage(doc, spotlightPrice);
      Double savings = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#dealprice_savings .priceBlockSavingsString",
         null, false, ',', session);

      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#buyBoxInner .a-list-item span:nth-child(2n)", null, false, ',', session);
      if (savings != null) {
         priceFrom = spotlightPrice + savings;
      }

      return PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setPriceFrom(priceFrom)
         .setBankSlip(BankSlipBuilder.create().setFinalPrice(spotlightPrice).setOnPageDiscount(0d).build())
         .build();
   }

   private String scrapSellerName(Element oferta){
      String name = "";
      if(oferta != null){
         String rawSallerName = CrawlerUtils.scrapStringSimpleInfoByAttribute(oferta, ".a-button-inner input", "aria-label");

         if(rawSallerName == null){
            rawSallerName = CrawlerUtils.scrapStringSimpleInfo(oferta, ".a-button-inner span .a-offscreen", false);
         }

         String split = rawSallerName != null?rawSallerName.split("do vendedor ")[1]: null;
         name = split != null? split.split("e preço")[0]: null;
      }
      return name;
   }

   private Offers scrapOffers(Document doc, List<Document> offersPages, Offer mainPageOffer) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      int pos = 1;


      if(mainPageOffer != null){
         mainPageOffer.setSellersPagePosition(pos);
         offers.add(mainPageOffer);
         pos = 2;
      }

      if (!offersPages.isEmpty()) {
         for (Document offerPage : offersPages) {
            Elements ofertas = offerPage.select("#aod-offer");
            for (Element oferta : ofertas) {

               String name = scrapSellerName(oferta).trim();

               Pricing pricing = scrapSellersPagePricing(oferta);
               String sellerUrl = CrawlerUtils.scrapUrl(oferta, ".a-size-small.a-link-normal:first-child", "href", "https", HOST);

               String sellerId = scrapSellerIdByUrl(sellerUrl);
               boolean isMainRetailer = name.equalsIgnoreCase(SELLER_NAME) || name.equalsIgnoreCase(SELLER_NAME_2) || name.equalsIgnoreCase(SELLER_NAME_3);

               if (sellerId == null) {
                  sellerId = CommonMethods.toSlug(SELLER_NAME);
               }

               offers.add(OfferBuilder.create()
                  .setInternalSellerId(sellerId)
                  .setSellerFullName(name)
                  .setSellersPagePosition(pos)
                  .setIsBuybox(false)
                  .setIsMainRetailer(isMainRetailer)
                  .setPricing(pricing)
                  .build());

               pos++;
            }
         }
      }

      return offers;
   }

   private Pricing scrapSellersPagePricing(Element doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".a-price span", null, false, ',', session);
      if(spotlightPrice == null) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".a-price .a-offscreen", null, false, ',', session);
      }
      CreditCards creditCards = scrapCreditCardsFromSellersPage(doc, spotlightPrice);

      return PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(BankSlipBuilder.create().setFinalPrice(spotlightPrice).setOnPageDiscount(0d).build())
         .build();
   }

   private CreditCards scrapCreditCardsFromSellersPage(Element doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments regularCard = new Installments();
      regularCard.add(InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      Pair<Integer, Float> installment = CrawlerUtils.crawlSimpleInstallment(
         "#installmentCalculator_feature_div", doc, false, "x", "juro", false, ',');

      if (!installment.isAnyValueNull()) {
         regularCard.add(InstallmentBuilder.create()
            .setInstallmentNumber(installment.getFirst())
            .setInstallmentPrice(MathUtils.normalizeTwoDecimalPlaces(installment.getSecond().doubleValue()))
            .build());
      }

      for (String brand : cards) {
         creditCards.add(CreditCardBuilder.create()
            .setBrand(brand)
            .setIsShopCard(false)
            .setInstallments(regularCard)
            .build());
      }

      return creditCards;
   }

   private RatingsReviews crawlRating(Document document, String internalId) {

      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
      RatingsReviews ratingReviews = new RatingsReviews();

      if (document.select("#cm-cr-dp-no-reviews-message").isEmpty()) {
         ratingReviews.setDate(session.getDate());

         if (internalId != null) {
            Integer totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtml(document,
               "#acrCustomerReviewText, #reviews-medley-cmps-expand-head > #dp-cmps-expand-header-last > span:not([class])", true, 0);
            Double avgRating = getTotalAvgRating(document);

            ratingReviews.setInternalId(internalId);
            ratingReviews.setTotalRating(totalNumOfEvaluations);
            ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
            ratingReviews.setAverageOverallRating(avgRating);
            ratingReviews.setAdvancedRatingReview(scrapAdvancedRatingReviews(document, totalNumOfEvaluations));
         }
      }

      return ratingReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReviews(Document doc, Integer totalNumOfEvaluations) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      if (totalNumOfEvaluations > 0) {
         for (Element starElement : doc.select("#histogramTable tr")) {
            Integer star = CrawlerUtils.scrapIntegerFromHtml(starElement, ".aok-nowrap .a-size-base a", true, 0);
            Integer percentage = CrawlerUtils.scrapIntegerFromHtml(starElement, ".a-text-right .a-size-base a", true, 0);
            Integer total = percentage > 0 ? Math.round((totalNumOfEvaluations * (percentage / 100f))) : 0;

            if (star == 1) {
               star1 = total;
            } else if (star == 2) {
               star2 = total;
            } else if (star == 3) {
               star3 = total;
            } else if (star == 4) {
               star4 = total;
            } else if (star == 5) {
               star5 = total;
            }
         }
      }


      return new AdvancedRatingReview.Builder().totalStar1(star1).totalStar2(star2).totalStar3(star3).totalStar4(star4).totalStar5(star5).build();
   }

   private Double getTotalAvgRating(Document doc) {
      Double avgRating = 0d;
      Element reviews =
         doc.select("#reviewsMedley [data-hook=rating-out-of-text], #reviews-medley-cmps-expand-head > #dp-cmps-expand-header-last span.a-icon-alt")
            .first();

      String text;

      if (reviews != null) {
         text = reviews.ownText().trim();
      } else {
         text = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".reviewCountTextLinkedHistogram[title]", "title");
      }

      if (text != null && text.contains("de")) {
         String avgText = text.split("de")[0].replaceAll("[^0-9,]", "").replace(",", ".").trim();

         if (!avgText.isEmpty()) {
            avgRating = Double.parseDouble(avgText);
         }
      }

      return avgRating;
   }

   private boolean isProductPage(Document doc) {
      return doc.select("#dp").first() != null;
   }

   private String crawlInternalId(Document doc) {
      String internalId = null;

      Element internalIdElement = doc.select("input[name^=ASIN]").first();
      Element internalIdElementSpecial = doc.select("input.askAsin").first();

      if (internalIdElement != null) {
         internalId = internalIdElement.val();
      } else if (internalIdElementSpecial != null) {
         internalId = internalIdElementSpecial.val();
      }

      return internalId;
   }


   private String crawlName(Document document) {
      String name = null;

      Element nameElement = document.select("#centerCol h1#title").first();
      Element nameElementSpecial = document.select("#productTitle").first();

      if (nameElement != null) {
         name = nameElement.text().trim();
      } else if (nameElementSpecial != null) {
         name = nameElementSpecial.text().trim();
      }

      return name;
   }

   private Document fetchDocumentsOffersRequest(int page, String internalId){

      String urlMarketPlace = "https://www.amazon.com.br/gp/aod/ajax/ref=aod_page_" + page + "?asin=" + internalId + "&pageno=" + page;

      Map<String, String> headers = new HashMap<>();
      headers.put("upgrade-insecure-requests", "1");
      headers.put("referer", session.getOriginalURL());

      String response =  amazonScraperUtils.fetchPage(urlMarketPlace, headers, cookies, this.dataFetcher);
      Document doc = Jsoup.parse(response);
      headers.put("referer", urlMarketPlace);

      return doc;
   }

   /**
    * Fetch pages when have marketplace info
    *
    * @param id
    * @return documents
    */
   private List<Document> fetchDocumentsOffers(Document doc, String internalId) {
      List<Document> docs = new ArrayList<>();

      Element marketplaceUrl = doc.selectFirst("#moreBuyingChoices_feature_div .a-box.a-text-center h5 span");

      int page = 1;

      if(marketplaceUrl == null){
         marketplaceUrl = doc.selectFirst(".a-section.a-spacing-base span .a-declarative a");
      }

      if(marketplaceUrl == null){
         marketplaceUrl = doc.selectFirst(".a-box-inner .olp-text-box");
      }

      if (marketplaceUrl != null) {

         Document docMarketplace = fetchDocumentsOffersRequest(page,internalId);
         docs.add(docMarketplace);

         int totalOffers = CrawlerUtils.scrapIntegerFromHtml(docMarketplace,"#aod-filter-offer-count-string" ,false);
         Elements offers = docMarketplace.select("#aod-offer");

         if(totalOffers != offers.size()) {
            page++;
            docMarketplace = fetchDocumentsOffersRequest(page,internalId);
            docs.add(docMarketplace);
         }
      }
      return docs;
   }

   /**
    * @param document
    * @return
    */
   private CategoryCollection crawlCategories(Document document) {
      CategoryCollection categories = new CategoryCollection();
      Elements elementCategories = document.select("#wayfinding-breadcrumbs_feature_div ul li:not([class]) a");

      for (Element e : elementCategories) {
         String cat = e.ownText().trim();

         if (!cat.isEmpty()) {
            categories.add(cat);
         }
      }

      return categories;
   }

   private String crawlDescription(Document doc) {
      StringBuilder description = new StringBuilder();
      Element prodInfoElement = doc.selectFirst("#prodDetails");

      Elements elementsDescription =
         doc.select("#detail-bullets_feature_div, #detail_bullets_id, #feature-bullets, #bookDescription_feature_div, #aplus_feature_div");

      for (Element e : elementsDescription) {
         description.append(e.html().replace("noscript", "div"));
      }

      Elements longDescription = doc.select(".feature[id^=btfContent]");

      for (Element e : longDescription) {
         Element compare = e.select("#compare").first();

         if (compare == null) {
            description.append(e.html());
         }
      }

      Elements scripts = doc.select("script[type=\"text/javascript\"]");
      String token = "var iframeContent =";

      for (Element e : scripts) {
         String script = e.html();

         if (script.contains(token)) {

            if (script.contains("iframeDocument.getElementById")) {
               continue;
            }

            String iframeDesc = CrawlerUtils.extractSpecificStringFromScript(script, token, false, ";", false);

            try {
               description.append(URLDecoder.decode(iframeDesc, "UTF-8"));
            } catch (UnsupportedEncodingException ex) {
               Logging.printLogError(logger, session, CommonMethods.getStackTrace(ex));
            }

            break;
         }
      }

      if (prodInfoElement != null) {
         description.append(prodInfoElement.toString());
      }

      return description.toString();
   }

   private String crawlEan(Document doc) {
      String ean = null;

      List<String> eanKeys = Arrays.asList("código de barras:", "ean:", "eans:", "código de barras", "codigo de barras", "ean", "eans");

      Elements attributes = doc.select(".pdTab table tr:not([class]):not([id]):not(:last-child)");
      for (Element att : attributes) {
         String key = CrawlerUtils.scrapStringSimpleInfo(att, ".label", true).toLowerCase();

         if (eanKeys.contains(key)) {
            ean = CrawlerUtils.scrapStringSimpleInfo(att, ".value", true);

            break;
         }
      }

      return ean;
   }
}
