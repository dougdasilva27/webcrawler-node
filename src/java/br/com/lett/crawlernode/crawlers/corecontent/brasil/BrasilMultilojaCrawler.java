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
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilMultilojaCrawler extends Crawler {

   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString());

   private static final String SELLER_NAME = "Multiloja";

   public BrasilMultilojaCrawler(Session session) {
      super(session);
   }

   @Override
   protected JSONObject fetch() {
      JSONObject jsonObject = new JSONObject();
      String id = getProductPid();
      Map<String, String> headers = new HashMap<>();
      headers.put("Authorization", "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkV2ZXJ0b24iLCJhZG1pbiI6dHJ1ZSwianRpIjoiN2M0Y2FiOTc0MDcyMTRkODAyNWQ5MzFkNTY4MThlYmI1MDQ5OWFmOWVmMGMyNjM3NGI3ZTRlNWU3ZmRjNDVkYiIsImlhdCI6MTYxNjQ0NTI3OSwiZXhwIjoxNzczNjkzMjc5LCJlbWFpbCI6ImV2ZXJ0b25AcHJlY29kZS5jb20uYnIiLCJlbXByZXNhIjoiUHJlY29kZSIsInNjaGVtYSI6Ik11bHRpbG9qYSIsImlkU2NoZW1hIjo3LCJpZFNlbGxlciI6NCwiaWRVc2VyIjoxfQ==.cpC5OeaTtyTjHzNJ2r+JtKWjQ5Q8rgyZ/uPdNxZm0HY=");
      String API = "https://www.allfront.com.br/api/product/" + id;

      Request request = Request.RequestBuilder.create()
         .setUrl(API)
         .setHeaders(headers)
         .build();
      String content = this.dataFetcher
         .get(session, request)
         .getBody();


      JSONArray jsonArray = CrawlerUtils.stringToJsonArray(content);
      if (jsonArray != null && !jsonArray.isEmpty()) {
         jsonObject = (JSONObject) jsonArray.get(0);

      }

      return jsonObject;

   }


   private String getProductPid() {
      String url = null;
      Pattern pattern = Pattern.compile("\\/(.[0-9]*)\\/");
      Matcher matcher = pattern.matcher(session.getOriginalURL());
      if (matcher.find()) {
         url = matcher.group(1);
      }
      return url;
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();

      if (json != null && !json.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String internalPid = getProductPid();
         String description = json.optString("descricao_geral");
         CategoryCollection categoryCollection = getCategories(json);

         JSONArray variations = JSONUtils.getValueRecursive(json, "sellers.0.gradeSeller", JSONArray.class);
         for (Object variation : variations) {
            if (variation instanceof JSONObject) {
               JSONObject productInfo = (JSONObject) variation;
               String internalId = productInfo.optString("sku");
               Integer identifierImage = productInfo.optInt("idGradeX");

               String name = getName(json, identifierImage);
               List<String> images = getImage(json, identifierImage);
               String primaryImage = !images.isEmpty() ? images.remove(0) : null;
               int stock = productInfo.optInt("disponivel");
               boolean available = stock > 0;
               Offers offers = available ? scrapOffers(productInfo) : new Offers();

               // Creating the productInfo
               Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(images)
                  .setDescription(description)
                  .setCategories(categoryCollection)
                  .setOffers(offers)
                  .build();

               products.add(product);
            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private Offers scrapOffers(JSONObject product) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(product);
      List<String> sales = scrapSales(product);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_NAME)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private List<String> scrapSales(JSONObject product) {
      List<String> sales = new ArrayList<>();

      String salesOnJson = product.optString("boletoPercentual");
      sales.add(salesOnJson);
      return sales;
   }

   private CategoryCollection getCategories(JSONObject jsonObject) {
      CategoryCollection categoryCollection = new CategoryCollection();
      String firstCategorie = jsonObject.optString("categoria");
      String subCategorie = jsonObject.optString("subcategoria");
      String terCategorie = jsonObject.optString("tercategoria");

      if (firstCategorie != null && !firstCategorie.isEmpty()) {
         categoryCollection.add(firstCategorie);
      }
      if (subCategorie != null && !subCategorie.isEmpty()) {
         categoryCollection.add(subCategorie);
      }
      if (terCategorie != null && !terCategorie.isEmpty()) {
         categoryCollection.add(terCategorie);
      }

      return categoryCollection;
   }


   private String getName(JSONObject dataJson, Integer identifier) {
      StringBuilder stringBuilder = new StringBuilder();
      String color = getColor(dataJson, identifier);
      String name = dataJson.optString("nome");

      if (name != null && !name.isEmpty()) {
         stringBuilder.append(name);
         if (color != null && !color.isEmpty()) {
            stringBuilder.append(" - ").append(color);
         }
      }


      return stringBuilder.toString();

   }

   private String getColor(JSONObject dataJson, Integer identifier) {
      JSONArray variations = dataJson.optJSONArray("gradeX");
      String color = null;

      if (variations != null) {
         for (Object variation : variations) {
            if (variation instanceof JSONObject && ((JSONObject) variation).has("idGradeX")) {
               JSONObject gradeVariation = ((JSONObject) variation);
               Integer codeGrade = gradeVariation.optInt("idGradeX");
               if (codeGrade.equals(identifier)) {
                  color = gradeVariation.optString("descricao");
                  break;

               }
            }
         }
      }
      return color;
   }

   private List<String> getImage(JSONObject dataJson, Integer identifier) {
      List<String> imageList = new ArrayList<>();
      String image;
      JSONArray variationsImages = dataJson.optJSONArray("images");

      if (variationsImages != null) {
         for (Object variation : variationsImages) {
            if (variation instanceof JSONObject && ((JSONObject) variation).has("idGradeX")) {
               JSONObject gradeVariation = ((JSONObject) variation);
               Integer codeGrade = gradeVariation.optInt("idGradeX");
               if (codeGrade.equals(identifier)) {
                  image = gradeVariation.optString("images");
                  if (image != null && !image.isEmpty()) {
                     imageList.add(image);
                  }
               }
            }

         }
      }
      return imageList;

   }


   private Pricing scrapPricing(JSONObject product) throws MalformedPricingException {

      Double priceFrom = JSONUtils.getValueRecursive(product, "0.listPrices.real", Double.class);
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(product, "preco", true);
      Double bankSlipValue = JSONUtils.getDoubleValueFromJSON(product, "boletoValor", true);
      CreditCards creditCards = scrapCreditCards(product);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(bankSlipValue)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private CreditCards scrapCreditCards(JSONObject product) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      JSONArray installmentsArray = product.optJSONArray("parcelamento");
      Installments installments = new Installments();
      if (installmentsArray != null) {
         for (Object obj : installmentsArray) {
            if (obj instanceof JSONObject) {
               JSONObject installmentsJson = (JSONObject) obj;
               String numberInstallmentStr = installmentsJson.optString("parcela");
               Integer numberInstallment = numberInstallmentStr != null ? Integer.valueOf(numberInstallmentStr.replace("x", "")) : null;
               String valueStr = installmentsJson.optString("valor_total");
               String valueInstallmentStr = installmentsJson.optString("valor");
               Double valueInstallment = MathUtils.parseDoubleWithComma(valueInstallmentStr);
               Double value = valueStr != null ? MathUtils.parseDoubleWithComma(valueStr) : null;
               installments.add(Installment.InstallmentBuilder.create()
                  .setInstallmentNumber(numberInstallment)
                  .setFinalPrice(value)
                  .setInstallmentPrice(valueInstallment)
                  .build());

            }
         }
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
}
