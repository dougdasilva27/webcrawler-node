package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static models.pricing.BankSlip.BankSlipBuilder;

public class BrasilBiomundoCrawler extends Crawler {

    private static final String HOST_PAGE = "www.lojabiomundo.com.br";
    private static final String MAIN_SELLER_NAME = "biomundo";
    protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
            Card.HIPER.toString(), Card.AMEX.toString());

    public BrasilBiomundoCrawler(Session session) {
        super(session);
    }

    @Override
    public List<Product> extractInformation(Document doc) throws Exception {
        super.extractInformation(doc);
        List<Product> products = new ArrayList<>();

        if (isProductPage(doc)) {
            Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".item > meta", "content");
            String internalPid = internalId;
            String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".item > .fn", true);
            CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb > span > span > a", true);
            String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#img-product", Collections.singletonList("src"), "https://", HOST_PAGE);
            String description = CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList(".info-description"));
            boolean availability = scrapAvailability(doc);
            Offers offers = (availability) ? scrapOffers(doc) : new Offers();
            RatingsReviews ratings = scrapRatingReviews(doc);
            String paymentsLink = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#payments", "href");
            Document payments = sendRequest(paymentsLink);

            Product product = ProductBuilder.create()
                    .setUrl(session.getOriginalURL())
                    .setInternalId(internalId)
                    .setInternalPid(internalPid)
                    .setName(name)
                    .setCategory1(categories.getCategory(0))
                    .setCategory2(categories.getCategory(1))
                    .setCategory3(categories.getCategory(2))
                    .setPrimaryImage(primaryImage)
                    .setDescription(description)
                    .setOffers(offers)
                    .setRatingReviews(ratings)
                    .build();

            Elements productsElements = doc.select("#variations option:not([selected])");

            if (productsElements.size() > 0) {
                for (Element e : productsElements) {

                    Product productClone = product.clone();
                    productClone.setName(product.getName() + " - " + scrapVariationName(e));
                    productClone.setInternalId(product.getInternalId() + "-" + scrapVariationId(e));

                    if (e.hasClass("sold-out-box")) {
                        productClone.setOffers(new Offers());
                    }

                    products.add(productClone);
                }

            } else {
                products.add(product);
            }

        } else {
            Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
        }

        return products;
    }

    private boolean isProductPage(Document doc) {
        return doc.selectFirst(".product-contents") != null;
    }

    private boolean scrapAvailability(Document doc) {
        String availabilityLink = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "link[itemprop=\"availability\"]", "href");
        return availabilityLink.contains("InStock");
    }

    private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
        Offers offers = new Offers();
        Pricing pricing = scrapPricing(doc);

        if (pricing != null) {
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
        Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".sale", null, true, ',', session);
        Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".list-price > .list", null, true, ',', session);
        //TODO
        return PricingBuilder.create()
                .setPriceFrom(priceFrom)
                .setSpotlightPrice(spotlightPrice)
                .setCreditCards(scrapCreditCards(doc, spotlightPrice))
                .setBankSlip(scrapBankSlip(doc, spotlightPrice))
                .build();
    }

    private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
        CreditCards creditCards = new CreditCards();
        Installments installments = scrapInstallments(doc, spotlightPrice);
        for (String card : cards) {
            creditCards.add(
                    CreditCardBuilder.create()
                            .setBrand(card)
                            .setInstallments(installments)
                            .setIsShopCard(false).build());
        }
        return creditCards;
    }

    private Installments scrapInstallments(Document doc, Double spotlightPrice) throws MalformedPricingException {
        Installments installments = new Installments();
        installments.add(
                InstallmentBuilder.create()
                        .setInstallmentNumber(1)
                        .setInstallmentPrice(spotlightPrice).build());

        if (doc.selectFirst(".condition") != null) {
            int parcels = CrawlerUtils.scrapIntegerFromHtml(doc, ".parcels", true, 0);
            Double price = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".parcel-value", null, true, ',', session);
            installments.add(InstallmentBuilder.create()
                    .setInstallmentNumber(parcels)
                    .setInstallmentPrice(price)
                    .build());
        }
        return installments;
    }

    private BankSlip scrapBankSlip(Document doc, Double spotlightPrice) throws MalformedPricingException {
        if (doc.selectFirst(".savings") != null) {
            Double price = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".savings > b", null, true, ',', session);
            return BankSlipBuilder.create()
                    .setFinalPrice(price)
                    .build();

        }
        return BankSlipBuilder.create()
                .setFinalPrice(spotlightPrice)
                .build();
    }


    //TODO rename
    private BankSlip scrapBankSlip2(Document doc) throws MalformedPricingException {

        Element rows = doc.selectFirst("div.content > table:nth-child(1)");
        if (rows != null) {
            Element line = rows.selectFirst("nobr");
            String price = line.text();
            if (price.contains("(")) {
                String[] prices = price.split("\\(");
                String mainPrice = prices[0];
                String percentage = prices[1];
                //TODO use percentage
                Double finalPrice = Double.parseDouble(mainPrice.replaceAll("[^0-9,]", "").replace(",", "."));

                return BankSlipBuilder.create()
                        .setFinalPrice(finalPrice)
                        .build();
            }
            Double finalPrice = CrawlerUtils.scrapDoublePriceFromHtml(line, null, null, true, ',', this.session);
            return BankSlipBuilder.create()
                    .setFinalPrice(finalPrice)
                    .build();
        }
        return new BankSlip();
    }

    //TODO rename
    private Installments scrapInstallments2(Document doc) throws MalformedPricingException {
        Installments installments = new Installments();

        Element rows = doc.selectFirst("div.content > table:nth-child(5)");
        if (rows != null) {
            for (Element line : rows.select("div")) {
                String price = line.text();
                Integer parcel = 1;
                Double installmentPrice = null;
                Double totalPrice = null;

                if (price.contains("x")) {
                    String[] splittedText = price.split("x");
                    parcel = Integer.parseInt(splittedText[0].trim());
                    if (splittedText[1].contains("(")) {
                        String[] fullPrice = splittedText[1].split("\\(");
                        installmentPrice = Double.parseDouble(fullPrice[0].replaceAll("[^0-9,]", "").replace(",", "."));
                        totalPrice = Double.parseDouble(fullPrice[1].replaceAll("[^0-9,]", "").replace(",", "."));

                        installments.add(InstallmentBuilder.create()
                                .setInstallmentNumber(parcel)
                                .setInstallmentPrice(installmentPrice)
                                .setFinalPrice(totalPrice)
                                .build());
                    }
                    installmentPrice = Double.parseDouble(splittedText[1].replaceAll("[^0-9,]", "").replace(",", "."));
                } else {
                    installmentPrice = CrawlerUtils.scrapDoublePriceFromHtml(line, null, null, true, ',', session);
                }
                installments.add(InstallmentBuilder.create()
                        .setInstallmentNumber(parcel)
                        .setInstallmentPrice(installmentPrice)
                        .build());
            }
        }
        return installments;
    }

    private RatingsReviews scrapRatingReviews(Document doc) {
        RatingsReviews ratingReviews = new RatingsReviews();
        ratingReviews.setDate(session.getDate());

        Integer totalComments = CrawlerUtils.scrapIntegerFromHtml(doc, ".votes", true, 0);
        Double avgRating = Double.parseDouble(doc.selectFirst(".average").text());

        ratingReviews.setTotalRating(totalComments);
        ratingReviews.setTotalWrittenReviews(totalComments);
        ratingReviews.setAverageOverallRating(avgRating);

        return ratingReviews;
    }

    private Document sendRequest(String str) {
        Request request = RequestBuilder.create().setUrl(str).build();
        Response response = dataFetcher.get(session, request);
        String html = response.getBody();
        return Jsoup.parse(html);
    }

    private String scrapVariationId(Element variation) {
        return CrawlerUtils.scrapStringSimpleInfoByAttribute(variation, null, "value");
    }

    private String scrapVariationName(Element variation) {
        return CrawlerUtils.scrapStringSimpleInfo(variation, null, true);
    }

}
