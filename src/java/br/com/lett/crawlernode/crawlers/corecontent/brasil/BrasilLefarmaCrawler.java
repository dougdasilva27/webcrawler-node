package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.prices.Prices;

public class BrasilLefarmaCrawler extends Crawler {

  public BrasilLefarmaCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    JSONObject fetchedJson = new JSONObject();
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      JSONObject script = CrawlerUtils.selectJsonFromHtml(doc, "script", "dataLayer[0]['product'] = ", ";", false, true);
      if (script.has("id")) {
        String url = "https://www.lefarma.com.br/public_api/v1/products/" + script.get("id");

        Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
        fetchedJson = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
      }

      String internalPid = crawlInternalPid(fetchedJson);
      String mainName = fetchedJson.has("name") ? fetchedJson.get("name").toString().trim() : null;
      Float price = CrawlerUtils.scrapSimplePriceFloat(doc, "span[itemprop=price]", false);
      String description = crawlDescription(fetchedJson);
      CategoryCollection categories = crawlCategories(fetchedJson);

      for (Object obj : fetchedJson.getJSONArray("variants")) {

        JSONObject jsonObject = (JSONObject) obj;

        String name = crawlName(jsonObject);

        if (mainName != null && !mainName.toLowerCase().contains(name.toLowerCase())) {
          name = mainName + " " + name;
        }

        String internalId = crawlInternalId(jsonObject);
        String primaryImage = crawlPrimaryImage(jsonObject);
        boolean available = crawlAvailability(jsonObject);
        Prices prices = crawlPrices(jsonObject);

        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(null).setDescription(description)
            .setMarketplace(null).build();

        products.add(product);

      }
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private String crawlDescription(JSONObject obj) {
    String descprition = null;

    if (obj.has("htmlDescriptions")) {
      for (Object objDescriptions : obj.getJSONArray("htmlDescriptions")) {
        JSONObject jsonObject = (JSONObject) objDescriptions;
        if (jsonObject.has("description")) {
          descprition = jsonObject.getString("description");
        }
      }
    }

    return descprition;
  }

  private CategoryCollection crawlCategories(JSONObject obj) {
    CategoryCollection categories = new CategoryCollection();
    if (obj.has("breadcrumbs")) {
      for (Object category : obj.getJSONArray("breadcrumbs")) {
        JSONObject jsonObjCategory = (JSONObject) category;
        if (jsonObjCategory.has("name")) {
          categories.add(jsonObjCategory.getString("name"));
        }
      }
    }
    return categories;
  }

  private String crawlName(JSONObject obj) {
    String name = null;

    if (obj.has("definition1Value")) {
      name = obj.getString("definition1Value").trim();
    }

    return name;
  }

  private String crawlInternalId(JSONObject obj) {
    String internalId = null;

    if (obj.has("id")) {
      internalId = obj.get("id").toString();
    }

    return internalId;
  }

  private String crawlInternalPid(JSONObject obj) {
    String internalPid = null;

    if (obj.has("erpId")) {
      internalPid = obj.getString("erpId");
    }

    return internalPid;
  }


  private String crawlPrimaryImage(JSONObject obj) {
    String primaryImage = null;

    if (obj.has("mainImage")) {
      primaryImage = obj.getString("mainImage");
    }

    return primaryImage;
  }

  private boolean crawlAvailability(JSONObject obj) {
    return obj.has("available") ? obj.getBoolean("available") : false;
  }

  private Prices crawlPrices(JSONObject obj) {
    Prices prices = new Prices();
    Map<Integer, Float> installmentPriceMap = new TreeMap<>();

    if (!obj.isNull("discountValue")) {
      if (obj.has("promotionPrice")) {
        prices.setBankTicketPrice(CrawlerUtils.getDoubleValueFromJSON(obj, "promotionPrice"));
        prices.setPriceFrom(CrawlerUtils.getDoubleValueFromJSON(obj, "price"));
      }
    } else {
      prices.setBankTicketPrice(CrawlerUtils.getDoubleValueFromJSON(obj, "price"));
    }

    if (obj.has("hasInstallmentsWithInterest") && obj.getBoolean("hasInstallmentsWithInterest")) {

      if (obj.has("quantityOfInstallmentsWithInterest") && !obj.isNull("quantityOfInstallmentsWithInterest")) {
        installmentPriceMap.put(obj.getInt("quantityOfInstallmentsWithInterest"),
            CrawlerUtils.getFloatValueFromJSON(obj, "valueOfInstallmentsWithInterest"));
      }

    } else {

      if (obj.has("quantityOfInstallmentsNoInterest") && !obj.isNull("quantityOfInstallmentsNoInterest")) {
        installmentPriceMap.put(obj.getInt("quantityOfInstallmentsNoInterest"),
            CrawlerUtils.getFloatValueFromJSON(obj, "valueOfInstallmentsNoInterest"));
      }

    }
    prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);

    return prices;
  }



  private boolean isProductPage(Element e) {
    return e.selectFirst(".produto_comprar") != null;
  }
}
