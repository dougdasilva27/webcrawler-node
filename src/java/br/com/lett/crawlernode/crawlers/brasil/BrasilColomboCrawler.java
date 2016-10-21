package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;

public class BrasilColomboCrawler extends Crawler {

	public BrasilColomboCrawler(CrawlerSession session) {
		super(session);
	}


	@Override
	public boolean shouldVisit() {
		String href = session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && href.startsWith("https://www.colombo.com.br");
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		Element productElement = doc.select(".detalhe-produto").first();

		if (session.getOriginalURL().startsWith("https://www.colombo.com.br/produto/") && !session.getOriginalURL().contains("?") && (productElement != null)) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			Elements selections = doc.select(".dados-itens-table.dados-itens-detalhe tr");

			// ID interno
			String internalId = null;
			Element elementInternalID = doc.select("input[type=radio][checked]").first();
			if (elementInternalID != null) {
				internalId = elementInternalID.attr("value").trim();
			} else {
				elementInternalID = doc.select("#itemAviso").first();
				if (elementInternalID != null) {
					internalId = elementInternalID.attr("value").trim();
				}
				
			}

			// Pid
			String internalPid = null;
			Element elementInternalPid = doc.select(".codigo-produto").first();
			if (elementInternalPid != null) {
				internalPid = elementInternalPid.attr("content").trim();
				if(internalPid.isEmpty()){
					internalPid = elementInternalPid.text().replaceAll("[^0-9]", "").trim();
				}
			}

			// Nome
			String name = null;
			Element elementName = doc.select("h1.nome-produto").first();
			if (elementName != null) {
				name = elementName.ownText().trim();
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

			Element elementPrimaryImage = doc.select("li.js_slide picture img[data-slide-position=0]").first();
			String primaryImage = null;
			if(elementPrimaryImage != null){				
				primaryImage = sanitizeImageURL("http:" + elementPrimaryImage.attr("src").trim().replace("400x400", "800x800"));
			} else {
				elementPrimaryImage = doc.select("li.js_slide picture img[data-slide-position=1]").first();
				
				if(elementPrimaryImage != null){
					primaryImage = sanitizeImageURL("http:" + elementPrimaryImage.attr("src").trim().replace("400x400", "800x800"));
				}
			}
			
			// Imagens -- Caso principal
			Elements elementSecondaryImages = doc.select("li.js_slide picture img");
			String secondaryImages = null;
			JSONArray secondaryImagesArray = new JSONArray();
			
			for (Element el : elementSecondaryImages) {
				String image = "http:" + el.attr("src").trim().replace("400x400", "800x800");
				if (!image.equals(primaryImage)) { // imagem primária 
					secondaryImagesArray.put( sanitizeImageURL( image ) );
				}
			}
			
			if (secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
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
	

				Product product = new Product();
				product.setUrl(session.getOriginalURL());
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


					Product product = new Product();
					product.setUrl(session.getOriginalURL());
					product.setInternalId(variationInternalId);
					product.setInternalPid(internalPid);
					product.setName(variationName);
					product.setPrice(price);
					product.setCategory1(category1);
					product.setCategory2(category2);
					product.setCategory3(category3);
					product.setPrimaryImage( primaryImage );
					product.setSecondaryImages(secondaryImages);
					product.setDescription(description);
					product.setStock(stock);
					product.setMarketplace(marketplace);
					product.setAvailable(variationAvailable);

					products.add(product);

				}

			}

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return products;
	}

	private String sanitizeImageURL(String imageURL) {
		String sanitizedURL = null;
		
		if(imageURL != null){
			if (imageURL.contains("?")) { // removendo parâmetros da url da imagem, senão não passa no crawler de imagens
				int index = imageURL.indexOf("?");
				sanitizedURL = imageURL.substring(0, index);
			} else {
				sanitizedURL = imageURL;
			}
		}

		return sanitizedURL;		
	}
}
