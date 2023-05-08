package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Parser;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.Pair;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BrasilGazinCrawler extends Crawler {
   public BrasilGazinCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.HTML);
   }

   private final static String SELLER_NAME = "gazin";
   private final static String baseUrl = "https://www.gazin.com.br/produto/";
   private final int id_seller = session.getOptions().optInt("id_seller");

   @Override
   protected Response fetchResponse() {
      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setFollowRedirects(true)
         .build();
      return this.dataFetcher.get(session, request);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<Product>();
      JSONObject dataJson = getScriptJson(doc);
      if (dataJson != null) {
         String internalPid = dataJson.optString("id", null);
         String baseName = dataJson.optString("titulo");
         String description = dataJson.optString("seo_descricao");
         JSONArray variations = dataJson.optJSONArray("variacoes");
         for (Object v : variations) {
            JSONObject variation = (JSONObject) v;
            String internalId = variation.optString("id");
            List<String> images = getImages(variation);
            String primaryImage = images.size() > 0 ? images.remove(0) : null;
            Pair<String, String> variationsLabel = scrapVariationsLabels(variation);
            String color = variationsLabel.getFirst();
            String voltage = variationsLabel.getSecond();
            String name = scrapName(baseName, color, voltage);
            String url = scrapUrl(session.getOriginalURL(), color, voltage);
            JSONObject objectForOffers = getObjectPrice(variation);
            int stock = objectForOffers.optInt("estoque", 0);
            Offers offers = stock > 0 ? scrapOffers(objectForOffers) : new Offers();
            Product product = ProductBuilder.create()
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setUrl(url)
               .setName(name)
               .setOffers(offers)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(images)
               .setDescription(description)
               .build();
            products.add(product);

         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }

   private JSONObject getScriptJson(Document doc) {
      String dataScript = CrawlerUtils.scrapScriptFromHtml(doc, "#__NEXT_DATA__");
      if (dataScript != null && !dataScript.isEmpty()) {
         JSONArray arrayJson = CrawlerUtils.stringToJsonArray(dataScript);
         if (arrayJson != null && !arrayJson.isEmpty()) {
            return JSONUtils.getValueRecursive(arrayJson, "0.props.pageProps.conteudoProduto", JSONObject.class);
         }

      }
      return new JSONObject();
   }

   private boolean validString(String value, String attribute) {
      return value != null && !value.isEmpty() && !value.equals("sem-" + attribute);
   }

   private String scrapName(String baseName, String color, String voltage) {
      if (validString(color, "cor")) {
         baseName += " - " + color;
      }
      if (validString(voltage, "voltagem")) {
         baseName += " - " + voltage;
      }
      return baseName;
   }

   private String scrapUrl(String baseUrl, String color, String voltage) {
      baseUrl += validString(color, "cor") ? "?cor=" + color : "?cor=sem-cor";
      baseUrl += validString(voltage, "voltage") ? "&voltage=" + voltage.replace(" ", "-") : "&voltage=sem-voltage";
      return baseUrl + "&seller_id" + id_seller;
   }

   private Pair<String, String> scrapVariationsLabels(JSONObject variationJson) {
      String color = null;
      String voltage = null;
      JSONArray combinations = variationJson.optJSONArray("combinacoes");
      if (combinations != null) {
         for (Object c : combinations) {
            JSONObject combination = (JSONObject) c;
            String slug = combination.optString("atributo_slug", null);
            String value = combination.optString("valor_slug");
            if (slug != null) {
               if (slug.equals("cor")) {
                  color = value;
               } else if (slug.equals("voltagem")) {
                  voltage = value.replace(" ", "-");
               }
            }
         }
         return new Pair<>(color, voltage);
      }
      return new Pair<>();
   }

   private List<String> getImages(JSONObject json) {
      List<String> images = new ArrayList<String>();
      JSONArray arrayImages = JSONUtils.getValueRecursive(json, "images", JSONArray.class, new JSONArray());
      if (!arrayImages.isEmpty()) {
         for (Object o : arrayImages) {
            JSONObject imgJson = (JSONObject) o;
            String urlImage = imgJson.optString("url");
            if (urlImage != null && !urlImage.isEmpty()) {
               images.add(urlImage);
            }
         }
      }
      return images;
   }

   private JSONObject getObjectPrice(JSONObject json) {
      JSONArray advertisementsArray = json.optJSONArray("anuncios");
      if (advertisementsArray != null && !advertisementsArray.isEmpty()) {
         for (Object a : advertisementsArray) {
            JSONObject advertisement = (JSONObject) a;
            Integer seller = JSONUtils.getValueRecursive(advertisement, "seller.id", Integer.class, null);
            if (seller != null && seller == id_seller) {
               return advertisement;
            }
         }
      }
      return new JSONObject();
   }

   private Offers scrapOffers(JSONObject data) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();
      Pricing pricing = scrapPricing(data);
      sales.add(CrawlerUtils.calculateSales(pricing));
      offers.add(Offer.OfferBuilder.create()
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSales(sales)
         .setSellerFullName(SELLER_NAME)
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());
      return offers;
   }

   private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {
      JSONObject priceObject = json.optJSONObject("preco");
      Double priceFrom = priceObject.optDouble("de");
      Double price = priceObject.optDouble("por");
      if (priceFrom.equals(price)) {
         priceFrom = null;
      }

      CreditCards creditCards = scrapCreditCards(price);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(price)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(price)
         .setPriceFrom(priceFrom)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.HIPERCARD.toString(),
         Card.DINERS.toString(), Card.HIPER.toString(), Card.ELO.toString(), Card.SOROCRED.toString(), Card.AMEX.toString());

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

