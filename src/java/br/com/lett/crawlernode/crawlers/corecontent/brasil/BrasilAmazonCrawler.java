package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.aws.s3.S3Service;
import br.com.lett.crawlernode.core.fetcher.DataFetcherNO;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.POSTFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.exceptions.ResponseCodeException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.Seller;
import models.prices.Prices;

/**
 * Date: 15/11/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilAmazonCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.amazon.com.br";
  private static final String SELLER_NAME_LOWER = "amazon.com.br";

  public BrasilAmazonCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }


  @Override
  protected Document fetch() {
    return Jsoup.parse(fetchPage(session.getOriginalURL(), new HashMap<>()));
  }

  public String fetchPage(String url, Map<String, String> headers) {
    String content = "";

    headers.put("Content-Encoding", "");
    headers.put("Accept-Encoding", "no");

    JSONObject fetcherPayload = POSTFetcher.fetcherPayloadBuilder(url, "GET", true, null, headers,
        Arrays.asList(ProxyCollection.STORM_RESIDENTIAL_US, ProxyCollection.STORM_RESIDENTIAL_EU), null, false);

    try {
      JSONObject fetcherResponse = POSTFetcher.requestWithFetcher(session, fetcherPayload, false);

      if (fetcherResponse.has("response")) {
        JSONObject responseJSON = fetcherResponse.getJSONObject("response");
        DataFetcherNO.setRequestProxyForFetcher(session, fetcherResponse, fetcherPayload.getString("url"));
        session.addRedirection(fetcherPayload.getString("url"), fetcherResponse.getJSONObject("response").getString("redirect_url"));

        content = responseJSON.getString("body");
        S3Service.uploadCrawlerSessionContentToAmazon(session, DataFetcherNO.generateRequestHash(session), content);

        if (fetcherResponse.has("request_status_code")) {
          int responseCode = fetcherResponse.getInt("request_status_code");
          if (Integer.toString(responseCode).charAt(0) != '2' && Integer.toString(responseCode).charAt(0) != '3' && responseCode != 404) { // errors
            throw new ResponseCodeException(responseCode);
          }
        }
      }
    } catch (Exception e) {
      Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
    }

    if (content.isEmpty()) {
      content = DataFetcherNO.fetchString(DataFetcherNO.GET_REQUEST, session, url, null, cookies);
    }

    return content;
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(doc);
      String internalPid = internalId;
      String name = crawlName(doc);
      CategoryCollection categories = crawlCategories(doc);

      JSONArray images = crawlImages(doc);
      String primaryImage = crawlPrimaryImage(images, doc);
      String secondaryImages = crawlSecondaryImages(images);

      String description = crawlDescription(doc);
      Integer stock = null;

      List<Document> docMarketPlaces = fetchDocumentMarketPlace(doc, internalId);
      Map<String, Prices> marketplaceMap = crawlMarketplaces(docMarketPlaces, doc);
      Marketplace marketplace = crawlMarketplace(marketplaceMap);

      Float price = crawlPrice(marketplaceMap);
      Prices prices = crawlPrices(marketplaceMap);
      boolean available = crawlAvailability(marketplaceMap) && price != null;
      String ean = crawlEan(doc);

      List<String> eans = new ArrayList<>();
      eans.add(ean);

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setStock(stock).setMarketplace(marketplace).setEans(eans).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    return doc.select("#dp").first() != null;
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element internalIdElement = doc.select("input[name^=ASIN]").first();
    Element internalIdElementSpecial = doc.select("input.askAsin").first();

    if (internalIdElement != null) {
      internalId = internalIdElement.val();
    } else if (internalIdElementSpecial != null) {
      internalId = internalIdElementSpecial.val();
    }

    return internalId;
  }


  private String crawlName(Document document) {
    String name = null;

    Element nameElement = document.select("#centerCol h1#title").first();
    Element nameElementSpecial = document.select("#productTitle").first();

    if (nameElement != null) {
      name = nameElement.text().trim();
    } else if (nameElementSpecial != null) {
      name = nameElementSpecial.text().trim();
    }

    return name;
  }

  /**
   * Get json of images inside html
   * 
   * @param doc
   * @return
   */
  private JSONArray crawlImages(Document doc) {
    JSONArray images = new JSONArray();

    JSONObject data = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"text/javascript\"]", "vardata=", ";", true, false);

    if (data.has("imageGalleryData")) {
      images = data.getJSONArray("imageGalleryData");
    } else if (data.has("colorImages")) {
      JSONObject colorImages = data.getJSONObject("colorImages");

      if (colorImages.has("initial")) {
        images = colorImages.getJSONArray("initial");
      }
    }

    return images;
  }

  private Float crawlPrice(Map<String, Prices> marketplaces) {
    Float price = null;

    Prices prices = null;

    if (marketplaces.containsKey(SELLER_NAME_LOWER)) {
      prices = marketplaces.get(SELLER_NAME_LOWER);
    }

    if (prices != null && !prices.isEmpty() && prices.getCardPaymentOptions(Card.VISA.toString()).containsKey(1)) {
      Double priceDouble = prices.getCardPaymentOptions(Card.VISA.toString()).get(1);
      price = priceDouble.floatValue();
    }

    return price;
  }

  private boolean crawlAvailability(Map<String, Prices> marketplaces) {
    boolean available = false;

    for (String seller : marketplaces.keySet()) {
      if (seller.equalsIgnoreCase(SELLER_NAME_LOWER)) {
        available = true;
        break;
      }
    }

    return available;
  }

  private Float crawlPriceForPrincipalSeller(Document document) {
    Float price = null;
    Element salePriceElement = document.select(".a-box .a-section.a-spacing-none.a-padding-none .a-color-price").first();
    Element specialPrice = document.select("#priceblock_dealprice").first();
    Element foodPrice = document.select("#priceblock_ourprice").first();

    if (salePriceElement != null) {
      price = MathUtils.parseFloatWithComma(salePriceElement.text().trim());
    } else {
      salePriceElement = document.select("#buybox .a-color-price").first();

      if (salePriceElement != null) {
        price = MathUtils.parseFloatWithComma(salePriceElement.ownText().trim());
      }
    }

    if (price == null && specialPrice != null) {
      price = MathUtils.parseFloatWithComma(specialPrice.ownText().trim());
    } else if (price == null && foodPrice != null) {
      price = MathUtils.parseFloatWithComma(foodPrice.ownText());
    }

    return price;
  }

  /**
   * Fetch pages when have marketplace info
   * 
   * @param id
   * @return documents
   */
  private List<Document> fetchDocumentMarketPlace(Document doc, String internalId) {
    List<Document> docs = new ArrayList<>();

    Element marketplaceUrl = doc.select("#moreBuyingChoices_feature_div .a-box .a-padding-base .a-size-small a[href]").first();

    if (marketplaceUrl != null) {
      String urlMarketPlace = HOME_PAGE + "/gp/offer-listing/" + internalId + "/ref=olp_page_next?ie=UTF8&f_all=true&f_new=true&startIndex=0";

      if (!urlMarketPlace.contains("amazon.com")) {
        urlMarketPlace = HOME_PAGE + urlMarketPlace;
      }

      Map<String, String> headers = new HashMap<>();
      headers.put("upgrade-insecure-requests", "1");
      headers.put("referer", session.getOriginalURL());

      Document docMarketplace = Jsoup.parse(fetchPage(urlMarketPlace, headers));
      docs.add(docMarketplace);

      headers.put("referer", urlMarketPlace);

      Element nextPage = docMarketplace.select(".a-last:not(.a-disabled)").first();
      int page = 1;

      while (nextPage != null) {
        String nextUrl = HOME_PAGE + "/gp/offer-listing/" + internalId + "/ref=olp_page_next?ie=UTF8&f_all=true&f_new=true&startIndex=" + page * 10;

        Document nextDocMarketPlace = Jsoup.parse(fetchPage(nextUrl, headers));
        docs.add(nextDocMarketPlace);
        nextPage = nextDocMarketPlace.select(".a-last:not(.a-disabled)").first();
        headers.put("referer", nextUrl);

        page++;
      }

    }

    return docs;
  }

  private String crawlPrincipalSeller(Document doc) {
    String principalSeller = SELLER_NAME_LOWER;

    Element name = doc.select("#merchant-info").first();
    Element nameSpecial = doc.select("#merchant-info > a").first();

    if (nameSpecial != null) {
      principalSeller = nameSpecial.ownText().toLowerCase().trim();
    } else if (name != null) {
      String text = name.ownText().toLowerCase().trim();

      if (text.contains("por")) {
        int x = text.indexOf("por") + 3;

        if (text.contains("embalagem")) {
          int y = text.indexOf("embalagem", x);

          principalSeller = text.substring(x, y).trim();
        } else {
          principalSeller = text.substring(x).trim();
        }


        if (principalSeller.endsWith(".")) {
          principalSeller = principalSeller.substring(0, principalSeller.length() - 1);
        }
      }
    }

    return principalSeller;
  }

  private Map<String, Prices> crawlMarketplaces(List<Document> docsMarketplaceInfo, Document doc) {
    Map<String, Prices> marketplace = new HashMap<>();

    String principalSellerFrontPage = crawlPrincipalSeller(doc);

    for (Document docMarketplaceInfo : docsMarketplaceInfo) {
      Elements lines = docMarketplaceInfo.select(".a-row.olpOffer");

      for (Element linePartner : lines) {
        Element name = linePartner.select(".olpSellerName a").first();
        Element nameImg = linePartner.select(".olpSellerName img").first();
        Element priceS = linePartner.select(".olpOfferPrice").first();

        if ((name != null || nameImg != null) && priceS != null) {
          String partnerName = nameImg != null ? nameImg.attr("alt").trim().toLowerCase() : name.text().trim().toLowerCase();
          Float partnerPrice = MathUtils.parseFloatWithComma(priceS.ownText());

          if (partnerName.equals(principalSellerFrontPage)) {
            marketplace.put(partnerName, crawlPrices(doc, null));
          } else {
            marketplace.put(partnerName, crawlPrices(doc, partnerPrice));
          }
        }
      }
    }

    if (!marketplace.containsKey(principalSellerFrontPage)) {
      marketplace.put(principalSellerFrontPage, crawlPrices(doc, null));
    }

    return marketplace;
  }

  private Marketplace crawlMarketplace(Map<String, Prices> marketplacesMap) {
    Marketplace marketplaces = new Marketplace();

    for (Entry<String, Prices> marketplaceEntry : marketplacesMap.entrySet()) {
      String sellerName = marketplaceEntry.getKey();

      if (!sellerName.equals(SELLER_NAME_LOWER)) {
        JSONObject sellerJSON = new JSONObject();
        sellerJSON.put("name", sellerName);

        Prices prices = marketplaceEntry.getValue();

        if (!prices.isEmpty() && prices.getCardPaymentOptions(Card.VISA.toString()).containsKey(1)) {
          // Pegando o preço de uma vez no cartão
          Double price = prices.getCardPaymentOptions(Card.VISA.toString()).get(1);
          Float priceFloat = price.floatValue();

          sellerJSON.put("price", priceFloat); // preço de boleto é o mesmo de preço uma vez.
        }

        sellerJSON.put("prices", prices.toJSON());

        try {
          Seller seller = new Seller(sellerJSON);
          marketplaces.add(seller);
        } catch (Exception e) {
          Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
        }
      }
    }

    return marketplaces;
  }

  private String crawlPrimaryImage(JSONArray images, Document doc) {
    String primaryImage = null;

    if (images.length() > 0) {
      JSONObject image = images.getJSONObject(0);

      if (image.has("mainUrl") && image.get("mainUrl") instanceof String) {
        primaryImage = image.get("mainUrl").toString().trim();
      } else if (image.has("thumbUrl") && image.get("thumbUrl") instanceof String) {
        primaryImage = image.get("thumbUrl").toString().trim();
      } else if (image.has("hiRes") && image.get("hiRes") instanceof String) {
        primaryImage = image.get("hiRes").toString().trim();
      } else if (image.has("large") && image.get("large") instanceof String) {
        primaryImage = image.get("large").toString().trim();
      } else if (image.has("thumb") && image.get("thumb") instanceof String) {
        primaryImage = image.get("thumb").toString().trim();
      }

    } else {
      Element img = doc.select("#ebooksImageBlockContainer img").first();

      if (img != null) {
        primaryImage = img.attr("src").trim();
      }
    }

    return primaryImage;
  }

  /**
   * Quando este crawler foi feito, nao tinha imagens secundarias
   * 
   * @param doc
   * @return
   */
  private String crawlSecondaryImages(JSONArray images) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();


    for (int i = 1; i < images.length(); i++) { // first index is the primary Image
      JSONObject imageJson = images.getJSONObject(i);

      String image = null;

      if (imageJson.has("mainUrl")) {
        image = imageJson.get("mainUrl").toString().trim();
      } else if (imageJson.has("thumbUrl")) {
        image = imageJson.get("thumbUrl").toString().trim();
      } else if (imageJson.has("hiRes")) {
        image = imageJson.get("hiRes").toString().trim();
      } else if (imageJson.has("large")) {
        image = imageJson.get("large").toString().trim();
      } else if (imageJson.has("thumb")) {
        image = imageJson.get("thumb").toString().trim();
      }

      if (image != null) {
        secondaryImagesArray.put(image);
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
    Elements elementCategories = document.select("#wayfinding-breadcrumbs_feature_div ul li:not([class]) a");

    for (Element e : elementCategories) {
      String cat = e.ownText().trim();

      if (!cat.isEmpty()) {
        categories.add(cat);
      }
    }

    return categories;
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();
    Element prodInfoElement = doc.selectFirst("#prodDetails");

    Elements elementsDescription =
        doc.select("#detail-bullets_feature_div, #detail_bullets_id, #feature-bullets, #bookDescription_feature_div, #aplus_feature_div");

    for (Element e : elementsDescription) {
      description.append(e.html().replace("noscript", "div"));
    }

    Elements longDescription = doc.select(".feature[id^=btfContent]");

    for (Element e : longDescription) {
      Element compare = e.select("#compare").first();

      if (compare == null) {
        description.append(e.html());
      }
    }

    Elements scripts = doc.select("script[type=\"text/javascript\"]");
    String token = "var iframeContent =";

    for (Element e : scripts) {
      String script = e.html();

      if (script.contains(token)) {

        if (script.contains("iframeDocument.getElementById")) {
          continue;
        }

        String iframeDesc = CrawlerUtils.extractSpecificStringFromScript(script, token, ";", false);

        try {
          description.append(URLDecoder.decode(iframeDesc, "UTF-8"));
        } catch (UnsupportedEncodingException ex) {
          Logging.printLogError(logger, session, CommonMethods.getStackTrace(ex));
        }

        break;
      }
    }

    if (prodInfoElement != null) {
      description.append(prodInfoElement.toString());
    }

    return description.toString();
  }


  /**
   * 
   * @param marketplaceMap
   * @return
   */
  private Prices crawlPrices(Map<String, Prices> marketplaceMap) {
    Prices prices = new Prices();

    if (marketplaceMap.containsKey(SELLER_NAME_LOWER)) {
      prices = marketplaceMap.get(SELLER_NAME_LOWER);
    }

    return prices;
  }

  /**
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Document doc, Float price) {
    Prices prices = new Prices();

    Map<Integer, Float> installments = new HashMap<>();

    if (price != null) {
      installments.put(1, price);
    } else {
      Float frontPagePrice = crawlPriceForPrincipalSeller(doc);
      if (frontPagePrice != null) {
        installments.put(1, frontPagePrice);
      }

      Elements pricesElement = doc.select("div.a-popover-preload[id^=a-popover] > div > table:not([border]) tr");

      if (pricesElement.isEmpty()) {
        pricesElement = doc.select("div.a-popover-preload[id^=a-popover] > table:not([border]) tr");
      }

      for (Element e : pricesElement) {
        Elements info = e.select("td");

        if (info.size() > 1) {
          String installment = info.get(0).ownText().replaceAll("[^0-9]", "").trim();
          Float value = MathUtils.parseFloatWithComma(info.get(1).ownText());

          if (!installment.isEmpty() && value != null) {
            installments.put(Integer.parseInt(installment), value);
          }
        }
      }
    }

    if (!installments.isEmpty()) {
      prices.insertCardInstallment(Card.VISA.toString(), installments);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installments);
      prices.insertCardInstallment(Card.ELO.toString(), installments);
      prices.insertCardInstallment(Card.DINERS.toString(), installments);
    }

    return prices;
  }

  private String crawlEan(Document doc) {
    Element e = doc.select(".pdTab table tr:not([class]):not([id]):not(:last-child)").last();
    String ean = null;

    if (e != null) {
      Element subE = e.selectFirst(".value");

      if (subE != null) {
        ean = subE.text().trim();
      }
    }

    return ean;
  }
}
