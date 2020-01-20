package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
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
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import models.AdvancedRatingReview;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;


public class BrasilBemolCrawler extends Crawler {

  private static final String HOME_PAGE = "http://www.bemol.com.br/";

  public BrasilBemolCrawler(Session session) {
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

      String internalId = crawlInternalId(doc);
      String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, ".reference [itemprop=productID]", true);
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".information .name", true);
      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".buy-box .sale-price", null, false, ',', session);
      Prices prices = crawlPrices(price, internalId, doc);
      boolean available = doc.selectFirst(".wd-buy-button  > div:not([style$=none])") != null;
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrum-product li:not(.first) a span");
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".wd-product-media-selector .image.selected img, .wd-product-media-selector .image:not(.selected) img",
          Arrays.asList("data-image-large", "data-small", "data-image-big", "src"), "https:", "d3ddx6b2p2pevg.cloudfront.net");

      if (primaryImage != null && primaryImage.endsWith("/Custom/Content")) {
        primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".wd-product-media-selector .image.selected img, .wd-product-media-selector .image:not(.selected) img",
            Arrays.asList("data-image-big", "data-small", "src"), "https:", "d3ddx6b2p2pevg.cloudfront.net");
      }
      RatingsReviews ratingsReviews = scrapRating(doc, internalId);
      String secondaryImages = scrapSimpleSecondaryImages(doc, ".wd-product-media-selector .image:not(.selected) img", Arrays.asList("data-image-large", "data-image-big", "data-small", "src"),
          "https:", "d3ddx6b2p2pevg.cloudfront.net", primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".wrapper-detalhe-produto .descriptions", ".wrapper-detalhe-produto .caracteristicas"));

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
          .setRatingReviews(ratingsReviews)
          .setMarketplace(new Marketplace())
          .build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    return !doc.select("input[name=ProductID]").isEmpty();
  }

  private static String crawlInternalId(Document doc) {
    String internalId = null;

    Element infoElement = doc.selectFirst("input[name=ProductID]");
    if (infoElement != null) {
      internalId = infoElement.val();
    }

    return internalId;
  }

  public static String scrapSimpleSecondaryImages(Document doc, String cssSelector, List<String> attributes, String protocol, String host, String primaryImage) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements images = doc.select(cssSelector);
    for (Element e : images) {
      String image = sanitizeUrl(e, attributes, protocol, host);

      if ((primaryImage == null || !primaryImage.equals(image)) && image != null) {
        secondaryImagesArray.put(image);
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  public static String sanitizeUrl(Element element, List<String> attributes, String protocol, String host) {
    String sanitizedUrl = null;

    for (String att : attributes) {
      String url = element.attr(att).trim();

      if (!url.isEmpty() && !url.equalsIgnoreCase("https://d8xabijtzlaac.cloudfront.net/Custom/Content")) {
        sanitizedUrl = CrawlerUtils.completeUrl(url, protocol, host);
        break;
      }
    }

    return sanitizedUrl;
  }

  /**
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, String internalId, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      Request request = RequestBuilder.create().setUrl(HOME_PAGE + "widget/product_payment_options?SkuID=" + internalId + "&ProductID=" + internalId
          + "&Template=wd.product.payment.options.result.template&ForceWidgetToRender=true&nocache=1108472214").setCookies(cookies).build();
      Document docPrices = Jsoup.parse(this.dataFetcher.get(session, request).getBody());

      Elements cards = docPrices.select(".modal-wd-product-payment-options .grid table");
      for (Element e : cards) {
        Element cardElement = e.selectFirst(".payment-description");
        if (cardElement != null) {
          Map<Integer, Float> installmentPriceMap = new TreeMap<>();
          installmentPriceMap.put(1, price);

          Card card = null;
          String text = cardElement.ownText().toLowerCase();

          if (text.contains("visa")) {
            card = Card.VISA;
          } else if (text.contains("amex")) {
            card = Card.AMEX;
          } else if (text.contains("elo")) {
            card = Card.ELO;
          } else if (text.contains("master")) {
            card = Card.MASTERCARD;
          } else if (text.contains("diners")) {
            card = Card.DINERS;
          } else if (text.contains("bemol")) {
            card = Card.SHOP_CARD;

            Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(".cartao-bemol strong", doc, true, "de");

            if (!pair.isAnyValueNull()) {
              installmentPriceMap.put(pair.getFirst(), pair.getSecond());
            }
          }

          if (card != null) {

            Elements installments = e.select("tbody tr td");
            for (Element i : installments) {
              Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(null, i, false, "de", "juros", true, ',');

              if (!pair.isAnyValueNull()) {
                installmentPriceMap.put(pair.getFirst(), pair.getSecond());
              }
            }

            prices.insertCardInstallment(card.toString(), installmentPriceMap);
          } else if (text.contains("boleto")) {
            Element bank = e.selectFirst("tbody tr td");

            if (bank != null) {
              prices.setBankTicketPrice(MathUtils.parseFloatWithComma(bank.ownText()));
            }
          }
        }
      }
    }

    return prices;
  }

  private RatingsReviews scrapRating(Document document, String internalId) {
    RatingsReviews ratingReviews = new RatingsReviews();
    Integer totalNumOfEvaluations = getTotalNumOfRatings(document);
    Double avgRating = getTotalAvgRating(document);
    AdvancedRatingReview advancedRating = getAdvancedRating(internalId, totalNumOfEvaluations);
    ratingReviews.setDate(session.getDate());
    ratingReviews.setInternalId(internalId);
    ratingReviews.setTotalRating(totalNumOfEvaluations);
    ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
    ratingReviews.setAverageOverallRating(avgRating);
    ratingReviews.setAdvancedRatingReview(advancedRating);

    return ratingReviews;
  }

  private AdvancedRatingReview getAdvancedRating(String internalId, int totalNumOfEvaluations) {
    int i = 0, total = 0;
    AdvancedRatingReview advancedRating = new AdvancedRatingReview();
    advancedRating.setTotalStar1(0);
    advancedRating.setTotalStar2(0);
    advancedRating.setTotalStar3(0);
    advancedRating.setTotalStar4(0);
    advancedRating.setTotalStar5(0);
    String response = null;
    Document document = null;
    do {
      String url = new StringBuilder().append("https://www.bemol.com.br/widget/product_reviews?ProductID=").append(internalId)
          .append("&PageIndex=").append(i)
          .append("&PageSize=").append("20")
          .append("&Template=").append("wd.product.reviews.paginate.template")
          .append("&DisplayEmail=").append("false")
          .toString();
      Map<String, String> headers = new HashMap<>();

      headers.put("Content-Type", "application/x-www-form-urlencoded");
      headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.117 Safari/537.36");
      Request request = RequestBuilder.create().setUrl(url).setHeaders(headers).mustSendContentEncoding(false).build();
      response = this.dataFetcher.get(session, request).getBody();

      if (!response.isEmpty()) {
        document = Jsoup.parse(response);

        for (Iterator<Element> iterator = document.select(".avaliacao :nth-child(2)").iterator(); iterator.hasNext();) {
          Element element = iterator.next();
          int star = MathUtils.parseInt(element.text());
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
          total++;
        }
      }
      i++;
    } while (total < totalNumOfEvaluations && !response.isEmpty() && (document instanceof Document));
    return advancedRating;
  }

  private Double getTotalAvgRating(Document docRating) {
    Double avgRating = CrawlerUtils.scrapDoublePriceFromHtml(docRating, ".wd-product-rating .rating-average", null, true, ',', session);

    if (avgRating == null) {
      avgRating = 0d;
    }

    return avgRating;
  }

  private Integer getTotalNumOfRatings(Document doc) {
    Integer totalRating = 0;
    Elements rating = doc.select(".wd-product-rating [itemprop=reviewCount]");

    if (rating != null) {
      String votes = rating.attr("content").replaceAll("[^0-9]", "").trim();

      if (!votes.isEmpty()) {
        totalRating += Integer.parseInt(votes);
      }
    }

    return totalRating;
  }
}
