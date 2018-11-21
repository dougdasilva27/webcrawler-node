package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.methods.POSTFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

public class ArgentinaVeaCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.veadigital.com.ar/";
  private static final String IMAGE_FIRST_PART = HOME_PAGE + "VeaComprasArchivos/Archivos/ArchivosMxM/";

  public ArgentinaVeaCrawler(Session session) {
    super(session);
  }

  @Override
  public void handleCookiesBeforeFetch() {
    Logging.printLogDebug(logger, session, "Adding cookie...");
    this.cookies = CrawlerUtils.fetchCookiesFromAPage(HOME_PAGE + "Login/PreHome.aspx", Arrays.asList("ASP.NET_SessionId"), "www.veadigital.com.ar",
        "/", session);
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

    JSONObject apiJson = crawlProductApi(doc);

    if (apiJson.length() > 0) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalPid = crawlInternalPid(apiJson);
      String internalId = crawlInternalId(apiJson);
      String name = crawlName(apiJson);
      Float price = crawlPrice(apiJson);
      Integer stock = crawlStock(apiJson);
      Prices prices = crawlPrices(price);
      boolean available = stock != null && stock > 0;
      CategoryCollection categories = new CategoryCollection();
      String primaryImage = crawlPrimaryImage(apiJson);
      String secondaryImages = crawlSecondaryImages();
      String description = crawlDescription(internalId);

      // Creating the product
      Product product = ProductBuilder.create().setUrl(crawlNewUrl(internalId)).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setStock(stock).setMarketplace(new Marketplace()).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;

  }

  private String crawlInternalPid(JSONObject json) {
    String internalPid = null;

    if (json.has("Codigo")) {
      internalPid = json.get("Codigo").toString();
    }

    return internalPid;
  }

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("IdArticulo")) {
      internalId = json.get("IdArticulo").toString();
    }

    return internalId;
  }

  private String crawlName(JSONObject json) {
    String name = null;

    if (json.has("DescripcionArticulo")) {
      name = json.getString("DescripcionArticulo");
    }

    return name;
  }

  private Float crawlPrice(JSONObject json) {
    Float price = null;

    if (json.has("Precio")) {
      String priceText = json.getString("Precio").replaceAll(",", "").trim();

      if (!priceText.isEmpty()) {
        price = Float.parseFloat(priceText);
      }
    }

    return price;
  }

  private Integer crawlStock(JSONObject json) {
    Integer stock = null;

    if (json.has("Stock")) {
      String text = json.get("Stock").toString().replaceAll("[^0-9.]", "");

      if (!text.isEmpty()) {
        stock = ((Double) Double.parseDouble(text)).intValue();
      }
    }

    return stock;
  }

  private String crawlPrimaryImage(JSONObject json) {
    String primaryImage = null;

    if (json.has("IdArchivoBig")) {
      String image = json.getString("IdArchivoBig").trim();

      if (!image.isEmpty()) {
        primaryImage = IMAGE_FIRST_PART + image;
      }
    }

    if ((primaryImage == null || primaryImage.isEmpty()) && json.has("IdArchivoSmall")) {
      String image = json.getString("IdArchivoSmall").trim();

      if (!image.isEmpty()) {
        primaryImage = IMAGE_FIRST_PART + image;
      }
    }

    if (primaryImage != null && primaryImage.isEmpty()) {
      primaryImage = null;
    }

    return primaryImage;
  }

  /**
   * There is no secondary Images in this market.
   * 
   * @param document
   * @return
   */
  private String crawlSecondaryImages() {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private String crawlDescription(String pid) {
    StringBuilder description = new StringBuilder();
    String url = HOME_PAGE + "Comprar/HomeService.aspx/ObtenerDetalleDelArticuloLevex";
    String payload = "{code:'" + pid + "'}";

    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");

    String response = POSTFetcher.fetchPagePOSTWithHeaders(url, session, payload, cookies, 1, headers);

    if (response != null && response.contains("descr")) {
      JSONObject json = CrawlerUtils.stringToJson(response);

      if (json.has("d")) {
        JSONObject jsonD = CrawlerUtils.stringToJson(json.get("d").toString());
        if (jsonD.has("descr")) {
          description.append(jsonD.getString("descr"));
        }
      }
    }

    return description.toString();
  }

  /**
   * There is no bankSlip price.
   * 
   * There is no card payment options, other than cash price. So for installments, we will have only
   * one installment for each card brand, and it will be equals to the price crawled on the sku main
   * page.
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

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.CABAL.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);
    }

    return prices;
  }

  private String crawlNewUrl(String internalId) {
    String url = session.getOriginalURL();

    if (!url.contains("prod/")) {
      url = HOME_PAGE + "prod/" + internalId;
    }

    return url;
  }

  private JSONObject crawlProductApi(Document doc) {
    JSONObject json = new JSONObject();

    Element e = doc.selectFirst("#hfProductData");
    if (e != null) {
      json = CrawlerUtils.stringToJson(e.val());
    } else if (session.getOriginalURL().contains("_query=")) {
      json = crawlProductOldApi(session.getOriginalURL());
    }

    return json;
  }

  /**
   * Crawl api of search when probably has only one product
   * 
   * @param url
   * @return
   */
  private JSONObject crawlProductOldApi(String url) {
    JSONObject json = new JSONObject();

    Map<String, String> headers = new HashMap<>();
    headers.put("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
    headers.put("Content-Type", "application/json; charset=UTF-8");
    headers.put("X-Requested-With", "XMLHttpRequest");

    // Nome do produto na busca
    String[] tokens = url.split("=");

    String urlSearch = HOME_PAGE + "Comprar/HomeService.aspx/ObtenerArticulosPorDescripcionMarcaFamiliaLevex";
    String urlParameters = "{IdMenu:\"\",textoBusqueda:\"" + CommonMethods.removeAccents(tokens[tokens.length - 1]) + "\","
        + " producto:\"\", marca:\"\", pager:\"\", ordenamiento:0, precioDesde:\"\", precioHasta:\"\"}";

    String jsonString = POSTFetcher.fetchPagePOSTWithHeaders(urlSearch, session, urlParameters, cookies, 1, headers);

    if (jsonString != null && jsonString.startsWith("{")) {
      json = new JSONObject(jsonString);
    }


    if (json.has("d")) {
      JSONObject jsonD = CrawlerUtils.stringToJson(json.get("d").toString());
      if (jsonD.has("ResultadosBusquedaLevex")) {
        JSONArray products = jsonD.getJSONArray("ResultadosBusquedaLevex");

        if (products.length() > 0) {
          json = products.getJSONObject(0);
        }
      }
    }

    return json;
  }
}
