package br.com.lett.crawlernode.crawlers.corecontent.portoalegre;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

public class PortoalegrePetshopmolecaoCrawler extends Crawler {


  private static final String PRICE_API_PARAMETER = "variant_price";
  private static final String IMAGES_API_PARAMETER = "variant_gallery";

  private static final String IMAGES_SELECTOR = "#carousel li a[href]:not(.cloud-zoom-gallery-video), .produto-imagem a";
  private static final String IMAGES_HOST = "#carousel li a[href]:not(.cloud-zoom-gallery-video)";
  private static final String PRICE_SELECTOR = "#variacaoPreco";

  public PortoalegrePetshopmolecaoCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      JSONObject productJson = CrawlerUtils.selectJsonFromHtml(doc, "script", "dataLayer = [", "]", false, true);

      String internalPid = productJson.has("idProduct") && !productJson.isNull("idProduct") ? productJson.get("idProduct").toString() : null;
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb div .breadcrumb-item a", true);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#descricao", /* "#caracteristicas", */ "#garantia"));
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "#title .product-title", true);

      JSONArray skus = JSONUtils.getJSONArrayValue(productJson, "listSku");
      if (skus.length() > 0) {

        for (Object obj : skus) {
          JSONObject skuJson = (JSONObject) obj;

          if (skuJson.has("idSku") && !skuJson.isNull("idSku")) {
            String internalId = skuJson.get("idSku").toString();
            String variationId = CommonMethods.getLast(internalId.split("-"));
            String variationName = skuJson.has("nameSku") && !skuJson.isNull("nameSku") ? skuJson.get("nameSku").toString() : null;

            Document docPrices = fetchVariationApi(internalPid, variationId, PRICE_API_PARAMETER);
            Document docImages = fetchVariationApi(internalPid, variationId, IMAGES_API_PARAMETER);

            boolean available = docPrices.selectFirst("#nao_disp") == null;
            Float price = CrawlerUtils.scrapFloatPriceFromHtml(docPrices, PRICE_SELECTOR, null, false, ',', session);
            Prices prices = scrapPrices(docPrices, price);
            String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(docImages, IMAGES_SELECTOR, Arrays.asList("href"), "https", IMAGES_HOST);
            String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(docImages, IMAGES_SELECTOR, Arrays.asList("href"), "https", IMAGES_HOST,
                primaryImage);

            String ean = JSONUtils.getStringValue(skuJson, "EAN");
            List<String> eans = ean != null ? Arrays.asList(ean) : null;

            // Creating the product
            Product product = ProductBuilder.create()
                .setUrl(session.getOriginalURL())
                .setInternalId(internalId)
                .setInternalPid(internalPid)
                .setName(variationName != null ? name + " " + variationName : name)
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
                .setEans(eans)
                .build();

            products.add(product);
          }
        }
      } else {

        String internalId = internalPid;
        Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, PRICE_SELECTOR, null, false, ',', session);
        Prices prices = scrapPrices(doc, price);
        String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, IMAGES_SELECTOR, Arrays.asList("href"), "https", IMAGES_HOST);
        String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, IMAGES_SELECTOR, Arrays.asList("href"), "https", IMAGES_HOST,
            primaryImage);
        boolean available = doc.selectFirst("#nao_disp") == null;
        String ean = JSONUtils.getStringValue(productJson, "EAN");
        List<String> eans = ean != null ? Arrays.asList(ean) : null;

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
            .setEans(eans)
            .build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".product-title") != null;
  }

  private Document fetchVariationApi(String internalPid, String variationId, String type) {
    Request request = RequestBuilder.create()
        .setUrl(
            "https://www.petshopmolecao.com.br/mvc/store/product/" + type + "/?loja=560844&variant_id="
                + variationId + "&product_id=" + internalPid
        )
        .setCookies(cookies)
        .build();

    return Jsoup.parse(this.dataFetcher.get(session, request).getBody());
  }

  private Prices scrapPrices(Document doc, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      prices.setBankTicketPrice(price);

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
    }

    return prices;
  }
}
