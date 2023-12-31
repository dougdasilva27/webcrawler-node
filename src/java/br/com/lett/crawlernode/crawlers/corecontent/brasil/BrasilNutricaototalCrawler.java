package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class BrasilNutricaototalCrawler extends Crawler {
   private static final String HOME_PAGE = "https://www.nutricaototal.com.br/";
   private static final String SELLER_FULL_NAME = "Nutrição total brasil";

   public BrasilNutricaototalCrawler(Session session) {
      super(session);
   }

   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString(),
      Card.DINERS.toString());

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "div.price-box.price-final_price", "data-product-id");
         String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, "[itemprop=\"sku\"]", true);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.page-title > span", true);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs li[class^=category]");

         ArrayList<String> images = crawlImages(doc);

         String primaryImage = !images.isEmpty() ? images.remove(0) : null;
         List<String> secondaryImages = !images.isEmpty() ? images : new ArrayList<>();

         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("div.product.attribute.overview > div > b", "div.product.attribute.overview > div"));

         boolean available = checkAvaliability(doc, "#product-addtocart-button > span");
         Offers offers = available ? scrapOffers(doc) : new Offers();

         RatingsReviews ratingReviews = crawlRating(doc, internalId);

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setOffers(offers)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setRatingReviews(ratingReviews)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".product-info-main") != null;
   }

   private ArrayList<String> crawlImages(Document doc) {
      String secondaryImages = null;

      ArrayList<String> images = new ArrayList<>();

      JSONObject productInfo = CrawlerUtils.selectJsonFromHtml(doc, ".product.media [type=\"text/x-magento-init\"]", null, null, true, false);

      if (productInfo.has("[data-gallery-role=gallery-placeholder]")) {
         productInfo = productInfo.getJSONObject("[data-gallery-role=gallery-placeholder]");
         if (productInfo.has("mage/gallery/gallery")) {
            productInfo = productInfo.getJSONObject("mage/gallery/gallery");
            if (productInfo.has("data")) {
               JSONArray imagesArray = productInfo.getJSONArray("data");
               for (int i = 0; i < imagesArray.length(); i++) {
                  JSONObject jsonImg = imagesArray.getJSONObject(i);
                  String image = jsonImg.getString("img");
                  if (image.startsWith(HOME_PAGE)) {
                     images.add(CommonMethods.sanitizeUrl(image));
                  } else {
                     images.add(CommonMethods.sanitizeUrl(HOME_PAGE + image));
                  }
               }
            }


         }

      }

      return images;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing);

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

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span[data-price-type=oldPrice] .price", null, true, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span[data-price-type=finalPrice] .price", null, true, ',', session);

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

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();

      String salesOnPrice = CrawlerUtils.calculateSales(pricing);
      if (salesOnPrice != null) {
         sales.add(salesOnPrice);
      }
      return sales;
   }

   private RatingsReviews crawlRating(Document doc, String internalId) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      String str = "https://www.nutricaototal.com.br/review/product/listAjax/id/" + internalId;

      Document docRating = sendRequest(str);

      Integer totalNumOfEvaluations = crawlNumOfEvaluations(docRating, "ol > li");
      Double avgRating = crawlAvgRating(docRating, "ol > li span[itemprop=\"ratingValue\"]");
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(docRating, "ol > li span[itemprop=\"ratingValue\"]");

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);

      return ratingReviews;
   }

   private Document sendRequest(String str) {
      Request request = RequestBuilder.create().setUrl(str).setCookies(cookies).build();
      String endpointResponseStr = this.dataFetcher.get(session, request).getBody();

      return Jsoup.parse(endpointResponseStr);
   }

   private Integer crawlNumOfEvaluations(Document doc, String selector) {
      Integer numRating = 0;
      Elements el = doc.select(selector);

      for (Element reviews : el) {
         numRating++;
      }
      return numRating;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc, String selector) {
      String starsText = "";
      Integer stars = null;

      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Elements reviews = doc.select(selector);


      for (Element review : reviews) {

         starsText = review.text().replace("%", "");
         starsText = starsText.replaceAll("[^0-9]", "");
         stars = Integer.parseInt(starsText);
         stars = (stars * 5) / 100;


         // On a html this value will be like this: (1)


         switch (stars) {
            case 5:
               star5++;
               break;
            case 4:
               star4++;
               break;
            case 3:
               star3++;
               break;
            case 2:
               star2++;
               break;
            case 1:
               star1++;
               break;
            default:
               break;
         }


      }

      return new AdvancedRatingReview.Builder()
         .totalStar1(star1)
         .totalStar2(star2)
         .totalStar3(star3)
         .totalStar4(star4)
         .totalStar5(star5)
         .build();
   }

   private Double crawlAvgRating(Document doc, String selector) {
      Double num = null;
      Elements e = doc.select(selector);

      for (Element el : e) {
         String avRating = el.text().replace("%", "");
         num = Double.parseDouble(avRating);
         num = (num * 5) / 100;
      }

      return num;
   }

   private boolean checkAvaliability(Document doc, String selector) {
      return doc.selectFirst(selector) != null;
   }
}
