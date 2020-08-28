package br.com.lett.crawlernode.crawlers.corecontent.florianopolis;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.AngelonieletroUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.*;
import models.prices.Prices;
import models.pricing.*;
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.Map.Entry;

/**
 * This crawler uses WebDriver to detect when we have sku variations on the same page.
 * <p>
 * Sku price crawling notes: 1) The payment options can change between different card brands. 2) The
 * ecommerce share the same cad payment options between sku variations. 3) The cash price (preço a
 * vista) can change between sku variations, and it's crawled from the main page. 4) To get card
 * payments, first we perform a POST request to get the list of all card brands, then we perform one
 * POST request for each card brand.
 * <p>
 * org.openqa.selenium.StaleElementReferenceException:
 * http://www.angeloni.com.br/eletro/p/lavadora-de-roupas-brastemp-11kg-ative-bwl11a-branco-2344440
 * normal: http://www.angeloni.com.br/eletro/p/lava-e-seca-lg-85kg-wd1485at-aco-escovado-3868980
 *
 * @author Samir Leao
 */



public class FlorianopolisAngelonieletroCrawler extends Crawler {

   private final String HOME_PAGE = "https://www.angeloni.com.br/";
   private static final String SELLER_FULL_NAME = "Angeloni Eletro";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public FlorianopolisAngelonieletroCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      boolean shouldVisit = false;
      shouldVisit = !FILTERS.matcher(href).matches()
         && (href.startsWith("http://www.angeloni.com.br/eletro/") || href.startsWith("https://www.angeloni.com.br/eletro/"));

      return shouldVisit;
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#titulo[data-productid]", "data-productid");
         String mainId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#titulo[data-sku]", "data-sku");

         Document voltageAPi = AngelonieletroUtils.fetchVoltageApi(internalPid, mainId, session, cookies, dataFetcher);
         Elements voltageVariation = voltageAPi.select("#formGroupVoltage input[name=voltagem]");


         if (!voltageVariation.isEmpty()) {
            for (Element e : voltageVariation) {
               Product p = crawlProduct(AngelonieletroUtils.fetchSkuHtml(doc, e, mainId, session, cookies, dataFetcher));
               String variationName = CrawlerUtils.scrapStringSimpleInfo(voltageAPi, "label[for=" + e.attr("id") + "]", true);

               if (variationName != null && !p.getName().toLowerCase().contains(variationName.toLowerCase())) {
                  p.setName(p.getName() + " " + variationName);
               }
               products.add(p);
            }
         } else {
            products.add(crawlProduct(doc));
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   /*******************************
    * Product page identification *
    *******************************/

   private boolean isProductPage(Document doc) {
      return !doc.select("#titulo").isEmpty();
   }
   


   private Product crawlProduct(Document doc) throws Exception {
      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#titulo[data-productid]", "data-productid");
      String internalId = AngelonieletroUtils.crawlInternalId(doc);
      String name = crawlName(doc);
      boolean available = doc.selectFirst("#btnComprarDesc") != null;
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#imagem-grande > div[data-zoom-image]", Arrays.asList("data-zoom-image"),
         "https:", "dy3cxdqdg9dx0.cloudfront.net");
      String secondaryImages = crawlSecondaryImages(doc, primaryImage);
      Integer stock = null;
      String description = crawlDescription(doc);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li:not(:first-child) a span");
      RatingsReviews ratingReviews = crawlRating(doc, internalId);
      List<String> eans = crawlEan(doc);
      Offers offers = available ? scrapOffer(doc) : new Offers();

      return ProductBuilder.create()
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
   }


   private String crawlName(Document document) {
      String name = null;

      Element elementName = document.select("#titulo [itemprop=name]").first();
      if (elementName != null) {
         name = elementName.text();
      }

      return name;
   }



   private String crawlSecondaryImages(Document document, String primaryImage) {
      String secondaryImages = null;

      Elements elementsSecondaryImages = document.select("#galeria .thumbImage:not(.active) div[onclick]");
      JSONArray secondaryImagesArray = new JSONArray();

      for (Element e : elementsSecondaryImages) {
         String onclick = e.attr("onclick");

         if (onclick.contains("'//")) {
            int x = onclick.indexOf("('") + 2;
            int y = onclick.indexOf("',", x);

            String image = CrawlerUtils.completeUrl(onclick.substring(x, y), "https:", "dy3cxdqdg9dx0.cloudfront.net");
            if (!image.equalsIgnoreCase(primaryImage)) {
               secondaryImagesArray.put(image);
            }
         }
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   private String crawlDescription(Document document) {
      String description = null;
      Elements elementsDescription = document.select("section#abas .tab-content div[role=tabpanel]:not([id=tab-avaliacoes-clientes])");
      description = elementsDescription.html();

      return description;
   }

   private List<String> crawlEan(Document doc) {
      String ean = null;
      Elements elmnts = doc.select(".tab-content .tab-pane .caracteristicas tbody tr");

      for (Element e : elmnts) {
         String aux = e.text();

         if (aux.contains("EAN")) {
            aux = aux.replaceAll("[^0-9]+", "");

            if (!aux.isEmpty()) {
               ean = aux;
            }
         }
      }

      return Arrays.asList(ean);
   }

   private RatingsReviews crawlRating(Document doc, String internalId) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = CrawlerUtils.scrapSimpleInteger(doc, ".avaliacoes > span", true);
      Double avgRating = CrawlerUtils.scrapSimplePriceDoubleWithDots(doc, "#starsProductDescription > span", true);
      AdvancedRatingReview advacedRatingReview = scrapAdvancedRatingReview(doc);

      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations != null ? totalNumOfEvaluations : 0);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations != null ? totalNumOfEvaluations : 0);
      ratingReviews.setAverageOverallRating(avgRating != null ? avgRating : 0);
      ratingReviews.setAdvancedRatingReview(advacedRatingReview);

      return ratingReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Elements reviews = doc.select(".avaliacoes li .avaliacao");
      for (Element review : reviews) {

         Element elementStarNumber = review.selectFirst(".estrelas");

         if (elementStarNumber != null) {

            String stringStarNumber = elementStarNumber.attr("data-estrelas");
            String sN = stringStarNumber.replaceAll("[^0-9]", "").trim();
            Integer numberOfStars = !sN.isEmpty() ? Integer.parseInt(sN) : 0;

            switch (numberOfStars) {
               case 5:
                  star5 += 1;
                  break;
               case 4:
                  star4 += 1;
                  break;
               case 3:
                  star3 += 1;
                  break;
               case 2:
                  star2 += 1;
                  break;
               case 1:
                  star1 += 1;
                  break;
               default:
                  break;
            }
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

   private Offers scrapOffer(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = new ArrayList<>();

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
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".esquerda div span", null, false, '.', session);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(null)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();


   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

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

}


