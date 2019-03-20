package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcherNO;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

/************************************************************************************************************************************************************************************
 * Crawling notes (29/08/2016):
 * 
 * 1) For this crawler, we have one URL for multiple skus.
 * 
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking for an specific html element.
 * 
 * 5) If the sku is unavailable, it's price is not displayed.
 * 
 * 6) The price of sku, found in json script, is wrong when the same is unavailable, then it is not
 * crawled.
 * 
 * 7) There is internalPid for skus in this ecommerce. The internalPid is a number that is the same
 * for all the variations of a given sku.
 * 
 * 7) The primary image is the first image on the secondary images.
 * 
 * 8) Variations of skus are not crawled if the variation is unavailable because it is not displayed
 * for the user, except if there is a html element voltage, because variations of voltage are
 * displayed for the user even though unavailable.
 * 
 * 9) The url of the primary images are changed to bigger dimensions manually.
 * 
 * Examples: ex1 (available):
 * http://www.clubedolar.com.br/ventilador-de-teto-3-pas--vt655--latina-black-/p ex2 (unavailable):
 * http://www.clubedolar.com.br/topo-para-bebedouros-azul-novo-latina/p
 *
 *
 ************************************************************************************************************************************************************************************/

public class BrasilClubedolarCrawler extends Crawler {

  private final String HOME_PAGE = "http://www.clubedolar.com.br/";

  public BrasilClubedolarCrawler(Session session) {
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
    List<Product> products = new ArrayList<Product>();

    if (isProductPage(session.getOriginalURL())) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      // Pid
      String internalPid = crawlInternalPid(doc);

      // Categories
      ArrayList<String> categories = crawlCategories(doc);
      String category1 = getCategory(categories, 0);
      String category2 = getCategory(categories, 1);
      String category3 = getCategory(categories, 2);

      // Description
      String description = crawlDescription(doc);

      // Stock
      Integer stock = null;

      // Marketplace map
      Map<String, Float> marketplaceMap = crawlMarketplace(doc);

      // Marketplace
      Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap);

      // Primary image
      String primaryImage = crawlPrimaryImage(doc);

      // Secondary images
      String secondaryImages = crawlSecondaryImages(doc);

      // Desconto boleto
      Integer discount = crawlDiscount(doc);

      // sku data in json
      JSONArray arraySkus = crawlSkuJsonArray(doc);

      // ean data in json
      JSONArray arrayEans = CrawlerUtils.scrapEanFromVTEX(doc);

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        // Availability
        boolean available = crawlAvailability(jsonSku);

        // InternalId
        String internalId = crawlInternalId(jsonSku);

        // Price
        Float price = crawlMainPagePrice(jsonSku, available);

        // Name
        String name = crawlName(doc, jsonSku);

        // Prices
        Prices prices = crawlPrices(internalId, price, discount);

        // Ean
        String ean = i < arrayEans.length() ? arrayEans.getString(i) : null;

        List<String> eans = new ArrayList<>();
        eans.add(ean);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(category1).setCategory2(category2).setCategory3(category3)
            .setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description).setStock(stock).setMarketplace(marketplace)
            .setEans(eans).build();

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

  private boolean isProductPage(String url) {
    if (url.endsWith("/p") || url.contains("/p?attempt="))
      return true;
    return false;
  }

  /*******************
   * General methods *
   *******************/

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("sku")) {
      internalId = Integer.toString((json.getInt("sku"))).trim();
    }

    return internalId;
  }


  private String crawlInternalPid(Document document) {
    String internalPid = null;
    Element internalPidElement = document.select("#___rc-p-id").first();

    if (internalPidElement != null) {
      internalPid = internalPidElement.attr("value").toString().trim();
    }

    return internalPid;
  }

  private String crawlName(Document document, JSONObject jsonSku) {
    String name = null;
    Element nameElement = document.select(".productName").first();

    String nameVariation = jsonSku.getString("skuname");

    if (nameElement != null) {
      name = nameElement.text().toString().trim();

      if (nameVariation.length() > name.length()) {
        name = nameVariation;
      } else if (!name.contains(nameVariation)) {
        name = name + " " + nameVariation;
      }
    }

    return name;
  }

  private Float crawlMainPagePrice(JSONObject json, boolean available) {
    Float price = null;

    if (json.has("bestPriceFormated") && available) {
      price = Float.parseFloat(json.getString("bestPriceFormated").replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
    }

    return price;
  }

  private boolean crawlAvailability(JSONObject json) {

    if (json.has("available"))
      return json.getBoolean("available");

    return false;
  }

  private Map<String, Float> crawlMarketplace(Document document) {
    return new HashMap<String, Float>();
  }

  private Marketplace assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
    return new Marketplace();
  }

  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;
    Element image = doc.select("#image a").first();

    if (image != null) {
      primaryImage = image.attr("href");
    } else {
      image = doc.select("#image img").first();
      primaryImage = image.attr("src");
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document doc) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();
    Elements images = doc.select(".thumbs li a");


    for (int i = 1; i < images.size(); i++) { // starts with index 1, because the first image is the
                                              // primary image

      String urlImage = images.get(i).attr("zoom");

      if (!urlImage.startsWith("http")) {
        urlImage = images.get(i).attr("rel");

        if (!urlImage.startsWith("http")) {
          Element e = images.get(i).select("img").first();

          if (e != null) {
            urlImage = images.get(i).attr("src");
          }
        }
      }

      secondaryImagesArray.put(urlImage);

    }


    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }


  private ArrayList<String> crawlCategories(Document document) {
    ArrayList<String> categories = new ArrayList<String>();
    Elements elementCategories = document.select(".bread-crumb > ul li a");

    for (int i = 1; i < elementCategories.size(); i++) { // starting from index 1, because the first
                                                         // is the market name
      categories.add(elementCategories.get(i).text().trim());
    }

    return categories;
  }

  private String getCategory(ArrayList<String> categories, int n) {
    if (n < categories.size()) {
      return categories.get(n);
    }

    return "";
  }

  private String crawlDescription(Document document) {
    String description = "";
    Element descriptionElement = document.select(".productDescription").first();
    Element specElement = document.select("#caracteristicas").first();

    if (descriptionElement != null)
      description = description + descriptionElement.html();
    if (specElement != null)
      description = description + specElement.html();

    return description;
  }

  private Integer crawlDiscount(Document doc) {
    Integer discount = null;
    Element discountElement = doc.select(".cont_flags > p").first();

    if (discountElement != null) {
      discount = Integer.parseInt(discountElement.text().replaceAll("[^0-9]", ""));
    }

    return discount;
  }

  private Prices crawlPrices(String internalId, Float price, Integer discountBoleto) {
    Prices prices = new Prices();

    if (price != null) {
      String url = "http://www.clubedolar.com.br/productotherpaymentsystems/" + internalId;

      Document doc = DataFetcherNO.fetchDocument(DataFetcherNO.GET_REQUEST, session, url, null, cookies);

      Element bank = doc.select("#ltlPrecoWrapper em").first();
      if (bank != null) {
        Float bankTicketPrice = Float.parseFloat(bank.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());

        if (discountBoleto != null) {
          Float result = (float) (bankTicketPrice - (bankTicketPrice * (discountBoleto.floatValue() / 100.0)));
          bankTicketPrice = MathUtils.normalizeTwoDecimalPlaces(result);
        }

        prices.setBankTicketPrice(bankTicketPrice);
      }

      Elements cardsElements = doc.select("#ddlCartao option");

      for (Element e : cardsElements) {
        String text = e.text().toLowerCase();

        if (text.contains("visa")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
          prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);

        } else if (text.contains("mastercard")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
          prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);

        } else if (text.contains("diners")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
          prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);

        } else if (text.contains("american") || text.contains("amex")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
          prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);

        } else if (text.contains("hipercard") || text.contains("amex")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
          prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);

        } else if (text.contains("credicard")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
          prices.insertCardInstallment(Card.CREDICARD.toString(), installmentPriceMap);

        } else if (text.contains("elo")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
          prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);

        }
      }


    }

    return prices;
  }

  private Map<Integer, Float> getInstallmentsForCard(Document doc, String idCard) {
    Map<Integer, Float> mapInstallments = new HashMap<>();

    Elements installmentsCard = doc.select(".tbl-payment-system#tbl" + idCard + " tr");
    for (Element i : installmentsCard) {
      Element installmentElement = i.select("td.parcelas").first();

      if (installmentElement != null) {
        String textInstallment = removeAccents(installmentElement.text().toLowerCase());
        Integer installment = null;

        if (textInstallment.contains("vista")) {
          installment = 1;
        } else {
          installment = Integer.parseInt(textInstallment.replaceAll("[^0-9]", "").trim());
        }

        Element valueElement = i.select("td:not(.parcelas)").first();

        if (valueElement != null) {
          Float value = Float.parseFloat(valueElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());

          mapInstallments.put(installment, value);
        }
      }
    }

    return mapInstallments;
  }

  private String removeAccents(String str) {
    str = Normalizer.normalize(str, Normalizer.Form.NFD);
    str = str.replaceAll("[^\\p{ASCII}]", "");
    return str;
  }

  /**
   * Get the script having a json with the availability information
   * 
   * @return
   */
  private JSONArray crawlSkuJsonArray(Document document) {
    Elements scriptTags = document.getElementsByTag("script");
    JSONObject skuJson = null;
    JSONArray skuJsonArray = null;

    for (Element tag : scriptTags) {
      for (DataNode node : tag.dataNodes()) {
        if (tag.html().trim().startsWith("var skuJson_0 = ")) {

          skuJson = new JSONObject(node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1]
              + node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1].split(Pattern.quote("}]};"))[0]);

        }
      }
    }

    try {
      skuJsonArray = skuJson.getJSONArray("skus");
    } catch (Exception e) {
      e.printStackTrace();
    }

    return skuJsonArray;
  }
}
