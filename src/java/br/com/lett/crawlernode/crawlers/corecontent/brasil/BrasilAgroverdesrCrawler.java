package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import br.com.lett.crawlernode.core.fetcher.models.Request;
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
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class BrasilAgroverdesrCrawler extends Crawler {


   private static final String HOME_PAGE = "www.agroverdesr.com.br";
   private static final String SELLER_FULL_NAME = "Agroverde sr brasil";
   private final Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPER.toString(), Card.HIPERCARD.toString(),
      Card.JCB.toString(), Card.ELO.toString());

   public BrasilAgroverdesrCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());


         JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, ".content.produto script[type=\"application/ld+json\"]", "", "", false, true);
         JSONArray skus = json.has("offers") && json.get("offers") instanceof JSONArray ? json.getJSONArray("offers") : new JSONArray();

         if (!skus.isEmpty()) {

            String productId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#hdnProdutoId", "value");

            JSONArray variations = ScrapVariantes(doc);
            for (Object o : variations) {

               String scrapKG = o instanceof JSONObject ? ((JSONObject) o).optString("Característica") : null;

               Map<String, String> headers = new HashMap<>();
               headers.put("Content-type", "application/x-www-form-urlencoded");

               Request req = Request.RequestBuilder.create()
                  .setUrl("https://www.agroverdesr.com.br/Produto/AtualizarProduto")
                  .setPayload(
                     "atributoProduto=" + scrapKG + ";178"
                        + "&atributoSelecionado=" + scrapKG + ";178"
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

               Integer internalIdInt = JSONUtils.getIntegerValueFromJSON(apiJSON, "produtoVarianteId",null);

               String internalId = internalIdInt != null ? internalIdInt.toString() : null;
               String internalPid = internalId;
               String name = JSONUtils.getStringValue(apiJSON, "nomeProdutoVariante");
               CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".fbits-breadcrumb li", true);
               String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".fbits-produto-imagens ul > li > a",
                  Arrays.asList("data-zoom-image", "data-image"), "https", HOME_PAGE);
               String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".fbits-produto-imagens ul > li > a",
                  Arrays.asList("data-zoom-image", "data-image"), "https", HOME_PAGE, primaryImage);
               String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".informacao-abas"));
               RatingsReviews ratingReviews = scrapRatingsReviews(doc);
               boolean avaliable = apiJSON.optBoolean("disponivel");
               Offers offers = avaliable? scrapOffersForProductsWithVariation(doc, apiJSON) : new Offers();

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
         }
      }
      else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("#bodyProduto") != null;
   }

   private JSONArray ScrapVariantes(Document document) {
      JSONArray array = new JSONArray();
      String tag = "Fbits.Produto.AtributosProduto = ";
      Elements elements = document.select("#bodyProduto  script");

      for (Element element : elements) {
         for (DataNode dataNode : element.dataNodes()) {

            if (dataNode.getWholeData().contains(tag)) {
               String script = dataNode.getWholeData();
               int inicialIndex = script.indexOf(tag) + tag.length();
               int finalIndex = script.indexOf(";", inicialIndex);
               array = JSONUtils.stringToJsonArray(script.substring(inicialIndex, finalIndex));
            }
         }
      }
      return array;
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

   private JSONObject jsonInfoFromHtml(Document doc) {
      JSONObject jsonOffers = new JSONObject();

      JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, ".content.produto script[type=\"application/ld+json\"]", "", "", false, true);
      JSONArray skus = json.has("offers") && json.get("offers") instanceof JSONArray ? json.getJSONArray("offers") : new JSONArray();

      for (Object obj : skus) {
         if (obj instanceof JSONObject) {
            jsonOffers = (JSONObject) obj;
            break;
         }
      }
      return jsonOffers;
   }


   /* Start capturing offers for products without variation */

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(doc);

      offers.add(Offer.OfferBuilder.create()
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
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(CrawlerUtils.scrapDoublePriceFromHtml(doc, ".fbits-parcelamento-ultima-parcela.precoParcela .fbits-parcela", null, false, ',', session))
         .build();


      return Pricing.PricingBuilder.create()
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
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
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

      installments.add(Installment.InstallmentBuilder.create()
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

      offers.add(Offer.OfferBuilder.create()
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
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(JSONUtils.getDoubleValueFromJSON(apiURL, "precoProduto", false))
         .build();


      return Pricing.PricingBuilder.create()
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
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   public Installments scrapInstallmentsForProductsWithVariation(JSONObject apiURL) throws MalformedPricingException {
      Installments installments = new Installments();

      if (apiURL != null && !apiURL.isEmpty()) {
         String installmentString = JSONUtils.getStringValue(apiURL, "precoProdutoParcelado").split("x")[0];
         Integer installment = MathUtils.parseInt(installmentString.replaceAll("[^0-9]", "").trim());

         String valueString = JSONUtils.getStringValue(apiURL, "precoProdutoParcelado");
         int RS = valueString.indexOf("R$");
         Double value = !valueString.isEmpty() && valueString.contains("R$") ? MathUtils.parseDoubleWithComma(valueString.substring(RS)) : null;


         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(installment)
            .setInstallmentPrice(value)
            .build());
      }
      return installments;
   }

}
