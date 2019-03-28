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
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.prices.Prices;

public class BrasilBenoitCrawler extends Crawler {

  public BrasilBenoitCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
    String url = session.getOriginalURL().concat(".json");

    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
    JSONObject json = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      if (json.has("Model")) {
        JSONObject model = json.getJSONObject("Model");
        String internalPid = model.has("ProductID") ? model.get("ProductID").toString() : null;
        Float price = crawlPrice(model);
        String primaryImage = crawlPrimaryImage(model);
        String secondaryImages = crawlSecondaryImages(model, primaryImage);
        CategoryCollection categories = crawlCategories(model);
        String description = crawlDesciption(model);

        for (Object obj : model.getJSONArray("Items")) {
          JSONObject sku = (JSONObject) obj;

          // This verification exists to the json don't return the empty object.

          if (sku.has("Items") && sku.getJSONArray("Items").length() < 1) {
            String internalId = sku.has("ProductID") ? sku.get("ProductID").toString() : null;
            Prices prices = crawlPrices(internalId, internalPid);
            String name = crawlName(sku);
            boolean available = crawlAvailability(sku);


            Product product = ProductBuilder.create().setUrl(url).setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price)
                .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
                .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
                .setStock(null).setMarketplace(new Marketplace()).build();

            products.add(product);
          }
        }
      }


    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private Prices crawlPrices(String internalId, String internalPid) {
    Prices prices = new Prices();

    String url = "https://www.benoit.com.br/widget/product_payment_options?SkuID=" + internalId + "&ProductID=" + internalPid
        + "&Template=wd.product.payment.options.result.template&ForceWidgetToRender=true";

    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
    Document fetchPage = Jsoup.parse(this.dataFetcher.get(session, request).getBody());
    Element firstGrid = fetchPage.selectFirst(".grid");

    if (firstGrid != null) {
      Elements table = fetchPage.select(".grid tbody tr");
      Elements cards = firstGrid.select("table span");
      for (Element element : cards) {
        Map<Integer, Float> installments = new HashMap<>();
        for (Element e : table) {
          Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(null, e, false, "x", "juros", true);

          if (!pair.isAnyValueNull()) {
            installments.put(pair.getFirst(), pair.getSecond());
          }
        }

        String cardName = scrapCardName(element);
        prices.insertCardInstallment(cardName, installments);
      }
      prices.setBankTicketPrice(CrawlerUtils.scrapDoublePriceFromHtml(fetchPage, ".wd-content > div > .grid:last-child", null, false, ','));

    }

    return prices;
  }

  private String scrapCardName(Element cardElement) {
    String resultCard = null;
    Card[] cards = Card.values();

    String cardName = CommonMethods.getLast(cardElement.text().toLowerCase().trim().split(" "));

    for (Card card : cards) {
      if (card.toString().startsWith(cardName)) {
        resultCard = card.toString();
      }
    }

    return resultCard;
  }

  private String crawlSecondaryImages(JSONObject model, String primaryImage) {
    String secondaryImages = null;

    JSONArray secondaryImagesArray = new JSONArray();

    if (model.has("MediaGroups") && !model.isNull("MediaGroups")) {
      JSONArray mediaGroups = model.getJSONArray("MediaGroups");

      for (Object object : mediaGroups) {
        String secondaryImage = null;
        JSONObject media = (JSONObject) object;

        if (media.has("Large") && !media.isNull("Large")) {
          JSONObject large = media.getJSONObject("Large");

          if (large.has("MediaPath")) {
            secondaryImage = CrawlerUtils.completeUrl(large.getString("MediaPath"), "https", "d296pbmv9m7g8v.cloudfront.net");

          }
        } else if (media.has("Medium") && !media.isNull("Medium")) {
          JSONObject medium = media.getJSONObject("Medium");

          if (medium.has("MediaPath")) {
            secondaryImage = CrawlerUtils.completeUrl(medium.getString("MediaPath"), "https", "d296pbmv9m7g8v.cloudfront.net");

          }

        } else if (media.has("Small") && !media.isNull("Medium")) {
          JSONObject small = media.getJSONObject("Small");

          if (small.has("MediaPath")) {
            secondaryImage = CrawlerUtils.completeUrl(small.getString("MediaPath"), "https", "d296pbmv9m7g8v.cloudfront.net");

          }
        }

        if (secondaryImage != null && !secondaryImage.equals(primaryImage)) {
          secondaryImagesArray.put(secondaryImage);
        }
      }
    }

    if (secondaryImagesArray.length() > 1) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private String crawlPrimaryImage(JSONObject model) {
    String primaryImage = null;

    if (model.has("MediaGroups") && !model.isNull("MediaGroup")) {
      JSONArray mediaGroups = model.getJSONArray("MediaGroups");

      for (Object object : mediaGroups) {
        JSONObject media = (JSONObject) object;

        if (media.has("Large") && !media.isNull("Large")) {
          JSONObject large = media.getJSONObject("Large");

          if (large.has("MediaPath") && !large.isNull("MediaPath")) {
            primaryImage = CrawlerUtils.completeUrl(large.getString("MediaPath"), "https", "d296pbmv9m7g8v.cloudfront.net");
            break;

          }
        } else if (media.has("Medium") && !media.isNull("Medium")) {
          JSONObject medium = media.getJSONObject("Medium");

          if (medium.has("MediaPath") && !medium.isNull("MediaPath")) {
            primaryImage = CrawlerUtils.completeUrl(medium.getString("MediaPath"), "https", "d296pbmv9m7g8v.cloudfront.net");
            break;

          }

        } else if (media.has("Small") && !media.has("Small")) {
          JSONObject small = media.getJSONObject("Small");

          if (small.has("MediaPath") && !small.isNull("MediaPath")) {
            primaryImage = CrawlerUtils.completeUrl(small.getString("MediaPath"), "https", "d296pbmv9m7g8v.cloudfront.net");
            break;

          }
        }
      }
    }
    return primaryImage;
  }

  private String crawlName(JSONObject sku) {
    String name = null;

    if (sku.has("Name")) {
      name = sku.getString("Name");

      if (sku.has("SKUOptions") && name != null) {
        JSONArray options = sku.getJSONArray("SKUOptions");

        for (Object object : options) {
          JSONObject opt = (JSONObject) object;

          if (opt.has("Title")) {
            name = name.concat(" ").concat(opt.getString("Title"));
          }
        }
      }
    }

    return name;
  }


  private Float crawlPrice(JSONObject json) {
    Float price = null;

    if (json.has("Price")) {
      JSONObject priceJson = json.getJSONObject("Price");

      if (priceJson.has("BestInstallment")) {
        JSONObject bestInstallment = priceJson.getJSONObject("BestInstallment");
        price = CrawlerUtils.getFloatValueFromJSON(bestInstallment, "InstallmentPrice");

      }
    }

    return price;
  }

  private boolean crawlAvailability(JSONObject sku) {
    boolean availability = false;

    if (sku.get("Availability").toString().equals("I")) {
      availability = true;
    }

    return availability;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".x-product-top-main") != null;
  }

  private CategoryCollection crawlCategories(JSONObject model) {
    CategoryCollection categories = new CategoryCollection();

    if (model.has("Navigation")) {
      JSONArray navigation = model.getJSONArray("Navigation");

      for (Object object : navigation) {

        JSONObject cat = (JSONObject) object;
        if (cat.has("Text")) {
          categories.add(cat.getString("Text"));
        }
      }
    }

    return categories;
  }

  private String crawlDesciption(JSONObject model) {
    String finalDescription = "";

    if (model.has("Descriptions")) {
      JSONArray descriptions = model.getJSONArray("Descriptions");

      for (Object object : descriptions) {
        JSONObject description = (JSONObject) object;

        if (description.has("Alias")) {
          String alias = description.getString("Alias");

          if (alias.equals("LongDescription") && description.has("Title") && description.has("Value")) {
            finalDescription = finalDescription.concat(description.getString("Title")).concat(" ").concat(description.getString("Value"));
          }

          if (alias.equals("Specifications") && description.has("Title") && description.has("Value")) {
            finalDescription = finalDescription.concat(description.getString("Title")).concat(" ").concat(description.getString("Value"));
          }
        }
      }
    }

    return finalDescription;
  }
}
