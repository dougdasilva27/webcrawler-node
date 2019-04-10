package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
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
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXCrawlersUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.prices.Prices;

public class BrasilPoloarCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.poloar.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "poloar ar condicionado";

  public BrasilPoloarCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  /**
   * Installments Problem:
   * 
   * <div class="is-hidden" id="installments-qty"> <span>
   * 
   * 5,5
   * 
   * </span>
   * 
   * Obs: não tirar esse <span></span> apenas alterar a quantidade de parcelas e porcentagem máxima de
   * Desconto
   * 
   * Explicação rápida: Porcentagem máxima de Desconto, Quantidade de Parcelas com Desconto Exemplo:
   * quero que em 1x seja 5% de desconto o primeiro valor será 5, após isso separe esse 5 com virgula
   * '5,' e informe até quantas vezes ainda terá desconto tendo em mente que cada parcela tirará 1% do
   * desconto. Se você colocar 5,5 ficará assim: 1x 5% 2x 4% 3x 3% ... até ficar 5x 1% e parar, se
   * você colocar 5,3 ficara apenas até o 3x 3%</div>
   */

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies, dataFetcher);
      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

      String internalPid = vtexUtil.crawlInternalPid(skuJson);
      CategoryCollection categories = crawlCategories(doc);

      // sku data in json
      JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

      // ean data in json
      JSONArray arrayEan = CrawlerUtils.scrapEanFromVTEX(doc);

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = vtexUtil.crawlInternalId(jsonSku);
        JSONObject apiJSON = vtexUtil.crawlApi(internalId);
        String name = vtexUtil.crawlName(jsonSku, skuJson);
        Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, false);
        Marketplace marketplace = vtexUtil.assembleMarketplaceFromMap(marketplaceMap);
        boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER);
        String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
        String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
        Prices prices = scrapPrices(apiJSON, doc);
        Float price = vtexUtil.crawlMainPagePrice(prices);
        Integer stock = vtexUtil.crawlStock(apiJSON);
        String finalUrl =
            internalId != null && !internalId.isEmpty() ? CrawlerUtils.crawlFinalUrl(session.getOriginalURL(), session) : session.getOriginalURL();

        String ean = i < arrayEan.length() ? arrayEan.getString(i) : null;

        List<String> eans = new ArrayList<>();
        eans.add(ean);

        String description = crawlDescription(doc);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(finalUrl).setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price)
            .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
            .setStock(stock).setMarketplace(marketplace).setEans(eans).build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }


  private Prices scrapPrices(JSONObject apiJSON, Document doc) {
    Prices prices = new Prices();

    Float price = CrawlerUtils.getFloatValueFromJSON(apiJSON, "Price");

    if (price != null) {
      Map<Integer, Float> installmentMap = new TreeMap<>();
      installmentMap.put(1, price);

      prices.setBankTicketPrice(price);
      prices.setPriceFrom(CrawlerUtils.getDoubleValueFromJSON(apiJSON, "ListPrice"));

      Integer installmentNumber = CrawlerUtils.getIntegerValueFromJSON(apiJSON, "BestInstallmentNumber", null);

      Pair<Integer, Integer> discount = scrapDiscountWithParcels(doc);
      if (!discount.isAnyValueNull()) {

        for (int i = 1; i <= installmentNumber; i++) {
          Integer discountPrice = discount.getFirst();
          Integer numberInstallmentWithDiscount = discount.getSecond();

          if (i <= numberInstallmentWithDiscount) {
            Float priceWithDiscount = MathUtils.normalizeTwoDecimalPlaces(((price - (price * (discountPrice - (i - 1)) / 100f)) / i));

            installmentMap.put(i, priceWithDiscount);

            if (i == 1) {
              prices.setBankTicketPrice(priceWithDiscount);

            }
          } else {
            installmentMap.put(i, MathUtils.normalizeTwoDecimalPlaces(price / i));
          }
        }
      } else {
        installmentMap.put(installmentNumber, MathUtils.normalizeTwoDecimalPlaces(price / installmentNumber));
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentMap);
    }

    return prices;
  }

  private Pair<Integer, Integer> scrapDiscountWithParcels(Document doc) {
    Pair<Integer, Integer> discount = new Pair<>();
    Element installmentDiscount = doc.selectFirst("#installments-qty span");

    if (installmentDiscount != null && !installmentDiscount.text().isEmpty()) {
      String installmentDiscountString = installmentDiscount.text();

      if (installmentDiscountString.contains(",")) {
        String[] separatedDiscount = installmentDiscountString.split(",");

        if (separatedDiscount.length > 1) {
          String discountNumber = separatedDiscount[0].replaceAll("[^0-9]", "");
          String discountParcels = separatedDiscount[1].replaceAll("[^0-9]", "");

          if (!discountParcels.isEmpty() && !discountNumber.isEmpty()) {
            discount.set(Integer.parseInt(discountNumber), Integer.parseInt(discountParcels));
          }
        }
      }
    }

    return discount;
  }

  private boolean isProductPage(Document document) {
    return document.select(".productName").first() != null;
  }

  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select(".bread-crumb li > a");

    for (int i = 1; i < elementCategories.size(); i++) { // first item is the home page
      categories.add(elementCategories.get(i).text().trim());
    }

    return categories;
  }

  private String crawlDescription(Document document) {
    String description = "";

    Element descElement = document.select("#detalhes-do-produto").first();
    Element specElement = document.select("#especificacoes-tecnicas").first();

    if (specElement != null) {
      description = description + specElement.html();
    }
    if (descElement != null) {
      description = description + descElement.html();
    }

    return description;
  }
}
