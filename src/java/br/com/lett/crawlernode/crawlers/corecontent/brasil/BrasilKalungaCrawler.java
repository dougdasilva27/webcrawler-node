package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

public class BrasilKalungaCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.kalunga.com.br/";
   private static final String SELLER_FULL_NAME = "Kalunga";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilKalungaCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
   }

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

         String internalId = crawlInternalId(doc);
         String internalPid = null;
         String name = crawlName(doc);

         CategoryCollection categories = crawlCategories(doc);
         String primaryImage = crawlPrimaryImage(doc);
         String secondaryImages = crawlSecondaryImages(doc, primaryImage);
         String description = crawlDescription(doc);
         Integer stock = null;
         RatingsReviews ratingReviews = crawRating(doc);
         boolean available = crawlAvailability(doc);
         Offers offers = available ? offers(doc, internalId) : new Offers();

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
            .setRatingReviews(ratingReviews)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean isProductPage(Document doc) {
      return doc.select("input#hdnProduto").first() != null;
   }

   private String crawlInternalId(Document doc) {
      String internalId = null;

      Element internalIdElement = doc.select("input#hdnProduto").first();
      if (internalIdElement != null) {
         internalId = internalIdElement.val();
      }

      return internalId;
   }

   private String crawlName(Document document) {
      String name = null;
      Element nameElement = document.select("#header-infos-produto h1").first();

      if (nameElement != null) {
         name = nameElement.ownText().trim();
      }

      return name;
   }

   private String crawlPrimaryImage(Document doc) {
      String primaryImage = null;
      Element elementPrimaryImage = doc.select(".carousel-produto-grande .item figure > a").first();

      if (elementPrimaryImage != null) {
         primaryImage = elementPrimaryImage.attr("href");
      }

      return primaryImage;
   }

   /**
    * Quando este crawler foi feito, nao tinha imagens secundarias
    *
    * @param doc
    * @return
    */
   private String crawlSecondaryImages(Document doc, String primaryImage) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      Elements images = doc.select(".carousel-produto-grande .item figure > a");

      for (Element e : images) {
         String image = e.attr("href");

         if (!image.equals(primaryImage)) {
            secondaryImagesArray.put(image);
         }
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   /**
    * @param document
    * @return
    */
   private CategoryCollection crawlCategories(Document document) {
      CategoryCollection categories = new CategoryCollection();
      Elements elementCategories = document.select(".breadcrumbs > a");

      for (int i = 1; i < elementCategories.size(); i++) {
         String cat = elementCategories.get(i).ownText().trim();

         if (!cat.isEmpty()) {
            categories.add(cat);
         }
      }

      return categories;
   }

   private String crawlDescription(Document doc) {
      StringBuilder description = new StringBuilder();

      Element elementDescription = doc.select("#ctl00_Body_dvEspecificacaoAdicionalTop").first();

      if (elementDescription != null) {
         description.append(elementDescription.html());
      }

      Element specs = doc.select("#descricaoPadrao").first();

      if (specs != null) {
         description.append(specs.html());
      }

      return description.toString();
   }

   private boolean crawlAvailability(Document doc) {
      return doc.select(".btn-comprar").first() != null;
   }

   private RatingsReviews crawRating(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());



      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(doc);
      Integer totalNumOfEvaluations = CrawlerUtils.extractReviwsNumberOfAdvancedRatingReview(advancedRatingReview);
      Double avgRating = MathUtils.normalizeNoDecimalPlacesUp(CrawlerUtils.extractRatingAverageFromAdvancedRatingReview(advancedRatingReview));

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
      Map<Integer, Integer> starsCount = new HashMap<>();

      int totalOfEvaluations = CrawlerUtils.scrapIntegerFromHtml(doc, "#total_avaliacoes", true, 0);

      Elements starsProgress = doc.select(".grafics__item");
      for (Element starProg : starsProgress) {

         int starNumber = CrawlerUtils.scrapIntegerFromHtml(starProg, ".grafics__text--small", true, 0);

         if (starNumber > 0 && starNumber < 6) {
            String style = CrawlerUtils.scrapStringSimpleInfoByAttribute(starProg, ".grafics__progress--bar", "style");
            Double percentage = MathUtils.parseDoubleWithDot(style.replaceAll("[^0-9.]", ""));

            if (percentage != null) {
               Double count = MathUtils.normalizeNoDecimalPlaces((totalOfEvaluations * (percentage / 100d)));
               starsCount.put(starNumber, count.intValue());
            }
         }

      }
      return new AdvancedRatingReview.Builder()
         .allStars(starsCount)
         .build();
   }

   private Offers offers(Document doc, String internalId) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc, internalId);
      List<String> sales = scrapSales(doc);

      offers.add(OfferBuilder.create()
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

   private List<String> scrapSales(Document doc) {
      List<String> sales = new ArrayList<>();

      Element salesOneElement = doc.selectFirst(".badge.badge-success");
      String firstSales = salesOneElement != null ? salesOneElement.text() : null;

      if (firstSales != null && !firstSales.isEmpty()) {
         sales.add(firstSales);
      }

      return sales;
   }

   private Pricing scrapPricing(Document doc, String internalId) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#desconto_content_info_produto p del", null, false, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".produtoinfos__price", null, false, ',', session);
      CreditCards creditCards = scrapCreditCards(doc, internalId, spotlightPrice);
      BankSlip bankSlip = BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private CreditCards scrapCreditCards(Document doc, String internalId, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      if (spotlightPrice != null) {

         Installments installments = scrapInstallments(doc);
         installments.add(InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());

         for (String card : cards) {
            creditCards.add(CreditCardBuilder.create()
               .setBrand(card)
               .setInstallments(installments)
               .setIsShopCard(false)
               .build());
         }
      }

      return creditCards;
   }

   public Installments scrapInstallments(Document doc) throws MalformedPricingException {
      Installments installments = new Installments();

      Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment("#container_produto #parcelamento", doc, true);

      if (!pair.isAnyValueNull()) {
         installments.add(InstallmentBuilder.create()
            .setInstallmentNumber(pair.getFirst())
            .setInstallmentPrice(MathUtils.normalizeTwoDecimalPlaces(pair.getSecond().doubleValue()))
            .build());
      }

      return installments;
   }

}
