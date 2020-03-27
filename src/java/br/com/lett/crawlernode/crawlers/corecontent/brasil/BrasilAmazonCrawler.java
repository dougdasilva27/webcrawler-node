package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.AmazonScraperUtils;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.prices.Prices;
import models.pricing.BankSlip;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

/**
 * Date: 15/11/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilAmazonCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.amazon.com.br";
   private static final String SELLER_NAME_LOWER = "amazon.com.br";

   private static final String IMAGES_HOST = "images-na.ssl-images-amazon.com";
   private static final String IMAGES_PROTOCOL = "https";
   
   protected Set<String> cards = Sets.newHashSet(Card.DINERS.toString(), Card.VISA.toString(), 
         Card.MASTERCARD.toString(), Card.ELO.toString());

   public BrasilAmazonCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
      super.config.setFetcher(FetchMode.FETCHER);
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

         List<Document> docMarketPlaces = fetchDocumentsOffers(doc, internalId);
         Map<String, Prices> marketplaceMap = crawlMarketplaces(docMarketPlaces, doc);
         
         String ean = crawlEan(doc);
         Offers offers = scrapOffers(doc, docMarketPlaces);

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
   
   private Offers scrapOffers(Document doc, List<Document> offersPages) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      
      for(int i = 0; i < offersPages.size(); i++) {
         Document offerPage = offersPages.get(i);
         Elements ofertas = offerPage.select("#olpOfferList .olpOffer");
         
         for(Element oferta : ofertas) {
            String name = CrawlerUtils.scrapStringSimpleInfo(oferta, "h3.olpSellerName", false);
            Pricing pricing = scrapPricing(oferta);
            
            if(name.isEmpty()) {
               name = CrawlerUtils.scrapStringSimpleInfoByAttribute(oferta, "h3.olpSellerName img", "alt");
            }
            
            if(name.toLowerCase().equals(SELLER_NAME_LOWER)) {
              offers.add(OfferBuilder.create()
                  .setUseSlugNameAsInternalSellerId(true)
                  .setSellerFullName(name)
                  .setMainPagePosition(i+1)
                  .setIsBuybox(false)
                  .setIsMainRetailer(true)
                  .setPricing(pricing)
                  .build());
            } else {
               offers.add(OfferBuilder.create()
                     .setUseSlugNameAsInternalSellerId(true)
                     .setSellerFullName(name)
                     .setMainPagePosition(i+1)
                     .setIsBuybox(false)
                     .setIsMainRetailer(false)
                     .setPricing(pricing)
                     .build());
            }
         }
      }
      
      return offers;
   }
   
   private Pricing scrapPricing(Element doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".olpOfferPrice", null, false, ',', session);
      CreditCards creditCards = scrapCreditCardsFromProductPage(doc, spotlightPrice);
      BankSlip bankSlip = scrapBankslip(doc, spotlightPrice);
      
      return PricingBuilder.create()
            .setSpotlightPrice(spotlightPrice)
            .setCreditCards(creditCards)
            .build();
   }
   
   private CreditCards scrapCreditCardsFromProductPage(Element doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      
      // TODO: here
      Installments regularCard = scrapInstallments(doc, "");
      if (regularCard.getInstallments().isEmpty()) {
         regularCard.add(InstallmentBuilder.create()
               .setInstallmentNumber(1)
               .setInstallmentPrice(spotlightPrice)
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
   
   // TODO
   private Installments scrapInstallments(Element doc, String selector) throws MalformedPricingException {
      Installments installments = new Installments();

      return installments;
   }
   
   // TODO
   private BankSlip scrapBankslip(Element doc, Double spotlightPrice) throws MalformedPricingException {
      return BankSlipBuilder.create()
            .build();
   }

   private RatingsReviews crawlRating(Document document, String internalId) {

      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
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

      if (reviews != null) {
         String text = reviews.ownText().trim();

         if (text.contains("de")) {
            String avgText = text.split("de")[0].replaceAll("[^0-9,]", "").replace(",", ".").trim();

            if (!avgText.isEmpty()) {
               avgRating = Double.parseDouble(avgText);
            }
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

   private Float crawlPriceForPrincipalSeller(Document document) {
      Float price = null;
      Element salePriceElement = document.select(".a-box .a-section.a-spacing-none.a-padding-none .a-color-price").first();
      Element specialPrice = document.select("#priceblock_dealprice").first();
      Element foodPrice = document.select("#priceblock_ourprice").first();

      if (salePriceElement != null) {
         price = MathUtils.parseFloatWithComma(salePriceElement.text().trim());
      } else {
         salePriceElement = document.select("#buybox .a-color-price").first();

         if (salePriceElement != null) {
            price = MathUtils.parseFloatWithComma(salePriceElement.ownText().trim());
         }
      }

      if (price == null && specialPrice != null) {
         price = MathUtils.parseFloatWithComma(specialPrice.ownText().trim());
      } else if (price == null && foodPrice != null) {
         price = MathUtils.parseFloatWithComma(foodPrice.ownText());
      }

      return price;
   }

   /**
    * Fetch pages when have marketplace info
    * 
    * @param id
    * @return documents
    */
   private List<Document> fetchDocumentsOffers(Document doc, String internalId) {
      List<Document> docs = new ArrayList<>();

      Element marketplaceUrl = doc.selectFirst("#moreBuyingChoices_feature_div");

      if (marketplaceUrl != null) {
         String urlMarketPlace = HOME_PAGE + "/gp/offer-listing/" + internalId + "/ref=olp_page_next?ie=UTF8&f_all=true&f_new=true&startIndex=0";

         if (!urlMarketPlace.contains("amazon.com")) {
            urlMarketPlace = HOME_PAGE + urlMarketPlace;
         }

         Map<String, String> headers = new HashMap<>();
         headers.put("upgrade-insecure-requests", "1");
         headers.put("referer", session.getOriginalURL());

         Document docMarketplace = Jsoup.parse(amazonScraperUtils.fetchPage(urlMarketPlace, headers, cookies, this.dataFetcher));
         docs.add(docMarketplace);

         headers.put("referer", urlMarketPlace);

         Element nextPage = docMarketplace.select(".a-last:not(.a-disabled)").first();
         int page = 1;

         while (nextPage != null) {
            String nextUrl = HOME_PAGE + "/gp/offer-listing/" + internalId + "/ref=olp_page_next?ie=UTF8&f_all=true&f_new=true&startIndex=" + page * 10;

            Document nextDocMarketPlace = Jsoup.parse(amazonScraperUtils.fetchPage(nextUrl, headers, cookies, this.dataFetcher));
            docs.add(nextDocMarketPlace);
            nextPage = nextDocMarketPlace.select(".a-last:not(.a-disabled)").first();
            headers.put("referer", nextUrl);

            page++;
         }

      }

      return docs;
   }

   private String crawlPrincipalSeller(Document doc) {
      String principalSeller = null;

      Element name = doc.select("#merchant-info").first();
      Element nameSpecial = doc.select("#merchant-info > a").first();

      if (nameSpecial != null) {
         principalSeller = nameSpecial.ownText().toLowerCase().trim();
      } else if (name != null) {
         String text = name.ownText().toLowerCase().trim();

         if (text.contains("por")) {
            int x = text.indexOf("por") + 3;

            if (text.contains("embalagem")) {
               int y = text.indexOf("embalagem", x);

               principalSeller = text.substring(x, y).trim();
            } else {
               principalSeller = text.substring(x).trim();
            }


            if (principalSeller.endsWith(".")) {
               principalSeller = principalSeller.substring(0, principalSeller.length() - 1);
            }
         }
      }

      return principalSeller;
   }

   private Map<String, Prices> crawlMarketplaces(List<Document> docsMarketplaceInfo, Document doc) {
      Map<String, Prices> marketplace = new HashMap<>();

      String principalSellerFrontPage = crawlPrincipalSeller(doc);

      for (Document docMarketplaceInfo : docsMarketplaceInfo) {
         Elements lines = docMarketplaceInfo.select(".a-row.olpOffer");
         
         for (Element linePartner : lines) {
            Element name = linePartner.select(".olpSellerName a").first();
            Element nameImg = linePartner.select(".olpSellerName img").first();
            Element priceS = linePartner.select(".olpOfferPrice").first();

            if ((name != null || nameImg != null) && priceS != null) {
               String partnerName = nameImg != null ? nameImg.attr("alt").trim().toLowerCase() : name.text().trim().toLowerCase();
               Float partnerPrice = MathUtils.parseFloatWithComma(priceS.ownText());

               if (partnerName.equals(principalSellerFrontPage)) {
                  marketplace.put(partnerName, crawlPrices(doc, null));
               } else {
                  marketplace.put(partnerName, crawlPrices(doc, partnerPrice));
               }
            }
         }
      }

      if (principalSellerFrontPage != null && !marketplace.containsKey(principalSellerFrontPage)) {
         marketplace.put(principalSellerFrontPage, crawlPrices(doc, null));
      }

      return marketplace;
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

            String iframeDesc = CrawlerUtils.extractSpecificStringFromScript(script, token, ";", false);

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

   /**
    * 
    * @param doc
    * @param price
    * @return
    */
   private Prices crawlPrices(Document doc, Float price) {
      Prices prices = new Prices();

      Map<Integer, Float> installments = new HashMap<>();

      if (price != null) {
         installments.put(1, price);
         prices.setBankTicketPrice(price);
      } else {
         Float frontPagePrice = crawlPriceForPrincipalSeller(doc);
         if (frontPagePrice != null) {
            installments.put(1, frontPagePrice);
            prices.setBankTicketPrice(price);
         }

         Elements pricesElement = doc.select("div.a-popover-preload[id^=a-popover] > div > table:not([border]) tr");

         if (pricesElement.isEmpty()) {
            pricesElement = doc.select("div.a-popover-preload[id^=a-popover] > table:not([border]) tr");
         }

         for (Element e : pricesElement) {
            Elements info = e.select("td");

            if (info.size() > 1) {
               String installment = info.get(0).ownText().replaceAll("[^0-9]", "").trim();
               Float value = MathUtils.parseFloatWithComma(info.get(1).ownText());

               if (!installment.isEmpty() && value != null) {
                  installments.put(Integer.parseInt(installment), value);

                  if (installment.equals("1")) {
                     prices.setBankTicketPrice(value);
                  }
               }
            }
         }
      }

      if (!installments.isEmpty()) {
         for(String card : cards) {
            prices.insertCardInstallment(card, installments);
         }
      }

      return prices;
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
