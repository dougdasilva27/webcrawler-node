package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
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
import exceptions.OfferException;
import models.Marketplace;
import models.Offer;
import models.Offer.OfferBuilder;
import models.Offers;
import models.Seller;
import models.Util;
import models.prices.Prices;

/**
 * Date: 10/07/2019
 * 
 * @author gabriel
 *
 */
public class BrasilJcdistribuicaoCrawler extends Crawler {

  private static final String HOME_PAGE = "https://jcdistribuicao.maxb2b.com.br/";
  private static final String API_URL = "https://api.heylabs.io/graphql";
  private static final String SELLER_NAME = "JC Distribuição";
  private static final String PRICE_KEY = "pricePerUnit";
  private static final String COMPANY_ID = "6486bb697694a5aed9c0b7729734372f";

  // In this market api, they have seven keys for description like this: "description1": "",
  // "description2": "" ...
  // this variable informs the numbber of descriptions number on api
  private static final Integer DESCRIPTIONS_NUMBER = 7;

  public BrasilJcdistribuicaoCrawler(Session session) {
    super(session);
    super.config.setFetcher(FetchMode.FETCHER);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  protected Object fetch() {
    JSONObject productsInfo = new JSONObject();

    String path = session.getOriginalURL().replace(HOME_PAGE, "");

    // url must be in this format:
    // https://jcdistribuicao.maxb2b.com.br/{subsidiaryId}/search/{ean}?id={id}
    if (path.contains("?id=")) {
      String[] parametersPath = path.split("/");
      String subsidiaryId = parametersPath[0];

      // string ids must be this part of path: {ean}?id={id} -> [{ean}, id={id}],
      // so we need to transform to {id}-{ean}
      String[] ids = CommonMethods.getLast(parametersPath).split("\\?");
      String id = CommonMethods.getLast(CommonMethods.getLast(ids).split("&")[0].split("=")) + "-" + ids[0];

      JSONObject payload = new JSONObject();
      payload.put("operationName", "product");

      // this field was extracted on api call, this will not change
      payload.put("query", "query product($id: String!, $subsidiaryId: String, $paymentPlanId: String, $billingTypeId: String) "
          + "{\n  product(id: $id, subsidiaryId: $subsidiaryId, paymentPlanId: $paymentPlanId, billingTypeId: $billingTypeId) "
          + "{\n    ...FullProduct\n    __typename\n  }\n}\n\nfragment FullProduct on Product {\n  ...BasicProduct\n  "
          + "description1\n  description2\n  description3\n  description4\n  description5\n  description6\n  description7\n"
          + "  techInfo\n  notes\n  wholesaleQuantity\n  similar {\n    ...BasicProduct\n    __typename\n  }\n  section "
          + "{\n    id\n    name\n    __typename\n  }\n  subsection {\n    id\n    name\n    __typename\n  }\n  __typename\n}"
          + "\n\nfragment BasicProduct on Product {\n  id\n  code\n  barCode\n  isAvailable\n  name\n  image\n  price\n  "
          + "formerPrice\n  pricePerUnit\n  showStock\n  validateStock\n  quantityInStock\n  wrapper\n  unit\n  multiplier\n  "
          + "factoryCode\n  originalCode\n  hasCampaign\n  campaignMessage\n  campaignCode\n  __typename\n}\n");

      JSONObject variables = new JSONObject();
      variables.put("id", id);
      variables.put("subsidiaryId", subsidiaryId);

      payload.put("variables", variables);

      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
      headers.put("CompanyId", COMPANY_ID);

      Request request = RequestBuilder.create()
          .setUrl(API_URL)
          .setCookies(cookies)
          .setPayload(payload.toString())
          .setHeaders(headers)
          .mustSendContentEncoding(false)
          .build();

      JSONObject apiResponse = CrawlerUtils.stringToJson(this.dataFetcher.post(session, request).getBody());
      if (apiResponse.has("data") && apiResponse.get("data") instanceof JSONObject) {
        JSONObject data = apiResponse.getJSONObject("data");

        if (data.has("product") && data.get("product") instanceof JSONObject) {
          productsInfo = data.getJSONObject("product");
        }
      }
    }

    return productsInfo;
  }

  @Override
  public List<Product> extractInformation(JSONObject jsonSku) throws Exception {
    super.extractInformation(jsonSku);
    List<Product> products = new ArrayList<>();
    String productUrl = session.getOriginalURL();

    if (isProductPage(jsonSku)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(jsonSku);
      String internalPid = crawlInternalPid(jsonSku);
      String description = crawlDescription(jsonSku);
      CategoryCollection categories = crawlCategories(jsonSku);
      boolean available = crawlAvailability(jsonSku);
      Float price = available ? CrawlerUtils.getFloatValueFromJSON(jsonSku, PRICE_KEY) : null;
      String primaryImage = crawlPrimaryImage(jsonSku);
      String name = crawlName(jsonSku);
      Prices prices = crawlPrices(price);
      Marketplace marketplace = crawlMarketplace(price, prices, jsonSku);
      List<String> eans = scrapEan(jsonSku);
      Offers offers = scrapBuyBox(jsonSku);

      // Creating the product
      Product product = ProductBuilder.create()
          .setUrl(productUrl)
          .setInternalId(internalId)
          .setInternalPid(internalPid)
          .setName(name)
          .setPrices(prices)
          .setPrice(price)
          .setAvailable(available)
          .setPrimaryImage(primaryImage)
          .setDescription(description)
          .setCategory1(categories.getCategory(0))
          .setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2))
          .setMarketplace(marketplace)
          .setEans(eans)
          .setOffers(offers)
          .build();

      products.add(product);
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private Offers scrapBuyBox(JSONObject jsonSku) {
    Offers offers = new Offers();
    try {
      String slugSellerName = CrawlerUtils.toSlug(SELLER_NAME);

      Offer offer = new OfferBuilder().setSellerFullName(SELLER_NAME).setSlugSellerName(slugSellerName).setInternalSellerId(slugSellerName)
          .setMainPagePosition(1).setIsBuybox(false).setMainPrice(CrawlerUtils.getDoubleValueFromJSON(jsonSku, PRICE_KEY, false, false)).build();

      offers.add(offer);
    } catch (OfferException e) {
      Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
    }

    return offers;
  }

  private boolean isProductPage(JSONObject jsonSku) {
    return jsonSku.length() > 0;
  }

  private List<String> scrapEan(JSONObject jsonSku) {
    List<String> eans = new ArrayList<>();

    if (jsonSku.has("barCode") && !jsonSku.isNull("barCode")) {
      eans.add(jsonSku.get("barCode").toString());
    }

    return eans;
  }

  private CategoryCollection crawlCategories(JSONObject jsonSku) {
    CategoryCollection categories = new CategoryCollection();

    List<String> categoriesKeys = Arrays.asList("section", "subsection");
    for (String key : categoriesKeys) {
      if (jsonSku.has(key) && jsonSku.get(key) instanceof JSONObject) {
        JSONObject categoryJson = jsonSku.getJSONObject(key);

        if (categoryJson.has("name") && !categoryJson.isNull("name")) {
          categories.add(categoryJson.get("name").toString());
        }
      }
    }

    return categories;
  }

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("id") && !json.isNull("id")) {
      internalId = json.get("id").toString();
    }

    return internalId;
  }


  private String crawlInternalPid(JSONObject json) {
    String internalPid = null;

    if (json.has("factoryCode") && !json.isNull("factoryCode")) {
      internalPid = json.get("factoryCode").toString();
    }

    return internalPid;
  }

  private String crawlName(JSONObject json) {
    String name = null;

    if (json.has("name") && json.get("name") instanceof String) {
      name = json.getString("name");
    }

    return name;
  }

  private boolean crawlAvailability(JSONObject json) {
    return json.has("isAvailable") && json.get("isAvailable") instanceof Boolean && json.getBoolean("isAvailable");
  }

  private String crawlPrimaryImage(JSONObject json) {
    String primaryImage = null;

    if (json.has("image") && json.get("image") instanceof String) {
      primaryImage = CrawlerUtils.completeUrl(json.getString("image"), "https", "jcdistribuicaoadmin.maxb2b.com.br");
    }

    return primaryImage;
  }

  private String crawlDescription(JSONObject json) {
    StringBuilder description = new StringBuilder();

    for (int i = 1; i <= DESCRIPTIONS_NUMBER; i++) {
      String key = "description" + i;
      if (json.has(key) && !json.isNull(key)) {
        description.append(json.get(key) + " ");
      }
    }

    return description.toString().trim();
  }

  private Marketplace crawlMarketplace(Float price, Prices prices, JSONObject jsonSku) {
    Marketplace marketplace = new Marketplace();

    if (price != null && jsonSku.has("store_name")) {
      String sellerName = jsonSku.get("store_name").toString().replace("null", "").trim();

      if (!sellerName.isEmpty()) {

        try {
          Seller seller = new Seller(new JSONObject().put("price", price).put("prices", prices.toJSON()).put("name", sellerName));
          marketplace.add(seller);
        } catch (Exception e) {
          Logging.printLogError(logger, session, Util.getStackTraceString(e));
        }
      }
    }

    return marketplace;
  }

  /**
   * In this site has no information of installments
   * 
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price) {
    Prices p = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();
      installmentPriceMap.put(1, price);
      p.setBankTicketPrice(price);

      p.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
    }

    return p;
  }
}
