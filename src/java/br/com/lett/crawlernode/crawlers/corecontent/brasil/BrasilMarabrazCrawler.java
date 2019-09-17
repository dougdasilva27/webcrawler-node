package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

public class BrasilMarabrazCrawler extends Crawler {

  public BrasilMarabrazCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {

      String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, "span[itemprop=\"sku\"]", false);
      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[id=\"productId\"]", "value");
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1[itemprop=\"name\"]", false);
      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".special-price .price", "content", false, '.', session);
      Prices prices = scrapPrices(internalPid);
      boolean available = scrapAvailability(doc);
      CategoryCollection categories = scrapCategories(doc);
      String primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-image img", "src");
      String secondaryImages = scrapSecondaryImages(doc);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".product-collateral"));
      List<String> eans = new ArrayList<>();

      eans.add(CrawlerUtils.scrapStringSimpleInfo(doc, "span[itemprop=\"gtin13\"]", false));

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
          .setEans(eans)
          .build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".product-essential") != null;
  }

  private CategoryCollection scrapCategories(Document doc) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = doc.select(".breadcrumbs ul li");

    // This iteraction get only the middle categories (excludes last and first element from breadcrumbs)
    for (int i = 1; i < elementCategories.size() - 1; i++) {
      categories.add(elementCategories.get(i).text().replace("|", "").trim());
    }

    return categories;
  }

  private String scrapSecondaryImages(Document doc) {
    JSONArray secondaryImagesArray = new JSONArray();
    Elements secondaryElements = doc.select(".product-image-thumbs .swiper-wrapper div");
    String style = null;

    for (Element element : secondaryElements) {
      if (element.hasAttr("style")) {

        style = element.attr("style");
        secondaryImagesArray.put(getUrlFromStyle(style));
      }
    }

    return secondaryImagesArray.toString();
  }

  /*
   * Example style:
   * "background-image: url('https://i2marabraz-a.akamaihd.net/65x65/59/00280341542__2_B_ND.jpg');background-repeat:no-repeat; background-position: 0;"
   */

  private String getUrlFromStyle(String style) {
    String url = null;

    if (style.contains("url")) {
      url = style.substring(style.indexOf("url('"), style.indexOf("')"));
    }

    return url;
  }

  private boolean scrapAvailability(Document doc) {
    Element buttonElement = doc.selectFirst(".add-to-cart-buttons button");
    boolean availability = false;

    if (buttonElement != null) {
      availability = buttonElement.hasAttr("title") ? buttonElement.attr("title").equalsIgnoreCase("comprar") : false;
    }

    return availability;
  }

  private JSONObject getJsonPrices(String internalPid) {
    String url = "https://www.marabraz.com.br/ajax/product/prices?product_ids[]=" + internalPid + "&";

    Request request = RequestBuilder.create().setCookies(cookies).setUrl(url).build();
    Response response = dataFetcher.get(session, request);

    JSONObject jsonPrices = new JSONObject(response.getBody());
    JSONObject prices = new JSONObject();

    if (jsonPrices.has(internalPid) && !jsonPrices.isNull(internalPid)) {
      prices = jsonPrices.getJSONObject(internalPid);
    }

    return prices;
  }

  private Prices scrapPrices(String internalPid) {
    Prices prices = new Prices();
    Double priceFrom = null;
    Float bankTicketPrice = null;
    Map<Integer, Float> installmentPriceMap = new TreeMap<>();
    Integer installment = null;
    Float installmentePrice = null;
    JSONObject jsonPrices = getJsonPrices(internalPid);

    if (jsonPrices.has("price_from") && !jsonPrices.isNull("price_from")) {
      priceFrom = jsonPrices.getDouble("price_from");
    }

    if (jsonPrices.has("boleto_value") && !jsonPrices.isNull("price_from")) {
      bankTicketPrice = jsonPrices.getFloat("boleto_value");
    }

    if (jsonPrices.has("installments_info") && !jsonPrices.isNull("installments_info")) {
      JSONArray installmentsJsonArray = jsonPrices.getJSONArray("installments_info");

      for (Object object : installmentsJsonArray) {
        JSONObject priceJsonObject = (JSONObject) object;

        if (priceJsonObject.has("count") && !priceJsonObject.isNull("count")) {
          installment = priceJsonObject.getInt("count");
        }

        if (priceJsonObject.has("price") && !priceJsonObject.isNull("price")) {
          installmentePrice = priceJsonObject.getFloat("price");
        }

        installmentPriceMap.put(installment, installmentePrice);
      }

    }

    prices.setPriceFrom(priceFrom);
    prices.setBankTicketPrice(bankTicketPrice);

    prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
    prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
    prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);

    return prices;
  }

}
