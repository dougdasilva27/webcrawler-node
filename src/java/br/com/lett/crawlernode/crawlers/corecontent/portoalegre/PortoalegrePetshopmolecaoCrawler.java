package br.com.lett.crawlernode.crawlers.corecontent.portoalegre;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import com.google.common.collect.Sets;
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
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

public class PortoalegrePetshopmolecaoCrawler extends Crawler {


  private static final String PRICE_API_PARAMETER = "variant_price";
  private static final String IMAGES_API_PARAMETER = "variant_gallery";

  private static final String IMAGES_SELECTOR = "#carousel li a[href]:not(.cloud-zoom-gallery-video), .produto-imagem a";
  private static final String IMAGES_HOST = "#carousel li a[href]:not(.cloud-zoom-gallery-video)";
  private static final String PRICE_SELECTOR = "#variacaoPreco";
  
  private static final String MAIN_SELLER_NAME = "Pet Shop Molecão";
  private Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.ELO.toString(), Card.DINERS.toString(), Card.AMEX.toString());

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

            String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(docImages, IMAGES_SELECTOR, Arrays.asList("href"), "https", IMAGES_HOST);
            String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(docImages, IMAGES_SELECTOR, Arrays.asList("href"), "https", IMAGES_HOST,
                primaryImage);
            Offers offers = scrapOffers(docPrices);

            String ean = JSONUtils.getStringValue(skuJson, "EAN");
            List<String> eans = ean != null ? Arrays.asList(ean) : null;

            // Creating the product
            Product product = ProductBuilder.create()
                .setUrl(session.getOriginalURL())
                .setInternalId(internalId)
                .setInternalPid(internalPid)
                .setName(variationName != null ? name + " " + variationName : name)
                .setCategory1(categories.getCategory(0))
                .setCategory2(categories.getCategory(1))
                .setCategory3(categories.getCategory(2))
                .setPrimaryImage(primaryImage)
                .setSecondaryImages(secondaryImages)
                .setDescription(description)
                .setEans(eans)
                .setOffers(offers)
                .build();

            products.add(product);
          }
        }
      } else {

        String internalId = internalPid;
        String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, IMAGES_SELECTOR, Arrays.asList("href"), "https", IMAGES_HOST);
        String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, IMAGES_SELECTOR, Arrays.asList("href"), "https", IMAGES_HOST,
            primaryImage);
        String ean = JSONUtils.getStringValue(productJson, "EAN");
        List<String> eans = ean != null ? Arrays.asList(ean) : null;
        Offers offers = scrapOffers(doc);
        
        // Creating the product
        Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setEans(eans)
            .setOffers(offers)
            .build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }
  
  private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
     Offers offers = new Offers();
     Pricing pricing = scrapPricing(doc);

     if(pricing != null) {
       offers.add(OfferBuilder.create()
             .setUseSlugNameAsInternalSellerId(true)
             .setSellerFullName(MAIN_SELLER_NAME)
             .setSellersPagePosition(1)
             .setIsBuybox(false)
             .setIsMainRetailer(true)
             .setPricing(pricing)
             .build());
     }

     return offers;
  }
  
  private Pricing scrapPricing(Document doc) throws MalformedPricingException {
     Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, PRICE_SELECTOR, null, false, ',', session);

     if(spotlightPrice != null) {
       CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);

       return PricingBuilder.create()
             .setSpotlightPrice(spotlightPrice)
             .setCreditCards(creditCards)
             .setBankSlip(BankSlipBuilder.create().setFinalPrice(spotlightPrice).setOnPageDiscount(0.0d).build())
             .build();
     }

     return null;
  }
  
  private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
     CreditCards creditCards = new CreditCards();

     Installments installments = new Installments();
     installments.add(InstallmentBuilder.create()
           .setInstallmentNumber(1)
           .setInstallmentPrice(spotlightPrice)
           .build());

     for (String brand : cards) {
        creditCards.add(CreditCardBuilder.create()
              .setBrand(brand)
              .setIsShopCard(false)
              .setInstallments(installments)
              .build());
     }

     return creditCards;
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
}
