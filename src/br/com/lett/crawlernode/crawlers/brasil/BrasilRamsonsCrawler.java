package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.Crawler;
import br.com.lett.crawlernode.kernel.CrawlerSession;
import br.com.lett.crawlernode.kernel.fetcher.DataFetcher;
import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.util.Logging;

public class BrasilRamsonsCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.ramsons.com.br/";

	public BrasilRamsonsCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}


	@Override
	public List<Product> extractInformation(Document doc) {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		// usar esse identificador para página de produto, caso necessite
		Element elementProduct = doc.select(".product_info_container").first();

		if ( isProductPage(this.session.getUrl()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			// ID interno
			String internalId = null;
			Element elementInternalId = doc.select("#ProdutoCodigo").first();
			if(elementInternalId != null) {
				internalId = elementInternalId.attr("value").trim();
			}

			// Pid
			String internalPid = null;
			Element internalPidElement = doc.select(".lstReference dd").get(1);
			if (internalPidElement != null) {
				internalPid = internalPidElement.text().trim();
			}

			// Nome
			String name = null;
			Element elementName = doc.select(".titProduto").first();
			if(elementName != null) {
				name = elementName.text();
			}

			// Preço
			Float price = null;
			Element elementPrice = doc.select(".price_content .price #spanPrecoPor strong").first();
			if(elementPrice != null) {
				price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

			// Disponibilidade
			boolean available = true;
			if (price == null) {
				available = false;
			}

			// Categoria
			String category1 = "";
			String category2 = "";
			String category3 = "";
			Elements elementCategories = doc.select("#breadcrumbs a");
			for (int i = 1; i < elementCategories.size(); i++) {
				if (category1.isEmpty()) {
					category1 = elementCategories.get(i).text().trim();
				} else if (category2.isEmpty()) {
					category2 = elementCategories.get(i).text().trim();
				} else if (category3.isEmpty()) {
					category3 = elementCategories.get(i).text().trim();
				}
			}

			// Imagem primária
			String primaryImage = null;
			Element elementPrimaryImage = doc.select("a#Zoom1").first();
			if(elementPrimaryImage != null) {
				primaryImage = elementPrimaryImage.attr("href");
			} 
			if (primaryImage == null || primaryImage.isEmpty()) {
				Element elementPrimaryImage2 = elementPrimaryImage.select("img").first();
				if (elementPrimaryImage2 != null) {
					primaryImage = elementPrimaryImage2.attr("src").trim();
				}
			}

			// Imagens secundárias
			Elements elementImages = elementProduct.select(".lstThumbs li a img");
			String secondaryImages = null;
			JSONArray secondaryImagesArray = new JSONArray();

			for(Element image : elementImages) {
				if( !image.attr("src").isEmpty() ) {
					if (doc.select(".cliqueParaAmpliar").first() != null) { // significa que tem a ampliada

						// Olhar se a versão da imagem realmente existe
						// Foi preciso fazer essa checagem, pois haviam imagens secundárias de um produto
						// onde a url da imagem secundária de um produto não foi encontrada
						String imgAmpliada = image.attr("src").replace("Detalhes", "Ampliada");
						Integer responseCode = DataFetcher.getUrlResponseCode(imgAmpliada, this.session, 1);

						if (responseCode != null && responseCode != 404) {
							secondaryImagesArray.put(image.attr("src").replace("Detalhes", "Ampliada"));
						} else {
							secondaryImagesArray.put(image.attr("src"));
						}
					} else {
						secondaryImagesArray.put(image.attr("src"));
					}
				}
			}			
			if (secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			String description = "";
			Element elementDescription = doc.select("#box_descricao").first();
			Element elementSpecs = doc.select("#box_caracteristicas").first();
			if (elementDescription != null) {
				description = description + elementDescription.html();
			}
			if (elementSpecs != null) {
				description = description + elementSpecs.html();
			}

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			Product product = new Product();
			product.setSeedId(this.session.getSeedId());
			product.setUrl(this.session.getUrl());
			product.setInternalId(internalId);
			product.setInternalPid(internalPid);
			product.setName(name);
			product.setPrice(price);
			product.setCategory1(category1);
			product.setCategory2(category2);
			product.setCategory3(category3);
			product.setPrimaryImage(primaryImage);
			product.setSecondaryImages(secondaryImages);
			product.setDescription(description);
			product.setStock(stock);
			product.setMarketplace(marketplace);
			product.setAvailable(available);
			
			products.add(product);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}
		
		return products;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return url.endsWith("/p");
	}

}
