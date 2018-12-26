package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.GETFetcher;
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
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.Seller;
import models.Util;
import models.prices.Prices;

/**
 * 02/02/2018
 * 
 * @author gabriel
 *
 */
public class BrasilCarrefourCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.carrefour.com.br/";
  private static final String SELLER_NAME_LOWER = "carrefour";

  public BrasilCarrefourCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  public void handleCookiesBeforeFetch() {
    this.cookies = CrawlerUtils.fetchCookiesFromAPage(HOME_PAGE, null, "www.carrefour.com.br", "/", session);
  }

  @Override
  protected Object fetch() {
    return Jsoup.parse(fetchPage(session.getOriginalURL()));
  }

  private String fetchPage(String url) {
    String response = "";

    Map<String, String> headers = new HashMap<>();
    headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
    headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");
    headers.put("upgrade-insecure-requests", "1");

    String resp = POSTFetcher.requestStringUsingFetcher(url, cookies, headers, null, DataFetcher.GET_REQUEST, session, false);
    if (!resp.isEmpty()) {
      response = resp;
    } else {
      response = GETFetcher.fetchPageGETWithHeaders(session, url, cookies, headers, 1);
    }

    return response;
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());


      String internalPid = crawlInternalPid(session.getOriginalURL());
      String name = crawlName(doc);
      CategoryCollection categories = crawlCategories(doc);
      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = crawlSecondaryImages(doc);
      String description = crawlDescription(doc);
      Integer stock = null;
      String internalId = crawlInternalId(doc);
      Elements marketplacesElements = doc.select(".list-group-item");
      Map<String, Prices> marketplaceMap;

      Double priceFrom = crawlPriceFrom(doc);

      if (marketplacesElements.isEmpty()) {
        marketplaceMap = crawlMarketplaceForSingleSeller(doc, internalId);
      } else {
        marketplaceMap = crawlMarketplaceForMutipleSellers(marketplacesElements);
      }

      Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap, internalId, marketplaceMap.size() > 1 ? null : priceFrom);

      boolean available = marketplaceMap.containsKey(SELLER_NAME_LOWER);
      Prices prices = available ? marketplaceMap.get(SELLER_NAME_LOWER) : new Prices();
      Float price = available ? crawlPrice(prices) : null;

      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setStock(stock).setMarketplace(marketplace).build();

      products.add(product);

    } else

    {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }


  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(Document doc) {
    return doc.select(".product-details-panel").first() != null;
  }



  /*******************
   * General methods *
   *******************/

  private String crawlInternalId(Document document) {
    String internalId = null;
    Element internalIdElement = document.select("#productCod").first();

    if (internalIdElement != null) {
      internalId = internalIdElement.val().trim();
    }

    return internalId;
  }

  private String crawlInternalPid(String url) {
    String internalPid = null;

    if (url.contains("?")) {
      url = url.split("\\?")[0];
    }

    if (url.contains("/p/")) {
      String[] tokens = url.split("p/");

      if (tokens.length > 1 && tokens[1].contains("/")) {
        internalPid = tokens[1].split("/")[0];
      } else if (tokens.length > 1) {
        internalPid = tokens[1];
      }
    }
    return internalPid;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select("h1[itemprop=name]").first();

    if (nameElement != null) {
      name = nameElement.text().trim();
    }

    return name;
  }

  private Float crawlMainPagePrice(Document document) {
    Float price = null;
    Element specialPrice = document.select(".prince-product-default").first();

    if (specialPrice != null) {
      price = Float.parseFloat(specialPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
    }

    return price;
  }

  private Map<String, Prices> crawlMarketplaceForSingleSeller(Document document, String internalId) {
    Map<String, Prices> marketplaces = new HashMap<>();

    Element oneMarketplaceInfo = document.select(".block-add-cart #moreInformation" + internalId).first();
    Element oneMarketplace = document.select(".block-add-cart > span").first();
    Element notifyMeElement = document.select(".text-not-product-avisme").first();

    if (notifyMeElement == null) {
      Float price = crawlMainPagePrice(document);
      Prices prices = crawlPrices(price, document, internalId);

      if (oneMarketplaceInfo != null && oneMarketplace != null) {
        String text = oneMarketplace.ownText().trim().toLowerCase();

        if (text.contains("por") && text.contains(".")) {
          int x = text.indexOf("por") + 3;
          int y = text.lastIndexOf('.');

          String sellerName = text.substring(x, y).trim();

          marketplaces.put(sellerName, prices);
        }
      } else {
        marketplaces.put(SELLER_NAME_LOWER, prices);
      }
    }

    return marketplaces;
  }

  private Map<String, Prices> crawlMarketplaceForMutipleSellers(Elements marketplacesElements) {
    Map<String, Prices> marketplaces = new HashMap<>();

    for (Element e : marketplacesElements) {
      String idOffer = e.attr("data-id-offers");
      Element name = e.select(".font-mirakl-vendor-name strong").first();
      Element price = e.select("span.big-price").first();

      if (name != null && price != null) {
        String sellerName = name.ownText().trim().toLowerCase();
        Float sellerPrice = MathUtils.parseFloatWithComma(price.ownText());

        if (sellerPrice != null && !sellerName.isEmpty()) {
          marketplaces.put(sellerName, crawlPrices(sellerPrice, e, idOffer));
        }
      }
    }

    return marketplaces;
  }

  private Float crawlPrice(Prices prices) {
    Float price = null;

    if (prices != null && !prices.isEmpty() && prices.getCardPaymentOptions(Card.VISA.toString()).containsKey(1)) {
      Double priceDouble = prices.getCardPaymentOptions(Card.VISA.toString()).get(1);
      price = priceDouble.floatValue();
    }

    return price;
  }

  private Marketplace assembleMarketplaceFromMap(Map<String, Prices> marketplaceMap, String internalId, Double priceFrom) {
    Marketplace marketplace = new Marketplace();

    for (String sellerName : marketplaceMap.keySet()) {
      if (!sellerName.equalsIgnoreCase(SELLER_NAME_LOWER) && !sellerName.equalsIgnoreCase(SELLER_NAME_LOWER)) {
        JSONObject sellerJSON = new JSONObject();
        sellerJSON.put("name", sellerName);

        Prices prices = marketplaceMap.get(sellerName);

        sellerJSON.put("price", crawlPrice(prices));
        sellerJSON.put("prices", prices.toJSON());

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

  private Double crawlPriceFrom(Element e) {
    Double price = null;

    Element priceFrom = e.select(".price-old").first();
    if (priceFrom != null) {
      price = MathUtils.parseDoubleWithComma(priceFrom.text());
    }

    return price;
  }

  private String crawlPrimaryImage(Document document) {
    String primaryImage = null;
    Element primaryImageElement = document.select(".sust-gallery div.item .thumb img").first();

    if (primaryImageElement != null) {
      String image = primaryImageElement.attr("data-zoom-image");
      if (image != null) {
        primaryImage = image.trim();
      }

    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document document) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements imagesElement = document.select(".sust-gallery div.item .thumb img");

    for (int i = 1; i < imagesElement.size(); i++) { // start with index 1 because the first image
                                                     // is the primary image
      Element e = imagesElement.get(i);

      if (e.attr("data-zoom-image") != null && !e.attr("data-zoom-image").isEmpty()) {
        secondaryImagesArray.put(e.attr("data-zoom-image").trim());
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
  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select(".breadcrumb > li > a");

    for (int i = 1; i < elementCategories.size() - 1; i++) { // starting from index 1, because the
      categories.add(elementCategories.get(i).text().trim());
    }

    return categories;
  }

  private String crawlDescription(Document document) {
    StringBuilder description = new StringBuilder();

    Element desc2 = document.select(".productDetailsPageShortDescription").first();
    if (desc2 != null) {
      description.append(desc2.outerHtml());
    }

    Elements descriptionElements = document.select("#accordionFichaTecnica");
    if (descriptionElements != null) {
      description.append(descriptionElements.html());
    }

    Element desc = document.select(".productDetailsPageDescription").first();
    if (desc != null) {
      description.append(desc.outerHtml());
    }

    return description.toString();
  }

  private Prices crawlPrices(Float price, Element e, String internalId) {
    Prices prices = new Prices();

    if (price != null) {
      String url = "https://www.carrefour.com.br/installment/creditCard?productPrice=" + price + "&productCode=" + internalId;
      String json = fetchPage(url);

      prices.setBankTicketPrice(price);
      prices.setPriceFrom(crawlPriceFrom(e));

      JSONObject jsonPrices = new JSONObject();

      try {
        jsonPrices = new JSONObject(json);
      } catch (Exception ex) {
        Logging.printLogError(logger, session, CommonMethods.getStackTrace(ex));
      }

      if (jsonPrices.has("maestroInstallments")) {
        Map<Integer, Float> installmentPriceMap = crawlInstallment(jsonPrices, "maestroInstallments");
        prices.insertCardInstallment(Card.MAESTRO.toString(), installmentPriceMap);
      }

      if (jsonPrices.has("carrefourInstallments")) {
        Map<Integer, Float> installmentPriceMap = crawlInstallment(jsonPrices, "carrefourInstallments");

        prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);
      }

      if (jsonPrices.has("mastercardInstallments")) {
        Map<Integer, Float> installmentPriceMap = crawlInstallment(jsonPrices, "mastercardInstallments");
        prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      }

      if (jsonPrices.has("dinersInstallments")) {
        Map<Integer, Float> installmentPriceMap = crawlInstallment(jsonPrices, "dinersInstallments");
        prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      }

      if (jsonPrices.has("visaInstallments")) {
        Map<Integer, Float> installmentPriceMap = crawlInstallment(jsonPrices, "visaInstallments");
        prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      } else {
        Map<Integer, Float> installmentPriceMap = new HashMap<>();
        installmentPriceMap.put(1, price);
        prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      }

      if (jsonPrices.has("hipercardInstallments")) {
        Map<Integer, Float> installmentPriceMap = crawlInstallment(jsonPrices, "hipercardInstallments");
        prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
      }

      if (jsonPrices.has("amexInstallments")) {
        Map<Integer, Float> installmentPriceMap = crawlInstallment(jsonPrices, "amexInstallments");
        prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      }

      if (jsonPrices.has("eloInstallments")) {
        Map<Integer, Float> installmentPriceMap = crawlInstallment(jsonPrices, "eloInstallments");
        prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      }
    }

    return prices;
  }

  private Map<Integer, Float> crawlInstallment(JSONObject jsonPrices, String keyCard) {
    Map<Integer, Float> installmentPriceMap = new HashMap<>();
    JSONArray installments = jsonPrices.getJSONArray(keyCard);

    for (int i = 0; i < installments.length(); i++) {
      JSONObject jsonInstallment = installments.getJSONObject(i);

      if (jsonInstallment.has("index")) {
        Integer installment = jsonInstallment.getInt("index");

        if (jsonInstallment.has("value")) {
          Float value =
              Float.parseFloat(jsonInstallment.getString("value").replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());

          installmentPriceMap.put(installment, value);
        }
      }
    }

    return installmentPriceMap;
  }
}
