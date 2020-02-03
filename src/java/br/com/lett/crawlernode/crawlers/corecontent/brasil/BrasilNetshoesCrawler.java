package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import br.com.lett.crawlernode.core.fetcher.FetchUtilities;
import br.com.lett.crawlernode.core.fetcher.models.LettProxy;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.Seller;
import models.Util;
import models.prices.Prices;
import org.apache.http.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

/**
 * date: 27/03/2018
 * 
 * @author gabriel
 *
 */

public class BrasilNetshoesCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.netshoes.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "netshoes";

  public BrasilNetshoesCrawler(Session session) {
    super(session);
  }

  private String userAgent;
  private LettProxy proxyUsed;

  @Override
  public void handleCookiesBeforeFetch() {
    this.userAgent = FetchUtilities.randUserAgent();

    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.USER_AGENT, this.userAgent);

    Request request = RequestBuilder.create().setUrl(HOME_PAGE).setCookies(cookies).setHeaders(headers).build();
    Response response = this.dataFetcher.get(session, request);

    this.proxyUsed = response.getProxyUsed();

    for (Cookie cookieResponse : response.getCookies()) {
      BasicClientCookie cookie = new BasicClientCookie(cookieResponse.getName(), cookieResponse.getValue());
      cookie.setDomain(".netshoes.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);
    }
  }

  @Override
  protected Object fetch() {
    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.USER_AGENT, this.userAgent);

    Request request = RequestBuilder.create().setUrl(session.getOriginalURL()).setCookies(cookies).setHeaders(headers).setProxy(proxyUsed).build();
    return Jsoup.parse(this.dataFetcher.get(session, request).getBody());
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

      JSONObject chaordicJson = crawlChaordicJson(doc);

      String internalPid = crawlInternalPid(chaordicJson);
      CategoryCollection categories = crawlCategories(doc);
      String description = crawlDescription(doc);

      // sku data in json
      JSONArray arraySkus = chaordicJson != null && chaordicJson.has("skus") ? chaordicJson.getJSONArray("skus") : new JSONArray();

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = crawlInternalId(jsonSku);
        String name = crawlName(chaordicJson, jsonSku);
        boolean availableToBuy = jsonSku.has("status") && jsonSku.get("status").toString().equals("available");
        Document docSku = availableToBuy && arraySkus.length() > 1 ? crawlDocumentSku(internalId, jsonSku, doc) : doc;

          Map<String, Prices> marketplaceMap = availableToBuy ? crawlMarketplace(docSku) : new HashMap<>();
          boolean available = availableToBuy && marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER);

        Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap);
        Prices prices = available ? marketplaceMap.get(MAIN_SELLER_NAME_LOWER) : new Prices();
        Float price = crawlPrice(prices);
        String primaryImage = crawlPrimaryImage(docSku);
        String secondaryImages = crawlSecondaryImages(docSku);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
            .setMarketplace(marketplace).build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(Document document) {
    return document.select(".reference").first() != null;
  }

  /*******************
   * General methods *
   *******************/

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("sku")) {
      internalId = json.getString("sku").trim();
    }

    return internalId;
  }

  private String crawlInternalPid(JSONObject skuJson) {
    String internalPid = null;

    if (skuJson.has("id")) {
      internalPid = skuJson.get("id").toString();
    }

    return internalPid;
  }

  private String crawlName(JSONObject chaordicJson, JSONObject skuJson) {
    StringBuilder name = new StringBuilder();

    if (chaordicJson.has("name")) {
      name.append(chaordicJson.getString("name"));

      if (skuJson.has("specs")) {
        JSONObject specs = skuJson.getJSONObject("specs");

        Set<String> keys = specs.keySet();

        for (String key : keys) {
          if (!key.equalsIgnoreCase("color")) {
            name.append(" " + specs.get(key));
          }
        }
      }
    }

    return name.toString();
  }

  private Document crawlDocumentSku(String internalId, JSONObject skuJson, Document doc) {
    Document docSku = doc;

    if (skuJson.has("specs")) {
      JSONObject specs = skuJson.getJSONObject("specs");

      if (specs.has("size")) {
        String url = "https://www.netshoes.com.br/refactoring/" + internalId;
        String payload = "sizeLabelSelected=" + specs.get("size") + "&isQuickView=false";

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put(HttpHeaders.USER_AGENT, this.userAgent);

        Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).setPayload(payload).setProxy(proxyUsed).build();
        String body = this.dataFetcher.get(session, request).getBody();

        if (body != null) {
          docSku = Jsoup.parse(body);

          Element test = docSku.select("div").first();

          if (test == null) {
            docSku = doc;
          }
        }
      }
    }

    return docSku;
  }

  private Float crawlPrice(Prices prices) {
    Float price = null;

    if (!prices.isEmpty() && prices.getCardPaymentOptions(Card.VISA.toString()).containsKey(1)) {
      Double priceDouble = prices.getCardPaymentOptions(Card.VISA.toString()).get(1);
      price = priceDouble.floatValue();
    }

    return price;
  }

  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;

    Element image = doc.select(".photo-figure img.zoom").first();

    if (image != null) {
      primaryImage = image.attr("data-large-img-url");

      if (!primaryImage.startsWith("http")) {
        primaryImage = "https:" + primaryImage;
      }
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document doc) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements images = doc.select(".swiper-slide:not(.active) img");

    for (Element e : images) {
      String image = e.attr("data-src-large").trim();

      if (image.isEmpty()) {
        image = e.attr("src");
      }

      if (!image.startsWith("http")) {
        image = "https:" + image;
      }

      secondaryImagesArray.put(image);
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private Map<String, Prices> crawlMarketplace(Document doc) {
    Map<String, Prices> marketplace = new HashMap<>();

    String sellerName = MAIN_SELLER_NAME_LOWER;
    Element sellerNameElement = doc.select(".product-seller-name").first();

    if (sellerNameElement != null) {
      sellerName = sellerNameElement.ownText().toLowerCase();
    }

    marketplace.put(sellerName, crawlPrices(doc));

    return marketplace;

  }

  private Marketplace assembleMarketplaceFromMap(Map<String, Prices> marketplaceMap) {
    Marketplace marketplace = new Marketplace();

    for (String seller : marketplaceMap.keySet()) {
      if (!seller.equalsIgnoreCase(MAIN_SELLER_NAME_LOWER)) {
        Prices prices = marketplaceMap.get(seller);

        JSONObject sellerJSON = new JSONObject();
        sellerJSON.put("name", seller);
        sellerJSON.put("price", crawlPrice(prices));
        sellerJSON.put("prices", prices.toJSON());

        try {
          Seller s = new Seller(sellerJSON);
          marketplace.add(s);
        } catch (Exception e) {
          Logging.printLogError(logger, session, Util.getStackTraceString(e));
        }
      }
    }

    return marketplace;
  }

  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select(".breadcrumb li > a span");

    for (int i = 1; i < elementCategories.size(); i++) { // first item is the home page
      categories.add(elementCategories.get(i).text().trim());
    }

    return categories;
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Element shortDescription = doc.select(".description").first();
    if (shortDescription != null) {
      description.append(shortDescription.html());
    }

    Element elementInformation = doc.select("#features").first();
    if (elementInformation != null) {
      description.append(elementInformation.html());
    }

    return description.toString();
  }

  /**
   * To crawl this prices is accessed a api Is removed all accents for crawl price 1x like this: Visa
   * à vista R$ 1.790,00
   * 
   * @param internalId
   * @param price
   * @return
   */
  private Prices crawlPrices(Document doc) {
    Prices prices = new Prices();

    Element priceElement = doc.select(".price > strong").first();

    if (priceElement != null) {
      Float price = MathUtils.parseFloatWithComma(priceElement.ownText());
      prices.setBankTicketPrice(price);

      Map<Integer, Float> mapInstallments = new HashMap<>();
      mapInstallments.put(1, price);

      Element installmentsElement = doc.select(".installments-price").first();

      if (installmentsElement != null) {
        String text = installmentsElement.ownText().toLowerCase();

        if (text.contains("x")) {
          int x = text.indexOf('x');

          String installment = text.substring(0, x).replaceAll("[^0-9]", "").trim();
          Float priceInstallment = MathUtils.parseFloatWithComma(text.substring(x));

          if (!installment.isEmpty() && priceInstallment != null) {
            mapInstallments.put(Integer.parseInt(installment), priceInstallment);
          }
        }
      }

      prices.insertCardInstallment(Card.VISA.toString(), mapInstallments);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), mapInstallments);
      prices.insertCardInstallment(Card.AMEX.toString(), mapInstallments);
      prices.insertCardInstallment(Card.DINERS.toString(), mapInstallments);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), mapInstallments);
      prices.insertCardInstallment(Card.ELO.toString(), mapInstallments);
      prices.insertCardInstallment(Card.SHOP_CARD.toString(), mapInstallments);
    }

    return prices;
  }

  private JSONObject crawlChaordicJson(Document doc) {
    JSONObject skuJson = new JSONObject();

    Elements scripts = doc.select("script");

    for (Element e : scripts) {
      String script = e.outerHtml();


      if (script.contains("freedom.metadata.chaordic(")) {
        String token = "loader.js', '";
        int x = script.indexOf(token) + token.length();
        int y = script.indexOf("');", x);

        String json = script.substring(x, y);

        if (json.startsWith("{") && json.endsWith("}")) {
          try {
            JSONObject chaordic = new JSONObject(json);

            if (chaordic.has("product")) {
              skuJson = chaordic.getJSONObject("product");
            }
          } catch (Exception e1) {
            Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e1));
          }
        }

        break;
      }
    }

    return skuJson;
  }
}
