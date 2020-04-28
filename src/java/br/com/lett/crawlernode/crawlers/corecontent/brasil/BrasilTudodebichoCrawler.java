package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

public class BrasilTudodebichoCrawler extends Crawler {

   private static final String HOME_PAGE = "tudodebicho.com.br";
   private static final String SELLER_FULL_NAME = "Tudo de Bicho";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilTudodebichoCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, ".content.produto script[type=\"application/ld+json\"]", "", "", false, true);
         JSONArray skus = json.has("offers") && json.get("offers") instanceof JSONArray ? json.getJSONArray("offers") : new JSONArray();

         if (skus.length() > 1) {

            String productId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#hdnProdutoId", "value");

            Elements variations = doc.select(".container-tamanhos .valorAtributo");
            for (Element e : variations) {

               String scrapKG = e != null ? e.attr("data-valoratributo") : null;

               Map<String, String> headers = new HashMap<>();
               headers.put("Content-type", "application/x-www-form-urlencoded");

               Request req = RequestBuilder.create()
                     .setUrl("https://www.tudodebicho.com.br/Produto/AtualizarProduto")
                     .setPayload(
                           "atributoProduto=" + scrapKG + ";175"
                                 + "&atributoSelecionado=" + scrapKG + ";175"
                                 + "&produtoId=" + productId
                                 + "&comboIdSelecionado=0"
                                 + "&opcaoParalelaSelecionada=0"
                                 + "&optionString=Selecione"
                                 + "&isThumb=false"
                                 + "&produtoVarianteIdAdicional=0"
                                 + "&assinaturaSelecionada=false"
                                 + "&isPagProduto=true"
                                 + "&quantidade=1"
                                 + "&sellerId=0").mustSendContentEncoding(true)

                     .setHeaders(headers).build();
               String res = this.dataFetcher.post(session, req).getBody();


               JSONObject apiJSON = JSONUtils.stringToJson(res);

               Integer internalIdInt = JSONUtils.getIntegerValueFromJSON(apiJSON, "produtoVarianteId", 0);

               String internalId = internalIdInt != null ? internalIdInt.toString() : null;
               String internalPid = internalId;
               String name = JSONUtils.getStringValue(apiJSON, "nomeProdutoVariante");
               CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".fbits-breadcrumb li", true);
               String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".fbits-produto-imagens ul > li > a",
                     Arrays.asList("data-zoom-image", "data-image"), "http", HOME_PAGE);
               String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".fbits-produto-imagens ul > li > a",
                     Arrays.asList("data-zoom-image", "data-image"), "http", HOME_PAGE, primaryImage);
               String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".informacao-abas"));
               RatingsReviews ratingReviews = scrapRatingsReviews(doc);
               Offers offers = scrapOffersForProductsWithVariation(doc, apiJSON);

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
                     .setRatingReviews(ratingReviews)
                     .setOffers(offers)
                     .build();

               products.add(product);
            }


         } else {

            JSONObject jsonInfo = jsonInfo(doc);

            String internalId = JSONUtils.getStringValue(jsonInfo, "sku");
            String internalPid = JSONUtils.getStringValue(jsonInfo, "mpn");
            String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".item-name h1", true);
            CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".fbits-breadcrumb li", true);
            String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".fbits-produto-imagens ul > li > a",
                  Arrays.asList("data-zoom-image", "data-image"), "http", HOME_PAGE);
            String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".fbits-produto-imagens ul > li > a",
                  Arrays.asList("data-zoom-image", "data-image"), "http", HOME_PAGE, primaryImage);
            String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".informacao-abas"));
            RatingsReviews ratingReviews = scrapRatingsReviews(doc);
            boolean available = JSONUtils.getStringValue(jsonInfo, "availability") != null && JSONUtils.getStringValue(jsonInfo, "availability").toLowerCase().contains("instock");
            Offers offers = available ? scrapOffers(doc) : new Offers();
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
                  .setRatingReviews(ratingReviews)
                  .setOffers(offers)
                  .build();

            products.add(product);
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("#bodyProduto") != null;
   }


   private RatingsReviews scrapRatingsReviews(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = 0;
      Integer totalWrittenReviews = 0;
      Double avgRating = 0.0d;

      Element ratingElement = doc.selectFirst("[itemprop=\"aggregateRating\"]");
      if (ratingElement != null) {

         totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtmlAttr(ratingElement, "[itemprop=\"ratingCount\"]", "content", 0);
         totalWrittenReviews = CrawlerUtils.scrapIntegerFromHtmlAttr(ratingElement, "[itemprop=\"reviewCount\"]", "content", 0);
         avgRating = CrawlerUtils.scrapDoublePriceFromHtml(ratingElement, "[itemprop=\"ratingValue\"]", "content", false, '.', session);

         ratingReviews.setTotalRating(totalNumOfEvaluations);
         ratingReviews.setAverageOverallRating(avgRating != null ? avgRating : 0.0d);
         ratingReviews.setTotalWrittenReviews(totalWrittenReviews);
      }

      return ratingReviews;
   }

   private JSONObject jsonInfo(Document doc) {
      JSONObject jsonOffers = new JSONObject();

      JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, ".content.produto script[type=\"application/ld+json\"]", "", "", false, true);
      JSONArray skus = json.has("offers") && json.get("offers") instanceof JSONArray ? json.getJSONArray("offers") : new JSONArray();

      for (Object obj : skus) {
         if (obj instanceof JSONObject) {
            jsonOffers = (JSONObject) obj;

         }
      }
      return jsonOffers;
   }


   /* Start capturing offers for products without variation */

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(doc);

      offers.add(OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_FULL_NAME)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(sales)
            .build());

      return offers;
   }

   private List<String> scrapSales(Document doc) {
      List<String> sales = new ArrayList<>();

      Element salesOneElement = doc.selectFirst("#fbits-div-preco-off .fbits-preco-off");
      String firstSales = salesOneElement != null ? salesOneElement.text() : null;

      if (firstSales != null && !firstSales.isEmpty()) {
         sales.add(firstSales);
      }

      return sales;
   }


   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".fbits-preco .precoDe", null, false, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".fbits-preco .precoPor", null, false, ',', session);
      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);
      BankSlip bankSlip = BankSlipBuilder.create()
            .setFinalPrice(CrawlerUtils.scrapDoublePriceFromHtml(doc, ".fbits-parcelamento-ultima-parcela.precoParcela .fbits-parcela", null, false, ',', session))
            .build();


      return PricingBuilder.create()
            .setPriceFrom(priceFrom)
            .setSpotlightPrice(spotlightPrice)
            .setCreditCards(creditCards)
            .setBankSlip(bankSlip)
            .build();
   }

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = scrapInstallments(doc);
      if (installments.getInstallments().isEmpty()) {
         installments.add(InstallmentBuilder.create()
               .setInstallmentNumber(1)
               .setInstallmentPrice(spotlightPrice)
               .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCardBuilder.create()
               .setBrand(card)
               .setInstallments(installments)
               .setIsShopCard(false)
               .build());
      }

      return creditCards;
   }

   public Installments scrapInstallments(Document doc) throws MalformedPricingException {
      Installments installments = new Installments();

      Integer installment = CrawlerUtils.scrapIntegerFromHtml(doc, ".fbits-componente-parcelamento .precoParcela .numeroparcelas", false, null);
      Double value = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".fbits-componente-parcelamento .precoParcela .parcelavalor", null, false, ',', session);

      installments.add(InstallmentBuilder.create()
            .setInstallmentNumber(installment)
            .setInstallmentPrice(value)
            .build());

      return installments;
   }

   /* End of capture of offers for products without variation */


   /* Start capturing offers for products with variation */


   private Offers scrapOffersForProductsWithVariation(Document doc, JSONObject apiURL) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricingForProductsWithVariation(apiURL);
      List<String> sales = scrapSalesForProductsWithVariation(doc);

      offers.add(OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_FULL_NAME)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(sales)
            .build());

      return offers;
   }

   private List<String> scrapSalesForProductsWithVariation(Document doc) {
      List<String> sales = new ArrayList<>();

      Element salesOneElement = doc.selectFirst("#fbits-div-preco-off .fbits-preco-off");
      String firstSales = salesOneElement != null ? salesOneElement.text() : null;

      if (firstSales != null && !firstSales.isEmpty()) {
         sales.add(firstSales);
      }

      return sales;
   }


   private Pricing scrapPricingForProductsWithVariation(JSONObject apiURL) throws MalformedPricingException {
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(apiURL, "precoProduto", false);
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(apiURL, "precoProduto", false);
      CreditCards creditCards = scrapCreditCardsForProductsWithVariation(apiURL, spotlightPrice);
      BankSlip bankSlip = BankSlipBuilder.create()
            .setFinalPrice(JSONUtils.getDoubleValueFromJSON(apiURL, "precoProduto", false))
            .build();


      return PricingBuilder.create()
            .setPriceFrom(priceFrom)
            .setSpotlightPrice(spotlightPrice)
            .setCreditCards(creditCards)
            .setBankSlip(bankSlip)
            .build();
   }

   private CreditCards scrapCreditCardsForProductsWithVariation(JSONObject apiURL, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = scrapInstallmentsForProductsWithVariation(apiURL);
      if (installments.getInstallments().isEmpty()) {
         installments.add(InstallmentBuilder.create()
               .setInstallmentNumber(1)
               .setInstallmentPrice(spotlightPrice)
               .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCardBuilder.create()
               .setBrand(card)
               .setInstallments(installments)
               .setIsShopCard(false)
               .build());
      }

      return creditCards;
   }

   public Installments scrapInstallmentsForProductsWithVariation(JSONObject apiURL) throws MalformedPricingException {
      Installments installments = new Installments();

      String installmentString = JSONUtils.getStringValue(apiURL, "precoProdutoParcelado").split("x")[0];
      Integer installment = Integer.parseInt(installmentString.replaceAll("[^0-9]", "").trim());

      String valueString = JSONUtils.getStringValue(apiURL, "precoProdutoParcelado");
      int RS = valueString.indexOf("R$");
      Double value = !valueString.isEmpty() && valueString.contains("R$") ? MathUtils.parseDoubleWithComma(valueString.substring(RS)) : null;


      installments.add(InstallmentBuilder.create()
            .setInstallmentNumber(installment)
            .setInstallmentPrice(value)
            .build());

      return installments;
   }

}
