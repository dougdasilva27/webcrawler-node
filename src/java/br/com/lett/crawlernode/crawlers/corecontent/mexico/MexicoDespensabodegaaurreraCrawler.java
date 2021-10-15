package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
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
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class MexicoDespensabodegaaurreraCrawler extends Crawler {

    private final static String STORE_ID = "565";
    private final static String API_URL = "https://deadpool.instaleap.io/api/v2";
    private final static String SELLER_FULLNAME = "despensa bodega aurrera ";
    //The location was not requested in this market.

    protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
            Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

    public MexicoDespensabodegaaurreraCrawler(Session session) {
        super(session);
        config.setFetcher(FetchMode.JSOUP);
    }

    @Override
    protected JSONObject fetch() {
        Map<String, String> headers = new HashMap<>();
        String slugUrl = CommonMethods.getLast(session.getOriginalURL().split("/"));

        headers.put("authority", "deadpool.instaleap.io");
        headers.put("content-type", "application/json");

        String body = "{\"variables\":{\"storeId\":\"" + STORE_ID + "\",\"filter\":{\"sku\":{\"eq\":\"" + slugUrl + "\"}}},\"query\":\"query ($pagination: paginationInput, $search: SearchInput, $storeId: ID!, $categoryId: ID, $onlyThisCategory: Boolean, $filter: ProductsFilterInput, $orderBy: productsSortInput) {  getProducts(pagination: $pagination, search: $search, storeId: $storeId, categoryId: $categoryId, onlyThisCategory: $onlyThisCategory, filter: $filter, orderBy: $orderBy) {    redirectTo    products {      id      description      name      photosUrls      sku      unit      price      specialPrice      promotion {        description        type        isActive        conditions        __typename      }      stock      nutritionalDetails      clickMultiplier      subQty      subUnit      maxQty      minQty      specialMaxQty      ean      boost      showSubUnit      isActive      slug      categories {        id        name        __typename      }      __typename    }    paginator {      pages      page      __typename    }    __typename  }}\"}";

        Request request = Request.RequestBuilder.create()
                .setUrl(API_URL)
                .setPayload(body)
                .setHeaders(headers)
                .build();

        Response response = this.dataFetcher.post(session, request);
        return JSONUtils.stringToJson(response.getBody());
    }

    @Override
    public List<Product> extractInformation(JSONObject json) throws Exception {
        super.extractInformation(json);
        List<Product> products = new ArrayList<>();

        JSONArray productsArray = JSONUtils.getValueRecursive(json, "data.getProducts.products", JSONArray.class);

        if (!productsArray.isEmpty()) {
            Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

            JSONObject jsonProduct = productsArray.optJSONObject(0);

            String internalPid = jsonProduct.optString("sku");
            String internalId = jsonProduct.optString("id");
            String name = jsonProduct.optString("name");
            CategoryCollection categories = scrapCategories(jsonProduct);
            List<String> images = JSONUtils.jsonArrayToStringList(jsonProduct.optJSONArray("photosUrls"));
            String primaryImage = !images.isEmpty() ? images.remove(0) : null;
            String description = jsonProduct.optString("description");
            boolean isAvailable = jsonProduct.getInt("stock") != 0;
            Offers offers = isAvailable ? scrapOffers(jsonProduct) : new Offers();

            Product product = ProductBuilder.create()
                    .setUrl(session.getOriginalURL())
                    .setInternalId(internalId)
                    .setInternalPid(internalPid)
                    .setName(name)
                    .setCategories(categories)
                    .setPrimaryImage(primaryImage)
                    .setSecondaryImages(images)
                    .setDescription(description)
                    .setOffers(offers)
                    .build();

            products.add(product);
        } else {
            Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
        }

        return products;
    }

    private CategoryCollection scrapCategories(JSONObject json) {
        CategoryCollection categories = new CategoryCollection();

        JSONArray categoriesJson = json.optJSONArray("categories");

        for (Object o : categoriesJson) {
            JSONObject categoryJson = (JSONObject) o;
            categories.add(categoryJson.optString("name"));
        }

        return categories;
    }

    private Offers scrapOffers(JSONObject jsonObject) throws OfferException, MalformedPricingException {
        Offers offers = new Offers();
        List<String> sales = new ArrayList<>();

        Pricing pricing = scrapPricing(jsonObject);

        if(pricing.getPriceFrom() != null){
            sales.add(CrawlerUtils.calculateSales(pricing));
        }

        offers.add(Offer.OfferBuilder.create()
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

    private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {
        Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(json, "specialPrice", false);
        Double priceFrom = JSONUtils.getDoubleValueFromJSON(json, "price", false);

        if (spotlightPrice == 0d) {
            spotlightPrice = priceFrom;
            priceFrom = null;
        }

        CreditCards creditCards = scrapCreditCards(spotlightPrice);

        return Pricing.PricingBuilder.create()
                .setPriceFrom(priceFrom)
                .setSpotlightPrice(spotlightPrice)
                .setCreditCards(creditCards)
                .build();
    }

    private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
        CreditCards creditCards = new CreditCards();

        Installments installments = new Installments();
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
}
