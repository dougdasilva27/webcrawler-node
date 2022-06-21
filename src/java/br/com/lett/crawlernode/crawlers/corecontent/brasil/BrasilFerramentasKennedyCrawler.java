package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchUtilities;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.YourreviewsRatingCrawler;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.apache.http.HttpHeaders;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilFerramentasKennedyCrawler extends Crawler {
   public BrasilFerramentasKennedyCrawler(Session session) {
      super(session);
   }

   private static final String SELLER_FULL_NAME = "Ferramentas Kennedy";

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      if (doc.selectFirst("span.sku") != null) {

         Elements variants = doc.select("select.form-control option:not(:first-child)");
         if (variants.size() > 0) {
            for (Element variant : variants) {
               String variantUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(variant, "option", "value");
               variantUrl = "https://www.ferramentaskennedy.com.br/" + variantUrl + "/true";
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
         Logging.printLogDebug(logger, session, "Not a product page:   " + this.session.getOriginalURL());
      }
      return products;
   }

   protected Document fetchDoc(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("x-requested-with", "XMLHttpRequest");
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();
      Response response = this.dataFetcher.get(session, request);

      return Jsoup.parse(response.getBody());
   }

   private boolean mustAddProduct(String url, List<Product> products) {
      for (Product product : products) {
         if (product.getUrl().equals(url)) {
            return false;
         }
      }
      return true;
   }

   private Product addProduct(Document doc, String url) throws MalformedProductException, OfferException, MalformedPricingException {
      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "div.top input[id*=produto]", "value");
      String internalPid = crawlInternalPid(doc);

      List<String> images = crawlImage(doc);
      String primaryImage = !images.isEmpty() ? images.remove(0) : null;

      CategoryCollection categoryCollection = CrawlerUtils.crawlCategories(doc, "li[itemprop=itemListElement]");

      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.title-product", true);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("div.descricao-prod"));
      boolean available = doc.selectFirst("div.product-config button.btn-success") != null;
      Offers offers = available ? scrapOffers(doc) : new Offers();
      RatingsReviews ratingReviews = crawlRating(internalId);

      Product product = ProductBuilder.create()
         .setUrl(session.getOriginalURL())
         .setInternalPid(internalPid)
         .setInternalId(internalId)
         .setName(name)
         .setOffers(offers)
         .setRatingReviews(ratingReviews)
         .setPrimaryImage(primaryImage)
         .setCategories(categoryCollection)
         .setSecondaryImages(images)
         .setDescription(description)
         .build();
      return product;
   }

   private List<String> crawlImage(Document doc) {
      List<String> imagesLista = new ArrayList<>();
      Elements images = doc.select("div[id=custom-dots] .item");
      for (Element e : images) {
         String img = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "div", "style");
         String regex = "'(.*)'";

         Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
         Matcher matcher = pattern.matcher(img);
         if (matcher.find()) {
            imagesLista.add(matcher.group(1).replace("90", "1200"));
         }
      }
      return imagesLista;
   }

   private RatingsReviews crawlRating(String internalId) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      YourreviewsRatingCrawler yourReviews = new YourreviewsRatingCrawler(session, cookies, logger);
      Document docRating = yourReviews.crawlPageRatingsFromYourViews(internalId, "9d67f294-34c7-4661-b56d-affc18bd5d98", dataFetcher);
      Integer totalNumOfEvaluations = getTotalNumOfRatingsFromYourViews(docRating, "div.yv-star-reviews span.yv-span");
      Double avgRating = getTotalAvgRatingFromYourViews(docRating, "strong[style]");

      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);

      return ratingReviews;
   }

   private Integer getTotalNumOfRatingsFromYourViews(Document doc, String cssSelector) {
      Integer totalRating = 0;
      Element totalRatingElement = doc.select(cssSelector).first();

      if (totalRatingElement != null) {
         String totalText = totalRatingElement.text().replaceAll("[^0-9]", "").trim();

         if (!totalText.isEmpty()) {
            totalRating = Integer.parseInt(totalText);
         }
      }
      return totalRating;
   }

   private Double getTotalAvgRatingFromYourViews(Document docRating, String cssSelector) {
      Double avgRating = 0d;
      Element rating = docRating.select(cssSelector).first();

      if (rating != null) {
         avgRating = MathUtils.parseDoubleWithDot(rating.text().trim());
      }
      return avgRating;
   }

   private String crawlInternalPid(Document doc) {
      String id = CrawlerUtils.scrapStringSimpleInfo(doc, "span.sku", true);
      String regex = ": ([0-9]*)";

      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      Matcher matcher = pattern.matcher(id);
      if (matcher.find()) {
         return matcher.group(1);
      }
      return null;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();

      Pricing pricing = scrapPricing(doc);
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSales(sales)
         .setSellerFullName(SELLER_FULL_NAME)
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div.product-price p", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div.product-price del", null, false, ',', session);

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

      Set<String> cards = Sets.newHashSet(Card.AMEX.toString(), Card.AURA.toString(), Card.DINERS.toString(), Card.ELO.toString(),
         Card.HIPERCARD.toString(), Card.HIPERCARD.toString(), Card.VISA.toString());

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }
}
