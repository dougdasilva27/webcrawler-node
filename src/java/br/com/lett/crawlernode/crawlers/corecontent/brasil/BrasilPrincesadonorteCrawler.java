package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
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
import models.AdvancedRatingReview;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

/**
 * 
 * @author Joao Pedro
 *
 */
public class BrasilPrincesadonorteCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.princesadonorteonline.com.br/";

  public BrasilPrincesadonorteCrawler(Session session) {
    super(session);
    super.config.setFetcher(FetchMode.FETCHER);
  }


  @Override
  protected Object fetch() {
    Request request = RequestBuilder.create().setUrl(session.getOriginalURL()).setCookies(cookies).build();
    return Jsoup.parse(this.dataFetcher.get(session, request).getBody());
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
      Elements scripts = doc.select("script[type=\"application/ld+json\"]");

      String description =
          CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#product_tabs_description_contents", "#product_tabs_additional_contents"));
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-img-box  #items a", Arrays.asList("href"),
          "https", "www.princesadonorteonline.com.br");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".product-img-box  #items a", Arrays.asList("href"),
          "https", "www.princesadonorteonline.com.br", primaryImage);

      CategoryCollection categories = crawlCategories(doc);

      for (Element script : scripts) {
        JSONObject json = extractJsonFromScriptTag(script);
        if (json.has("@type") && json.getString("@type").equalsIgnoreCase("product")) {
          String internalId = crawlInternalId(json);
          String internalPid = crawInternalPid(doc);
          String name = crawlName(json);
          Float price = crawlPrice(json);
          Prices prices = crawlPrices(price, doc);
          boolean available = crawlAvailability(json);
          RatingsReviews ratingsReviews = scrapRatingReviews(doc);

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
              .setMarketplace(new Marketplace())
              .setRatingReviews(ratingsReviews)
              .build();

          products.add(product);
        }
      }



    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private String crawInternalPid(Document doc) {
    Element internalPidElement = doc.selectFirst("input[name='product']");
    String internalPid = null;

    if (internalPidElement != null) {
      internalPid = internalPidElement.val();
    }

    return internalPid;
  }


  private JSONObject extractJsonFromScriptTag(Element script) {
    String strJson = script.toString();
    JSONObject json = new JSONObject();
    if (strJson.contains("{") && strJson.contains("}")) {
      strJson = strJson.substring(strJson.indexOf("{"), strJson.lastIndexOf("}") + 1);
      json = new JSONObject(strJson);
    }

    return json;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".product-view") != null;
  }

  private String crawlInternalId(JSONObject json) {
    String internalId = null;
    if (json.has("sku")) {
      internalId = json.get("sku").toString();
    }
    return internalId;
  }

  private String crawlName(JSONObject json) {
    String name = null;

    if (json.has("name")) {
      name = json.getString("name");
    }

    return name;
  }

  private Float crawlPrice(JSONObject json) {
    Float price = null;

    if (json.has("offers")) {
      JSONArray prices = json.getJSONArray("offers");
      for (Object offer : prices) {
        if (offer instanceof JSONObject) {
          JSONObject value = (JSONObject) offer;
          if (value.has("@type") && value.getString("@type").equalsIgnoreCase("offer")) {
            price = value.getFloat("price");
          }
        }

      }
    }

    return price;
  }


  private boolean crawlAvailability(JSONObject json) {
    boolean available = false;

    if (json.has("offers")) {
      JSONArray offers = json.getJSONArray("offers");

      for (Object o : offers) {
        JSONObject offer = o instanceof JSONObject ? (JSONObject) o : new JSONObject();
        if (offer.has("availability")) {
          String availability = offer.get("availability").toString().toLowerCase();

            available = !availability.endsWith("outofstock");
          break;
        }
      }
    }

    return available;
  }

  /**
   * @param document
   * @return
   */
  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select(".breadcrumbs ul li > a");

    for (int i = 1; i < elementCategories.size(); i++) {
      String cat = elementCategories.get(i).ownText().trim();

      if (!cat.isEmpty()) {
        categories.add(cat);
      }
    }

    return categories;
  }


  /**
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      prices.setBankTicketPrice(price);

      Float priceBank = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".product-shop .boleto", null, true, ',', session);
      if (priceBank != null) {
        prices.setBankTicketPrice(priceBank);
      }

      Element priceFrom = doc.selectFirst(".old-price .price");
      if (priceFrom != null) {
        prices.setPriceFrom(MathUtils.parseDoubleWithComma(priceFrom.text()));
      }

      Element installments = doc.selectFirst(".special-price .price");

      if (installments != null) {
        String text = installments.ownText().toLowerCase();

        if (text.contains("x")) {
          int x = text.indexOf('x');

          String parcel = text.substring(0, x).replaceAll("[^0-9]", "").trim();
          Float value = MathUtils.parseFloatWithComma(text.substring(x));

          if (!parcel.isEmpty() && value != null) {
            installmentPriceMap.put(Integer.parseInt(parcel), value);
          }
        }
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.SOROCRED.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.CABAL.toString(), installmentPriceMap);
    }

    return prices;
  }

  private RatingsReviews scrapRatingReviews(Document doc){

     RatingsReviews ratingsReviews = new RatingsReviews();

     int totalReviews = doc.select(".ratings-table").size();
     double avgRating = 0.0;

     Double percentageRating = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-shop .rating", "style", true, '.', session);

     if(percentageRating != null){
        avgRating = percentageRating / 100 * 5;
     }

     ratingsReviews.setTotalRating(totalReviews);
     ratingsReviews.setTotalWrittenReviews(totalReviews);
     ratingsReviews.setAverageOverallRating(avgRating);
     ratingsReviews.setAdvancedRatingReview(scrapAdvancedRatingReview(doc));

     return ratingsReviews;
  }

  private AdvancedRatingReview scrapAdvancedRatingReview(Document doc){

     AdvancedRatingReview advancedRatingReview = new AdvancedRatingReview();

     Elements ratings = doc.select(".ratings-table");

     int stars1 = 0;
     int stars2 = 0;
     int stars3 = 0;
     int stars4 = 0;
     int stars5 = 0;

     for(Element e: ratings){

        int stars = 0;
        Double starsformatted = CrawlerUtils.scrapDoublePriceFromHtml(e, ".ratings-table tr:first-child .rating", "style", true, '.', session);

        if(starsformatted != null){
           stars = (int)(starsformatted / 100 * 5);
        }

        switch (stars){
           case 1:
              stars1++;
              break;
           case 2:
              stars2++;
              break;
           case 3:
              stars3++;
              break;
           case 4:
              stars4++;
              break;
           case 5:
              stars5++;
              break;
           default:
        }
     }

     advancedRatingReview.setTotalStar1(stars1);
     advancedRatingReview.setTotalStar2(stars2);
     advancedRatingReview.setTotalStar3(stars3);
     advancedRatingReview.setTotalStar4(stars4);
     advancedRatingReview.setTotalStar5(stars5);

     return advancedRatingReview;

  }

}
