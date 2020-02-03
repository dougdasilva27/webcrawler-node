package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.YourreviewsRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Date: 15/08/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilFarma22Crawler extends Crawler {

  private static final String HOME_PAGE = "http://www.farma22.com.br/";

  public BrasilFarma22Crawler(Session session) {
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

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalPid = crawlInternalPid(doc);
      CategoryCollection categories = crawlCategories(doc);
      String description = crawlDescription(doc);
      Integer stock = null;
      Marketplace marketplace = new Marketplace();
      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);
      JSONArray eanArray = CrawlerUtils.scrapEanFromVTEX(doc);
      // sku data in json
      JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = crawlInternalId(jsonSku);
        boolean available = crawlAvailability(jsonSku);
        Float price = crawlMainPagePrice(jsonSku, available);
        String primaryImage = crawlPrimaryImage(doc);
        String name = crawlName(doc, jsonSku);
        String secondaryImages = crawlSecondaryImages(doc);
        Prices prices = crawlPrices(internalId, price, jsonSku);
        String ean = i < eanArray.length() ? eanArray.getString(i) : null;

        List<String> eans = new ArrayList<>();
        eans.add(ean);

        RatingsReviews ratingsReviews = crawlRating(internalId, internalPid);

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
            .setRatingReviews(ratingsReviews)
            .build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private RatingsReviews crawlRating(String internalId, String internalPid) {
    RatingsReviews ratingReviews = new RatingsReviews();
    ratingReviews.setDate(session.getDate());

    YourreviewsRatingCrawler yourReviews = new YourreviewsRatingCrawler(session, cookies, logger);
    Document docRating = yourReviews.crawlPageRatingsFromYourViews(internalPid, "d8f4f406-5164-4042-81aa-a7fe0ec787f0", dataFetcher);
    Integer totalNumOfEvaluations = yourReviews.getTotalNumOfRatingsFromYourViews(docRating);
    Double avgRating = yourReviews.getTotalAvgRatingFromYourViews(docRating);

    ratingReviews.setTotalRating(totalNumOfEvaluations);
    ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
    ratingReviews.setAverageOverallRating(avgRating);
    ratingReviews.setInternalId(internalId);

    return ratingReviews;
  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(Document document) {
      return document.select(".productName").first() != null;
  }

  /*******************
   * General methods *
   *******************/

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("sku")) {
      internalId = Integer.toString(json.getInt("sku")).trim();
    }

    return internalId;
  }

  private String crawlInternalPid(Document document) {
    String internalPid = null;
    Element internalPidElement = document.select("#___rc-p-id").first();

    if (internalPidElement != null) {
      internalPid = internalPidElement.val();
    }

    return internalPid;
  }

  private String crawlName(Document document, JSONObject jsonSku) {
    String name = null;
    Element nameElement = document.select(".productName").first();

    String nameVariation = jsonSku.getString("skuname");

    if (nameElement != null) {
        name = nameElement.text().trim();

      if (name.length() > nameVariation.length()) {
        name += " " + nameVariation;
      } else {
        name = nameVariation;
      }
    }

    return name;
  }

  private Float crawlMainPagePrice(JSONObject json, boolean available) {
    Float price = null;

    if (json.has("bestPriceFormated") && available) {
      price = MathUtils.parseFloatWithComma(json.getString("bestPriceFormated"));
    }

    return price;
  }

  private boolean crawlAvailability(JSONObject json) {
    if (json.has("available")) {
      return json.getBoolean("available");
    }
    return false;
  }

  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;

    Element image = doc.select("#botaoZoom").first();

    if (image != null) {
      primaryImage = image.attr("zoom").trim();

      if (primaryImage == null || primaryImage.isEmpty()) {
        primaryImage = image.attr("rel").trim();
      }
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document doc) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements imageThumbs = doc.select("#botaoZoom");

    for (int i = 1; i < imageThumbs.size(); i++) { // starts with index 1, because the first image
                                                   // is the primary image
      String url = imageThumbs.get(i).attr("zoom");

      if (url == null || url.isEmpty()) {
        url = imageThumbs.get(i).attr("rel");
      }

      if (url != null && !url.isEmpty()) {
        secondaryImagesArray.put(url);
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select(".bread-crumb > ul li");

    for (int i = 1; i < elementCategories.size(); i++) { // first item is the home page
      categories.add(elementCategories.get(i).text().trim());
    }

    return categories;
  }

  private String crawlDescription(Document document) {
    String description = "";

    Element descElement = document.select(".product-description").first();

    if (descElement != null) {
      description = description + descElement.html();
    }

    return description;
  }

  /**
   * To crawl this prices is accessed a api Is removed all accents for crawl price 1x like this: Visa
   * à vista R$ 1.790,00
   * 
   * @param internalId
   * @param price
   * @return
   */
  private Prices crawlPrices(String internalId, Float price, JSONObject jsonSku) {
    Prices prices = new Prices();

    if (price != null) {
      String url = "http://www.farma22.com.br/productotherpaymentsystems/" + internalId;
      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      Document doc = Jsoup.parse(this.dataFetcher.get(session, request).getBody());

      Element bank = doc.select("#ltlPrecoWrapper em").first();
      if (bank != null) {
        prices.setBankTicketPrice(MathUtils.parseFloatWithComma(bank.text()));
      }

      if (jsonSku.has("listPriceFormated")) {
        prices.setPriceFrom(MathUtils.parseDoubleWithComma(jsonSku.get("listPriceFormated").toString()));
      }

      Elements cardsElements = doc.select("#ddlCartao option");

      for (Element e : cardsElements) {
        String text = e.text().toLowerCase();

        if (text.contains("visa")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
          prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);

        } else if (text.contains("mastercard")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
          prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);

        } else if (text.contains("diners")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
          prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);

        } else if (text.contains("american") || text.contains("amex")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
          prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);

        } else if (text.contains("hipercard") || text.contains("amex")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
          prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);

        } else if (text.contains("credicard")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
          prices.insertCardInstallment(Card.CREDICARD.toString(), installmentPriceMap);

        } else if (text.contains("elo")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
          prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);

        }
      }
    }

    return prices;
  }

  private Map<Integer, Float> getInstallmentsForCard(Document doc, String idCard) {
    Map<Integer, Float> mapInstallments = new HashMap<>();

    Elements installmentsCard = doc.select(".tbl-payment-system#tbl" + idCard + " tr");
    for (Element i : installmentsCard) {
      Element installmentElement = i.select("td.parcelas").first();

      if (installmentElement != null) {
        String textInstallment = installmentElement.text().toLowerCase();
        Integer installment;

        if (textInstallment.contains("vista")) {
          installment = 1;
        } else {
          installment = Integer.parseInt(textInstallment.replaceAll("[^0-9]", "").trim());
        }

        Element valueElement = i.select("td:not(.parcelas)").first();

        if (valueElement != null) {
          Float value = Float.parseFloat(valueElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());

          mapInstallments.put(installment, value);
        }
      }
    }

    return mapInstallments;
  }
}
