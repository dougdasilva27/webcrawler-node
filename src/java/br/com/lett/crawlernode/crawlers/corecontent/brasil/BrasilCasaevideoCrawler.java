package br.com.lett.crawlernode.crawlers.corecontent.brasil;

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
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 09/01/2017
 * 
 * 1) For this crawler, we have one URL for multiple skus.. 2) Prices are found in this api:
 * http://www.casaevideo.com.br/productotherpaymentsystems/ + internalId
 * 
 * obs: Vtex market
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilCasaevideoCrawler extends Crawler {

  private static final String HOME_PAGE = "http://www.casaevideo.com.br/";

  public BrasilCasaevideoCrawler(Session session) {
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

      // Pid
      String internalPid = crawlInternalPid(doc);

      // Categories
      CategoryCollection categories = crawlCategories(doc);

      // Description
      String description = crawlDescription(doc);

      // Stock
      Integer stock = null;

      // Marketplace
      Marketplace marketplace = assembleMarketplaceFromMap();

      // sku data in json
      JSONArray arraySkus = crawlSkuJsonArray(doc);

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        // Availability
        boolean available = crawlAvailability(jsonSku);

        // InternalId
        String internalId = crawlInternalId(jsonSku);

        // Price
        Float price = crawlMainPagePrice(jsonSku, available);

        // Primary image
        String primaryImage = crawlPrimaryImage(doc);

        // Name
        String name = crawlName(doc, jsonSku);

        // Secondary images
        String secondaryImages = crawlSecondaryImages(doc);

        // Prices
        Prices prices = crawlPrices(internalId, price, doc);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
            .setStock(stock).setMarketplace(marketplace).build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(Document document) {
    if (document.select(".productName").first() != null) {
      return true;
    }
    return false;
  }

  /*******************
   * General methods *
   *******************/

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("sku")) {
      internalId = Integer.toString(json.getInt("sku")).trim();
    }

    return internalId;
  }


  private String crawlInternalPid(Document document) {
    String internalPid = null;
    Element internalPidElement = document.select("#___rc-p-id").first();

    if (internalPidElement != null) {
      internalPid = internalPidElement.val().trim();
    }

    return internalPid;
  }

  private String crawlName(Document document, JSONObject jsonSku) {
    String name = null;
    Element nameElement = document.select(".productName").first();

    String nameVariation = jsonSku.getString("skuname");

    if (nameElement != null) {
      name = nameElement.text().toString().trim();

      if (name.length() > nameVariation.length()) {
        name += " " + nameVariation;
      } else {
        name = nameVariation;
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
    if (json.has("available")) {
      return json.getBoolean("available");
    }
    return false;
  }

  private Marketplace assembleMarketplaceFromMap() {
    return new Marketplace();
  }

  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;

    Element image = doc.select("#botaoZoom").first();

    if (image != null) {
      primaryImage = image.attr("zoom").trim();

      if (primaryImage == null || primaryImage.isEmpty()) {
        primaryImage = image.attr("rel").trim();
      }
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document doc) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements imageThumbs = doc.select("#botaoZoom");

    for (int i = 1; i < imageThumbs.size(); i++) { // starts with index 1, because the first image is the primary image
      String url = imageThumbs.get(i).attr("zoom");

      if (url == null || url.isEmpty()) {
        url = imageThumbs.get(i).attr("rel");
      }

      if (url != null && !url.isEmpty()) {
        secondaryImagesArray.put(url);
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select(".bread-crumb > ul li a");

    for (int i = 1; i < elementCategories.size(); i++) { // starting from index 1, because the first is the market name
      categories.add(elementCategories.get(i).text().trim());
    }

    return categories;
  }


  private String crawlDescription(Document document) {
    String description = "";

    Element descElement = document.select(".productDescription").first();

    if (descElement != null) {
      description = description + descElement.html();
    }

    Element spec = document.select("#specification").first();

    if (spec != null) {
      description += spec.outerHtml();
    }

    return description;
  }

  /**
   * To crawl this prices is accessed a api Is removed all accents for crawl price 1x like this: Visa
   * à vista R$ 1.790,00
   * 
   * @param internalId
   * @param price
   * @return
   */
  private Prices crawlPrices(String internalId, Float price, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      String url = "http://www.casaevideo.com.br/productotherpaymentsystems/" + internalId;

      Document docPrices = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, cookies);

      // O preço no boleto não aparece com javascript desligado, mas aparece a porcentagem de desconto
      // Assim é calculado o preço no boleto de acordo com o preço principal.
      Element bankDiscount = doc.select(".product-discount-info-list > li.row > span").first();
      if (bankDiscount != null) {
        Integer discount = Integer.parseInt(bankDiscount.text().replaceAll("[^0-9]", "").trim());
        Float result = (float) (price - (price * (discount.floatValue() / 100.0)));

        Float bankTicketPrice = MathCommonsMethods.normalizeTwoDecimalPlaces(result);
        prices.setBankTicketPrice(bankTicketPrice);
      } else {
        prices.setBankTicketPrice(price);
      }


      Elements cardsElements = docPrices.select("#ddlCartao option");

      for (Element e : cardsElements) {
        String text = e.text().toLowerCase();

        if (text.contains("visa")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.val());
          prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);

        } else if (text.contains("mastercard")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.val());
          prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);

        } else if (text.contains("diners")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.val());
          prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);

        } else if (text.contains("american") || text.contains("amex")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.val());
          prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);

        } else if (text.contains("hipercard")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.val());
          prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);

        } else if (text.contains("credicard")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.val());
          prices.insertCardInstallment(Card.CREDICARD.toString(), installmentPriceMap);

        } else if (text.contains("elo")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.val());
          prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);

        }
      }


    }

    return prices;
  }

  Map<Integer, Float> getInstallmentsForCard(Document doc, String idCard) {
    Map<Integer, Float> mapInstallments = new HashMap<>();

    Elements installmentsCard = doc.select(".tbl-payment-system#tbl" + idCard + " tr");
    for (Element i : installmentsCard) {
      Element installmentElement = i.select("td.parcelas").first();

      if (installmentElement != null) {
        String textInstallment = installmentElement.text().toLowerCase();
        Integer installment;

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

  /**
   * Get the script having a json with the availability information
   * 
   * @return
   */
  private JSONArray crawlSkuJsonArray(Document document) {
    Elements scriptTags = document.getElementsByTag("script");
    JSONObject skuJson = null;
    JSONArray skuJsonArray = null;

    String split = "var skuJson_0 = ";

    for (Element tag : scriptTags) {
      for (DataNode node : tag.dataNodes()) {
        if (tag.html().trim().startsWith(split)) {
          skuJson = new JSONObject(node.getWholeData().split(Pattern.quote(split))[1]
              + node.getWholeData().split(Pattern.quote(split))[1].split(Pattern.quote("}]};"))[0]);
        }
      }
    }

    if (skuJson != null && skuJson.has("skus")) {
      skuJsonArray = skuJson.getJSONArray("skus");
    }

    if (skuJsonArray == null) {
      skuJsonArray = new JSONArray();
    }

    return skuJsonArray;
  }
}
