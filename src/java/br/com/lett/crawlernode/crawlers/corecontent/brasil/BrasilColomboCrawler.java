package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
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

public class BrasilColomboCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.colombo.com.br";
   private static final String IMAGE_HOST = "images.colombo.com.br";

   private static final String SELLER_NAME_LOWER = "colombo";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilColomboCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(session.getOriginalURL(), doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         Elements variations = doc.select(".dados-itens-table tr[data-item]");

         for (Element sku : variations) {
            String internalId = sku.attr("data-item");
            String internalPid = scrapInternalPid(doc);
            String name = scrapVariationName(doc, "h1.nome-produto", sku);
            CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb a", true);
            String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "li.js_slide picture img[data-slide-position=0]", Arrays.asList("src", "srcset"), "https:", IMAGE_HOST);
            String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "li.js_slide picture img", Arrays.asList("src", "srcset"), "https:", IMAGE_HOST, primaryImage);
            String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#produto-descricao", "#produto-caracteristicas"));
            boolean availableToBuy = doc.selectFirst(".dados-produto-botoes:not(.hide) [value=adicionarCesta]") != null;
            Offers offers = availableToBuy ? scrapOffers(doc) : new Offers();
            RatingsReviews ratingsReviews = scrapRatingReviews(doc);

            // Stuff that were not on site when crawler was made
            Integer stock = null;
            List<String> eans = null;

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
                  .setRatingReviews(ratingsReviews)
                  .setOffers(offers)
                  .setEans(eans)
                  .build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(String url, Document doc) {
      return url.contains("/produto/") && doc.selectFirst(".detalhe-produto") != null;
   }

   private String scrapInternalPid(Document doc) {
      String internalPid = null;
      Element internalPidElement = doc.selectFirst(".codigo-produto");

      if (internalPidElement != null) {
         internalPid = internalPidElement.attr("content").trim();

         if (internalPid.isEmpty()) {
            internalPid = internalPidElement.text().replaceAll("[^0-9]", "").trim();
         }
      }

      return internalPid;
   }

   private String scrapVariationName(Document doc, String nameCssSelector, Element variationElement) {
      StringBuilder variationName = new StringBuilder();
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, nameCssSelector, true);

      if (name == null) {
         return null;
      }

      variationName.append(name);

      String volts = CrawlerUtils.scrapStringSimpleInfo(variationElement, ".caracteristicasdados-itens-table-caracteristicas > label,"
            + " .dados-itens-table-caracteristicas > label", true);

      if (volts != null) {
         variationName.append(" ");
         variationName.append(volts.contains("- R") ? volts.split("- R")[0] : volts);
      }

      return variationName.toString();
   }


   private RatingsReviews scrapRatingReviews(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalComments = CrawlerUtils.scrapIntegerFromHtml(doc, ".header-avaliacao-produto .quantidade-avaliacoes", false, 0);
      Double avgRating = scrapAvgRating(doc);
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(doc);


      ratingReviews.setTotalRating(totalComments);
      ratingReviews.setTotalWrittenReviews(totalComments);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   private Double scrapAvgRating(Document doc) {
      Double avg = 0d;

      Double percentage = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".avalicoes-count strong", null, false, ',', session);
      if (percentage != null) {
         avg = (percentage * 100) / 5;
      }
      return avg;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Elements reviews = doc.select(".avalicoes .info-item-body .tabela-barras-progresso tr");

      for (Element review : reviews) {

         Element starNumber = review.selectFirst("td:first-child");

         if (starNumber != null) {

            String sN = starNumber.text().replaceAll("[^0-9]", ""); // "1" or ""
            Integer val1 = !sN.isEmpty() ? Integer.parseInt(sN) : 0;

            Element voteNumber = review.selectFirst("td:last-child");

            if (voteNumber != null) {

               String vN = voteNumber.text().replaceAll("[^0-9]", "");
               Integer val2 = !vN.isEmpty() ? Integer.parseInt(vN) : 0;

               // On a html this value will be like this: (1)

               switch (val1) {
                  case 5:
                     star5 = val2;
                     break;
                  case 4:
                     star4 = val2;
                     break;
                  case 3:
                     star3 = val2;
                     break;
                  case 2:
                     star2 = val2;
                     break;
                  case 1:
                     star1 = val2;
                     break;
                  default:
                     break;
               }
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

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = new ArrayList<>(); // When this new offer model was implemented, no sales was found

      offers.add(OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_NAME_LOWER)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(sales)
            .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".parcelas-produto-table .parcelas-produto-table-valor", null, false, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".parcelas-produto-table .parcelas-produto-table-valor", null, false, ',', session);
      BankSlip bankSlip = BankSlipBuilder.create()
            .setFinalPrice(spotlightPrice)
            .build();
      CreditCards creditCards = scrapCreditcards(doc, spotlightPrice);

      return PricingBuilder.create()
            .setPriceFrom(priceFrom)
            .setSpotlightPrice(spotlightPrice)
            .setBankSlip(bankSlip)
            .setCreditCards(creditCards)
            .build();

   }

   private CreditCards scrapCreditcards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = scrapInstallments(doc, spotlightPrice);

      for (String card : cards) {
         creditCards.add(CreditCardBuilder.create()
               .setBrand(card)
               .setInstallments(installments)
               .setIsShopCard(false)
               .build());
      }

      return creditCards;
   }

   public Installments scrapInstallments(Document doc, Double spotlightPrice) throws MalformedPricingException {
      Installments installments = new Installments();

      Map<Integer, Double> installmentPriceMap = new HashMap<>();

      installmentPriceMap.put(1, spotlightPrice);
      Elements parcelas = doc.select(".detalhe-produto-dados__dados-comprar .parcelas-produto-table tr");

      for (Element e : parcelas) {
         Element index = e.select(".parcelas-produto-table-index").first();

         if (index != null) {
            Integer installment = Integer.parseInt(index.text().replaceAll("[^0-9]", "").trim());

            Element valor = e.select(".parcelas-produto-table-valor").first();

            if (valor != null) {
               Double value = Double.parseDouble(valor.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replace(",", ".").trim());

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
