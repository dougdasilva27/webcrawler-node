package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;

public class SaopauloImigrantesbebidasCrawler extends CrawlerRankingKeywords {

    private static final String BASE_URL = "www.imigrantesbebidas.com.br";

    public SaopauloImigrantesbebidasCrawler(Session session) {
        super(session);
    }

    @Override
    protected void extractProductsFromCurrentPage() throws MalformedProductException {
        this.log("Página " + this.currentPage);

        this.pageSize = 20;
        String url = "https://www.imigrantesbebidas.com.br/bebida/advanced_search_result.php?page=" + this.currentPage + "&keywords=" + this.keywordEncoded;

        this.log("Link onde são feitos os crawlers: " + url);
        this.currentDoc = fetchDocument(url, cookies);

        Elements products = this.currentDoc.select(".categoryListing__list  .productRow__item");

        if (!products.isEmpty()) {
            if (this.totalProducts == 0)
                setTotalProducts();

            for (Element product : products) {
                String productUrl = CrawlerUtils.scrapUrl(product, ".product__link", "href", "https", BASE_URL);
                String internalId = scrapInternalId(product);
                String internalPid = internalId;
                String name = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".productItem__name", "title");
                String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(product, ".productItem__image > img", List.of("data-src"), "https", BASE_URL);
                Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".productItem__price .productItem__price--value", null, false, ',', session, 0);
                boolean isAvailable = product.selectFirst("article") != null && !product.selectFirst("article").classNames().contains("productItem--out-of-stock");

                RankingProduct productRanking = RankingProductBuilder.create()
                    .setUrl(productUrl)
                    .setInternalId(internalId)
                    .setInternalPid(internalPid)
                    .setName(name)
                    .setImageUrl(imageUrl)
                    .setPriceInCents(price)
                    .setAvailability(isAvailable)
                    .build();

                saveDataProduct(productRanking);

                if (this.arrayProducts.size() == productsLimit)
                    break;
            }
        } else {
            this.result = false;
            this.log("Keyword sem resultado!");
        }

        this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
    }

    @Override
    protected void setTotalProducts() {
        this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".pagination > li:nth-last-child(1) > a", true, 0);
        super.setTotalProducts();
    }

    @Override
    protected boolean hasNextPage() {
        return this.currentPage < totalProducts;
    }

    private String scrapInternalId(Element product) {
        String id = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".frmCartQuantity input[name=\"products_id\"]", "value");
        if (id == null) {
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".productItem__image > img", "data-src");
            String[] split = imageUrl.split("products/");
            id = split[1].split("_")[0];
            id = id.split("-")[0];
        }
        return id;
    }
}
