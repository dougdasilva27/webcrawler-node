package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
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
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

public class ColombiaMerqueoCrawler extends Crawler {

  public ColombiaMerqueoCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
    JSONObject apiJson = scrapApiJson(session.getOriginalURL());
    JSONObject data = new JSONObject();

    if (apiJson.has("data") && !apiJson.isNull("data")) {
      data = apiJson.getJSONObject("data");
    }

    if (isProductPage(data)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(data);
      String name = crawlName(data);
      Float price = crawlPrice(data);
      boolean available = crawlAvailable(data);

      CategoryCollection categories = crawlCategories(data);
      Prices prices = crawlPrices(price);
      String primaryImage = crawlPrimaryImage(data);
      String secondaryImages = crawlSecondaryImage(data, primaryImage);
      String description = crawlDescription(data);
      Integer stock = crawlStock(data);

      // Creating the product
      Product product = ProductBuilder.create()
          .setUrl(session.getOriginalURL())
          .setInternalId(internalId)
          .setName(name)
          .setPrice(price)
          .setPrices(prices)
          .setAvailable(available)
          .setCategory1(categories.getCategory(0))
          .setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2))
          .setPrimaryImage(primaryImage)
          .setSecondaryImages(secondaryImages)
          .setDescription(description)
          .setMarketplace(new Marketplace())
          .setStock(stock)
          .build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private Integer crawlStock(JSONObject data) {
    Integer stock = null;

    if (data.has("quantity")) {
      stock = MathUtils.parseInt(data.get("quantity").toString());
    }

    return stock;
  }

  private CategoryCollection crawlCategories(JSONObject data) {
    CategoryCollection categories = new CategoryCollection();
    JSONObject shelf = new JSONObject();

    if (data.has("shelf") && !data.isNull("shelf")) {
      shelf = data.getJSONObject("shelf");
      if (shelf.has("name") && !shelf.isNull("name")) {
        categories.add(shelf.getString("name"));
      }
    }

    if (data.has("department") && !data.isNull("department")) {
      shelf = data.getJSONObject("department");
      if (shelf.has("name") && !shelf.isNull("name")) {
        categories.add(shelf.getString("name"));
      }
    }

    return categories;
  }

  private String crawlDescription(JSONObject data) {
    String description = null;

    if (data.has("description") && !data.isNull("description")) {
      description = data.getString("description");
    }

    return description;
  }

  private String crawlPrimaryImage(JSONObject data) {
    String primaryImage = null;

    if (data.has("imageLargeUrl") && !data.isNull("imageLargeUrl")) {
      primaryImage = data.getString("imageLargeUrl");

    } else if (data.has("imageMediumUrl") && !data.isNull("imageMediumUrl")) {
      primaryImage = data.getString("imageMediumUrl");

    } else if (data.has("imageSmallUrl") && !data.isNull("imageSmallUrl")) {
      primaryImage = data.getString("imageSmallUrl");

    }

    return primaryImage;
  }
  
  private String crawlSecondaryImage(JSONObject data, String primaryImage) {
	    String secondaryImage = null;
	    JSONArray jsonArrImg = null;
	    
	    if(data.has("images") && !data.isNull("images") && data.get("images") instanceof JSONArray)  
	    	jsonArrImg = data.getJSONArray("images");
	    else
	    	jsonArrImg = new JSONArray();
	    System.err.println(jsonArrImg);
	    for(int i = 0; i < jsonArrImg.length();i++ ) {
	    	JSONObject jsonObjImg = (jsonArrImg.get(i) != null && jsonArrImg.get(i) instanceof JSONObject) ? jsonArrImg.getJSONObject(i): new JSONObject();
	    	System.err.println(jsonObjImg);
	    	if (jsonObjImg.has("imageLargeUrl") && !jsonObjImg.isNull("imageLargeUrl") && !jsonObjImg.get("imageLargeUrl").equals(primaryImage)) {
		    	secondaryImage = jsonObjImg.getString("imageLargeUrl");

		    } else if (jsonObjImg.has("imageMediumUrl") && !jsonObjImg.isNull("imageMediumUrl") && !jsonObjImg.getString("imageMediumUrl").equals(primaryImage)) {
		    	secondaryImage = jsonObjImg.getString("imageMediumUrl");

		    } else if (jsonObjImg.has("imageSmallUrl") && !jsonObjImg.isNull("imageSmallUrl") && !jsonObjImg.getString("imageSmallUrl").equals(primaryImage)) {
		    	secondaryImage = jsonObjImg.getString("imageSmallUrl");
		    }

	    }
	    
	    return secondaryImage;
	  }

  private boolean crawlAvailable(JSONObject data) {
    boolean availability = false;

    if (data.has("availability") && !data.isNull("availability")) {
      availability = data.getBoolean("availability");
    }

    return availability;
  }

  private Float crawlPrice(JSONObject data) {
    Float price = null;

    if (data.has("price") && !data.isNull("price")) {
      price = CrawlerUtils.getFloatValueFromJSON(data, "price");
    }

    return price;
  }

  private String crawlName(JSONObject data) {
    String name = null;

    if (data.has("name") && !data.isNull("name")) {
      name = data.getString("name");
    }

    return name;
  }

  private JSONObject scrapApiJson(String originalURL) {
    List<String> slugs = scrapSlugs(originalURL);

    String apiUrl =
        "https://merqueo.com/api/2.0/stores/63/find?department_slug=" + slugs.get(1)
            + "&shelf_slug=" + slugs.get(2)
            + "&product_slug=" + slugs.get(3)
            + "&limit=7";

    Request request = RequestBuilder
        .create()
        .setUrl(apiUrl)
        .mustSendContentEncoding(false)
        .build();

    return CrawlerUtils.stringToJson(new FetcherDataFetcher().get(session, request).getBody());
  }

  /*
   * Url exemple:
   * https://merqueo.com/bogota/aseo-del-hogar/detergentes/ariel-concentrado-doble-poder-detergente-la
   * -quido-2-lt
   */
  private List<String> scrapSlugs(String originalURL) {
    List<String> slugs = new ArrayList<>();
    String[] slug = originalURL.split("/");

    for (int i = 3; i < slug.length; i++) {
      slugs.add(slug[i]);
    }

    return slugs;
  }

  private boolean isProductPage(JSONObject data) {
    return data.has("id");
  }

  private String crawlInternalId(JSONObject data) {
    String internalId = null;

    if (data.has("id") && !data.isNull("id")) {
      internalId = data.get("id").toString();
    }

    return internalId;
  }

  /**
   * In the time when this crawler was made, this market hasn't installments informations
   * 
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMapShop = new HashMap<>();
      installmentPriceMapShop.put(1, price);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMapShop);
    }

    return prices;
  }
}
