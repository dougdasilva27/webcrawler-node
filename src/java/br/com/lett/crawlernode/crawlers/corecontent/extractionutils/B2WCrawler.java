package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import com.google.common.net.HttpHeaders;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import enums.OfferField;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Marketplace;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.Seller;
import models.Util;
import models.prices.Prices;

public class B2WCrawler extends Crawler {
  protected Map<String, String> headers = new HashMap<>();
  private static final String MAIN_B2W_NAME_LOWER = "b2w";
  private static final Card DEFAULT_CARD = Card.VISA;
  protected String sellerNameLower;
  protected List<String> subSellers;
  protected String homePage;

  public B2WCrawler(Session session) {
    super(session);
    super.config.setFetcher(FetchMode.FETCHER);
    super.config.setMustSendRatingToKinesis(true);
    this.setHeaders();
  }

  protected void setHeaders() {
    headers.put(HttpHeaders.REFERER, this.homePage);
    headers.put(
        HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3"
    );
    headers.put(HttpHeaders.CACHE_CONTROL, "max-age=0");
    headers.put(HttpHeaders.CONNECTION, "keep-alive");
    headers.put(HttpHeaders.ACCEPT_LANGUAGE, "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");
    headers.put(HttpHeaders.ACCEPT_ENCODING, "no");
    headers.put("Upgrade-Insecure-Requests", "1");
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(homePage));
  }

  @Override
  protected Document fetch() {
    return Jsoup.parse(fetchPage(session.getOriginalURL(), session));
  }

  public String fetchPage(String url, Session session) {
    Request request = RequestBuilder.create()
        .setUrl(url)
        .setCookies(this.cookies)
        .setHeaders(this.headers)
        .mustSendContentEncoding(false)
        .setFetcheroptions(
            FetcherOptionsBuilder.create()
                .mustUseMovingAverage(false)
                .mustRetrieveStatistics(true)
                .setForbiddenCssSelector("#px-captcha")
                .build()
        ).setProxyservice(
            Arrays.asList(
                ProxyCollection.INFATICA_RESIDENTIAL_BR,
                ProxyCollection.STORM_RESIDENTIAL_EU,
                ProxyCollection.STORM_RESIDENTIAL_US,
                ProxyCollection.BUY
            )
        ).build();

    String content = this.dataFetcher.get(session, request).getBody();

    if (content == null || content.isEmpty()) {
      content = new ApacheDataFetcher().get(session, request).getBody();
    }

    return content;
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    List<Product> products = new ArrayList<>();

    // Json da pagina principal
    JSONObject frontPageJson = SaopauloB2WCrawlersUtils.getDataLayer(doc);
    // Pega só o que interessa do json da api
    JSONObject infoProductJson = SaopauloB2WCrawlersUtils.assembleJsonProductWithNewWay(frontPageJson);

    // verifying if url starts with home page because on crawler seed,
    // some seeds can be of another store
    if (infoProductJson.has("skus") && session.getOriginalURL().startsWith(this.homePage)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalPid = this.crawlInternalPid(infoProductJson);
      CategoryCollection categories = crawlCategories(infoProductJson);
      boolean hasImages = doc.select(".main-area .row > div > span > img:not([src])").first() == null && doc.select(".gallery-product") != null;
      String primaryImage = hasImages ? this.crawlPrimaryImage(infoProductJson) : null;
      String secondaryImages = hasImages ? this.crawlSecondaryImages(infoProductJson) : null;
      String description = this.crawlDescription(internalPid, doc);
      RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
      List<String> eans = crawlEan(infoProductJson);

      Map<String, String> skuOptions = this.crawlSkuOptions(infoProductJson, doc);

      for (Entry<String, String> entry : skuOptions.entrySet()) {
        String internalId = entry.getKey();
        String name = entry.getValue().trim();
        Map<String, Prices> marketplaceMap = this.crawlMarketplace(infoProductJson, internalId);
        Marketplace variationMarketplace = this.assembleMarketplaceFromMap(marketplaceMap);
        boolean available = this.crawlAvailability(marketplaceMap);
        Float variationPrice = this.crawlPrice(marketplaceMap);
        Prices prices = crawlPrices(marketplaceMap);
        List<JSONObject> buyBox = scrapBuyBox(doc, internalId, internalPid);
        Offers offers = assembleOffers(buyBox);

        RatingsReviews ratingReviews = crawlRatingReviews(frontPageJson, internalPid);


        RatingsReviews clonedRatingReviews = ratingReviews.clone();
        clonedRatingReviews.setInternalId(internalId);
        ratingReviewsCollection.addRatingReviews(clonedRatingReviews);

        // Creating the product
        Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrice(variationPrice)
            .setPrices(prices)
            .setAvailable(available)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setMarketplace(variationMarketplace)
            .setOffers(offers)
            .setRatingReviews(clonedRatingReviews)
            .setEans(eans)
            .build();

        products.add(product);
      }
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  /**
   * Crawl rating and reviews stats using the bazaar voice endpoint. To get only the stats summary we
   * need at first, we only have to do one request. If we want to get detailed information about each
   * review, we must perform pagination.
   * 
   * The RatingReviews crawled in this method, is the same across all skus variations in a page.
   *
   * @param document
   * @return
   */
  private RatingsReviews crawlRatingReviews(JSONObject frontPageJson, String skuInternalPid) {
    RatingsReviews ratingReviews = new RatingsReviews();

    ratingReviews.setDate(session.getDate());

    String bazaarVoicePassKey = crawlBazaarVoiceEndpointPassKey(frontPageJson);

    String endpointRequest = assembleBazaarVoiceEndpointRequest(skuInternalPid, bazaarVoicePassKey, 0, 5);

    Request request = RequestBuilder.create().setUrl(endpointRequest).setCookies(cookies).build();
    JSONObject ratingReviewsEndpointResponse = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

    JSONObject reviewStatistics = getReviewStatisticsJSON(ratingReviewsEndpointResponse, skuInternalPid);
    AdvancedRatingReview advancedRatingReview = getTotalStarsFromEachValue(reviewStatistics);

    Integer totalRating = getTotalReviewCount(reviewStatistics);

    ratingReviews.setAdvancedRatingReview(advancedRatingReview);
    ratingReviews.setTotalRating(totalRating);
    ratingReviews.setTotalWrittenReviews(totalRating);
    ratingReviews.setAverageOverallRating(getAverageOverallRating(reviewStatistics));

    return ratingReviews;
  }

  private AdvancedRatingReview getTotalStarsFromEachValue(JSONObject reviewStatistics) {
    Integer star1 = 0;
    Integer star2 = 0;
    Integer star3 = 0;
    Integer star4 = 0;
    Integer star5 = 0;

    if (reviewStatistics.has("RatingDistribution")) {
      JSONArray ratingDistribution = reviewStatistics.getJSONArray("RatingDistribution");
      for (Object object : ratingDistribution) {
        JSONObject rating = (JSONObject) object;
        Integer option = CrawlerUtils.getIntegerValueFromJSON(rating, "RatingValue", 0);

        if (rating.has("RatingValue") && option == 1 && rating.has("Count")) {
          star1 = CrawlerUtils.getIntegerValueFromJSON(rating, "Count", 0);
        }

        if (rating.has("RatingValue") && option == 2 && rating.has("Count")) {
          star2 = CrawlerUtils.getIntegerValueFromJSON(rating, "Count", 0);
        }

        if (rating.has("RatingValue") && option == 3 && rating.has("Count")) {
          star3 = CrawlerUtils.getIntegerValueFromJSON(rating, "Count", 0);
        }

        if (rating.has("RatingValue") && option == 4 && rating.has("Count")) {
          star4 = CrawlerUtils.getIntegerValueFromJSON(rating, "Count", 0);
        }

        if (rating.has("RatingValue") && option == 5 && rating.has("Count")) {
          star5 = CrawlerUtils.getIntegerValueFromJSON(rating, "Count", 0);
        }

      }
    }

    return new AdvancedRatingReview.Builder().totalStar1(star1).totalStar2(star2).totalStar3(star3).totalStar4(star4).totalStar5(star5).build();
  }

  private Integer getTotalReviewCount(JSONObject reviewStatistics) {
    Integer totalReviewCount = null;
    if (reviewStatistics.has("TotalReviewCount")) {
      totalReviewCount = reviewStatistics.getInt("TotalReviewCount");
    }
    return totalReviewCount;
  }

  private Double getAverageOverallRating(JSONObject reviewStatistics) {
    Double avgOverallRating = null;
    if (reviewStatistics.has("AverageOverallRating")) {
      avgOverallRating = reviewStatistics.getDouble("AverageOverallRating");
    }
    return avgOverallRating;
  }

  /**
   * e.g: http://api.bazaarvoice.com/data/reviews.json?apiversion=5.4
   * &passkey=oqu6lchjs2mb5jp55bl55ov0d &Offset=0 &Limit=5 &Sort=SubmissionTime:desc
   * &Filter=ProductId:113048617 &Include=Products &Stats=Reviews
   * 
   * Endpoint request parameters:
   * <p>
   * &passKey: the password used to request the bazaar voice endpoint. This pass key e crawled inside
   * the html of the sku page, inside a script tag. More details on how to crawl this passKey
   * </p>
   * <p>
   * &Offset: the number of the chunk of data retrieved by the endpoint. If we want the second chunk,
   * we must add this value by the &Limit parameter.
   * </p>
   * <p>
   * &Limit: the number of reviews that a request will return, at maximum.
   * </p>
   * 
   * The others parameters we left as default.
   * 
   * Request Method: GET
   */
  private String assembleBazaarVoiceEndpointRequest(String skuInternalPid, String bazaarVoiceEnpointPassKey, Integer offset, Integer limit) {

    StringBuilder request = new StringBuilder();

    request.append("http://api.bazaarvoice.com/data/reviews.json?apiversion=5.4");
    request.append("&passkey=" + bazaarVoiceEnpointPassKey);
    request.append("&Offset=" + offset);
    request.append("&Limit=" + limit);
    request.append("&Sort=SubmissionTime:desc");
    request.append("&Filter=ProductId:" + skuInternalPid);
    request.append("&Include=Products");
    request.append("&Stats=Reviews");

    return request.toString();
  }

  /**
   * Crawl the bazaar voice endpoint passKey on the sku page. The passKey is located inside a script
   * tag, which contains a json object is several metadata, including the passKey.
   * 
   * @param document
   * @return
   */
  private String crawlBazaarVoiceEndpointPassKey(JSONObject embeddedJSONObject) {
    String passKey = null;
    if (embeddedJSONObject != null) {
      if (embeddedJSONObject.has("configuration")) {
        JSONObject configuration = embeddedJSONObject.getJSONObject("configuration");

        if (configuration.has("bazaarvoicePasskey")) {
          passKey = configuration.getString("bazaarvoicePasskey");
        }
      }
    }
    return passKey;
  }

  private JSONObject getReviewStatisticsJSON(JSONObject ratingReviewsEndpointResponse, String skuInternalPid) {
    if (ratingReviewsEndpointResponse.has("Includes")) {
      JSONObject includes = ratingReviewsEndpointResponse.getJSONObject("Includes");

      if (includes.has("Products")) {
        JSONObject products = includes.getJSONObject("Products");

        if (products.has(skuInternalPid)) {
          JSONObject product = products.getJSONObject(skuInternalPid);

          if (product.has("ReviewStatistics")) {
            return product.getJSONObject("ReviewStatistics");
          }
        }
      }
    }

    return new JSONObject();
  }

  private List<String> crawlEan(JSONObject infoProductJson) {
    List<String> eans = new ArrayList<>();
    if (infoProductJson.has("skus")) {
      JSONArray skusArray = infoProductJson.getJSONArray("skus");
      for (Object object : skusArray) {
        JSONObject skus = (JSONObject) object;

        if (skus.has("eans")) {
          JSONArray eansArray = skus.getJSONArray("eans");

          for (Object eansObject : eansArray) {
            String ean = (String) eansObject;
            eans.add(ean);
          }
        }
      }
    }

    return eans;
  }

  private Offers assembleOffers(List<JSONObject> buyBox) {
    Offers offers = new Offers();

    for (JSONObject jsonObject : buyBox) {
      try {
        Offer offer = new Offer(jsonObject);
        offers.add(offer);
      } catch (OfferException e) {
        Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      }
    }

    return offers;
  }

  private List<JSONObject> scrapBuyBox(Document doc, String internalId, String internalPid) {
    List<JSONObject> listBuyBox = new ArrayList<>();

    JSONObject jsonSeller = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.__PRELOADED_STATE__ =", ";", false, true);
    JSONObject offers = SaopauloB2WCrawlersUtils.extractJsonOffers(jsonSeller, internalPid);

    Map<String, Float> mapOfSellerIdAndPrice = new HashMap<>();

    // Getting informations from sellers.
    if (offers.has(internalId)) {
      JSONArray sellerInfo = offers.getJSONArray(internalId);

      // The Business logic is: if we have more than 1 seller is buy box
      boolean isBuyBox = sellerInfo.length() > 1;

      for (int i = 0; i < sellerInfo.length(); i++) {
        JSONObject buyBoxJson = new JSONObject();

        JSONObject info = (JSONObject) sellerInfo.get(i);

        if (info.has("sellerName") && !info.isNull("sellerName") && info.has("id") && !info.isNull("id")) {
          String name = info.get("sellerName").toString();
          String internalSellerId = info.get("id").toString();

          buyBoxJson.put(OfferField.IS_BUYBOX.toString(), isBuyBox);
          buyBoxJson.put(OfferField.SELLERS_PAGE_POSITION.toString(), JSONObject.NULL);
          buyBoxJson.put(OfferField.SELLER_FULL_NAME.toString(), name);
          buyBoxJson.put(OfferField.SLUG_SELLER_NAME.toString(), CommonMethods.toSlug(name));
          buyBoxJson.put(OfferField.INTERNAL_SELLER_ID.toString(), internalSellerId);

          Prices prices = crawlMarketplacePrices(info);
          Float price1x = !prices.isEmpty() ? prices.getCardInstallmentValue(DEFAULT_CARD.toString(), 1).floatValue() : null;
          Float bankTicket = CrawlerUtils.getFloatValueFromJSON(info, "bakTicket", true, false);
          Float defaultPrice = CrawlerUtils.getFloatValueFromJSON(info, "defaultPrice", true, false);


          if (i + 1 <= 3) {
            buyBoxJson.put(OfferField.MAIN_PAGE_POSITION.toString(), i + 1);
            Float featuredPrice = null;

            for (Float value : Arrays.asList(price1x, bankTicket, defaultPrice)) {
              if (featuredPrice == null || (value != null && value < featuredPrice)) {
                featuredPrice = value;
              }
            }

            buyBoxJson.put(OfferField.MAIN_PRICE.toString(), featuredPrice);
            mapOfSellerIdAndPrice.put(internalSellerId, featuredPrice);


          } else {
            Float featuredPrice = null;

            if (defaultPrice != null) {
              featuredPrice = defaultPrice;
            } else if (price1x != null) {
              featuredPrice = price1x;
            } else if (bankTicket != null) {
              featuredPrice = bankTicket;
            }

            mapOfSellerIdAndPrice.put(internalSellerId, featuredPrice);

            buyBoxJson.put(OfferField.MAIN_PRICE.toString(), featuredPrice);
            buyBoxJson.put(OfferField.MAIN_PAGE_POSITION.toString(), JSONObject.NULL);
          }

          listBuyBox.add(buyBoxJson);
        }
      }
    }

    if (listBuyBox.size() > 1) {
      // Sellers page positios is order by price, in this map, price is the value
      Map<String, Float> sortedMap = sortMapByValue(mapOfSellerIdAndPrice);

      int position = 1;

      for (Entry<String, Float> entry : sortedMap.entrySet()) {
        for (JSONObject buyBoxJson : listBuyBox) {
          if (buyBoxJson.has(OfferField.INTERNAL_SELLER_ID.toString())
              && buyBoxJson.get(OfferField.INTERNAL_SELLER_ID.toString()).toString().equals(entry.getKey())) {
            buyBoxJson.put(OfferField.SELLERS_PAGE_POSITION.toString(), position);
          }
        }

        position++;
      }
    }

    return listBuyBox;
  }

  /**
   * Sort map by Value
   * 
   * @param map
   * @return
   */
  private Map<String, Float> sortMapByValue(final Map<String, Float> map) {
    return map.entrySet()
        .stream()
        .sorted(Map.Entry.comparingByValue())
        .collect(
            Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                LinkedHashMap::new));
  }

  /*******************************
   * Product page identification *
   *******************************/

  private String crawlInternalPid(JSONObject assembleJsonProduct) {
    String internalPid = null;

    if (assembleJsonProduct.has("internalPid")) {
      internalPid = assembleJsonProduct.getString("internalPid").trim();
    }

    return internalPid;
  }

  private Map<String, String> crawlSkuOptions(JSONObject infoProductJson, Document doc) {
    Map<String, String> skuMap = new HashMap<>();

    boolean unnavailablePage = !doc.select("#title-stock").isEmpty();

    if (infoProductJson.has("skus")) {
      JSONArray skus = infoProductJson.getJSONArray("skus");

      for (int i = 0; i < skus.length(); i++) {
        JSONObject sku = skus.getJSONObject(i);

        if (sku.has("internalId")) {
          String internalId = sku.getString("internalId");
          StringBuilder name = new StringBuilder();

          String variationName = "";
          if (sku.has("variationName")) {
            variationName = sku.getString("variationName");
          }

          String varationNameWithoutVolts = variationName.replace("volts", "").trim();

          if (unnavailablePage || (variationName.isEmpty() && skus.length() < 2) && infoProductJson.has("name")) {
            name.append(infoProductJson.getString("name"));
          } else if (sku.has("name")) {
            name.append(sku.getString("name"));

            if (!name.toString().toLowerCase().contains(varationNameWithoutVolts.toLowerCase())) {
              name.append(" " + variationName);
            }
          }

          skuMap.put(internalId, name.toString().trim());
        }
      }
    }

    return skuMap;

  }

  private Map<String, Prices> crawlMarketplace(JSONObject skus, String internalId) {
    Map<String, Prices> marketplaces = new HashMap<>();

    if (skus.has("prices")) {
      JSONObject pricesJson = skus.getJSONObject("prices");

      if (pricesJson.has(internalId)) {
        JSONArray marketArrays = pricesJson.getJSONArray(internalId);

        for (int i = 0; i < marketArrays.length(); i++) {
          JSONObject seller = marketArrays.getJSONObject(i);

          if (seller.has("sellerName")) {
            String sellerName = seller.getString("sellerName");
            Prices prices = crawlMarketplacePrices(seller);

            if (marketArrays.length() == 1 && seller.has("priceFrom")) {
              String text = seller.get("priceFrom").toString();
              prices.setPriceFrom(MathUtils.parseDoubleWithComma(text));
            }

            marketplaces.put(sellerName, prices);
          }
        }
      }
    }

    return marketplaces;
  }

  private Prices crawlMarketplacePrices(JSONObject seller) {
    Prices prices = new Prices();

    if (seller.has("bankTicket")) {
      prices.setBankTicketPrice(seller.getDouble("bankTicket"));
    }

    if (seller.has("installments")) {
      Map<Integer, Float> installmentMapPrice = new HashMap<>();

      JSONArray installments = seller.getJSONArray("installments");

      for (int i = 0; i < installments.length(); i++) {
        JSONObject installment = installments.getJSONObject(i);

        if (installment.has("quantity") && installment.has("value")) {
          Integer quantity = installment.getInt("quantity");
          Double value = installment.getDouble("value");

          installmentMapPrice.put(quantity, MathUtils.normalizeTwoDecimalPlaces(value.floatValue()));
        }
      }

      // Isso acontece quando o seller principal não é a b2w, com isso não aparecem as parcelas
      // Na maioria dos casos a primeira parcela tem desconto e as demais não
      // O preço default seria o preço sem desconto.
      // Para pegar esse preço, dividimos ele por 2 e adicionamos nas parcelas como 2x esse preço
      if (!installmentMapPrice.isEmpty() && installmentMapPrice.containsKey(1) && seller.has("defaultPrice")) {
        Float defaultPrice = CrawlerUtils.getFloatValueFromJSON(seller, "defaultPrice");

        if (!defaultPrice.equals(installmentMapPrice.get(1))) {
          installmentMapPrice.put(2, MathUtils.normalizeTwoDecimalPlaces(defaultPrice / 2f));
        }
      }

      prices.insertCardInstallment(DEFAULT_CARD.toString(), installmentMapPrice);
      prices.insertCardInstallment(Card.VISA.toString(), installmentMapPrice);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentMapPrice);
      prices.insertCardInstallment(Card.AURA.toString(), installmentMapPrice);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentMapPrice);
      prices.insertCardInstallment(Card.HIPER.toString(), installmentMapPrice);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentMapPrice);
    }

    if (seller.has("installmentsShopCard")) {
      Map<Integer, Float> installmentMapPrice = new HashMap<>();

      JSONArray installments = seller.getJSONArray("installmentsShopCard");

      for (int i = 0; i < installments.length(); i++) {
        JSONObject installment = installments.getJSONObject(i);

        if (installment.has("quantity") && installment.has("value")) {
          Integer quantity = installment.getInt("quantity");
          Double value = installment.getDouble("value");

          installmentMapPrice.put(quantity, MathUtils.normalizeTwoDecimalPlaces(value.floatValue()));
        }
      }

      prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentMapPrice);
    }

    return prices;
  }

  /*******************
   * General methods *
   *******************/

  private Float crawlPrice(Map<String, Prices> marketplaces) {
    Float price = null;
    Prices prices = null;

    String sellerName = getPrincipalSellerName(marketplaces);
    if (sellerName != null) {
      prices = marketplaces.get(sellerName);
    }

    if (prices != null && prices.getCardPaymentOptions(Card.VISA.toString()).containsKey(1)) {
      Double priceDouble = prices.getCardPaymentOptions(Card.VISA.toString()).get(1);
      price = priceDouble.floatValue();
    }

    return price;
  }

  private boolean crawlAvailability(Map<String, Prices> marketplaces) {
    boolean available = false;

    String sellerName = getPrincipalSellerName(marketplaces);
    if (sellerName != null) {
      available = true;
    }

    return available;
  }

  private String crawlPrimaryImage(JSONObject infoProductJson) {
    String primaryImage = null;

    if (infoProductJson.has("images")) {
      JSONObject images = infoProductJson.getJSONObject("images");

      if (images.has("primaryImage")) {
        primaryImage = images.getString("primaryImage");
      }
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(JSONObject infoProductJson) {
    String secondaryImages = null;

    JSONArray secondaryImagesArray = new JSONArray();

    if (infoProductJson.has("images")) {
      JSONObject images = infoProductJson.getJSONObject("images");

      if (images.has("secondaryImages")) {
        secondaryImagesArray = images.getJSONArray("secondaryImages");
      }
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
  private CategoryCollection crawlCategories(JSONObject document) {
    CategoryCollection categories = new CategoryCollection();

    JSONArray categoryList = document.getJSONArray("categories");

    for (int i = categoryList.length() - 1; i >= 0; i--) { // Invert the Loop since the categorys in the JSONArray come reversed
      String cat = (categoryList.getJSONObject(i).get("name")).toString();

      if (!cat.isEmpty()) {
        categories.add(cat);
      }
    }

    return categories;
  }


  private Marketplace assembleMarketplaceFromMap(Map<String, Prices> marketplaceMap) {
    Marketplace marketplace = new Marketplace();

    boolean hasPrincipalSeller = marketplaceMap.containsKey(MAIN_B2W_NAME_LOWER) || marketplaceMap.containsKey(sellerNameLower);

    for (String sellerName : marketplaceMap.keySet()) {
      if (!sellerName.equalsIgnoreCase(sellerNameLower) && !sellerName.equalsIgnoreCase(MAIN_B2W_NAME_LOWER)) {
        if (!hasPrincipalSeller && this.subSellers.contains(sellerName)) {
          continue;
        }

        JSONObject sellerJSON = new JSONObject();
        sellerJSON.put("name", sellerName);

        Prices prices = marketplaceMap.get(sellerName);

        if (prices.getCardPaymentOptions(Card.VISA.toString()).containsKey(1)) {
          // Pegando o preço de uma vez no cartão
          Double price = prices.getCardPaymentOptions(Card.VISA.toString()).get(1);
          Float priceFloat = MathUtils.normalizeTwoDecimalPlaces(price.floatValue());

          sellerJSON.put("price", priceFloat); // preço de boleto é o mesmo de preço uma vez.
        }
        sellerJSON.put("prices", marketplaceMap.get(sellerName).toJSON());

        try {
          Seller seller = new Seller(sellerJSON);
          marketplace.add(seller);
        } catch (Exception e) {
          Logging.printLogError(logger, session, Util.getStackTraceString(e));
        }
      }
    }

    return marketplace;
  }

  private String crawlDescription(String internalPid, Document doc) {
    StringBuilder description = new StringBuilder();

    boolean alreadyCapturedHtmlSlide = false;

    Element datasheet = doc.selectFirst("#info-section");
    if (datasheet != null) {
      Element iframe = datasheet.selectFirst("iframe");

      if (iframe != null) {
        Document docDescriptionFrame = Jsoup.parse(fetchPage(iframe.attr("src"), session));
        if (docDescriptionFrame != null) {
          description.append(docDescriptionFrame.html());
        }
      }

      // https://www.shoptime.com.br/produto/8421276/mini-system-mx-hs6500-zd-bluetooth-e-funcao-karaoke-bivolt-preto-samsung
      // alreadyCapturedHtmlSlide as been moved here because of links like these.

      alreadyCapturedHtmlSlide = true;
      datasheet.select("iframe, h1.sc-hgHYgh").remove();
      description.append(datasheet.html().replace("hidden", ""));
    }

    if (internalPid != null) {
      Element desc2 = doc.select(".info-description-frame-inside").first();

      if (desc2 != null && !alreadyCapturedHtmlSlide) {
        String urlDesc2 = homePage + "product-description/acom/" + internalPid;
        Document docDescriptionFrame = Jsoup.parse(fetchPage(urlDesc2, session));
        if (docDescriptionFrame != null) {
          description.append(docDescriptionFrame.html());
        }
      }

      Element elementProductDetails = doc.select(".info-section").last();
      if (elementProductDetails != null) {
        elementProductDetails.select(".info-section-header.hidden-md.hidden-lg").remove();
        description.append(elementProductDetails.html());
      }
    }

    return Normalizer.normalize(description.toString(), Normalizer.Form.NFD).replaceAll("[^\n\t\r\\p{Print}]", "");
  }

  private Prices crawlPrices(Map<String, Prices> marketplaceMap) {
    Prices prices = new Prices();

    String sellerName = getPrincipalSellerName(marketplaceMap);
    if (sellerName != null) {
      prices = marketplaceMap.get(sellerName);
    }

    return prices;
  }

  /**
   * se retornar null o produto nao e vendido pela loja
   * 
   * @param marketplaceMap
   * @return
   */
  private String getPrincipalSellerName(Map<String, Prices> marketplaceMap) {
    String sellerName = null;

    if (marketplaceMap.containsKey(sellerNameLower)) {
      sellerName = sellerNameLower;
    } else if (marketplaceMap.containsKey(MAIN_B2W_NAME_LOWER)) {
      sellerName = MAIN_B2W_NAME_LOWER;
    } else {
      for (String seller : subSellers) {
        if (marketplaceMap.containsKey(seller)) {
          sellerName = seller;
          break;
        }
      }
    }

    return sellerName;
  }

}
