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
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.AdvancedRatingReview;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Date: 11/08/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilElonutricaoCrawler extends Crawler {

   private final String HOME_PAGE = "http://www.elonutricao.com.br/";

   public BrasilElonutricaoCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
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
         Float price = crawlPrice(doc);
         Prices prices = crawlPrices(price, doc);
         boolean available = crawlAvailability(doc);
         CategoryCollection categories = crawlCategories(doc);
         String primaryImage = crawlPrimaryImage(doc);
         String secondaryImages = crawlSecondaryImages(doc);
         String description = crawlDescription(doc);
         Integer stock = null;
         Marketplace marketplace = crawlMarketplace();
         RatingsReviews ratingReviews = crawRating(doc);
         // Creating the product
         Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
               .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
               .setStock(stock).setMarketplace(marketplace).setRatingReviews(ratingReviews).build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean isProductPage(Document doc) {
      return doc.select("#prod_padd").first() != null;
   }

   private String crawlInternalId(Document doc) {
      String internalId = null;

      Element internalIdElement = doc.select("#ProdutoId").first();
      if (internalIdElement != null) {
         internalId = internalIdElement.val();
      } else {
         Element urlMeta = doc.selectFirst("meta[property=\"og:url\"]");

         if (urlMeta != null) {
            internalId = CommonMethods.getLast(urlMeta.attr("content").split("/"));
         }
      }

      return internalId;
   }

   private String crawlName(Document document) {
      String name = null;
      Element nameElement = document.select(".prod_tit").first();

      if (nameElement != null) {
         name = nameElement.text().trim();
      }

      return name;
   }

   private Float crawlPrice(Document document) {
      Float price = null;
      Element salePriceElement = document.select(".prod_box_info_top span[itemprop=price]").first();

      if (salePriceElement != null) {
         price = MathUtils.parseFloatWithComma(salePriceElement.text());
      }

      return price;
   }

   private Marketplace crawlMarketplace() {
      return new Marketplace();
   }


   private String crawlPrimaryImage(Document doc) {
      String primaryImage = null;
      Element elementPrimaryImage = doc.select(".zoomPad .jqzoom").first();

      if (elementPrimaryImage != null) {
         primaryImage = elementPrimaryImage.attr("href");
      }

      return primaryImage;
   }

   /**
    * @param doc
    * @return
    */
   private String crawlSecondaryImages(Document doc) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      Elements imgs = doc.select(".galeria-produto li img");

      for (int i = 1; i < imgs.size(); i++) {
         secondaryImagesArray.put(imgs.get(i).attr("src").replace("min", "zoom"));
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
      Elements elementCategories = document.select(".prod_nav li a");

      for (int i = 0; i < elementCategories.size(); i++) {
         String cat = elementCategories.get(i).ownText().trim();

         if (!cat.isEmpty()) {
            categories.add(cat);
         }
      }

      return categories;
   }

   private String crawlDescription(Document doc) {
      StringBuilder description = new StringBuilder();

      Element title = doc.select(".prod_box_tit").first();
      if (title != null) {
         title.select("#aba-avaliacao-ancora").remove();
         description.append(title);
      }

      Element elementDescription = doc.select(".prod_box_descricao_conteudo").first();
      if (elementDescription != null) {
         description.append(elementDescription.html());
      }

      Element marca = doc.select("#fabricante-aba-produto").first();
      if (marca != null) {
         description.append(marca.html());
      }

      return description.toString();
   }

   private boolean crawlAvailability(Document doc) {
      return doc.select(".produto_detalhe_comprar_btn").first() != null;
   }

   /**
    * 
    * @param doc
    * @param price
    * @return
    */
   private Prices crawlPrices(Float price, Document doc) {
      Prices prices = new Prices();

      Element pricesUrl = doc.select("#btn-forma-pagamento-seta").first();

      Element priceFrom = doc.select(".prod_valor_de").first();
      if (priceFrom != null) {
         prices.setPriceFrom(MathUtils.parseDoubleWithComma(priceFrom.text()));
      }

      if (pricesUrl != null) {
         String url = HOME_PAGE + pricesUrl.attr("data-ajax-post-url");

         Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
         Document pricesDoc = Jsoup.parse(this.dataFetcher.get(session, request).getBody());
         Elements cards = pricesDoc.select(".produto_list_pagamento li");

         for (Element e : cards) {
            String cardName = crawlCardName(e);
            Map<Integer, Float> installmentPriceMap = crawlInstallments(e);

            if (cardName != null && cardName.equals("boleto")) {

               if (installmentPriceMap.containsKey(1)) {
                  prices.setBankTicketPrice(installmentPriceMap.get(1));
               }

            } else if (cardName != null) {
               prices.insertCardInstallment(cardName, installmentPriceMap);
            }
         }

      } else if (price != null) {
         Map<Integer, Float> installmentPriceMap = new TreeMap<>();
         prices.setBankTicketPrice(price);
         installmentPriceMap.put(1, price);

         prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.DISCOVER.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      }

      return prices;
   }

   private Map<Integer, Float> crawlInstallments(Element card) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();

      Elements installments = card.select(".parcelamento_tabela tr");

      for (Element x : installments) {
         String text = x.text();

         if (text.contains("x")) {
            int indexX = text.indexOf('x');
            int indexY = text.indexOf("(", indexX);

            String installmentText = text.substring(0, indexX).replaceAll("[^0-9]", "");
            Float value = MathUtils.parseFloatWithComma(text.substring(indexX, indexY).trim());

            if (!installmentText.isEmpty() && installmentText != "1" && value != null) {
               installmentPriceMap.put(Integer.parseInt(installmentText), value);
            }
         }
      }

      return installmentPriceMap;
   }

   private String crawlCardName(Element card) {
      String cardName = null;

      Element name = card.select("h4").first();

      if (name != null) {
         String option = name.ownText().toLowerCase();

         if (option.contains("visa")) {
            cardName = Card.VISA.toString();
         } else if (option.contains("mastercard")) {
            cardName = Card.MASTERCARD.toString();
         } else if (option.contains("elo")) {
            cardName = Card.ELO.toString();
         } else if (option.contains("diners")) {
            cardName = Card.DINERS.toString();
         } else if (option.contains("discover")) {
            cardName = Card.DISCOVER.toString();
         } else if (option.contains("boleto")) {
            cardName = "boleto";
         }
      }

      return cardName;
   }

   private RatingsReviews crawRating(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      String internalId = crawlInternalId(doc);

      if (internalId != null) {
         Integer totalNumOfEvaluations = getTotalNumOfRatings(doc);
         Double avgRating = getTotalAvgRating(doc, totalNumOfEvaluations);
         Integer totalWrittenReviews = getTotalNumOfRatings(doc);
         AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(doc);
         ratingReviews.setInternalId(internalId);
         ratingReviews.setTotalRating(totalNumOfEvaluations);
         ratingReviews.setAverageOverallRating(avgRating);
         ratingReviews.setTotalWrittenReviews(totalWrittenReviews);
         ratingReviews.setAdvancedRatingReview(advancedRatingReview);
      }

      return ratingReviews;
   }

   private Double getTotalAvgRating(Document doc, Integer ratingCount) {
      Double avgRating = 0d;

      if (ratingCount > 0) {
         Element avg = doc.selectFirst(".prod_box_avaliacao_bg_geral[style]");

         if (avg != null) {
            Double percentage = MathUtils
                  .normalizeTwoDecimalPlaces(Double.parseDouble(CommonMethods.getLast(avg.attr("style").split(";")).replaceAll("[^0-9.]", "").trim()));

            if (percentage != null) {
               avgRating = MathUtils.normalizeTwoDecimalPlaces(5 * (percentage / 100d));
            }
         }
      }

      return avgRating;
   }



   private Integer getTotalNumOfRatings(Document docRating) {
      Integer totalRating = 0;
      Element totalRatingElement = docRating.select(".prod_avaliacao_txt").first();

      if (totalRatingElement != null) {
         String text = totalRatingElement.ownText().replaceAll("[^0-9]", "").trim();

         if (!text.isEmpty()) {
            totalRating = Integer.parseInt(text);
         }
      }

      return totalRating;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Elements reviews = doc.select(".prod_box_avaliacao_bg_geral");

      for (Element review : reviews) {

         Element elementStarNumber = review.selectFirst(".prod_box_avaliacao_bg_geral:last-child");

         if (elementStarNumber != null) {

            String stringStarNumber = elementStarNumber.attr("style");
            if (stringStarNumber.contains("width")) {
               int i = stringStarNumber.indexOf("width");
               String StringStarNumberSub = stringStarNumber.substring(i);
               String sN = StringStarNumberSub.replaceAll("[^0-9]", "").trim();
               Integer numberOfStars = !sN.isEmpty() ? Integer.parseInt(sN) : 0;

               switch (numberOfStars) {
                  case 100:
                     star5 += 1;
                     break;
                  case 80:
                     star4 += 1;
                     break;
                  case 60:
                     star3 += 1;
                     break;
                  case 40:
                     star2 += 1;
                     break;
                  case 20:
                     star1 += 1;
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

}
