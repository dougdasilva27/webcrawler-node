package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
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
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 29/01/2018
 * 
 * @author gabriel
 *
 */
public class BrasilLojasmelCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.lojasmel.com/";

  public BrasilLojasmelCrawler(Session session) {
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

    if (isProductPage(session.getOriginalURL())) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      JSONObject jsonSku = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"text/javascript\"]", "dataProduct=[", "];");

      if (jsonSku.has("product") && jsonSku.getJSONArray("product").length() > 0) {
        jsonSku = jsonSku.getJSONArray("product").getJSONObject(0);
      }

      // Pid
      String internalPid = crawlInternalPid(jsonSku);

      // Categories
      CategoryCollection categories = crawlCategories(doc);

      // Description
      String description = crawlDescription(doc);

      // Stock
      Integer stock = null;

      // Primary image
      String primaryImage = crawlPrimaryImage(doc);

      // Secondary images
      String secondaryImages = crawlSecondaryImages(doc);

      // Availability
      boolean available = doc.select(".product-detail .bt-checkout[name=add_cart]").first() != null;

      // InternalId
      String internalId = crawlInternalId(jsonSku);

      // Price
      Float price = crawlMainPagePrice(doc);

      // Name
      String name = crawlName(doc);

      // Prices
      Prices prices = crawlPrices(doc, price);

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setStock(stock).setMarketplace(new Marketplace()).build();

      products.add(product);


    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(String url) {
    if (url.endsWith("/p")) {
      return true;
    }
    return false;
  }

  /*******************
   * General methods *
   *******************/

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("productSku")) {
      internalId = Integer.toString((json.getInt("productSku"))).trim();
    }

    return internalId;
  }


  private String crawlInternalPid(JSONObject json) {
    String internalPid = null;

    if (json.has("id")) {
      internalPid = json.getString("id");
    }

    return internalPid;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select("h1.name").first();

    if (nameElement != null) {
      name = nameElement.text().trim();
    }

    return name;
  }

  private Float crawlMainPagePrice(Document doc) {
    Float price = null;
    Element priceElement = doc.select(".sale_price").first();

    if (priceElement != null) {
      price = MathUtils.parseFloatWithComma(priceElement.ownText());
    }

    return price;
  }

  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;
    Element image = doc.select(".big-image > a").first();

    if (image != null) {
      primaryImage = image.attr("href");

      if (!primaryImage.startsWith("http")) {
        primaryImage = "https:" + primaryImage;
      }
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document doc) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();
    Elements images = doc.select(".thumbs li a");

    for (int i = 1; i < images.size(); i++) {// starts with index 1, because the first image is the primary image
      String urlImage = images.get(i).attr("big_img");

      if (!urlImage.startsWith("http")) {
        urlImage = "https:" + urlImage;
      }

      secondaryImagesArray.put(urlImage);
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }


  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select("ul.center > li:not(.home) > a");

    for (Element e : elementCategories) { // starting from index 1, because the first is the market name
      categories.add(e.text().trim());
    }

    return categories;
  }

  private String crawlDescription(Document document) {
    String description = "";
    Element descriptionElement = document.select(".description-table").first();

    if (descriptionElement != null)
      description = description + descriptionElement.html();

    return description;
  }

  private Prices crawlPrices(Document doc, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Element bank = doc.select(".get_price_boleto strong").first();
      if (bank != null) {
        Float bankTicketPrice = MathUtils.parseFloatWithComma(bank.ownText());

        prices.setBankTicketPrice(bankTicketPrice);
      }

      Map<Integer, Float> installmentPriceMap = new HashMap<>();

      String installmentsUrl = buildInstallmentsUrl(doc, price);

      Request request = RequestBuilder.create().setUrl(installmentsUrl).setCookies(cookies).build();
      String installmentsString = this.dataFetcher.get(session, request).getBody().trim();

      JSONArray installments = new JSONArray(installmentsString);

      for (int i = 0; i < installments.length(); i++) {
        JSONObject json = installments.getJSONObject(i);

        if (json.has("msg")) {
          String msg = json.getString("msg").toLowerCase();

          if (msg.contains("x")) {
            int x = msg.indexOf('x');

            String installmentString = msg.substring(0, x).replaceAll("[^0-9]", "").trim();
            Float value = MathUtils.parseFloatWithComma(msg.substring(x));

            if (!installmentString.isEmpty() && value != null) {
              installmentPriceMap.put(Integer.parseInt(installmentString), value);
            }
          } else {
            Float value = MathUtils.parseFloatWithComma(msg);

            if (value != null) {
              installmentPriceMap.put(1, value);
            }
          }
        }
      }

      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
    }

    return prices;
  }

  private String buildInstallmentsUrl(Document doc, Float price) {
    StringBuilder str = new StringBuilder();
    str.append("https://www.lojasmel.com");

    Elements scripts = doc.select("script[type=\"text/javascript\"]");
    String token = "product_url='";
    for (Element e : scripts) {
      String script = e.outerHtml().replace(" ", "");

      if (script.contains(token)) {
        int x = script.indexOf(token) + token.length();
        int y = script.indexOf('\'', x);

        str.append(script.substring(x, y));

        break;
      }
    }

    str.append("/installments/?price=" + price);

    return str.toString();
  }
}
