package br.com.lett.crawlernode.crawlers.corecontent.belohorizonte;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.fetcher.Fetcher;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;

public class BelohorizonteSupernossoCrawler extends Crawler {
	
	public BelohorizonteSupernossoCrawler(Session session) {
		super(session);
		super.config.setFetcher(Fetcher.SMART);
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// internal id
			String internalId = crawlInternalId(doc);

			// name
			String name = crawlName(doc);
//
//			// Preço
//			Elements elementPrice = doc.select(".preco");
//			Float price = Float.parseFloat(elementPrice.last().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
//
//			String category1;
//			String category2;
//			String category3;
//			category1 = null;
//			category2 = null;
//			category3 = null;
//
//			// Imagem primária
//			Element elementPrimaryImage = doc.select(".produto-detalhe-img").first();
//			String primaryImage = null;
//			if (elementPrimaryImage != null) {
//				primaryImage = elementPrimaryImage.attr("src").trim();
//			}
//			if (primaryImage.contains("produto_sem_foto"))
//				primaryImage = "";
//
//			// Imagens secundárias
//			String secondaryImages = null;
//
			// description
			String description = crawlDescription(doc);
//
//			// Marketplace
//			JSONArray marketplace = null;
//
//			// Disponibilidade
//			boolean available = true;
//
//			// Estoque
//			Integer stock = null;

			Product product = new Product();
			product.setUrl(this.session.getOriginalURL());
			product.setInternalId(internalId);
			product.setName(name);
//			product.setPrice(price);
//			product.setCategory1(category1);
//			product.setCategory2(category2);
//			product.setCategory3(category3);
//			product.setPrimaryImage(primaryImage);
//			product.setSecondaryImages(secondaryImages);
			product.setDescription(description);
//			product.setStock(stock);
//			product.setMarketplace(marketplace);
//			product.setAvailable(available);
			
			products.add(product);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
		}
		
		return products;
	}

	private boolean isProductPage(String url) {
		return url.startsWith("https://www.supernossoemcasa.com.br/e-commerce/p/");
	}
	
	private String crawlInternalId(Document document) {
		String internalId = null;
		Element internalIdElement = document.select("div.snc-product-code span[itemprop=sku]").first();
		if (internalIdElement != null) {
			internalId = internalIdElement.text().trim();
		}
		return internalId;
	}
	
	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("div.snc-product-info h1.snc-product-name").first();
		if (nameElement != null) {
			name = nameElement.text().trim();
		}
		return name;
	}
	
	private String crawlDescription(Document document) {
		StringBuilder description = new StringBuilder();
		
		Element productInfoElement = document.select("section.snc-product-info").first();
		if (productInfoElement != null) {
			description.append(productInfoElement.html());
		}
		
		return description.toString();
	}
}