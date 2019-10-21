package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

/**
 * Date: 30/08/17
 * 
 * @author gabriel
 *
 */

public class SaopauloMamboCrawler extends Crawler {

  private static final String HOME_PAGE = "http://www.mambo.com.br/";

  public SaopauloMamboCrawler(Session session) {
    super(session);
    super.config.setMustSendRatingToKinesis(true);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  protected JSONObject fetch() {
    JSONObject api = new JSONObject();

    String[] tokens = session.getOriginalURL().split("\\?")[0].split("/");
    String id = CommonMethods.getLast(tokens);
    String pathName = tokens[tokens.length - 2];

    String apiUrl =
        "https://www.mambo.com.br/ccstoreui/v1/pages/" + pathName + "/p/" + id + "?dataOnly=false&cacheableDataOnly=true&productTypesRequired=true";

    Request request = RequestBuilder.create().setUrl(apiUrl).setCookies(cookies).build();
    JSONObject response = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

    if (response.has("data")) {
      api = response.getJSONObject("data");
    }

    return api;
  }

  @Override
  public List<Product> extractInformation(JSONObject pageJson) throws Exception {
    super.extractInformation(pageJson);
    List<Product> products = new ArrayList<>();

    if (pageJson.has("page")) {
      JSONObject page = pageJson.getJSONObject("page");

      if (page.has("product")) {
        JSONObject json = page.getJSONObject("product");
        Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

        String internalPid = crawlInternalPid(json);
        String internalId = internalPid;
        String name = JSONUtils.getStringValue(json, "displayName");
        boolean available = scrapAvailability(internalPid);
        CategoryCollection categories = new CategoryCollection(); // This crawler is very hard to capture categories
        String primaryImage = crawlPrimaryImage(json);
        String secondaryImages = crawlSecondaryImages(json, primaryImage);
        RatingsReviews rating = scrapRating(internalId, new Document(""));

        JSONArray arraySkus = json.has("childSKUs") && !json.isNull("childSKUs") ? json.getJSONArray("childSKUs") : new JSONArray();

        if (arraySkus.length() > 0) {
          JSONObject jsonSku = arraySkus.getJSONObject(0);
          Float price = CrawlerUtils.getFloatValueFromJSON(jsonSku, "salePrice");
          price = price == null ? CrawlerUtils.getFloatValueFromJSON(jsonSku, "listPrice") : price;

          Prices prices = crawlPrices(price, jsonSku, pageJson);
          String description = crawlDescription(json, jsonSku);

          // Creating the product
          Product product = ProductBuilder.create()
              .setUrl(session.getOriginalURL())
              .setInternalId(internalId)
              .setInternalPid(internalPid)
              .setName(name).setPrice(price)
              .setPrices(prices)
              .setAvailable(available)
              .setCategory1(categories.getCategory(0))
              .setCategory2(categories.getCategory(1))
              .setCategory3(categories.getCategory(2))
              .setPrimaryImage(primaryImage)
              .setSecondaryImages(secondaryImages)
              .setDescription(description)
              .setMarketplace(new Marketplace())
              .setRatingReviews(rating)
              .build();

          products.add(product);
        }
      } else {
        Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
    }

    return products;

  }

  private RatingsReviews scrapRating(String internalId, Document doc) {
    TrustvoxRatingCrawler trustVox = new TrustvoxRatingCrawler(session, "944", logger);
    return trustVox.extractRatingAndReviews(internalId, doc, dataFetcher);
  }

  private String crawlInternalPid(JSONObject json) {
    String internalPid = null;

    if (json.has("id")) {
      internalPid = json.get("id").toString();
    }

    return internalPid;
  }

  private boolean scrapAvailability(String internalPid) {
    boolean available = false;

    String url = "https://www.mambo.com.br/ccstoreui/v1/stockStatus/" + internalPid;
    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
    JSONObject stockJson = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

    if (stockJson.has("stockStatus") && !stockJson.isNull("stockStatus")) {
      available = stockJson.get("stockStatus").toString().equalsIgnoreCase("IN_STOCK");
    }

    return available;
  }

  private String crawlPrimaryImage(JSONObject json) {
    String primaryImage = null;

    if (json.has("primaryFullImageURL") && !json.get("primaryFullImageURL").toString().equalsIgnoreCase("null")) {
      primaryImage = CrawlerUtils.completeUrl(json.get("primaryFullImageURL").toString(), "https:", "www.mambo.com.br");
    } else if (json.has("primaryLargeImageURL") && !json.get("primaryLargeImageURL").toString().equalsIgnoreCase("null")) {
      primaryImage = CrawlerUtils.completeUrl(json.get("primaryLargeImageURL").toString(), "https:", "www.mambo.com.br");
    } else if (json.has("primaryMediumImageURL") && !json.get("primaryMediumImageURL").toString().equalsIgnoreCase("null")) {
      primaryImage = CrawlerUtils.completeUrl(json.get("primaryMediumImageURL").toString(), "https:", "www.mambo.com.br");
    } else if (json.has("primarySmallImageURL") && !json.get("primarySmallImageURL").toString().equalsIgnoreCase("null")) {
      primaryImage = CrawlerUtils.completeUrl(json.get("primarySmallImageURL").toString(), "https:", "www.mambo.com.br");
    } else if (json.has("primaryThumbImageURL") && !json.get("primaryThumbImageURL").toString().equalsIgnoreCase("null")) {
      primaryImage = CrawlerUtils.completeUrl(json.get("primaryThumbImageURL").toString(), "https:", "www.mambo.com.br");
    }

    return primaryImage;
  }

  /**
   * @param doc
   * @return
   */
  private String crawlSecondaryImages(JSONObject json, String primaryImage) {
    String secondaryImages = null;

    JSONArray secondaryImagesArray = new JSONArray();
    JSONArray images = new JSONArray();

    if (verifyImagesArray(json, "fullImageURLs")) {
      images = json.getJSONArray("fullImageURLs");
    } else if (verifyImagesArray(json, "largeImageURLs")) {
      images = json.getJSONArray("largeImageURLs");
    } else if (verifyImagesArray(json, "mediumImageURLs")) {
      images = json.getJSONArray("mediumImageURLs");
    } else if (verifyImagesArray(json, "smallImageURLs")) {
      images = json.getJSONArray("smallImageURLs");
    } else if (verifyImagesArray(json, "thumbImageURLs")) {
      images = json.getJSONArray("thumbImageURLs");
    }

    for (Object o : images) {
      String image = CrawlerUtils.completeUrl(o.toString(), "https:", "www.mambo.com.br");

      if (!image.equalsIgnoreCase(primaryImage)) {
        secondaryImagesArray.put(image);
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private boolean verifyImagesArray(JSONObject json, String key) {
    if (json.has(key) && json.get(key) instanceof JSONArray) {
      JSONArray array = json.getJSONArray(key);

      if (array.length() > 0) {
        return true;
      }
    }

    return false;
  }

  private String crawlDescription(JSONObject json, JSONObject jsonSku) {
    StringBuilder description = new StringBuilder();

    if (json.has("longDescription")) {
      description.append(json.get("longDescription").toString());
    }

    description.append("<table>");
    Set<String> keys = jsonSku.keySet();
    for (String key : keys) {
      if ((key.startsWith("1_") || key.startsWith("x_")) && !key.equalsIgnoreCase("1_informaesAdicionais") && jsonSku.get(key) instanceof String) {
        description.append("<tr>");
        description.append("<td>")
            .append(CommonMethods.upperCaseFirstCharacter(CommonMethods.splitStringWithUpperCase(key.replace("1_", "").replace("x_", ""))))
            .append("</td>");

        description.append("<td>").append(jsonSku.get(key)).append("</td>");
        description.append("</tr>");
      }
    }
    description.append("</table>");

    if (jsonSku.has("1_informaesAdicionais")) {
      description.append("<h4> Informações Adicionais: </h4>\n ").append(jsonSku.get("1_informaesAdicionais").toString());
    }

    return description.toString();
  }

  /**
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, JSONObject jsonSku, JSONObject pageJson) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);

      prices.setPriceFrom(CrawlerUtils.getDoubleValueFromJSON(jsonSku, "listPrice", false, true));
      prices.setBankTicketPrice(price);

      if (pageJson.has("global")) {
        JSONObject global = pageJson.getJSONObject("global");

        if (global.has("site")) {
          JSONObject site = global.getJSONObject("site");

          if (site.has("extensionSiteSettings")) {
            JSONObject extensionSiteSettings = site.getJSONObject("extensionSiteSettings");

            if (extensionSiteSettings.has("discountSettings")) {
              JSONObject discountSettings = extensionSiteSettings.getJSONObject("discountSettings");

              if (discountSettings.has("descontoBoleto")) {
                String text = discountSettings.get("descontoBoleto").toString().replaceAll("[^0-9]", "").trim();

                if (!text.isEmpty()) {
                  prices.setBankTicketPrice(MathUtils.normalizeTwoDecimalPlaces(price - (price * (Integer.parseInt(text) / 100d))));
                }
              }
            }

            if (extensionSiteSettings.has("customSiteSettings")) {
              JSONObject customSiteSettings = extensionSiteSettings.getJSONObject("customSiteSettings");

              int minValuePerInstallment = 0;
              if (customSiteSettings.has("minValuePerInstallment")) {
                String text = customSiteSettings.get("minValuePerInstallment").toString().replaceAll("[^0-9]", "").trim();

                if (!text.isEmpty()) {
                  minValuePerInstallment = Integer.parseInt(text);
                }
              }

              int maxNumInstallment = 1;
              if (customSiteSettings.has("maxNumInstallment")) {
                String text = customSiteSettings.get("maxNumInstallment").toString().replaceAll("[^0-9]", "").trim();

                if (!text.isEmpty()) {
                  maxNumInstallment = Integer.parseInt(text);
                }
              }

              for (int i = 1; i <= maxNumInstallment; i++) {
                Float priceInstallment = MathUtils.normalizeTwoDecimalPlaces(price / i);

                if (priceInstallment >= minValuePerInstallment) {
                  installmentPriceMap.put(i, priceInstallment);
                } else {
                  break;
                }
              }
            }
          }
        }
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.CABAL.toString(), installmentPriceMap);
    }

    return prices;
  }
}
