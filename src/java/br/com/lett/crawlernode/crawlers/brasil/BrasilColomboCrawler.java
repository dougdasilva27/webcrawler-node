package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.kernel.task.Crawler;
import br.com.lett.crawlernode.kernel.task.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;

public class BrasilColomboCrawler extends Crawler {

	public BrasilColomboCrawler(CrawlerSession session) {
		super(session);
	}


	@Override
	public boolean shouldVisit() {
		String href = session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && href.startsWith("https://www.colombo.com.br");
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		Element productElement = doc.select(".detalhe-produto").first();

		if (session.getUrl().startsWith("https://www.colombo.com.br/produto/") && !session.getUrl().contains("?") && (productElement != null)) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			Elements selections = doc.select(".dados-itens-table.dados-itens-detalhe tr");

			// ID interno
			String internalId = null;
			Element elementInternalID = doc.select("input[type=radio][checked]").first();
			if (elementInternalID != null) {
				internalId = elementInternalID.attr("value").trim();
			}

			// Pid
			String internalPid = null;
			Element elementInternalPid = doc.select(".codigo-produto").first();
			if (elementInternalPid != null) {
				internalPid = elementInternalPid.attr("content").trim();
			}

			// Nome
			String name = null;
			Element elementName = doc.select("h1.nome-produto").first();
			if (elementName != null) {
				name = elementName.text().trim();
			}

			// Preço
			Float price = null;
			Element elementPrice = doc.select(".dados-condicao--texto b span").first();
			if (elementPrice != null) {
			    price = Float.parseFloat(elementPrice.text().trim().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

			// Categoria
			String category1 = "";
			String category2 = "";
			String category3 = "";
			Elements elementCategories = doc.select(".breadcrumb a");
			for (int i = 0; i < elementCategories.size()-1; i++) {
				if (category1.isEmpty()) {
					category1 = elementCategories.get(i).text().trim();
				} else if (category2.isEmpty()) {
					category2 = elementCategories.get(i).text().trim();
				} else if (category3.isEmpty()) {
					category3 = elementCategories.get(i).text().trim();
				}
			}

			// Descrição
			String description = "";
			Element elementDescription = doc.select("#produto-descricao").first();
			Element elementSpecs = doc.select("#produto-caracteristicas").first();
			if (elementDescription != null) {
				description = description + elementDescription.html().trim();
			}
			if (elementSpecs != null) {
				description = description + elementSpecs.html();
			}

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			if (selections.size() <= 1) { // sem variações

				// Disponibilidade
				boolean available = true;
				Element elementUnavailable = doc.select(".form-indisponivel").first();
				if (elementUnavailable != null) {
					available = false;
				}
				if (available == false) {
					price = null;
				}

				// Imagens
				Elements elementImages = doc.select("li.js_slide picture img");
				String primaryImage = null;
				String secondaryImages = null;
				JSONArray secondaryImagesArray = new JSONArray();

				for (Element e : elementImages) {
					String image = "http:" + e.attr("src").trim().replace("400x400", "800x800");
					if (primaryImage == null) {
						primaryImage = this.sanitizeImageURL(image);						
					} else {
						secondaryImagesArray.put(this.sanitizeImageURL(image));
					}
				}
				if (secondaryImagesArray.length() > 0) {
					secondaryImages = secondaryImagesArray.toString();
				}				

				Product product = new Product();
				product.setUrl(session.getUrl());
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

			}

			else { // múltiplas variações

				Elements variations = doc.select(".dados-itens-table.dados-itens-detalhe tr");
				String primaryImageTmp = null;
				String secondaryImagesTmp = null;

				for(Element e : variations) {

					// ID interno
					String variationInternalId = null;
					Element variationElementInternalID = e.select("input").first();
					if (variationElementInternalID != null) {
						variationInternalId = variationElementInternalID.attr("value").trim();
					}

					// Nome
					String variationName = null;
					Element variationElementName = e.select(".dados-itens-table-caracteristicas").first();
					if (variationElementName != null) {
						variationName = name + " " + variationElementName.textNodes().get(0).toString().trim();
					}

					// Available
					boolean variationAvailable = true;
					Element variationElementAvailable = e.select(".dados-itens-table-estoque").first();
					if (variationElementAvailable != null) {
						String tmp = variationElementAvailable.text().trim();
						if (tmp.contains("Esgotado")) {
							variationAvailable = false;
						}
					}					

					// Imagens -- Caso principal
					Elements elementImages = doc.select("li.js_slide picture img");
					String secondaryImages = null;
					String primaryImage = null;
					JSONArray secondaryImagesArray = new JSONArray();
					boolean getTmp = true;
					for (Element el : elementImages) {
						String image = "http:" + el.attr("src").trim().replace("400x400", "800x800");
						String tmp = el.attr("src").trim().replace("400x400", "800x800");
						if (primaryImage == null && el.attr("data-item").equals(variationInternalId)) {
							primaryImage = image;
							for (Element im : elementImages) {
								if ( !im.attr("src").trim().replace("400x400", "800x800").equals(tmp) ) {
									secondaryImagesArray.put("http:" + im.attr("src").trim().replace("400x400", "800x800"));
								}
							}
							if (secondaryImagesArray.length() > 0) {
								secondaryImages = secondaryImagesArray.toString();
							}
							primaryImageTmp = image;
							secondaryImagesTmp = secondaryImages;
							getTmp = false;
							break;
						}
					}
					if (getTmp) {
						primaryImage = primaryImageTmp;
						secondaryImages = secondaryImagesTmp;
					}

					// Imagens -- caso especial
					if (primaryImage == null && secondaryImages == null) {
						for (Element el : elementImages) {
							if (primaryImage == null && el.attr("data-slide-position").equals("0")) { // imagem primária 
								primaryImage = "http:" + el.attr("src").trim().replace("400x400", "800x800");
								getTmp = false;
							} else {
								secondaryImagesArray.put( "http:" + el.attr("src").trim().replace("400x400", "800x800") );
							}
						}
						if (secondaryImagesArray.length() > 0) {
							secondaryImages = secondaryImagesArray.toString();
						}

						primaryImageTmp = primaryImage;
						secondaryImagesTmp = secondaryImages;
						if (getTmp) {
							primaryImage = primaryImageTmp;
							secondaryImages = secondaryImagesTmp;
						}
					}

					// sanitize secondary images
					JSONArray sanitizedSecondaryImages = new JSONArray();
					for (int j = 0; j < secondaryImagesArray.length(); j++) {
						String image = (String) secondaryImagesArray.get(j);
						String sanitized = this.sanitizeImageURL(image);

						sanitizedSecondaryImages.put(sanitized);
					}
					secondaryImages = sanitizedSecondaryImages.toString();

					Product product = new Product();
					product.setUrl(session.getUrl());
					product.setInternalId(variationInternalId);
					product.setInternalPid(internalPid);
					product.setName(variationName);
					product.setPrice(price);
					product.setCategory1(category1);
					product.setCategory2(category2);
					product.setCategory3(category3);
					product.setPrimaryImage( this.sanitizeImageURL(primaryImage) );
					product.setSecondaryImages(secondaryImages);
					product.setDescription(description);
					product.setStock(stock);
					product.setMarketplace(marketplace);
					product.setAvailable(variationAvailable);

					products.add(product);

				}

			}

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}

		return products;
	}

	private String sanitizeImageURL(String imageURL) {
		String sanitizedURL = null;

		if (imageURL.contains("?")) { // removendo parâmetros da url da imagem, senão não passa no crawler de imagens
			int index = imageURL.indexOf("?");
			sanitizedURL = imageURL.substring(0, index);
		} else {
			sanitizedURL = imageURL;
		}

		return sanitizedURL;		
	}
}
