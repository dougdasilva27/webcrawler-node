package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BrasilDafitiCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.dafiti.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "dafiti";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilDafitiCrawler(Session session) {
      super(session);
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

         // Nome
         Elements elementPreName = doc.select("h1.product-name");
         String preName = elementPreName.text().replace("'", "").replace("’", "").trim();
         CategoryCollection categoryCollection = CrawlerUtils.crawlCategories(doc, "ul.breadcrumb2", true);


         // Imagem primária e imagens secundárias
         Elements elementPrimaryImage = doc.select(".gallery-thumbs ul.carousel-items").select("a");
         String primaryImage = null;
         String secondaryImages = null;
         JSONArray secondaryImagesArray = new JSONArray();

         for (Element e : elementPrimaryImage) {

            if (primaryImage == null) {
               primaryImage = e.attr("data-img-zoom");
            } else {
               secondaryImagesArray.put(e.attr("data-img-zoom"));
            }

         }

         if (secondaryImagesArray.length() > 0) {
            secondaryImages = secondaryImagesArray.toString();
         }

         // Descrição
         String description = "";
         Elements elementDescription = doc.select(".product-information-content");
         description = elementDescription.first().text().replace(".", ".\n").replace("'", "").replace("’", "").trim();

         Element elementSku = doc.select("#add-to-cart input[name=p]").first();

         try {
            String sku = elementSku.attr("value");

            // Pegando os produtos usando o endpoint da Dafiti

            String url = "https://www.dafiti.com.br/catalog/detailJson?sku=" + sku + "&_=1439492531368";

            Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
            JSONObject json = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

            JSONArray sizes = json.has("sizes") ? json.getJSONArray("sizes") : new JSONArray();

            /*
             * Pegar o restante das informações usando os objetos JSON vindos do endpoint da dafit
             */
            for (int i = 0; i < sizes.length(); i++) {

               // ID interno
               String internalId = sizes.getJSONObject(i).getString("sku");

               // Pid
               String internalPid = internalId.split("-")[0];

               // Nome - pré-nome pego anteriormente, acrescido do tamanho do sapato
               String name = preName + " (tamanho " + sizes.getJSONObject(i).getString("name") + ")";

               RatingsReviews ratingReviews = scrapRatingReviews(doc);

               Offers offers = scrapOffers(doc, json);

               Product product = ProductBuilder.create()
                  .setUrl(this.session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setCategories(categoryCollection)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
                  .setDescription(description)
                  .setOffers(offers)
                  .setRatingReviews(ratingReviews)
                  .build();

               products.add(product);

            }
         } catch (Exception e1) {
            e1.printStackTrace();
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private Offers scrapOffers(Document doc, JSONObject json) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc, json);

      String sellerName = MAIN_SELLER_NAME_LOWER;
      Element sellerNameElement = doc.select(".product-seller-name-link").first();

      if (sellerNameElement != null) {
         sellerName = sellerNameElement.ownText().toLowerCase();
      }


      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(sellerName)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(sellerName.toLowerCase(Locale.ROOT).equals(MAIN_SELLER_NAME_LOWER))
         .setPricing(pricing)
         .build());

      return offers;

   }

   /***********
    * Product page identification *
    ***********/

   private boolean isProductPage(Document document) {
      return (document.select(".container.product-page").first() != null);
   }

   private Pricing scrapPricing(Document doc, JSONObject skuJson) throws MalformedPricingException {

      Pricing pricing = null;

      Element priceElement = doc.select(".catalog-detail-price-value").first();

      if (priceElement != null) {
         Double price = MathUtils.parseDoubleWithComma(priceElement.ownText());
         CreditCards creditCards = scrapCard(doc, skuJson);
         pricing = Pricing.PricingBuilder.create()
            .setSpotlightPrice(price)
            .setCreditCards(creditCards)
            .build();
      }

      return pricing;
   }

   private CreditCards scrapCard(Document doc, JSONObject skuJson) throws MalformedPricingException {

      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      if (skuJson.has("installments")) {
         JSONObject installmentsObj = skuJson.getJSONObject("installments");

         if (installmentsObj.has("count") && installmentsObj.has("value")) {
            String installment = installmentsObj.get("count").toString().replaceAll("[^0-9]", "").trim();
            Double priceInstallment = MathUtils.parseDoubleWithComma(installmentsObj.get("value").toString());

            if (!installment.isEmpty() && priceInstallment != null) {
               installments.add(Installment.InstallmentBuilder.create()
                  .setInstallmentNumber(Integer.parseInt(installment))
                  .setInstallmentPrice(priceInstallment)
                  .build());
            }
         }

         for (String card : cards) {
            creditCards.add(CreditCard.CreditCardBuilder.create()
               .setBrand(card)
               .setInstallments(installments)
               .setIsShopCard(false)
               .build());
         }
      }
      return creditCards;
   }

   private RatingsReviews scrapRatingReviews(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalComments = scrapTotalComments(doc);
      Double avgRating = scrapAvgRating(doc);

      ratingReviews.setTotalRating(totalComments);
      ratingReviews.setTotalWrittenReviews(totalComments);
      ratingReviews.setAverageOverallRating(avgRating);

      return ratingReviews;
   }

   private Integer scrapTotalComments(Document doc) {
      Integer totalComments = 0;
      String total = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".ratings-reviews-component.ratings-aggregated a", "title");
      if (total != null) {
         int a = total.indexOf("com") + 4;
         int b = total.indexOf("avaliações") - 1;
         if (a < b) totalComments = MathUtils.parseInt(total.substring(a, b));
      } else {
         totalComments = CrawlerUtils.scrapIntegerFromHtml(doc, ".ratings-reviews-component.hide-mobile.clearflix h2", true, 0);
      }
      return totalComments;
   }

   private Double scrapAvgRating(Document doc) {
      Double avg = 0d;

      String avgr = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".ratings-reviews-component.ratings-aggregated a", "title");
      if (avgr != null) {
         avg = MathUtils.parseDoubleWithDot(avgr.substring(avgr.indexOf("nota") + 5, avgr.indexOf("de") - 1));
      }
      return avg;
   }
}
