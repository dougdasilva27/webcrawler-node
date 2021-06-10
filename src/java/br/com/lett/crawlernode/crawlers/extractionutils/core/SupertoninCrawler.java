package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
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
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class SupertoninCrawler extends Crawler {

   private final String id_armazem = session.getOptions().optString("IdArmazem");

   public SupertoninCrawler(Session session) {
      super(session);
      config.setFetcher(FetchMode.JSOUP);
   }

   @Override
   public Object fetch() {
      //https://www.supertonin.com.br/produto/10950/achocolatado-nestle-700-g-sache-classic#
      String productId = "";
      String[] spplittedUrl = session.getOriginalURL().split("/");

      if (spplittedUrl.length > 0) {
         productId = spplittedUrl[4];
      }

      String url = " https://www.supertonin.com.br/api/produto?id=" + productId;
      Map<String, String> headers = new HashMap<>();
      headers.put("Cookie", "ls.uid_armazem=" + id_armazem);
      headers.put("Accept", "application/json, text/plain, */*");
      headers.put("Accept-Encoding", "gzip, deflate, br");
      headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.212 Safari/537.36");

      BasicClientCookie cookie = new BasicClientCookie("ls.uid_armazem", id_armazem);
      cookie.setDomain("www.supertonin.com.br");
      cookie.setPath("/");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setCookies(Collections.singletonList(cookie))
         .build();

      return JSONUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(json)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         JSONObject jsonSku = json.optJSONArray("Produtos").optJSONObject(0);

         String internalPid = jsonSku.optString("id_produto");
         List<String> images = scrapImages(json);
         String primaryImage = images.remove(0);
         List<String> secondaryImages = !images.isEmpty() ? images : null;
         CategoryCollection categories = scrapCategories(jsonSku);
         String baseName = jsonSku.optString("str_nom_produto");
         List<String> eans = Collections.singletonList(jsonSku.optString("str_cod_barras_produto"));
         int stock = jsonSku.optInt("int_qtd_estoque_produto");
         RatingsReviews ratings = scrapRatingReviews(json);

         JSONArray variants = json.optJSONArray("Modelos");

         for (Object variant : variants) {
            JSONObject variantJson = (JSONObject) variant;
            String internalId = variantJson.optString("id_produto_modelo");
            String name = baseName + " - " + variantJson.optString("str_nom_produto_modelo");

            boolean available = !variantJson.optBoolean("bit_esgotado");
            Offers offers = available ? scrapOffers(variantJson) : new Offers();

            // Creating the product
            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setCategories(categories)
               .setEans(eans)
               .setStock(stock)
               .setOffers(offers)
               .setRatingReviews(ratings)
               .build();

            products.add(product);
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(JSONObject json) {
      return json.has("Produtos") && !json.optJSONArray("Produtos").isEmpty();
   }

   private List<String> scrapImages(JSONObject json) {
      List<String> imagesList = new ArrayList<>();

      JSONArray imgJson = json.optJSONArray("Imagens");

      if (imgJson != null && !imgJson.isEmpty()) {
         for (Object img : imgJson) {
            if (img instanceof JSONObject) {
               imagesList.add(((JSONObject) img).optString("str_img_path") + "-g.jpg");
            }
         }
      }

      return imagesList;
   }

   private CategoryCollection scrapCategories(JSONObject json) {
      CategoryCollection categories = new CategoryCollection();

      if (json.has("str_categoria")) {
         categories.add(json.optString("str_categoria"));
      }
      if (json.has("str_subcategoria")) {
         categories.add(json.optString("str_subcategoria"));
      }
      if (json.has("str_tricategoria")) {
         categories.add(json.optString("str_tricategoria"));
      }

      return categories;
   }

   private RatingsReviews scrapRatingReviews(JSONObject json) {
      RatingsReviews ratingReviews = new RatingsReviews();

      //If we find a rated product, we can probably find the ratings here:
      //json.optJSONArray("Avaliacoes");

      //Cannot find any rated product.
      //With only this information, we cannot build the object

      return ratingReviews;
   }

   public Offers scrapOffers(JSONObject jsonSku) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();

      Pricing pricing = scrapPricing(jsonSku);
      String sales = CrawlerUtils.calculateSales(pricing);

      Offer offer = new Offer.OfferBuilder()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName("super tonin")
         .setIsBuybox(false)
         .setPricing(pricing)
         .setIsMainRetailer(true)
         .setSales(Collections.singletonList(sales))
         .build();
      offers.add(offer);

      return offers;
   }

   public static Pricing scrapPricing(JSONObject jsonSku) throws MalformedPricingException {
      Double priceFrom = jsonSku.optDouble("mny_vlr_produto_tabela_preco", 0D);
      Double spotlightPrice = jsonSku.optDouble("mny_vlr_promo_tabela_preco", 0D);

      if (spotlightPrice.equals(priceFrom)) {
         priceFrom = null;
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }

   public static CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      Set<Card> cards = Sets.newHashSet(
         Card.VISA,
         Card.MASTERCARD,
         Card.DINERS,
         Card.AMEX,
         Card.ELO,
         Card.SHOP_CARD
      );

      for (Card card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card.toString())
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

}
