package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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

/**
 * Date: 25/06/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class ArgentinaLaanonimaonlineCrawler extends Crawler {

   private static final String HOME_PAGE = "http://www.laanonimaonline.com";

   public ArgentinaLaanonimaonlineCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   @Override
   public void handleCookiesBeforeFetch() {
      // Criando cookie da cidade CABA
      BasicClientCookie cookie = new BasicClientCookie("laanonimasucursalnombre", "9%20de%20Julio");
      cookie.setDomain("www.laanonimaonline.com");
      cookie.setPath("/");
      this.cookies.add(cookie);

      // Criando cookie da regiao sao nicolas
      BasicClientCookie cookie2 = new BasicClientCookie("laanonimasucursal", "138");
      cookie2.setDomain("www.laanonimaonline.com");
      cookie2.setPath("/");
      this.cookies.add(cookie2);
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
         String internalPid = crawlInternalPid(doc);
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
         String ean = crawlEan(doc);
         RatingsReviews ratingReviews = scrapRatingReviews(doc);
         List<String> eans = new ArrayList<>();
         eans.add(ean);

         // Creating the product
         Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPrice(price)
               .setPrices(prices)
               .setAvailable(available)
               .setCategory1(categories.getCategory(0))
               .setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2))
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setStock(stock)
               .setMarketplace(marketplace)
               .setEans(eans)
               .setRatingReviews(ratingReviews)
               .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean isProductPage(Document doc) {
      return doc.select("#id_item").first() != null;
   }

   private String crawlInternalId(Document doc) {
      String internalId = null;

      Element internalIdElement = doc.select("#id_item").first();
      if (internalIdElement != null) {
         internalId = internalIdElement.val();
      }

      return internalId;
   }

   private String crawlInternalPid(Document doc) {
      String internalPid = null;

      Element pidElement = doc.select("#cont_producto > div > div").first();
      if (pidElement != null) {
         internalPid = CommonMethods.getLast(pidElement.ownText().trim().split(" ")).trim();
      }

      return internalPid;
   }

   private String crawlName(Document doc) {
      String name = null;
      Element nameElement = doc.select("h1.titulo_producto").first();

      if (nameElement != null) {
         name = nameElement.ownText().trim();
      }

      return name;
   }

   private Float crawlPrice(Document doc) {
      Float price = null;

      Element salePriceElement = doc.select(".precio_carrito span").first();
      Element specialPrice = doc.select(".precio.destacado").first();

      if (specialPrice != null) {
         price = MathUtils.parseFloatWithComma(specialPrice.ownText());
      } else if (salePriceElement != null) {
         price = MathUtils.parseFloatWithComma(salePriceElement.ownText());
      }

      return price;
   }

   private boolean crawlAvailability(Document doc) {
      return doc.select(".sin_stock").isEmpty();
   }

   private Marketplace crawlMarketplace() {
      return new Marketplace();
   }


   private String crawlPrimaryImage(Document doc) {
      String primaryImage = null;
      Element primaryImageElement = doc.select("#img_producto > img").first();

      if (primaryImageElement != null) {
         primaryImage = primaryImageElement.attr("data-zoom-image").trim();
      }

      return primaryImage;
   }

   private String crawlSecondaryImages(Document doc) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      Elements imagesElement = doc.select("#galeria_img div > a");

      for (int i = 1; i < imagesElement.size(); i++) { // first index and last index is the primary image
         String image = imagesElement.get(i).attr("data-zoom-image").trim();
         secondaryImagesArray.put(image);
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   private CategoryCollection crawlCategories(Document doc) {
      CategoryCollection categories = new CategoryCollection();

      Elements elementCategories = doc.select(".barra_navegacion a.link_navegacion");
      for (Element e : elementCategories) {
         categories.add(e.text().trim());
      }

      return categories;
   }

   private String crawlDescription(Document doc) {
      StringBuilder description = new StringBuilder();

      Element shortDesc = doc.select("#detalle_producto .texto").first();

      if (shortDesc != null) {
         description.append(shortDesc.html());
      }

      Element descriptionElement = doc.select(".container_tabs").first();

      if (descriptionElement != null) {
         description.append(descriptionElement.html());
      }

      return description.toString();
   }

   private Double crawlPriceFrom(Document doc) {
      Double price = null;

      Element from = doc.select(".precio.anterior").first();
      if (from != null) {
         price = MathUtils.parseDoubleWithComma(from.ownText());
      }

      return price;
   }

   /**
    * There is no bankSlip price.
    * 
    * @param doc
    * @param price
    * @return
    */
   private Prices crawlPrices(Float price, Document doc) {
      Prices prices = new Prices();

      if (price != null) {
         prices.setPriceFrom(crawlPriceFrom(doc));

         Map<Integer, Float> installmentPriceMap = new TreeMap<>();
         installmentPriceMap.put(1, price);

         prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.NARANJA.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.CABAL.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      }

      return prices;
   }



   private RatingsReviews scrapRatingReviews(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalComments = doc.select("#ver_comentarios > div.comentario").size();
      Double avgRating = scrapAvgRating(doc);
      AdvancedRatingReview advancedRatingReview = getTotalAvgRating(doc);

      ratingReviews.setTotalRating(totalComments);
      ratingReviews.setTotalWrittenReviews(totalComments);

      ratingReviews.setAverageOverallRating(avgRating);

      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   private Double scrapAvgRating(Document doc) {
      Double avg = 0d;

      Elements percentage = doc.select(".cuerpo_valoracion .activa");

      Double avgRating = (double) percentage.size();

      if (avgRating != null) {
         avg = (avgRating * 100) / 5;
      }

      System.err.println(avg);
      return avg;
   }

   private AdvancedRatingReview getTotalAvgRating(Document doc) {

      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Element one = doc.select(".cuerpo_valoracion span[title=\"No recomendado\"].activa").first();
      Element two = doc.select(".cuerpo_valoracion span[title=\"Malo\"].activa").first();
      Element tree = doc.select(".cuerpo_valoracion span[title=\"Regular\"].activa").first();
      Element four = doc.select(".cuerpo_valoracion span[title=\"Bueno\"].activa").first();
      Element five = doc.select(".cuerpo_valoracion span[title=\"Muy Bueno\"].activa").first();


      if (five != null) {
         star5 += 1;
      } else if (four != null) {
         star4 += 1;
      } else if (tree != null) {
         star3 += 1;
      } else if (two != null) {
         star2 += 1;
      } else if (one != null) {
         star1 += 1;
      }


      return new AdvancedRatingReview.Builder()
            .totalStar1(star1)
            .totalStar2(star2)
            .totalStar3(star3)
            .totalStar4(star4)
            .totalStar5(star5)
            .build();
   }


   private String crawlEan(Document doc) {
      String ean = null;
      Element e = doc.selectFirst("script[data-flix-ean]");

      if (e != null) {
         String aux = e.attr("data-flix-ean");

         if (!aux.isEmpty()) {
            ean = aux;
         }
      }

      return ean;
   }
}
