package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.remote.JsonException;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Util;
import models.prices.Prices;

public class ArgentinaCarrefourCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.carrefour.com.ar/";

  public ArgentinaCarrefourCrawler(Session session) {
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

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(doc);
      String name = crawlName(doc);
      Float price = crawlPrice(doc);
      Prices prices = crawlPrices(doc);
      boolean available = crawlAvailability(doc);
      CategoryCollection categories = crawlCategories(doc);
      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = crawlSecondaryImages(doc);
      String description = crawlDescription(doc);
      Integer stock = null;

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setName(name).setPrice(price)
          .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setStock(stock).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();
    Elements descriptionElements = doc.select("div.product-collateral #collateral-tabs dd.tab-container");
    for (Element e : descriptionElements) {
      description.append(e.text().trim());
    }
    return description.toString();
  }

  private String crawlSecondaryImages(Document doc) {
    Elements elements = doc.select("div.product-image-gallery .gallery-image[data-zoom-image]");
    JSONArray images = new JSONArray();
    if (elements.size() > 1) {
      for (int i = 1; i < elements.size(); i++) {
        String imageUrl = elements.get(i).attr("data-zoom-image");
        images.put(imageUrl);
      }
    }
    return images.toString();
  }

  private String crawlPrimaryImage(Document doc) {
    Elements elements = doc.select("div.product-image-gallery .gallery-image[data-zoom-image]");
    if (!elements.isEmpty()) {
      return elements.get(0).attr("data-zoom-image");
    }
    return null;
  }

  private CategoryCollection crawlCategories(Document doc) {
    return new CategoryCollection();
  }

  private boolean crawlAvailability(Document doc) {
    if (doc.select("div.add-to-cart-buttons p.out-of-stock").first() != null) {
      return false;
    }
    return true;
  }

  /**
   * Possui várias bandeiras e para cada bandeira vários bancos. Apenas o cartão da loja está sendo
   * capturado.
   * 
   * @param price
   * @param doc
   * @return
   */
  private Prices crawlPrices(Document doc) {
    Prices prices = new Prices();
    Pair<String, String> idTarjeta = getIdTarjetaCarrefourId(doc);
    JSONObject dataBanking = parseDataBanking(doc);
    if (idTarjeta != null) {
      if (dataBanking.has(idTarjeta.getLeft())) {
        JSONObject tarjeta = dataBanking.getJSONObject(idTarjeta.getLeft());
        if (tarjeta.has(idTarjeta.getRight())) {
          JSONObject tarjetaCarrefour = tarjeta.getJSONObject(idTarjeta.getRight());
          if (tarjetaCarrefour.has("0")) {
            JSONObject installmentsJson = tarjetaCarrefour.getJSONObject("0");
            Set<String> keys = installmentsJson.keySet();
            Map<Integer, Float> installmentPriceMap = new TreeMap<>();
            for (String key : keys) {
              JSONObject installmentJson = installmentsJson.getJSONObject(key);
              if (installmentJson.has("name")) {
                Pattern p = Pattern.compile("(\\$[\\d\\.]+\\,\\d+)");
                Matcher m = p.matcher(installmentJson.getString("name"));
                if (m.find()) {
                  try {
                    Float installmentPrice =
                        Float.parseFloat(m.group(1).replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());
                    installmentPriceMap.put(Integer.parseInt(key), installmentPrice);
                  } catch (NumberFormatException ex) {
                    Logging.printLogWarn(logger, session, "Error parsing installment value from dataBanking. [" + ex.getMessage() + "]");
                  }
                }
              }
            }
            prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);
          }
        }
      }
    }
    return prices;
  }

  /**
   * Cada badneira de cartão possuía um id, o qual deve ser usado para pegar as informações das
   * parcelas no json parseado na função crawrlPrices.
   * 
   * @return
   */
  private Pair<String, String> getIdTarjetaCarrefourId(Document doc) {
    Elements elements = doc.select("#lc_pmnt_cc_id option");
    for (Element e : elements) {
      if (e.text().toLowerCase().contains("carrefour")) {
        return Pair.of(e.attr("value"), e.text());
      }
    }
    return null;
  }

  private Float crawlPrice(Document doc) {
    Float price = null;
    Element priceElement = doc.select("div.price-info div.price-box .special-price span.price").first();
    if (priceElement != null) {
      try {
        price = Float.parseFloat(priceElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());
      } catch (NumberFormatException numberFormatException) {
        Logging.printLogError(logger, session, "Error parsing price String to float.");
        Logging.printLogError(logger, session, CommonMethods.getStackTrace(numberFormatException));
      }
    } else {
      priceElement = doc.select("div.price-info span.regular-price span[itemprop=price]").first();
      if (priceElement != null) {
        String priceString = priceElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim();
        try {
          price = Float.parseFloat(priceString);
        } catch (NumberFormatException numberFormatException) {
          Logging.printLogError(logger, session, "Error parsing price String to float.");
          Logging.printLogError(logger, session, CommonMethods.getStackTrace(numberFormatException));
        }
      }
    }
    return price;
  }

  private String crawlName(Document doc) {
    if (doc.select("div.product-name h1").first() != null) {
      return doc.select("div.product-name h1").first().text().trim();
    }
    return null;
  }

  private boolean isProductPage(Document doc) {
    return doc.select(".product-view").first() != null;
  }

  private String crawlInternalId(Document doc) {
    JSONObject dataLayer = parseDataLayer(doc);
    if (dataLayer.has("productos")) {
      JSONArray products = dataLayer.getJSONArray("productos");
      if (products.length() > 0) {
        JSONObject product = products.getJSONObject(0);
        if (product.has("id")) {
          return product.getString("id");
        }
      }
    }
    return null;
  }

  /*
   * dataLayer.push({ "productos": [ { "name":
   * "Lavarropas Automático Samsung 8 KG WA80F5S4UTW Blanco", "id": "6468", "price": "15999,00",
   * "brand": "Samsung", "category": "Lavado y Secado" } ], "event": "productdetailview" });
   */
  private JSONObject parseDataLayer(Document document) {
    try {
      return CrawlerUtils.selectJsonFromHtml(document, "script", "dataLayer.push(", ");");
    } catch (Exception e) {
      Logging.printLogWarn(logger, session, Util.getStackTraceString(e));
      return new JSONObject();
    }
  }

  private JSONObject parseDataBanking(Document document) {
    Elements scripts = document.select("script");
    JSONObject dataBanking = new JSONObject();

    for (Element e : scripts) {
      String dataLayer = e.outerHtml().trim();

      if (dataLayer.contains("var dataBankingJson = {")) {
        int x = dataLayer.indexOf("= {") + 2;
        int y = dataLayer.indexOf("};", x);
        try {
          dataBanking = new JSONObject(dataLayer.substring(x, y + 1));
        } catch (JsonException jsonException) {
          Logging.printLogError(logger, session, "Error parsing dataBanking json object [" + jsonException.getMessage() + "]");
        }
      }
    }

    return dataBanking;
  }

}
