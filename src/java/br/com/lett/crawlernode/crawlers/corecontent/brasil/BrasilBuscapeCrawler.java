package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import models.AdvancedRatingReview;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

/**
 * Date: 26/03/2019
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilBuscapeCrawler extends Crawler {

  private static final String HOST = "www.buscape.com.br";
  private static final String PROTOCOL = "https";
  private static final String HOME_PAGE = "https://www.buscape.com.br/";

  public BrasilBuscapeCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {

    List<Product> products = new ArrayList<>();

    if (!doc.select(".prod-page").isEmpty()) {
      JSONObject productJson = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"application/ld+json\"]", null, null, false, false);

      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=prodid]", "value");
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name", false);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".zbreadcrumb li:not(:first-child) a");
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".tech-spec-table"));
      String primaryImage = scrapPrimaryImage(doc);
      String secondaryImages = scrapSecondaryImages(doc, primaryImage);
      List<Document> offersHtmls = getSellersHtmls(doc, internalId);
      Marketplace marketplace = scrapMarketplaces(offersHtmls);
      RatingsReviews ratingAndReviews = scrapRatingAndReviews(internalId, productJson);
      // Creating the product
      Product product = ProductBuilder.create()
          .setUrl(session.getOriginalURL())
          .setInternalId(internalId)
          .setName(name)
          .setPrices(new Prices())
          .setAvailable(false)
          .setCategory1(categories.getCategory(0))
          .setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2))
          .setPrimaryImage(primaryImage)
          .setRatingReviews(ratingAndReviews)
          .setSecondaryImages(secondaryImages)
          .setDescription(description)
          .setMarketplace(marketplace)
          .build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private List<Document> getSellersHtmls(Document doc, String internalId) {
    List<Document> htmls = new ArrayList<>();
    htmls.add(doc);

    Integer offersNumber = CrawlerUtils.scrapIntegerFromHtmlAttr(doc, ".offers-list[data-showing-total]", "data-showing-total", 0);
    int offersNumberCount = doc.select(".offers-list__offer").size();

    if (offersNumber > offersNumberCount) {
      int currentPage = 1;

      while (offersNumber > offersNumberCount) {
        String offerUrl = "https://www.buscape.com.br/ajax/product_desk?__pAct_=getmoreoffers"
            + "&prodid=" + internalId
            + "&pagenumber=" + currentPage
            + "&highlightedItemID=0"
            + "&resultorder=7";

        Request request = RequestBuilder.create()
            .setUrl(offerUrl)
            .setCookies(cookies)
            .build();

        Document offersDoc = Jsoup.parse(this.dataFetcher.get(session, request).getBody());
        Elements offersElements = offersDoc.select(".offers-list__offer");
        if (!offersElements.isEmpty()) {
          offersNumberCount += offersElements.size();
          htmls.add(offersDoc);
        } else {
          break;
        }

        currentPage++;
      }
    }

    return htmls;
  }

  private Marketplace scrapMarketplaces(List<Document> docuements) {
    Map<String, Prices> offersMap = new HashMap<>();

    for (Document doc : docuements) {
      Elements offers = doc.select(".offers-list__offer");
      for (Element e : offers) {
        String sellerName = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".col-store a", "title");

        if (sellerName != null) {
          String sellerNameLower = sellerName.replace("na", "").trim().toLowerCase();
          Prices prices = scrapPricesOnNewSite(doc);

          offersMap.put(sellerNameLower, prices);
        }
      }
    }

    return CrawlerUtils.assembleMarketplaceFromMap(offersMap, Arrays.asList("buscapé"), Card.VISA, session);
  }

  private Prices scrapPricesOnNewSite(Element doc) {
    Prices prices = new Prices();

    Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".price__total", null, true, ',', session);
    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      prices.setBankTicketPrice(price);

      Pair<Integer, Float> installments = CrawlerUtils.crawlSimpleInstallment(".price__parceled__parcels", doc, false);
      if (!installments.isAnyValueNull()) {
        installmentPriceMap.put(installments.getFirst(), installments.getSecond());
      }

      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
    }

    return prices;
  }

  private String scrapPrimaryImage(Document doc) {
    JSONArray jsonImg = getImageJSON(doc);
    String imagem = null;
    if (jsonImg instanceof JSONArray) {
      JSONObject imageJson = jsonImg.getJSONObject(0);
      imagem = getImageFromJSONOfNewSite(imageJson);
    }
    return imagem;
  }

  private String scrapSecondaryImages(Document doc, String primaryImage) {
    JSONArray secondaryImagesArray = new JSONArray();
    JSONArray jsonImg = getImageJSON(doc);

    for (Object object : jsonImg) {
      String image = getImageFromJSONOfNewSite((JSONObject) object);
      if (!image.equalsIgnoreCase(primaryImage)) {
        secondaryImagesArray.put(image);
      }
    }

    return secondaryImagesArray.toString();
  }

  private JSONArray getImageJSON(Document doc) {
    Elements scripts = doc.select("body > script");
    JSONArray jsonImg = null;
    JSONObject jsonProduct = null;
    for (Iterator<Element> iterator = scripts.iterator(); iterator.hasNext();) {
      Element element = iterator.next();
      if (element.toString().contains("ProductUrl:\"" + session.getOriginalURL() + '"')) {
        String jsonStringProduct = CrawlerUtils.extractSpecificStringFromScript(element.toString(), "Zoom.Context.load(", true, "});", false);
        jsonProduct = JSONUtils.stringToJson(jsonStringProduct);
        break;
      }
    }
    if (jsonProduct instanceof JSONObject) {
      JSONObject jsonSuperZoom = jsonProduct.optJSONObject("SuperZoom");
      if (jsonSuperZoom instanceof JSONObject) {
        jsonImg = jsonSuperZoom.optJSONArray("img");
      }
    }
    return jsonImg;
  }

  private String getImageFromJSONOfNewSite(JSONObject imageJson) {
    String image = null;

    if (imageJson.optString("l") instanceof String) {
      image = CrawlerUtils.completeUrl(imageJson.get("l").toString().trim(), PROTOCOL, HOST);
    } else if (imageJson.optString("m") instanceof String) {
      image = CrawlerUtils.completeUrl(imageJson.get("m").toString().trim(), PROTOCOL, HOST);
    } else if (imageJson.optString("t") instanceof String) {
      image = CrawlerUtils.completeUrl(imageJson.get("t").toString().trim(), PROTOCOL, HOST);
    }

    return image;
  }

  private RatingsReviews scrapRatingAndReviews(String internalId, JSONObject productJson) {
    RatingsReviews ratingReviews = new RatingsReviews();

    ratingReviews.setDate(session.getDate());
    ratingReviews.setInternalId(internalId);

    JSONObject ratingJson = productJson.optJSONObject("aggregateRating");
    if (ratingJson instanceof JSONObject) {
      Integer totalNumOfEvaluations = CrawlerUtils.getIntegerValueFromJSON(ratingJson, "reviewCount", 0);
      Double avgRating = CrawlerUtils.getDoubleValueFromJSON(ratingJson, "ratingValue", true, false);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAdvancedRatingReview(getAdvancedRating(internalId, totalNumOfEvaluations));
      ratingReviews.setAverageOverallRating(avgRating != null ? avgRating : 0D);
    }
    return ratingReviews;
  }

  private AdvancedRatingReview getAdvancedRating(String internalId, int total) {
    int pageNumber = 0;
    int totalProds = 0;
    AdvancedRatingReview advancedRating = new AdvancedRatingReview();
    advancedRating.setTotalStar1(0);
    advancedRating.setTotalStar2(0);
    advancedRating.setTotalStar3(0);
    advancedRating.setTotalStar4(0);
    advancedRating.setTotalStar5(0);
    String response = null;
    Document document = null;
    do {
      String url = new StringBuilder()
          .append(session.getOriginalURL()).append("?")
          .append("__pAct_=getmoreprodcomments")
          .append("&productid=").append(internalId)
          .append("&commentsorder=RELEVANCE")
          .append("&&pagenumber=").append(pageNumber).toString();

      Request request = RequestBuilder.create()
          .setUrl(url)
          .setCookies(cookies)
          .build();
      response = this.dataFetcher.get(session, request).getBody();

      if (!response.isEmpty()) {
        document = Jsoup.parse(response);

        for (Iterator<Element> iterator = document.select(".product-rating :first-child").iterator(); iterator.hasNext();) {
          Element element = iterator.next();
          int star = MathUtils.parseInt(element.className());
          totalProds++;
          switch (star) {
            case 1:
              advancedRating.setTotalStar1(advancedRating.getTotalStar1() + 1);
              break;
            case 2:
              advancedRating.setTotalStar2(advancedRating.getTotalStar2() + 1);
              break;
            case 3:
              advancedRating.setTotalStar3(advancedRating.getTotalStar3() + 1);
              break;
            case 4:
              advancedRating.setTotalStar4(advancedRating.getTotalStar4() + 1);
              break;
            case 5:
              advancedRating.setTotalStar5(advancedRating.getTotalStar5() + 1);
              break;
            default:
              break;
          }
        }
      }
      pageNumber++;
    } while (totalProds < total && !response.isEmpty());
    return advancedRating;
  }

}
