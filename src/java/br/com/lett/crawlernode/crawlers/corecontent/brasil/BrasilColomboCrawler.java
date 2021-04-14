package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import models.AdvancedRatingReview;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilColomboCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.colombo.com.br";
   private static final String IMAGE_HOST = "images.colombo.com.br";

   private static final String SELLER_NAME_LOWER = "colombo";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilColomboCrawler(Session session) {
      super(session);
   }

   @Override
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

         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#codigo-produto-async", "value");
         String sellerCode = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[id=seller]", "value");
         JSONObject jsonProduct = fetchProductJson(internalPid, sellerCode);
         JSONArray variations = jsonProduct.optJSONArray("itens");

         if (variations != null && !variations.isEmpty()) {
            for (Object e : variations) {
               if (e instanceof JSONObject) {
                  JSONObject sku = (JSONObject) e;

                  String internalId = sku.optString("codigo");
                  String name = scrapVariationName(jsonProduct, sku);
                  CategoryCollection categories = scrapCategories(jsonProduct);
                  String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "li.js_slide picture img[data-slide-position=0]", Arrays.asList("src", "srcset"), "https:", IMAGE_HOST);
                  List<String> secondaryImages = scrapSecondaryImages(doc, primaryImage);
                  String description = jsonProduct.optString("breveDescricao");

                  boolean availableToBuy = sku.optString("descricaoTipoEstoque").equalsIgnoreCase("Em estoque");
                  Offers offers = availableToBuy ? scrapOffers(jsonProduct, sku) : new Offers();
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
            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(String url, Document doc) {
      return url.contains("/produto/") && doc.selectFirst(".detalhe-produto") != null;
   }

   private JSONObject fetchProductJson(String internalPid, String sellerCode) {
      String url = "https://www.colombo.com.br/api/dados/produto?produto=" + internalPid + "&codigoSeller=" + sellerCode;

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .build();

      String response = this.dataFetcher.get(session, request).getBody();

      return CrawlerUtils.stringToJson(response);
   }

   private String scrapVariationName(JSONObject product, JSONObject sku) {
      return product.optString("descricao")
         + " - "
         + sku.optString("descricao");
   }

   private CategoryCollection scrapCategories(JSONObject product) {
      CategoryCollection categories = new CategoryCollection();

      String categoriesKey = product.optString("categoria");

      if (!categoriesKey.isEmpty()) {
         String[] splittedCategories = categoriesKey.split(";");
         categories.addAll(Arrays.asList(splittedCategories));
      }

      return categories;
   }

   private List<String> scrapSecondaryImages(Document doc, String primaryImage) {
      List<String> secondaryImagesList = new ArrayList<>();
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "li.js_slide picture img", Arrays.asList("src", "srcset"), "https:", IMAGE_HOST, primaryImage);

      if (secondaryImages != null) {
         String[] splittedArray = secondaryImages
            .replace("\"", "")
            .replace("[", "")
            .replace("]", "")
            .split(",");

         secondaryImagesList.addAll(Arrays.asList(splittedArray));
      }

      return secondaryImagesList;
   }


   private RatingsReviews scrapRatingReviews(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalComments = CrawlerUtils.scrapIntegerFromHtml(doc, ".header-avaliacao-produto .quantidade-avaliacoes", false, 0);
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(doc);
      Double avgRating = CrawlerUtils.extractRatingAverageFromAdvancedRatingReview(advancedRatingReview);


      ratingReviews.setTotalRating(totalComments);
      ratingReviews.setTotalWrittenReviews(totalComments);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
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

   private Offers scrapOffers(JSONObject product, JSONObject sku) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(product, sku);
      List<String> sales = new ArrayList<>(); // When this new offer model was implemented, no sales was found

      String sellerName = sku.optString("nomeSeller").toLowerCase();
      Boolean isMainSeller = sellerName.equals(SELLER_NAME_LOWER);

      offers.add(OfferBuilder.create()
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

   private Pricing scrapPricing(JSONObject product, JSONObject sku) throws MalformedPricingException {
      BankSlip bankSlip;
      CreditCards creditCards;
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(product, "precoDe", false);
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(sku,"precoFormatado",false);

      //this was necessary because the priceFrom is not from a especific offer. So, if the priceFrom is lower than spotlightPrice
      //this offer does not have priceFrom. The same goes to bankSlip and creditCard offers.
      if (priceFrom != null && priceFrom < spotlightPrice) {
         priceFrom = null;
         bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
         creditCards = scrapCreditcards(product, spotlightPrice);
      } else {
         bankSlip = CrawlerUtils.setBankSlipOffers(JSONUtils.getDoubleValueFromJSON(product, "precoBoleto", false), null);
         creditCards = scrapCreditcards(product, null);
      }

      return PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();

   }

   private CreditCards scrapCreditcards(JSONObject product, Double installmentPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = scrapInstallments(product, installmentPrice);

      for (String card : cards) {
         creditCards.add(CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   public Installments scrapInstallments(JSONObject product, Double installmentPrice) throws MalformedPricingException {
      Installments installments = new Installments();

      if (installmentPrice == null) {
         JSONArray installmentsJson = product.optJSONArray("parcelasSemJuros");

         for (Object e : installmentsJson) {
            if (e instanceof JSONObject) {
               JSONObject installment = (JSONObject) e;

               Integer installmentNumber = JSONUtils.getIntegerValueFromJSON(installment, "parcela", 1);
               Double installmentPriceJson = JSONUtils.getDoubleValueFromJSON(installment, "valor", false);

               installments.add(InstallmentBuilder.create()
                  .setInstallmentNumber(installmentNumber)
                  .setInstallmentPrice(installmentPriceJson)
                  .build());
            }
         }
      } else {
         installments.add(InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(installmentPrice)
            .build());
      }

      return installments;
   }
}
