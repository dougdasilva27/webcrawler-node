package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;

public class BrasilErgovitaCrawler extends Crawler {

    public BrasilErgovitaCrawler(Session session) {
        super(session);
    }

    @Override
    public List<Product> extractInformation(Document doc) throws Exception {
        List<Product> products = new ArrayList<>();

         /*if(isProductPage()){
             JSONObject jsonInfo = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"application/ld+json\"]", "", null, false, false);
         }*/

        return super.extractInformation(doc);
    }
}
