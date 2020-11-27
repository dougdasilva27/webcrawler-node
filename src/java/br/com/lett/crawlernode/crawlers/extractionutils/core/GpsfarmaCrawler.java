package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Date: 22/06/2018
 *
 * @author Gabriel Dornelas
 */
public class GpsfarmaCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.gpsfarma.com/";

   public GpsfarmaCrawler(Session session) {
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
         List<String> eans = new ArrayList<>();
         eans.add(ean);

         // Creating the product
         Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
               .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
               .setStock(stock).setMarketplace(marketplace).setEans(eans).build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean isProductPage(Document doc) {
      return doc.select("input[name=\"product\"]").first() != null;
   }


   private String crawlInternalId(Document doc) {
      String internalId = null;

      Element internalIdElement = doc.select("input[name=\"product\"]").first();
      if (internalIdElement != null) {
         internalId = internalIdElement.val();
      }

      return internalId;
   }

   private String crawlInternalPid(Document doc) {
      String internalPid = null;

      Element pidElement = doc.select("tr td.data").first();
      if (pidElement != null) {
         internalPid = pidElement.ownText().trim();
      }

      return internalPid;
   }

   private String crawlName(Document doc) {
      String name = null;
      Element nameElement = doc.select(".product-name").first();

      if (nameElement != null) {
         name = nameElement.text().trim();
      }

      return name;
   }

   private Float crawlPrice(Document doc) {
      Float price = null;

      Element salePriceElement = doc.select(".product-essential .regular-price .price").first();
      Element specialPrice = doc.select(".product-essential .special-price .price").first();

      if (specialPrice != null) {
         price = MathUtils.parseFloatWithComma(specialPrice.ownText());
      } else if (salePriceElement != null) {
         price = MathUtils.parseFloatWithComma(salePriceElement.ownText());
      }

      return price;
   }

   private boolean crawlAvailability(Document doc) {
      return !doc.select(".btn-cart").isEmpty() && doc.select(".sin-stock-prod-view").isEmpty();
   }

   private Marketplace crawlMarketplace() {
      return new Marketplace();
   }


   private String crawlPrimaryImage(Document doc) {
      String primaryImage = null;
      Element primaryImageElement = doc.select(".product-image-gallery img").first();

      if (primaryImageElement != null) {
         primaryImage = primaryImageElement.attr("data-zoom-image").trim();

         if (primaryImage.isEmpty()) {
            primaryImage = primaryImageElement.attr("src");
         }
      }

      return primaryImage;
   }

   private String crawlSecondaryImages(Document doc) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      Elements imagesElement = doc.select(".product-image-thumbs img");

      for (int i = 1; i < imagesElement.size(); i++) { // Ignore primary image get only the secondarys
         String image = imagesElement.get(i).attr("src").trim();
         secondaryImagesArray.put(image);
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   private CategoryCollection crawlCategories(Document doc) {
      CategoryCollection categories = new CategoryCollection();

      Elements elementCategories = doc.select(".breadcrumbs li[class^=category]");
      for (Element e : elementCategories) {
         categories.add(e.text().replace("/", "").trim());
      }

      return categories;
   }

   private String crawlDescription(Document doc) {
      StringBuilder description = new StringBuilder();
      Element descriptionElement = doc.select(".product-collateral .toggle-content").first();

      if (descriptionElement != null) {
         description.append(descriptionElement.html());
      }

      return description.toString();
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
         Element priceOld = doc.select(".product-essential .old-price").first();
         if (priceOld != null) {
            prices.setPriceFrom(MathUtils.parseDoubleWithComma(priceOld.text()));
         }

         Map<Integer, Float> installmentPriceMap = new TreeMap<>();
         installmentPriceMap.put(1, price);

         prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);
      }

      return prices;
   }

   private String crawlEan(Document doc) {
      String ean = null;
      Elements elmnts = doc.select(".data-table tbody tr");

      for (Element e : elmnts) {
         Element sub = e.selectFirst(".label");

         if (sub != null && sub.text().contains("Código de Barra")) {
            Element sub2 = e.selectFirst(".data");

            if (sub2 != null) {
               ean = sub2.text().trim();
               break;
            }
         }
      }

      return ean;
   }
}