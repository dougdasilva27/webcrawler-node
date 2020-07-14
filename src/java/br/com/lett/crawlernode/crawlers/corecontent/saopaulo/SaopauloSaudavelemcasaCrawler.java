package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip;
import models.pricing.CreditCards;
import models.pricing.Pricing;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SaopauloSaudavelemcasaCrawler extends Crawler {
    private static final String BASE_URL = "https://www.saudavelemcasa.com.br/";

    public SaopauloSaudavelemcasaCrawler(Session session) {
        super(session);
    }

    @Override
    public List<Product> extractInformation(Document doc) throws Exception {
        super.extractInformation(doc);
        List<Product> products = new ArrayList<>();
        if (isProductPage(doc)) {

            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#produto-id", "value");
            String internalPId = internalId;
            String name = CrawlerUtils.scrapStringSimpleInfo(doc, "#produto-nome", true);
            String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".thumbnails > a", Collections.singletonList("href"), "https", "static.saudavelemcasa.com.br");
            String secondaryImage = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".thumbnails > a", Collections.singletonList("href"), "https", "static.saudavelemcasa.com.br", primaryImage);
            CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb > a", true);
            String description = CrawlerUtils.scrapElementsDescription(doc, Collections.singletonList(".tab:not(:last-child)"));
            boolean availability = scrapAvailability(doc);
            Offers offers = availability ? scrapOffers(doc) : new Offers();
            RatingsReviews ratingsReviews = scrapRatings(doc);

        }


        return products;
    }

    private boolean isProductPage(Document doc) {
        return doc.selectFirst(".product") != null;
    }

    private boolean scrapAvailability(Document doc) {
        return doc.selectFirst(".btn-comprar.disabled") != null;
    }

    private Offers scrapOffers(Document doc) {
        Pricing pricing = scrapPricing(doc);
        return new Offers();
    }

    private Pricing scrapPricing(Document doc) {
        Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#preco-antigo", null, true, , ',', this.session);
        Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#preco", null, true, , ',', this.session);

        Pricing.PricingBuilder.create()
                .setSpotlightPrice(spotlightPrice)
                .setPriceFrom(priceFrom)
                .setCreditCards(scrapCreditCards(doc))
                .setBankSlip(scrapBankSlip(doc))
                .build();
    }

    private CreditCards scrapCreditCards(Document doc) {
    }

    private BankSlip scrapBankSlip(Document doc) {
        Double finalPrice =

        BankSlip.BankSlipBuilder.create()
                .setFinalPrice()
                .setOnPageDiscount()
                .build();
    }

    private RatingsReviews scrapRatings(Document doc) {
    }
}
