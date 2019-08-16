package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 03/12/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class TottusCrawler {

  private Logger logger;
  private Session session;
  private boolean priceWithComma;

  public TottusCrawler(Logger logger, Session session, boolean priceWithComma) {
    this.logger = logger;
    this.session = session;
    this.priceWithComma = priceWithComma;
  }

  public List<Product> extractInformation(Document doc) throws Exception {
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + session.getOriginalURL());

      String internalId = crawlInternalId(doc);
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".title h5", false) + " "
          + CrawlerUtils.scrapStringSimpleInfo(doc, ".caption-description .statement", true);
      Float price = priceWithComma ? CrawlerUtils.scrapSimplePriceFloat(doc, ".price-selector .active-price", false)
          : CrawlerUtils.scrapSimplePriceFloatWithDots(doc, ".price-selector .active-price", false);
      Prices prices = crawlPrices(price, doc);
      boolean available = doc.select(".product-detail .out-of-stock").isEmpty();
      CategoryCollection categories = crawlCategories(doc);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".caption-img > img", Arrays.asList("data-src", "src"), "http:",
          "s7d2.scene7.com");
      String secondaryImages = crawlSecondaryImagesByScript(doc, internalId, primaryImage);
      String description = crawlDescription(doc, internalId);

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setName(name).setPrice(price)
          .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setMarketplace(new Marketplace()).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
    }

    return products;

  }

  private String crawlDescription(Document doc, String internalId) {
    String id = internalId;
    Element descriptionHtml = doc.selectFirst(".wrap-text-descriptions");

    while (id != null && id.startsWith("0")) {
      id = id.substring(1);
    }


    StringBuilder description = new StringBuilder();
    Map<String, String> headers = new HashMap<>();

    headers.put("Accept", "");

    Request request = RequestBuilder.create().setUrl("https://api-fichas-tecnicas.firebaseio.com/fichas.json?orderBy=%22SKU%22&equalTo=" + id)
        .mustSendContentEncoding(false).build();
    JSONObject skuDescription = CrawlerUtils.stringToJson(new FetcherDataFetcher().get(session, request).getBody());

    if (descriptionHtml != null) {
      description.append(descriptionHtml.text());
    }

    if (skuDescription.has(id)) {
      JSONObject jsonDescription = skuDescription.getJSONObject(id);

      description.append(scrapTechnicDescription(doc.select("v-container > v-layout > v-flex:not([v-if]) v-list-tile-content"), jsonDescription));

      if (jsonDescription.has("TIPO")) {
        description.append(scrapTechnicDescription(doc.select("[v-if~='" + jsonDescription.getString("TIPO") + "'] v-list-tile-content, [v-if~='"
            + jsonDescription.getString("TIPO") + "'] [v-if~=tieneSellos]"), jsonDescription));
      }

      if (jsonDescription.has("DESCRIPCION_CORTA")) {
        description.append(scrapTechnicDescription(doc.select("[v-if=\"tieneDescripcion==true\"] v-flex:first-child"), jsonDescription));
      }

    }


    return description.toString();
  }

  private StringBuilder scrapTechnicDescription(Elements elements, JSONObject jsonDescription) {
    StringBuilder description = new StringBuilder();
    if (!elements.isEmpty()) {
      description.append("<table>");
      for (Element element : elements) {
        String str = element.text();

        if (str.contains("{{")) {
          if (str.contains("item.")) {
            String aux = str.substring(str.lastIndexOf("item"), str.indexOf("}}"));
            String keysHtml = aux.replace("item.", "").trim();

            if (jsonDescription.has(keysHtml)) {
              description.append("<td>");
              description.append(jsonDescription.get(keysHtml).toString());
              description.append("</td></tr>");
            }
          } else {
            String aux = str.substring(str.lastIndexOf("{"), str.indexOf("}"));
            String keysHtml = aux.replace("{", "").trim();

            if (jsonDescription.has(keysHtml)) {
              description.append("<td>");
              description.append(jsonDescription.get(keysHtml).toString());
              description.append("</td></tr>");
            }
          }
        } else {
          description.append("<tr><td>");
          description.append(str);
          description.append("</td>");
        }
      }
      description.append("</table>");
    }
    return description;
  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".product-detail").isEmpty();
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    JSONObject jsonHtml = CrawlerUtils.selectJsonFromHtml(doc, "script", "varjson=$.parseJSON('", "');", true, false);

    if (jsonHtml.has("ecommerce")) {
      JSONObject ecommerce = jsonHtml.getJSONObject("ecommerce");
      if (ecommerce.has("impressions")) {

        JSONArray impressions = ecommerce.getJSONArray("impressions");
        for (Object object : impressions) {
          JSONObject impression = (JSONObject) object;
          if (impression.has("id")) {
            internalId = impression.getString("id");
          }

        }
      }
    }

    return internalId;
  }

  public static CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select(".breadcrumb-nav a");

    for (Element e : elementCategories) {
      categories.add(e.text().replace(">", "").trim());
    }

    Element lastCategory = document.selectFirst(".breadcrumb-nav h3");
    if (lastCategory != null) {
      categories.add(lastCategory.ownText().replace("/", "").trim());
    }

    return categories;
  }

  private String crawlSecondaryImagesByScript(Document doc, String internalId, String primaryImage) {
    String secondaryImages =
        CrawlerUtils.scrapSimpleSecondaryImages(doc, ".caption-img img", Arrays.asList("src"), "http:", "s7d2.scene7.com", primaryImage);

    if (secondaryImages.contains("placeHold")) {
      JSONArray secondaryImagesJsonArray = new JSONArray(secondaryImages);
      JSONArray newSecondaryImagesJsonArray = new JSONArray();

      for (Object object : secondaryImagesJsonArray) {
        String urlSecondaryImage = (String) object;

        if (!urlSecondaryImage.contains("placeHold")) {
          newSecondaryImagesJsonArray.put(urlSecondaryImage);

        }
      }

      secondaryImages = newSecondaryImagesJsonArray.toString();
    }

    if (secondaryImages == null || secondaryImages.length() < 1) {
      JSONArray images = new JSONArray();

      String searchString = "varsearch_string_1='";
      String fpImgCount = "fp_img_count=";

      Elements scripts = doc.select("script[type=text/javascript]");
      for (Element e : scripts) {
        String html = e.html().replace(" ", "");

        if (html.contains(searchString) && html.contains(fpImgCount)) {
          int x = html.indexOf(searchString) + searchString.length();
          int y = html.indexOf("';", x);

          String match = html.substring(x, y);

          Pattern r = Pattern.compile(match);
          Matcher m = r.matcher(session.getOriginalURL().toLowerCase());

          if (m.find()) {
            int firstIndex = html.indexOf(fpImgCount) + fpImgCount.length();
            int lastIndex = html.indexOf(';', firstIndex);

            String imagesNumberString = html.substring(firstIndex, lastIndex).replaceAll("[^0-9]", "");

            if (!imagesNumberString.isEmpty()) {
              int imagesNumber = Integer.parseInt(imagesNumberString);

              for (int i = 1; i <= imagesNumber; i++) {
                images.put("http://s7d2.scene7.com/is/image/TottusPE/" + internalId + "_" + i + "?$S7Product$&wid=458&hei=458&op_sharpen=0");
              }
            }
          }
          break;
        }
      }

      if (images.length() > 0) {
        secondaryImages = images.toString();
      }
    }

    return secondaryImages;
  }


  /**
   * In the time when this crawler was made, this market hasn't installments informations
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();
      installmentPriceMap.put(1, price);

      Map<Integer, Float> installmentPriceMapShop = new HashMap<>();
      installmentPriceMapShop.put(1, price);

      prices.setPriceFrom(this.priceWithComma ? CrawlerUtils.scrapSimplePriceDouble(doc, ".price-selector .nule-price", false)
          : CrawlerUtils.scrapSimplePriceDoubleWithDots(doc, ".price-selector .nule-price", false));

      Element discounts = doc.selectFirst(".active-offer .red");
      if (discounts != null) {
        if (prices.getPriceFrom() == null) {
          prices.setPriceFrom(MathUtils.normalizeNoDecimalPlaces(price.doubleValue()));
        }

        String text = discounts.ownText().toLowerCase();

        if (!text.contains("2 unidades")) {
          String[] tokens = text.split(",");
          Float normalCardDiscount = 0f;
          Float shopCardDiscount = 0f;

          if (tokens.length > 1) {
            String shopDiscount = tokens[0].replaceAll("[^0-9]", "");
            String cardDiscount = tokens[1].replaceAll("[^0-9]", "");

            if (!shopDiscount.isEmpty()) {
              shopCardDiscount = Integer.parseInt(shopDiscount) / 100f;
            }

            if (!cardDiscount.isEmpty()) {
              normalCardDiscount = Integer.parseInt(cardDiscount) / 100f;
            }
          } else {
            String cardsDiscount = text.replaceAll("[^0-9]", "");

            if (!cardsDiscount.isEmpty()) {
              normalCardDiscount = Integer.parseInt(cardsDiscount) / 100f;
              shopCardDiscount = normalCardDiscount;
            }
          }

          Float priceWithDiscount = MathUtils.normalizeNoDecimalPlaces(price - (price * normalCardDiscount));
          if (priceWithDiscount > 0) {
            installmentPriceMap.put(1, priceWithDiscount);
          }

          Float shopPriceWithDiscount = MathUtils.normalizeNoDecimalPlaces(price - (price * shopCardDiscount));
          if (priceWithDiscount > 0) {
            installmentPriceMapShop.put(1, shopPriceWithDiscount);
          }
        }
      }

      prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMapShop);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
    }

    return prices;
  }

}
