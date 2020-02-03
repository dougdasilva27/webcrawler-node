package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrasilDolcegustoCrawler extends Crawler {

  private final String HOME_PAGE = "https://www.nescafe-dolcegusto.com.br/";

  public BrasilDolcegustoCrawler(Session session) {
    super(session);
    super.config.setMustSendRatingToKinesis(true);
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

    if (isProductPage(this.session.getOriginalURL(), doc)) {
      Logging.printLogDebug(logger, session,
          "Product page identified: " + this.session.getOriginalURL());

      // internalId
      Element elementInternalID = doc.select(".no-display [name=product]").first();
      String internalId = null;
      if (elementInternalID != null) {
        internalId = elementInternalID.attr("value");
      }

      // name
      Element elementName = doc.select(".product-main-info .product-name [itemprop=name]").first();
      String name = elementName.text().trim();

      // price
      Float price = crawlPrice(doc);

      // categories
      Elements elementsCategories = doc.select(".breadcrumbs ul li");
      String category1 = "";
      String category2 = "";
      String category3 = "";
      ArrayList<String> categories = new ArrayList<>();

      for (Element e : elementsCategories) {
        if (!e.attr("class").equals("product-back") && !e.attr("class").equals("home")
            && !e.attr("class").equals("product")) {
          categories.add(e.select("a").text());
        }
      }
      for (String category : categories) {
        if (category1.isEmpty()) {
          category1 = category;
        } else if (category2.isEmpty()) {
          category2 = category;
        } else if (category3.isEmpty()) {
          category3 = category;
        }
      }

      // images
      Elements elementImages = doc.select(".more-views ul li a");
      String primaryImage = parseImage(elementImages.first().attr("rel"));
      String secondaryImages = null;
      JSONArray secundaryImagesArray = new JSONArray();

      if (elementImages.size() > 1) {
        for (int i = 1; i < elementImages.size(); i++) { // primeira imagem eh primaria
          Element e = elementImages.get(i);
          if (!e.attr("class").equals("show-video")) { // nao pegar se for video
            String attrRel = e.attr("rel");
            secundaryImagesArray.put(parseImage(attrRel));
          }
        }
      }
      if (secundaryImagesArray.length() > 0) {
        secondaryImages = secundaryImagesArray.toString();
      }

      // description
      String description = "";
      Element elementDescription = doc.select(".tabs-content .tab.description-.active").first();
      Element elementIngredientes = doc.select(".tabs-content .tab.local-").first();
      Element elementSpecs = doc.select(".tabs-content #specs").first();
      if (elementDescription != null)
        description = description + elementDescription.html();
      if (elementSpecs != null)
        description = description + elementSpecs.html();
      if (elementIngredientes != null)
        description = description + elementIngredientes.html();

      // special description

      Element customPageDetail = doc.select(".product-custom-page.product-custom-detail").first();
      Element customPageCompare = doc.select(".product-custom-page.product-custom-compare").first();
      if (customPageDetail != null)
        description = description + customPageDetail.html();
      if (customPageCompare != null)
        description = description + customPageCompare.html();

      // availability
      boolean available = true;
      Element elementOutOfStock = doc.select(".pdp-actions .out-of-stock-label").first();
      if (elementOutOfStock != null) {
        available = false;
      }

      RatingsReviews ratingsReviews = scrapRatingReviews(doc, internalId);
      // stock
      Integer stock = null;

      // marketplace
      Marketplace marketplace = new Marketplace();

      // Prices
      Prices prices = crawlPrices(price);

      Product product = new Product();
      product.setUrl(this.session.getOriginalURL());
      product.setInternalId(internalId);
      product.setName(name);
      product.setRatingReviews(ratingsReviews);
      product.setPrice(price);
      product.setPrices(prices);
      product.setCategory1(category1);
      product.setCategory2(category2);
      product.setCategory3(category3);
      product.setPrimaryImage(primaryImage);
      product.setSecondaryImages(secondaryImages);
      product.setDescription(description);
      product.setStock(stock);
      product.setMarketplace(marketplace);
      product.setAvailable(available);

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private String parseImage(String text) {
    int begin = text.indexOf("largeimage:") + 11;
    String img = text.substring(begin);
    img = img.replace("'", " ").replace('}', ' ').trim();

    return img;
  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(String url, Document document) {
    String[] tokens = url.split("/");
    return !document.select(".product-essential").isEmpty() && tokens.length == 4
        && !url.endsWith("/");
  }

  private Float crawlPrice(Document doc) {
    Float price = null;
    Element elementPrice = doc.select(".product-shop .price-box .regular-price .price").first();
    if (elementPrice == null) {
      elementPrice = doc.select(".product-shop .price-box .special-price .price").first();
    }
    if (elementPrice != null) {
      price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "")
          .replaceAll(",", "."));
    }

    return price;
  }

  /**
   * In this market, installments not appear in product page
   *
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();

      installmentPriceMap.put(1, price);
      prices.setBankTicketPrice(price);

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
    }

    return prices;
  }

  private RatingsReviews scrapRatingReviews(Document doc, String internalId) {
    RatingsReviews ratingReviews = new RatingsReviews();

    ratingReviews.setInternalId(internalId);
    ratingReviews.setDate(session.getDate());
    ratingReviews.setTotalRating(computeTotalReviewsCount(doc));
    ratingReviews.setAverageOverallRating(crawlAverageOverallRating(doc));

    return ratingReviews;
  }

  private Integer computeTotalReviewsCount(Document doc) {
    int totalReviewsCount = 0;
    Element total = doc.select("meta[itemprop=reviewCount]").first();

    if (total != null) {
      try {
        totalReviewsCount = Integer.parseInt(total.attr("content"));
      } catch (Exception e) {
        Logging.printLogWarn(logger, CommonMethods.getStackTrace(e));
      }
    }

    return totalReviewsCount;
  }

  private Double crawlAverageOverallRating(Document document) {
    Double avgOverallRating = null;

    Integer percentage = CrawlerUtils.scrapIntegerFromHtmlAttr(document, "meta[itemprop=ratingValue]", "content", 0);

    if (percentage > 0) {
      avgOverallRating = MathUtils.normalizeTwoDecimalPlaces(5 * (percentage.doubleValue() / 100));
    }

    return avgOverallRating;
  }

}
