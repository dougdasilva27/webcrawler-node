package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
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
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

public class SaopauloPolipetCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.polipet.com.br/";
  private static final String URL_API = "https://www.polipet.com.br/ajaxpro/IKCLojaMaster.detalhes,IKCLojaMaster%202.2.ashx";
  private static final String VARIATIONS_AJAX_METHOD = "CarregaSKU";
  private static final String SKU_AJAX_METHOD = "DisponibilidadeSKU";
  private static final String VARIATION_NAME_PAYLOAD = "ColorCode";


  public SaopauloPolipetCrawler(Session session) {
    super(session);
    super.config.setFetcher(FetchMode.FETCHER);
  }

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  private String ajaxToken = null;

  @Override
  public void handleCookiesBeforeFetch() {
    Logging.printLogDebug(logger, session, "Adding cookie...");

    Request request = RequestBuilder.create()
        .setCookies(cookies)
        .setUrl(HOME_PAGE)
        .build();
    Response response = dataFetcher.get(session, request);

    List<Cookie> cookiesResponse = response.getCookies();
    for (Cookie cookieResponse : cookiesResponse) {
      BasicClientCookie cookie = new BasicClientCookie(cookieResponse.getName(), cookieResponse.getValue());
      cookie.setDomain("www.polipet.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);
    }

    this.ajaxToken = crawlToken(Jsoup.parse(response.getBody()));
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalPid = crawlInternalPid(doc);
      String name = this.crawlName(doc);
      Integer stock = null;
      Marketplace marketplace = new Marketplace();
      CategoryCollection categories = crawlCategories(doc);
      String description = this.crawlDescription(doc);
      String primaryImage = this.crawlPrimaryImage(doc);
      String secondaryImages = this.crawlSecondaryImages(doc, primaryImage);

      // Prices
      // It wasn't observed any price difference between variations
      // the only type of variations found was in product colors

      // Variations
      Map<String, String> skusMap = this.crawlSkuOptions(internalPid, this.session.getOriginalURL());

      if (skusMap.size() > 1) {

        Logging.printLogDebug(logger, session, "Crawling information of more than one product...");

        /*
         * Multiple variations
         */
        for (Entry<String, String> entry : skusMap.entrySet()) {

          String idVariation = entry.getKey();
          JSONObject skuInformation = crawlSkuInformations(idVariation, internalPid, this.session.getOriginalURL());
          String variationName = entry.getValue();
          String nameVariation = crawlNameVariation(variationName, name);
          String internalIdVariation = idVariation != null ? internalPid + "-" + idVariation : internalPid;
          String primaryImageVariation = crawlPrimaryImageVariation(skuInformation);
          String secondaryImagesVariation = crawlSecondaryImagesVariation(skuInformation);

          if (primaryImageVariation == null) {
            primaryImageVariation = primaryImage;
          }

          if (secondaryImagesVariation == null) {
            secondaryImagesVariation = secondaryImages;
          }

          boolean availableVariation = crawlAvailabilityVariation(skuInformation);
          Float priceVariation = crawlPriceVariation(skuInformation, availableVariation);
          Prices prices = crawlPrices(doc, priceVariation);

          // Creating the product
          Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalIdVariation).setInternalPid(internalPid)
              .setName(nameVariation).setPrice(priceVariation).setPrices(prices).setAvailable(availableVariation)
              .setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2))
              .setPrimaryImage(primaryImageVariation).setSecondaryImages(secondaryImagesVariation).setDescription(description).setStock(stock)
              .setMarketplace(marketplace).build();

          products.add(product);
        }

      }

      /*
       * Single product
       */
      else {
        Float price = this.crawlPrice(doc);
        Prices prices = crawlPrices(doc, price);
        boolean available = this.crawlAvailability(doc);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalPid).setInternalPid(internalPid)
            .setName(name).setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages).setDescription(description).setStock(stock).setMarketplace(marketplace).build();

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

  private boolean isProductPage(Document doc) {
    return doc.select("#info-product").first() != null;
  }

  private Float crawlPriceVariation(JSONObject jsonSku, boolean availbale) {
    Float price = null;

    if (jsonSku.has("price") && availbale) {
      price = Float.parseFloat(jsonSku.getString("price"));
    }

    return price;
  }

  private boolean crawlAvailabilityVariation(JSONObject skuInformation) {
    if (skuInformation.has("available"))
      return skuInformation.getBoolean("available");

    return false;
  }


  private String crawlPrimaryImageVariation(JSONObject json) {
    String primaryImage = null;

    if (json.has("primaryImage")) {
      primaryImage = json.getString("primaryImage");
    }

    return primaryImage;
  }

  private String crawlNameVariation(String variationName, String name) {
    String nameVariation = null;

    nameVariation = name + " " + variationName.trim();

    return nameVariation;
  }

  private String crawlSecondaryImagesVariation(JSONObject json) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    if (json.has("secondaryImages")) {
      secondaryImagesArray = json.getJSONArray("secondaryImages");
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private String crawlToken(Document doc) {
    String token = "";

    Elements scripts = doc.select("script[type=text/javascript]");

    for (Element e : scripts) {
      String script = e.outerHtml().replace(" ", "");

      if (script.contains("AjaxPro.token")) {

        if (script.contains("token=")) {
          int x = script.indexOf("token=") + 6;
          int y = script.indexOf(';', x);

          token = script.substring(x, y).replace("\"", "").trim();
        }
        break;
      }
    }

    return token;
  }

  /**
   * fetch json from api
   * 
   * @param idColor
   * @param internalIdMainPage
   * @param urlProduct
   * @return
   */
  private JSONObject fetchJSONFromApi(String urlProduct, String payload, String method) {
    Map<String, String> headers = new HashMap<>();

    headers.put("Referer", urlProduct);
    headers.put("X-AjaxPro-Method", method);
    headers.put("X-AjaxPro-Token", this.ajaxToken);

    Request request = RequestBuilder.create()
        .setUrl(URL_API)
        .setCookies(cookies)
        .setHeaders(headers)
        .setPayload(payload)
        .build();
    String response = this.dataFetcher.post(session, request).getBody();
    return CrawlerUtils.stringToJson(response);
  }

  /**
   * Get all variations of sku from json.
   * 
   * Essa api contém as variações(caso tenha) com o nome e o internalId do produto Nesse json contém
   * um html com essas informações Os ids dos produtos também servem para acessar outra api com
   * informações específicas de cada sku
   * 
   * @param internalIdMainPage
   * @param urlProduct
   * @return
   */
  private Map<String, String> crawlSkuOptions(String internalIdMainPage, String urlProduct) {
    Map<String, String> skusMap = new HashMap<>();
    String payload =
        "{\"ProdutoCodigo\": \"" + internalIdMainPage + "\", \"" + VARIATION_NAME_PAYLOAD + "\": \"0\", \"isRequiredCustomization\": false}";

    JSONObject colorsJson = fetchJSONFromApi(urlProduct, payload, VARIATIONS_AJAX_METHOD);

    if (colorsJson.has("value")) {
      String htmlColors = (colorsJson.getJSONArray("value").getString(0)).replaceAll("\t", "");

      Document doc = Jsoup.parse(htmlColors);

      System.err.println(doc);

      Elements colorsSkus = doc.select("li option");
      for (Element e : colorsSkus) {
        skusMap.put(e.val(), e.ownText());
      }
    }

    return skusMap;
  }

  /**
   * Get informations of sku from json
   * 
   * Nessa api conseguimos pegar as imagens e a disponibilidade de cada sku.
   * 
   * @param idVariation
   * @param internalIdMainPage
   * @param urlProduct
   * @return
   */
  private JSONObject crawlSkuInformations(String idVariation, String internalIdMainPage, String urlProduct) {
    JSONObject returnJson = new JSONObject();

    String payload = "{\"ProdutoCodigo\": \"" + internalIdMainPage + "\", \"CarValorCodigo1\": \"" + idVariation + "\", "
        + "\"CarValorCodigo2\": \"0\", \"CarValorCodigo3\": \"0\", "
        + "\"CarValorCodigo4\": \"0\", \"CarValorCodigo5\": \"0\", \"isRequiredCustomization\": false}";

    JSONObject jsonSku = fetchJSONFromApi(urlProduct, payload, SKU_AJAX_METHOD);

    if (jsonSku.has("value")) {
      JSONArray valueArray = jsonSku.getJSONArray("value");

      Integer numberProduct = 0;

      for (int i = 0; i < valueArray.length(); i++) {
        if (valueArray.get(i) instanceof Integer) {
          numberProduct = valueArray.getInt(i);
          break;
        }
      }

      String price = getPriceFromJSON(valueArray);
      boolean available = numberProduct != 0;

      Map<String, String> imagesMap = new HashMap<>();
      JSONArray imagesArray = valueArray.getJSONArray(1);

      for (int i = 0; i < imagesArray.length(); i++) {
        String temp = imagesArray.getString(i).toLowerCase();

        if (i < imagesArray.length() - 1) {
          imagesMap.put(imagesArray.getString(i + 1), temp);
        }
      }

      String primaryImage = null;

      if (imagesMap.containsKey("ImagemAmpliadaFoto")) {
        primaryImage = addDomainToImage(imagesMap.get("ImagemAmpliadaFoto"));
      } else if (imagesMap.containsKey("ProdutoImagem")) {
        primaryImage = addDomainToImage(imagesMap.get("ProdutoImagem"));
      }

      JSONArray secondaryImagesArray = new JSONArray();

      for (Entry<String, String> entry : imagesMap.entrySet()) {
        if (entry.getKey().startsWith("[name=liImgDetalhe") && !entry.getValue().isEmpty()) {
          secondaryImagesArray.put(addDomainToImage(imagesMap.get(entry.getKey()).replace("Detalhes", "Ampliada")));
        }
      }

      returnJson.put("available", available);

      if (price != null) {
        returnJson.put("price", price);
      }

      if (primaryImage != null) {
        returnJson.put("primaryImage", primaryImage);
      }

      if (secondaryImagesArray.length() > 0) {
        returnJson.put("secondaryImages", secondaryImagesArray);
      }

    }

    return returnJson;
  }

  /**
   * get price from json picked in api
   * 
   * @param skuArray
   * @return
   */
  private String getPriceFromJSON(JSONArray skuArray) {
    String price = null;

    JSONArray priceArray = skuArray.getJSONArray(0);

    if (!priceArray.getString(0).contains("Indisponível")) {

      for (int i = 0; i < priceArray.length(); i++) {
        String temp = priceArray.getString(i);

        if (temp.startsWith("<em>por")) {
          price = temp.replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim();
          break;
        }
      }
    }

    return price;
  }


  /*******************
   * General methods *
   *******************/

  private Float crawlPrice(Document doc) {
    Float price = null;
    Element priceElement = doc.select("#lblPrecos #lblPrecoPor strong").first();

    if (priceElement != null) {
      price = Float.parseFloat(priceElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
    }

    return price;
  }

  /**
   * The prices are the same across all variations and card brands. There is no bank slip payment
   * option in this ecommerce (sem boleto bancário).
   * 
   * @param doc
   * @param unnavailableForAll
   * @return
   */
  private Prices crawlPrices(Document doc, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Element boleto = doc.select("#lblPrecoAVista").first();
      if (boleto != null) {
        prices.setBankTicketPrice(MathUtils.parseFloatWithComma(boleto.text()));
      }

      // installments
      Map<Integer, Float> installments = new TreeMap<>();

      // 1x
      Element firstPaymentElement = doc.select("#infoPrices .price sale price-to strong").first();
      if (firstPaymentElement != null) { // 1x
        Float firstInstallmentPrice = MathUtils.parseFloatWithComma(firstPaymentElement.text());
        installments.put(1, firstInstallmentPrice);
      }

      // max installment number without intereset (maior número de parcelas sem taxa de juros)
      Element maxInstallmentNumberWithoutInterestElement = doc.select("#lblParcelamento1 strong").first();
      Element maxInstallmentPriceWithoutInterestElement = doc.select("#lblParcelamento2 strong").first();
      if (maxInstallmentNumberWithoutInterestElement != null && maxInstallmentPriceWithoutInterestElement != null) {
        List<String> parsedNumbers = MathUtils.parseNumbers(maxInstallmentNumberWithoutInterestElement.text());

        if (!parsedNumbers.isEmpty()) {
          Integer installmentNumber = Integer.parseInt(parsedNumbers.get(0));
          Float installmentPrice = MathUtils.parseFloatWithComma(maxInstallmentPriceWithoutInterestElement.text());

          installments.put(installmentNumber, installmentPrice);
        }
      }

      // max installment number with intereset (maior número de parcelas com taxa de juros)
      Element maxInstallmentNumberWithInterestElement = doc.select("#lblOutroParc strong").first();
      Element maxInstallmentPriceWithInterestElement = doc.select("#lblOutroParc strong").last();
      if (maxInstallmentNumberWithInterestElement != null && maxInstallmentPriceWithInterestElement != null) {
        List<String> parsedNumbers = MathUtils.parseNumbers(maxInstallmentNumberWithInterestElement.text());

        if (!parsedNumbers.isEmpty()) {
          Integer installmentNumber = Integer.parseInt(parsedNumbers.get(0));
          Float installmentPrice = MathUtils.parseFloatWithComma(maxInstallmentPriceWithInterestElement.text());

          installments.put(installmentNumber, installmentPrice);
        }
      }

      if (!installments.containsKey(1)) {
        installments.put(1, price);
      }

      if (installments.size() > 0) {
        prices.insertCardInstallment(Card.VISA.toString(), installments);
        prices.insertCardInstallment(Card.MASTERCARD.toString(), installments);
        prices.insertCardInstallment(Card.DINERS.toString(), installments);
        prices.insertCardInstallment(Card.AMEX.toString(), installments);
        prices.insertCardInstallment(Card.ELO.toString(), installments);
      }
    }

    return prices;
  }

  private String crawlInternalPid(Document doc) {
    String internalID = null;
    Element internalIdElement = doc.select("#ProdutoCodigo").first();

    if (internalIdElement != null) {
      internalID = internalIdElement.attr("value").trim();
    }

    return internalID;
  }

  private boolean crawlAvailability(Document doc) {
    return doc.select("#lblPrecos #lblPrecoPor strong").first() != null;
  }

  private String crawlPrimaryImage(Document document) {
    String primaryImage = null;
    Element primaryImageElements = document.select("#big_photo_container a").first();

    if (primaryImageElements != null) {
      primaryImage = addDomainToImage(primaryImageElements.attr("href"));
    } else {
      primaryImageElements = document.select("#big_photo_container img").first();
      if (primaryImageElements != null) {
        primaryImage = addDomainToImage(primaryImageElements.attr("src"));
      }
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document document, String primaryImage) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements elementFotoSecundaria = document.select("ul.thumbs li a");

    if (elementFotoSecundaria.size() > 1) {
      for (int i = 0; i < elementFotoSecundaria.size(); i++) {
        Element e = elementFotoSecundaria.get(i);
        String secondaryImagesTemp = addDomainToImage(e.attr("href"));

        if (!primaryImage.equals(secondaryImagesTemp) && !secondaryImagesTemp.equals(HOME_PAGE + "#")) { // identify if the image is the primary image
          secondaryImagesArray.put(secondaryImagesTemp);
        } else {
          Element x = e.select("img").first();

          if (x != null && !x.attr("src").isEmpty() && !x.attr("src").contains("gif")) {
            secondaryImagesArray.put(addDomainToImage(x.attr("src").replaceAll("Detalhes", "Ampliada")));
          }
        }

      }
    }


    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private String addDomainToImage(String image) {
    String imageWithDomain = image;

    if (!image.startsWith("http")) {
      imageWithDomain = (HOME_PAGE + image).replace(".br//", ".br/");
    }

    return imageWithDomain;
  }

  private String crawlName(Document doc) {
    String name = null;
    Element elementName = doc.select("#productName").first();
    if (elementName != null) {
      name = elementName.text().replace("'", "").replace("’", "").trim();
    }
    return name;
  }

  private CategoryCollection crawlCategories(Document document) {
    Elements elementCategories = document.select("#breadcrumbs span a span");
    CategoryCollection categories = new CategoryCollection();

    for (int i = 1; i < elementCategories.size(); i++) { // starts with index 1 because the first category is home page
      Element e = elementCategories.get(i);
      categories.add(e.text().trim());
    }

    return categories;
  }

  private String crawlDescription(Document document) {
    StringBuilder description = new StringBuilder();

    Element shortDescription = document.select("#lblDescricaoPreview").first();
    if (shortDescription != null) {
      description.append(shortDescription.html());
    }

    Element principalDescription = document.select("#description").first();
    if (principalDescription != null) {
      description.append(principalDescription.html());
    }

    Element elementProductDetails = document.select("#panCaracteristica").first();
    if (elementProductDetails != null) {
      description.append(elementProductDetails.html());
    }

    return description.toString();
  }
}
