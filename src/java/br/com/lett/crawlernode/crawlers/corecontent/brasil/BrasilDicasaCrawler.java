package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.prices.Prices;

public class BrasilDicasaCrawler extends Crawler {

  public BrasilDicasaCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {

      Elements variations = doc.select(".variations-wrapper span");
      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".produtoid", "value");
      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, "b[itemprop=\"price\"]", "content", false, '.', session);
      Prices prices = scrapPrices(doc, price);
      boolean available = !CrawlerUtils.scrapStringSimpleInfo(doc, ".availability", false).equalsIgnoreCase("Esgotado");
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb span[itemprop=\"title\"]", true);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".information-descriptions"));

      for (Element element : variations) {

        String internalId = scrapInternalId(internalPid, element);
        String name = scrapName(doc, element);

        String primaryImage = scrapPrimaryImage(doc, internalId, internalPid);
        String secondaryImages = scrapSecondaryImages(doc, internalId, internalPid, primaryImage);
        // Creating the product
        Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
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
            .setEans(new ArrayList<String>())
            .build();

        products.add(product);
      }
    }
    return products;
  }

  private JSONObject getImagesJsonObject(Document doc, String internalPid) {
    return CrawlerUtils.selectJsonFromHtml(doc, ".validate script", "varvariations" + internalPid
        + "=", null, true, false);
  }

  private String scrapSecondaryImages(Document doc, String internalId, String internalPid, String primaryImage) {
    JSONObject jsonImages = getImagesJsonObject(doc, internalPid);
    String keyVariation = "var" + internalId.replace(internalPid + "-", "");
    JSONArray secondaryImages = new JSONArray();

    if (jsonImages.has(keyVariation) && !jsonImages.isNull(keyVariation)) {
      JSONObject variation = jsonImages.getJSONObject(keyVariation);

      if (variation.has("images") && !variation.isNull("images")) {
        JSONObject images = variation.getJSONObject("images");

        if (images.has("z") && !images.isNull("z")) {
          JSONArray img = images.getJSONArray("z");

          for (Object object : img) {
            String url = CrawlerUtils.completeUrl((String) object, "https", "www.dicasashop.com.br");

            if (!url.equals(primaryImage)) {
              secondaryImages.put(url);
            }

          }

        } else if (images.has("g") && !images.isNull("g")) {
          JSONArray img = images.getJSONArray("g");
          for (Object object : img) {
            String url = CrawlerUtils.completeUrl((String) object, "https", "www.dicasashop.com.br");

            if (!url.equals(primaryImage)) {
              secondaryImages.put(url);
            }
          }

        } else if (images.has("e") && !images.isNull("e")) {
          JSONArray img = images.getJSONArray("e");

          for (Object object : img) {
            String url = CrawlerUtils.completeUrl((String) object, "https", "www.dicasashop.com.br");

            if (!url.equals(primaryImage)) {
              secondaryImages.put(url);
            }

          }
        } else if (images.has("p") && !images.isNull("p")) {
          JSONArray img = images.getJSONArray("p");

          for (Object object : img) {
            String url = CrawlerUtils.completeUrl((String) object, "https", "www.dicasashop.com.br");

            if (!url.equals(primaryImage)) {
              secondaryImages.put(url);
            }

          }
        }
      }
    }

    return secondaryImages.toString();
  }

  private String scrapPrimaryImage(Document doc, String internalId, String internalPid) {
    JSONObject jsonImages = getImagesJsonObject(doc, internalPid);
    String keyVariation = "var" + internalId.replace(internalPid + "-", "");
    String primaryImage = null;

    if (jsonImages.has(keyVariation) && !jsonImages.isNull(keyVariation)) {
      JSONObject variation = jsonImages.getJSONObject(keyVariation);

      if (variation.has("images") && !variation.isNull("images")) {
        JSONObject images = variation.getJSONObject("images");

        if (images.has("z") && !images.isNull("z")) {
          JSONArray img = images.getJSONArray("z");

          for (Object object : img) {
            primaryImage = CrawlerUtils.completeUrl((String) object, "https", "www.dicasashop.com.br");
            break;
          }

        } else if (images.has("g") && !images.isNull("g")) {
          JSONArray img = images.getJSONArray("g");

          for (Object object : img) {
            primaryImage = CrawlerUtils.completeUrl((String) object, "https", "www.dicasashop.com.br");
            break;
          }

        } else if (images.has("e") && !images.isNull("e")) {
          JSONArray img = images.getJSONArray("e");

          for (Object object : img) {
            primaryImage = CrawlerUtils.completeUrl((String) object, "https", "www.dicasashop.com.br");
            break;
          }

        } else if (images.has("p") && !images.isNull("p")) {
          JSONArray img = images.getJSONArray("p");
          for (Object object : img) {
            primaryImage = CrawlerUtils.completeUrl((String) object, "https", "www.dicasashop.com.br");
            break;
          }
        }
      }
    }
    return primaryImage;
  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".product-contents").isEmpty();
  }

  private Prices scrapPrices(Document doc, Float price) {
    Prices prices = new Prices();
    Double bankTicketPrice = null;
    Map<Integer, Float> installmentPriceMap = new TreeMap<>();
    Pair<Integer, Float> pairInstallment = CrawlerUtils.crawlSimpleInstallment(".product-contents .content .price .condition", doc, false, "x");

    if (!pairInstallment.isAnyValueNull()) {
      installmentPriceMap.put(pairInstallment.getFirst(), pairInstallment.getSecond());
    }

    Element bankTicketPriceElement = doc.selectFirst(".product-contents .content .price .savings b:first-child");

    if (bankTicketPriceElement != null) {
      bankTicketPrice = MathUtils.parseDoubleWithComma(bankTicketPriceElement.text());
    }

    prices.setBankTicketPrice(bankTicketPrice);
    prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
    prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
    prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
    prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);

    return prices;
  }

  private String scrapName(Document doc, Element element) {
    String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".item span[itemprop=\"name\"]", false);
    String brand = CrawlerUtils.scrapStringSimpleInfo(doc, ".brand span[itemprop=\"name\"]", false);
    String variation = CrawlerUtils.scrapStringSimpleInfoByAttribute(element, "label", "title");
    StringBuilder nameResult = new StringBuilder();
    nameResult.append(name);

    if (brand != null) {
      nameResult.append(" ");
      nameResult.append(brand);
      nameResult.append(" ");
    }

    if (variation != null) {
      nameResult.append(" ");
      nameResult.append(variation);
    }

    return nameResult.toString();
  }

  private String scrapInternalId(String internalPid, Element element) {
    return internalPid + "-" + CrawlerUtils.scrapStringSimpleInfoByAttribute(element, "input[type=radio]", "value");
  }

}
