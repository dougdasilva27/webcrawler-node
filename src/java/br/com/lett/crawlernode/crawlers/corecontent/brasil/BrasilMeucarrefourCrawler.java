package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchUtilities;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
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
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Date: 01/09/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilMeucarrefourCrawler extends Crawler {

  private static final String HOME_PAGE = "https://api.carrefour.com.br/";

  public BrasilMeucarrefourCrawler(Session session) {
    super(session);
  }

  @Override
  protected JSONObject fetch() {
    JSONObject api = new JSONObject();

    Map<String, String> headers = new HashMap<>();
    headers.put("User-Agent", FetchUtilities.randMobileUserAgent());
    String[] tokens = session.getOriginalURL().split("#");

    String url = "https://api.carrefour.com.br/mobile-food/v1/product/" + tokens[tokens.length - 1];


    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).build();

    String content = this.dataFetcher.get(session, request).getBody();

    if (content == null || content.isEmpty()) {
      content = new ApacheDataFetcher().get(session, request).getBody();
    }

    api = CrawlerUtils.stringToJson(content);

    return api;
  }

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  public List<Product> extractInformation(JSONObject json) throws Exception {
    super.extractInformation(json);
    List<Product> products = new ArrayList<>();

    if (isProductPage(session.getOriginalURL())) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(json);
      String name = crawlName(json);
      Float price = crawlPrice(json);
      Prices prices = crawlPrices(price);
      Integer stock = crawlStock(json);
      boolean available = stock != null && stock > 0;
      CategoryCollection categories = new CategoryCollection();
      String primaryImage = crawlPrimaryImage(json);
      String secondaryImages = crawlSecondaryImages(json);
      String description = crawlDescription(json);
      Marketplace marketplace = crawlMarketplace();

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(null).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setStock(stock).setMarketplace(marketplace).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(String url) {
    return url.contains("#");
  }

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("sku")) {
      internalId = json.getString("sku");
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

  private Integer crawlStock(JSONObject json) {
    Integer stock = null;

    if (json.has("stockLevel")) {
      Object stc = json.get("stockLevel");

      if (stc instanceof Integer) {
        stock = (Integer) stc;
      }
    }

    return stock;
  }

  private Float crawlPrice(JSONObject json) {
    Float price = null;

    if (json.has("price")) {
      price = MathUtils.parseFloatWithComma(json.getString("discountPrice"));
    }

    return price;
  }

  private Marketplace crawlMarketplace() {
    return new Marketplace();
  }


  private String crawlPrimaryImage(JSONObject json) {
    String primaryImage = null;

    if (json.has("gallery")) {
      JSONArray gallery = json.getJSONArray("gallery");

      if (gallery.length() > 0) {
        primaryImage = gallery.getString(0);
      }
    }

    return primaryImage;
  }

  /**
   * @param doc
   * @return
   */
  private String crawlSecondaryImages(JSONObject json) {
    String secondaryImages = null;

    if (json.has("gallery")) {
      JSONArray gallery = json.getJSONArray("gallery");

      if (gallery.length() > 1) {
        gallery.remove(0);
        secondaryImages = gallery.toString();
      }
    }

    return secondaryImages;
  }

  private String crawlDescription(JSONObject json) {
    StringBuilder description = new StringBuilder();

    if (json.has("classifications")) {
      description.append("<div id=\"description\">");
      JSONArray classifications = json.getJSONArray("classifications");

      for (int i = 0; i < classifications.length(); i++) {
        JSONObject obj = classifications.getJSONObject(i);

        if (obj.has("name")) {
          description.append("<div class=\"sub" + i + "\">");
          description.append("<h2>" + obj.getString("name") + "</h2>");

          if (obj.has("features")) {
            JSONArray features = obj.getJSONArray("features");
            description.append("<table>");

            for (int j = 0; j < features.length(); j++) {
              JSONObject objF = features.getJSONObject(j);

              if (objF.has("name")) {
                description.append("<tr>");
                description.append("<td>" + objF.getString("name") + "</td>");

                if (objF.has("values")) {
                  description.append("<td>" + (objF.get("values").toString().replace("[", "").replace("]", "").replace("\"", "").trim()));

                  if (objF.has("unit") && !objF.getString("unit").isEmpty()) {
                    description.append(" " + objF.getString("unit"));
                  }

                  description.append("</td>");
                }

                description.append("</tr>");
              }

            }

            description.append("</table>");
          }

          description.append("</div>");
        }
      }
      description.append("</div>");
    }

    return description.toString();
  }

  /**
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      prices.setBankTicketPrice(price);

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.CABAL.toString(), installmentPriceMap);
    }

    return prices;
  }

  protected String getRequestBody(InputStream t) throws IOException {
    InputStreamReader isr = new InputStreamReader(t, StandardCharsets.UTF_8);
    BufferedReader br = new BufferedReader(isr);

    int b;
    StringBuilder buf = new StringBuilder(512);
    while ((b = br.read()) != -1) {
      buf.append((char) b);
    }

    br.close();
    isr.close();

    return buf.toString();
  }
}
