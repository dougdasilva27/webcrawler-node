package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;

/**
 * Date: 09/07/2019
 *
 * @author Gabriel Dornelas
 */
public class KochCrawler extends Crawler {

   protected static final String LOCATION_COOKIE_NAME = "ls.uid_armazem";
   protected static final String LOCATION_COOKIE_DOMAIN = "www.superkoch.com.br";
   protected static final String LOCATION_COOKIE_PATH = "/";

   private static final String SELLER_FULLNAME = "Koch";
   private static final String PRODUCT_API_URL = "https://www.superkoch.com.br/api/produto?id=";
   private static final List<String> cards = Arrays.asList(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.DINERS.toString(), Card.ELO.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString());

   public KochCrawler(Session session) {
      super(session);
   }

   @Override
   protected Object fetch() {
      JSONObject api = new JSONObject();

      if (session.getOriginalURL().contains("produto/")) {
         String id = CommonMethods.getLast(session.getOriginalURL().split("produto/")).split("/")[0];

         Request req = RequestBuilder.create()
               .setUrl(PRODUCT_API_URL + id)
               .setCookies(cookies)
               .build();

         api = JSONUtils.stringToJson(this.dataFetcher.get(session, req).getBody());
      }

      return api;
   }

   @Override
   public List<Product> extractInformation(JSONObject api) throws Exception {
      super.extractInformation(api);
      List<Product> products = new ArrayList<>();

      if (api.has("Produtos") && api.optJSONArray("Produtos") != null) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = api.optString("id_produto", null);
         String primaryImage = scrapPrimaryImage(api);
         String secondaryImages = scrapSecondaryImages(api, primaryImage);

         JSONArray productsArray = api.optJSONArray("Produtos");
         for (Object obj : productsArray) {
            JSONObject skuJson = (JSONObject) obj;

            String description = skuJson.optString("str_html_descricao_produto");
            CategoryCollection categories = scrapCategories(skuJson);

            String name = skuJson.optString("str_nom_produto", null);
            String internalId = skuJson.optString("id_produto", null);
            boolean buyable = !skuJson.optBoolean("bit_esgotado", true);
            Offers offers = buyable ? scrapOffers(skuJson) : new Offers();

            Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
                  .setDescription(description)
                  .setOffers(offers)
                  .setCategory1(categories.getCategory(0))
                  .setCategory2(categories.getCategory(1))
                  .setCategory3(categories.getCategory(2))
                  .build();

            products.add(product);
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private String scrapPrimaryImage(JSONObject api) {
      String primaryImage = null;

      JSONArray images = api.optJSONArray("Imagens");
      if (images != null && images.length() > 0) {
         JSONObject image = images.optJSONObject(0);

         if (image != null) {
            primaryImage = image.optString("str_img_path_cdn", null);

            if (primaryImage != null && !primaryImage.endsWith(".jpg") && !primaryImage.endsWith(".png")) {
               primaryImage += "-g.jpg";
            }
         }
      }

      return primaryImage;
   }

   private String scrapSecondaryImages(JSONObject api, String primaryImage) {
      String secondaryImages = null;
      JSONArray arrayImages = new JSONArray();

      JSONArray images = api.optJSONArray("Imagens");
      if (images != null) {
         for (Object o : images) {
            JSONObject image = o instanceof JSONObject ? (JSONObject) o : new JSONObject();

            String secondaryImage = image.optString("str_img_path_cdn", null);

            if (secondaryImage != null && !secondaryImage.endsWith(".jpg") && !secondaryImage.endsWith(".png")) {
               secondaryImage += "-g.jpg";
            }

            if (!secondaryImage.equalsIgnoreCase(primaryImage)) {
               arrayImages.put(secondaryImage);
            }
         }
      }

      if (!arrayImages.isEmpty()) {
         secondaryImages = arrayImages.toString();
      }

      return secondaryImages;
   }

   private Offers scrapOffers(JSONObject skuJson) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      List<String> sales = scrapSales(skuJson);
      Pricing pricing = scrapPricing(skuJson);

      offers.add(new OfferBuilder()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_FULLNAME)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(sales)
            .build());

      return offers;
   }

   private List<String> scrapSales(JSONObject skuJson) {
      List<String> sales = new ArrayList<>();

      Double discount = skuJson.optDouble("mny_perc_desconto", 0d);
      if (discount > 0d) {
         sales.add("-" + Integer.toString(discount.intValue()) + "%");
      }

      return sales;
   }

   private Pricing scrapPricing(JSONObject productJson) throws MalformedPricingException {
      Double price = productJson.optDouble("mny_vlr_promo_tabela_preco", 0d);
      Double priceFrom = productJson.optDouble("mny_vlr_produto_tabela_preco", 0d);

      if (priceFrom <= price) {
         priceFrom = null;
      }

      CreditCards creditCards = scrapCreditCards(productJson, price);

      return Pricing.PricingBuilder.create()
            .setSpotlightPrice(price)
            .setPriceFrom(priceFrom)
            .setCreditCards(creditCards)
            .build();
   }

   private CreditCards scrapCreditCards(JSONObject productJson, Double price) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(price)
            .build());

      Integer qtdParcel = productJson.optInt("int_qtd_parcela");
      Double valueParcel = productJson.optDouble("mny_vlr_parcela");

      if (qtdParcel > 0 && valueParcel > 0d) {
         installments.add(InstallmentBuilder.create()
               .setInstallmentNumber(qtdParcel)
               .setInstallmentPrice(valueParcel)
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

   private CategoryCollection scrapCategories(JSONObject product) {
      CategoryCollection categories = new CategoryCollection();

      String cat1 = product.optString("str_categoria", null);
      if (cat1 != null) {
         categories.add(cat1);
      }

      String cat2 = product.optString("str_subcategoria", null);
      if (cat2 != null) {
         categories.add(cat2);
      }

      String cat3 = product.optString("str_tricategoria", null);
      if (cat3 != null) {
         categories.add(cat3);
      }

      return categories;
   }
}
