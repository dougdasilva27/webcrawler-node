package br.com.lett.crawlernode.crawlers.corecontent.peru;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
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
import models.Marketplace;
import models.prices.Prices;

public class PeruPlazaveaCrawler extends Crawler {
  public PeruPlazaveaCrawler(Session session) {
    super(session);
  }

  private static final String HOME_PAGE = "https://www.plazavea.com.pe/";
  private static final String MAIN_SELLER_NAME_LOWER = "plaza vea";

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    URL url = new URL(session.getOriginalURL());

    if (isProductPage(doc)) {
      VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, url.getProtocol() + "://" + url.getHost() + "/", cookies);

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);
      String internalPid = vtexUtil.crawlInternalPid(skuJson);

      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb li:not(:first-child) > a");
      String description =
          CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#product-caract", ".productDescription", "#product-spec .b12-product-descspec"));

      // sku data in json
      JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = vtexUtil.crawlInternalId(jsonSku);
        JSONObject apiJSON = vtexUtil.crawlApi(internalId);
        String name = vtexUtil.crawlName(jsonSku, skuJson);
        Double priceDiscount = scrapPriceDescuento(vtexUtil, internalId);
        Map<String, Prices> marketplaceMap = crawlMarketplace(apiJSON, jsonSku, doc, vtexUtil, priceDiscount);
        Marketplace marketplace = vtexUtil.assembleMarketplaceFromMap(marketplaceMap);
        boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER);
        String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
        String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);

        if (primaryImage == null) {
          primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".thumbs li a", Arrays.asList("zoom", "rel"), "https:", "plazavea.vteximg.com.br");
          secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".thumbs li a", Arrays.asList("zoom", "rel"), "https:",
              "plazavea.vteximg.com.br", primaryImage);
        }

        Prices prices = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER) ? marketplaceMap.get(MAIN_SELLER_NAME_LOWER) : new Prices();
        Float price = vtexUtil.crawlMainPagePrice(prices);
        Integer stock = vtexUtil.crawlStock(apiJSON);

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

  private boolean isProductPage(Document document) {
    return document.selectFirst(".productName") != null;
  }

  private Double scrapPriceDescuento(VTEXCrawlersUtils vtexUtil, String internalId) {
    Double discountPrice = null;

    JSONObject descApi = vtexUtil.crawlDescriptionAPI(internalId, "skuId");

    if (descApi.has("items")) {
      JSONArray items = descApi.getJSONArray("items");

      if (items.length() > 0) {
        JSONObject item = items.getJSONObject(0);

        if (item.has("sellers")) {
          JSONArray sellers = item.getJSONArray("sellers");

          if (sellers.length() > 0) {
            JSONObject seller = sellers.getJSONObject(0);

            if (seller.has("commertialOffer")) {
              JSONObject commertialOffer = seller.getJSONObject("commertialOffer");

              if (commertialOffer.has("Teasers")) {
                JSONArray teasers = commertialOffer.getJSONArray("Teasers");

                if (teasers.length() > 0) {
                  JSONObject teaser = teasers.getJSONObject(0);

                  if (teaser.has("<Effects>k__BackingField")) {
                    JSONObject bkField = teaser.getJSONObject("<Effects>k__BackingField");

                    if (bkField.has("<Parameters>k__BackingField")) {
                      JSONArray pBkFields = bkField.getJSONArray("<Parameters>k__BackingField");

                      if (pBkFields.length() > 0) {
                        JSONObject pBkField = pBkFields.getJSONObject(0);

                        if (pBkField.has("<Value>k__BackingField")) {
                          String text = pBkField.get("<Value>k__BackingField").toString().replaceAll("[^0-9]", "");

                          if (text.length() > 1) {
                            String[] chars = text.split("");
                            String priceLastPart = chars[chars.length - 2] + chars[chars.length - 1];
                            String priceFirstPart = text.replace(priceLastPart, "").trim();
                            String priceFinal = (priceFirstPart.isEmpty() ? "0" : priceFirstPart) + "." + priceLastPart;

                            discountPrice = Double.parseDouble(priceFinal);
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    return discountPrice;
  }

  public Map<String, Prices> crawlMarketplace(JSONObject apiJson, JSONObject skuJson, Document doc, VTEXCrawlersUtils vtexUtil,
      Double priceDiscount) {
    Map<String, Prices> marketplace = new HashMap<>();

    if (apiJson.has(VTEXCrawlersUtils.SELLERS_INFORMATION)) {
      JSONArray sellers = apiJson.getJSONArray(VTEXCrawlersUtils.SELLERS_INFORMATION);

      for (Object s : sellers) {
        JSONObject seller = (JSONObject) s;

        if (seller.has(VTEXCrawlersUtils.SELLER_NAME) && seller.has(VTEXCrawlersUtils.SELLER_PRICE)
            && seller.has(VTEXCrawlersUtils.SELLER_AVAILABLE_QUANTITY)
            && seller.get(VTEXCrawlersUtils.SELLER_AVAILABLE_QUANTITY) instanceof Integer) {

          if (seller.getInt(VTEXCrawlersUtils.SELLER_AVAILABLE_QUANTITY) < 1) {
            continue;
          }

          String nameSeller = seller.getString(VTEXCrawlersUtils.SELLER_NAME).toLowerCase().trim();
          Object priceObject = seller.get(VTEXCrawlersUtils.SELLER_PRICE);
          boolean isDefaultSeller = seller.has(VTEXCrawlersUtils.IS_DEFAULT_SELLER)
              && seller.get(VTEXCrawlersUtils.IS_DEFAULT_SELLER) instanceof Boolean && seller.getBoolean(VTEXCrawlersUtils.IS_DEFAULT_SELLER);

          if (priceObject instanceof Double) {
            marketplace.put(nameSeller, crawlPrices(MathUtils.normalizeTwoDecimalPlaces(((Double) priceObject).floatValue()), apiJson,
                isDefaultSeller, doc, vtexUtil, priceDiscount));
          } else if (priceObject instanceof Integer) {
            marketplace.put(nameSeller, crawlPrices(MathUtils.normalizeTwoDecimalPlaces(((Integer) priceObject).floatValue()), apiJson,
                isDefaultSeller, doc, vtexUtil, priceDiscount));
          }
        }
      }
    } else if (skuJson.has("bestPriceFormated")) {
      String nameSeller = skuJson.has("seller") ? skuJson.get("seller").toString().toLowerCase().trim() : MAIN_SELLER_NAME_LOWER;
      Float priceSeller = CrawlerUtils.getFloatValueFromJSON(skuJson, "bestPriceFormated");

      marketplace.put(nameSeller, crawlPrices(priceSeller, skuJson, true, doc, vtexUtil, priceDiscount));
    }

    return marketplace;
  }

  public Prices crawlPrices(Float price, JSONObject jsonSku, boolean marketplace, Document doc, VTEXCrawlersUtils vtexUtil,
      Double priceWithDiscount) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();
      installmentPriceMap.put(1, price);

      Double priceFrom = vtexUtil.crawlPriceFrom(jsonSku);
      if (priceFrom == null) {
        priceFrom = CrawlerUtils.getDoubleValueFromJSON(jsonSku, "listPriceFormated");
      }

      prices.setPriceFrom(priceFrom);

      if (marketplace && jsonSku.has(VTEXCrawlersUtils.BEST_INSTALLMENT_NUMBER) && jsonSku.has(VTEXCrawlersUtils.BEST_INSTALLMENT_VALUE)) {
        Float value = CrawlerUtils.getFloatValueFromJSON(jsonSku, VTEXCrawlersUtils.BEST_INSTALLMENT_VALUE);

        if (value != null) {
          installmentPriceMap.put(jsonSku.getInt(VTEXCrawlersUtils.BEST_INSTALLMENT_NUMBER), value);
        }
      }

      if (priceWithDiscount != null && priceWithDiscount > 0) {
        Map<Integer, Float> installmentPriceMapShopCard = new HashMap<>(installmentPriceMap);
        installmentPriceMapShopCard.put(1, MathUtils.normalizeTwoDecimalPlaces(priceWithDiscount.floatValue()));

        prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMapShopCard);
      } else {
        prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
    }

    return prices;
  }
}
