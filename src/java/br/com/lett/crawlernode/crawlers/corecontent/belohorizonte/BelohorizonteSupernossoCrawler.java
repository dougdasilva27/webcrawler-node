package br.com.lett.crawlernode.crawlers.corecontent.belohorizonte;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VtexRatingCrawler;
import models.RatingsReviews;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.Arrays;
import java.util.List;

/**
 * Date: 01/09/2017
 *
 * @author Gabriel Dornelas
 */
public class BelohorizonteSupernossoCrawler extends VTEXOldScraper {

    private static final String HOME_PAGE = "https://www.supernossoemcasa.com.br/";
    private static final List<String> MAIN_SELLER_NAME_LOWER = Arrays.asList("super nosso em casa");

    public BelohorizonteSupernossoCrawler(Session session) {
        super(session);
    }

    @Override
    protected String getHomePage() {
        return HOME_PAGE;
    }

    @Override
    protected List<String> getMainSellersNames() {
        return MAIN_SELLER_NAME_LOWER;
    }

    @Override
    protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
        return new VtexRatingCrawler(session, HOME_PAGE, logger, cookies)
                .extractRatingAndReviews(internalId, doc, dataFetcher);
    }
}
