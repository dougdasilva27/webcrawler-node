package br.com.lett.crawlernode.crawlers.extractionutils.core;

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
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;

public class RipleyCrawler extends Crawler {

    private static final String SELLER_NAME_LOWER = "ripley";
    private final String homePage = session.getOptions().optString("homePage");
    private final String apiKey = session.getOptions().optString("ratingsApiKey");
    private final String storeId = session.getOptions().optString("ratingsStoreId");
    private final String location = session.getOptions().optString("ratingsLocation");

    public RipleyCrawler(Session session) {
        super(session);
    }

    @Override
    public List<Product> extractInformation(Document doc) throws Exception {
        super.extractInformation(doc);
        List<Product> products = new ArrayList<>();

        if (isProductPage(doc)) {
            Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

            String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, "span.sku.sku-value", true);
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "button#buy-button", "data-product-id");
            CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "li.breadcrumbs a span", false);
            String description = CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList("div.RRT__container"));
            String name = CrawlerUtils.scrapStringSimpleInfo(doc, "section.product-header h1", true);
            List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, "li.thumbnail-item img", Collections.singletonList("data-src"), "https", "s3.amazonaws.com", null);
            String primaryImage = !secondaryImages.isEmpty() ? secondaryImages.remove(0) : "";
            RatingsReviews ratingsReviews = scrapRating(internalPid);
            boolean available = !doc.selectFirst("div.btn-ripley-wrapper").html().contains("Agotado");
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
                    .setOffers(offers)
                    .setRatingReviews(ratingsReviews)
                    .build();

            products.add(product);
        } else {
            Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
        }

        return products;
    }

    /**
     * @param doc
     * @return
     */
    private boolean isProductPage(Document doc) {
        return !doc.select(".product-item").isEmpty();
    }

    private RatingsReviews scrapRating(String internalPid) {

        JSONObject productReviews = getApiReviews(internalPid);
        RatingsReviews ratingsReviews = new RatingsReviews();
        JSONObject results = productReviews != null ? JSONUtils.getValueRecursive(productReviews, "results.0.rollup", JSONObject.class) : null;

        if (results != null) {

            double avgReviews = JSONUtils.getDoubleValueFromJSON(results, "average_rating", true);
            int totalRating = JSONUtils.getIntegerValueFromJSON(results, "review_count", 0);
            AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(results);

            ratingsReviews.setAverageOverallRating(MathUtils.normalizeTwoDecimalPlaces(avgReviews));
            ratingsReviews.setTotalRating(totalRating);
            ratingsReviews.setTotalWrittenReviews(totalRating);
            ratingsReviews.setAdvancedRatingReview(advancedRatingReview);

            return ratingsReviews;
        }

        return ratingsReviews;
    }

    private JSONObject getApiReviews(String internalPid) {
        String url = "https://display.powerreviews.com/m/" + storeId + "/l/" + location + "/product/" + internalPid + "/reviews?apikey=" + apiKey + "&_noconfig=true";

        Request request = Request.RequestBuilder.create()
                .setUrl(url)
                .build();
        String content = this.dataFetcher
                .get(session, request)
                .getBody();

        return CrawlerUtils.stringToJson(content);
    }

    private AdvancedRatingReview scrapAdvancedRatingReview(JSONObject results) {
        JSONArray stars = results.optJSONArray("rating_histogram");

        if (stars != null) {
            Integer star1 = stars.getInt(0);
            Integer star2 = stars.getInt(1);
            Integer star3 = stars.getInt(2);
            Integer star4 = stars.getInt(3);
            Integer star5 = stars.getInt(4);

            return new AdvancedRatingReview.Builder()
                    .totalStar1(star1)
                    .totalStar2(star2)
                    .totalStar3(star3)
                    .totalStar4(star4)
                    .totalStar5(star5)
                    .build();
        } else {
            return new AdvancedRatingReview();
        }
    }

    protected Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
        Offers offers = new Offers();
        Pricing pricing = scrapPricing(doc);
        List<String> sales = new ArrayList<>();

        if(pricing.getPriceFrom() != null) {
            sales.add(CrawlerUtils.calculateSales(pricing));
        }

        String seller = CrawlerUtils.scrapStringSimpleInfo(doc, "a.product-information-shop-name", true);
        boolean isMainSeller = false;

        if (seller == null) {
            seller = SELLER_NAME_LOWER;
            isMainSeller = true;
        }

        offers.add(Offer.OfferBuilder.create()
                .setMainPagePosition(1)
                .setIsBuybox(false)
                .setPricing(pricing)
                .setSales(sales)
                .setSellerFullName(seller)
                .setIsMainRetailer(isMainSeller)
                .setUseSlugNameAsInternalSellerId(true)
                .build());

        return offers;
    }

    private Pricing scrapPricing(Document doc) throws MalformedPricingException {
        Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div.product-internet-price dt.product-price", null, true, '.', session);
        Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div.product-normal-price dt.product-price", null, true, '.', session);

        if (spotlightPrice == null) {
            spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div.product-internet-price-not-best dt.product-price", null, true, '.', session);
        }

        CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);
        BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
                .setFinalPrice(spotlightPrice)
                .build();

        return Pricing.PricingBuilder.create()
                .setSpotlightPrice(spotlightPrice)
                .setPriceFrom(priceFrom)
                .setBankSlip(bankSlip)
                .setCreditCards(creditCards)
                .build();
    }

    private CreditCards scrapCreditCards(Document doc, Double spotlighPrice) throws MalformedPricingException {
        CreditCards creditCards = new CreditCards();
        Installments installments = new Installments();
        Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

        installments.add(Installment.InstallmentBuilder.create()
                .setInstallmentNumber(1)
                .setInstallmentPrice(spotlighPrice)
                .build());

        for (String card : cards) {
            creditCards.add(CreditCard.CreditCardBuilder.create()
                    .setBrand(card)
                    .setInstallments(installments)
                    .setIsShopCard(false)
                    .build());
        }

        Double shopcardPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div.product-ripley-price dt.product-price", null, true, '.', session);

        if (shopcardPrice != null) {
            Installments shopcardInstallments = new Installments();
            shopcardInstallments.add(Installment.InstallmentBuilder.create()
                    .setInstallmentNumber(1)
                    .setInstallmentPrice(shopcardPrice)
                    .build());

            creditCards.add(CreditCard.CreditCardBuilder.create()
                    .setBrand(Card.SHOP_CARD.toString())
                    .setInstallments(shopcardInstallments)
                    .setIsShopCard(true)
                    .build());
        }

        return creditCards;
    }
}