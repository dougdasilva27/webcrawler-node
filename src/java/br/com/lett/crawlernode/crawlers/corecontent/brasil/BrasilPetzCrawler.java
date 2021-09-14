package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
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
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * date: 27/03/2018
 *
 * @author gabriel
 */

public class BrasilPetzCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.petz.com.br/";
   private static final String SELLER_FULL_NAME = "Petz";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());


   public BrasilPetzCrawler(Session session) {
      super(session);
//      super.config.setFetcher(FetchMode.WEBDRIVER);
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String internalPid = crawlInternalPid(doc);
         CategoryCollection categories = crawlCategories(doc);
         Elements variations = doc.select(".opt_radio_variacao[data-urlvariacao]");

         if (variations.size() > 1) {
            Logging.printLogInfo(logger, session, "Page with more than one product.");
            for (Element e : variations) {
               String nameVariation = crawlNameVariation(e);

               if (e.hasClass("active")) {
                  Product p = crawlProduct(doc, nameVariation);
                  p.setInternalPid(internalPid);
                  p.setCategory1(categories.getCategory(0));
                  p.setCategory2(categories.getCategory(1));
                  p.setCategory3(categories.getCategory(2));

                  products.add(p);
               } else {
                  String url = (HOME_PAGE + e.attr("data-urlvariacao")).replace("br//", "br/");
                  Document docVariation = fetchPage(url);

                  Product p = crawlProduct(docVariation, nameVariation);
                  p.setInternalPid(internalPid);
                  p.setCategory1(categories.getCategory(0));
                  p.setCategory2(categories.getCategory(1));
                  p.setCategory3(categories.getCategory(2));

                  products.add(p);
               }
            }
         } else {
            Logging.printLogInfo(logger, session, "Page with only on product.");
            Product p = crawlProduct(doc, null);
            p.setInternalPid(internalPid);
            p.setCategory1(categories.getCategory(0));
            p.setCategory2(categories.getCategory(1));
            p.setCategory3(categories.getCategory(2));

            products.add(p);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page: " + this.session.getOriginalURL());
      }

      return products;
   }

   private Document fetchPage(String url) {
      Request request = Request.RequestBuilder.create().setCookies(cookies).setUrl(url).build();
      Response response = dataFetcher.get(session, request);

      return Jsoup.parse(response.getBody());
   }

   private Product crawlProduct(Document doc, String nameVariation) throws Exception {
      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String name = crawlName(doc, nameVariation);
         String internalId = crawlInternalId(doc);

         String description = crawlDescription(doc);
         String primaryImage = crawlPrimaryImage(doc);
         List<String> secondaryImages = crawlSecondaryImages(doc, primaryImage);
         List<String> eans = crawlEans(doc);
         RatingsReviews ratingReviews = crawlRating(doc);
         boolean available = doc.select(".is_available").first() != null;
         Offers offers = available ? scrapOffers(doc) : new Offers();

         return ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setEans(eans)
            .setRatingReviews(ratingReviews)
            .setOffers(offers)
            .build();

      }

      return new Product();
   }

   private List<String> crawlEans(Document doc) {
      List<String> eans = new ArrayList<>();
      Element meta = doc.selectFirst("meta[itemprop=\"gtin13\"]");

      if (meta != null) {
         eans.add(meta.attr("content"));
      }

      return eans;
   }

   /*******************************
    * Product page identification *
    *******************************/

   private boolean isProductPage(Document document) {
      return document.select(".prod-info").first() != null;
   }

   /*******************
    * General methods *
    *******************/

   private String crawlNameVariation(Element e) {
      String nameVariation = null;

      Element name = e.select("label > div").first();
      if (name != null) {
         nameVariation = name.ownText().replace("\"", "");
      }

      return nameVariation;
   }

   private RatingsReviews crawlRating(Document docProduct) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setInternalId(crawlInternalId(docProduct));
      ratingReviews.setDate(session.getDate());
      Integer totalRating = getTotalNumOfRatings(docProduct);
      ratingReviews.setTotalRating(totalRating);
      ratingReviews.setTotalWrittenReviews(totalRating);
      ratingReviews.setAverageOverallRating(totalRating > 0 ? getTotalAvgRating(docProduct) : 0d);

      return ratingReviews;
   }

   private Integer getTotalNumOfRatings(Document doc) {
      return doc.select(".ancora-depoimento").size();
   }

   /**
    * @param document
    * @return
    */
   private Double getTotalAvgRating(Document doc) {
      Double avgRating = 0d;

      JSONObject productJson = CrawlerUtils.selectJsonFromHtml(doc, "script", "\"extraInfo\":", ",", true, false);

      if (productJson.has("rating")) {
         avgRating = productJson.getDouble("rating");
      }

      return avgRating;
   }

   private String crawlInternalId(Document doc) {
      String internalId = null;

      Element sku = doc.select(".prod-info .reset-padding").first();
      if (sku != null) {
         internalId = sku.ownText().replace("\"", "").trim();
      }

      return internalId;
   }

   private String crawlInternalPid(Document doc) {
      String internalPid = null;

      Element pid = doc.select("#prodNotificacao").first();
      if (pid != null) {
         internalPid = pid.val();
      }

      return internalPid;
   }

   private String crawlName(Document doc, String nameVariation) {
      StringBuilder name = new StringBuilder();

      Element nameElement = doc.select("h1[itemprop=name]").first();

      if (nameElement != null) {
         name.append(nameElement.ownText());

         if (nameVariation != null) {
            name.append(" " + nameVariation);
         }
      }

      return name.toString();
   }


   private String crawlPrimaryImage(Document doc) {
      String primaryImage = null;

      Element image = doc.select(".sp-wrap a").first();

      if (image != null) {
         primaryImage = CrawlerUtils.sanitizeUrl(image, "href", "https:", "www.petz.com.br");
      }

      return primaryImage;
   }

   private List<String> crawlSecondaryImages(Document doc, String primaryImage) {
      List<String> secondaryImages = new ArrayList<>();

      Elements images = doc.select(".slick-list.draggable a");

      for (Element e : images) {
         String image = CrawlerUtils.sanitizeUrl(e, "href", "https:", "www.petz.com.br");

         if (image != null && !image.contains("youtube") && !secondaryImages.contains(image)) {
            if (!image.equals(primaryImage)) {
               secondaryImages.add(image);
            }
         }

      }


      return secondaryImages;
   }

   private CategoryCollection crawlCategories(Document document) {
      CategoryCollection categories = new CategoryCollection();
      Elements elementCategories = document.select("#breadcrumbList li[itemprop] a span");

      for (int i = 1; i < elementCategories.size(); i++) { // first item is the home page
         categories.add(elementCategories.get(i).text().trim());
      }

      return categories;
   }

   private String crawlDescription(Document doc) {
      StringBuilder description = new StringBuilder();

      Element prodInfo = doc.selectFirst(".col-md-7.prod-info > div:not([class])");
      if (prodInfo != null) {
         description.append(prodInfo.html());
      }

      Elements elementsInformation = doc.select(".infos, #especificacoes");
      for (Element e : elementsInformation) {
         if (e.select(".depoimento, #depoimentos, .depoimentoTexto").isEmpty()) {
            description.append(e.html());
         }
      }

      return description.toString();
   }


   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
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

      Element salesOneElement = doc.selectFirst(".badgeDesconto.badgeDescontoRed");
      String firstSales = salesOneElement != null ? salesOneElement.text() : null;

      if (firstSales != null && !firstSales.isEmpty()) {
         sales.add(firstSales);
      }

      return sales;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".opt-box .de-riscado", null, false, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".opt-box .price-current", null, true, ',', session);
      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);

      BankSlip bankTicket = BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bankTicket)
         .build();
   }

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = scrapInstallments(doc);
      if (installments.getInstallments().isEmpty()) {
         installments.add(InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   public Installments scrapInstallments(Document doc) throws MalformedPricingException {
      Installments installments = new Installments();

      Element installmentsCard = doc.selectFirst(".price-current .de-apagado");

      String installmentCard = installmentsCard != null ? installmentsCard.text() : null;

      if (installmentCard != null) {

         Integer ouIndex = installmentCard.contains("OU") ? installmentCard.indexOf("OU") : null;
         Integer xIndex = installmentCard.contains("x") ? installmentCard.lastIndexOf("x") : null;

         if (ouIndex != null && xIndex != null) {
            int installment = Integer.parseInt(installmentCard.substring(ouIndex, xIndex).replaceAll("[^0-9]", "").trim());

            String valueCard = installmentsCard.text();
            Integer deIndex = valueCard != null && valueCard.contains("R$") ? valueCard.indexOf("R$") : null;

            if (deIndex != null) {

               Double value = MathUtils.parseDoubleWithComma(valueCard.substring(deIndex));

               installments.add(InstallmentBuilder.create()
                  .setInstallmentNumber(installment)
                  .setInstallmentPrice(value)
                  .build());
            }
         }
      }
      return installments;
   }

}
