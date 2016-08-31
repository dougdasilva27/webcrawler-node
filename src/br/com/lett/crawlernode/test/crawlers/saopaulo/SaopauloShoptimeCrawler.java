package br.com.lett.crawlernode.test.crawlers.saopaulo;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.test.Logging;
import br.com.lett.crawlernode.test.kernel.fetcher.DataFetcher;
import br.com.lett.crawlernode.test.kernel.task.Crawler;
import br.com.lett.crawlernode.test.kernel.task.CrawlerSession;

public class SaopauloShoptimeCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.shoptime.com.br/";

	public SaopauloShoptimeCrawler(CrawlerSession session) {
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

		if ( isProductPage(this.session.getUrl()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			Elements elementsProductOptions = doc.select(".pure-select option"); // caso que tem seletor 110v e 220v

			/*
			 * ID interno
			 * 
			 * O id interno está sendo montado usando o código do produto (código exibido na página),
			 * acrescido de um hífen e em seguida o id interno do produto. Este id interno foi descoberto
			 * posteriormente.
			 */			
			String internalID = null;
			Element elementInternalID = doc.select(".p-name#main-product-name .p-code").first();
			if (elementInternalID != null) {
				internalID =  elementInternalID.text().split(" ")[1].replace(")", " ").trim() ;
			}
			String internalIDFirstPiece = internalID;

			// internalPid
			String internalPid = internalIDFirstPiece;

			boolean hasMoreProducts = this.hasMoreProducts(doc);

			// Disponibilidade
			boolean available = false;
			Element elementButtonBuy = doc.select(".btn-buy-form-submit.pure-button.pure-button-buy.button-large").first();
			Element soldAndDeliveredBy = doc.select(".p-deliveredby-store").first();
			if (elementButtonBuy != null) {
				if (soldAndDeliveredBy != null && soldAndDeliveredBy.text().equals("Shoptime")) {
					available = true;
				}
			}

			// Nome
			Element elementName = doc.select("#main-product-name").first();
			String name = null;
			if (elementName != null) {
				name = elementName.textNodes().get(0).toString().replace("'", "").replace("’","").trim();
			}

			// Preço
			Float preco = null;
			if (soldAndDeliveredBy != null && soldAndDeliveredBy.text().equals("Shoptime")) {
				preco = this.extractMainProductPagePrice(doc);
			}

			// Categorias
			Elements elementCategories = doc.select(".breadcrumb span a");
			String category1; 
			String category2; 
			String category3;

			String[] cat = new String[3];
			cat[0] = "";
			cat[1] = "";
			cat[2] = "";

			int j = 0;
			for(int i = 0; i < elementCategories.size(); i++) {
				Element e = elementCategories.get(i);
				cat[j] = e.text().toString();
				cat[j] = cat[j].replace(">", "");
				j++;
			}
			category1 = cat[0];
			category2 = cat[1];
			category3 = cat[2];

			Elements elementImages = doc.select(".p-gallery-image.swiper-slide a");

			String primaryImage = null;
			String secondaryImages = null;
			JSONArray secundaryImagesArray = new JSONArray();

			if (elementImages.size() > 0) {

				// Imagem primária
				primaryImage = elementImages.first().attr("href").trim();

				// Imagens secundárias
				for(int i = 1; i < elementImages.size(); i++) { // começando da segunda imagem porque a primeira é a imagem primária						
					String secondaryImage = elementImages.get(i).attr("href").trim();
					if (!secondaryImage.equals(primaryImage)) {
						secundaryImagesArray.put( secondaryImage );
					}
				}
			}

			if(secundaryImagesArray.length() > 0) {
				secondaryImages = secundaryImagesArray.toString();
			}				

			// Descrição
			String description = "";
			Element elementBasicInfo = doc.select("#basicinfotoggle").first();
			Element elementTechSpec = doc.select("#informacoes-tecnicas").first();
			if (elementBasicInfo != null) description = description + elementBasicInfo.html();
			if (elementTechSpec != null) description = description + elementTechSpec.html();

			// Marketplace -- vamos capturar essas informações logo antes de inserir no banco
			// o mesmo document capturado aqui será usado para ver marketplace das variações
			String urlMarketplaceInfo = this.session.getUrl().replaceAll("/produto/", "/parceiros/");
			Document docMarketplaceInfo = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, urlMarketplaceInfo, null, null);

			// Estoque
			Integer stock = null;

			if(hasMoreProducts) { // capturar e inserir os dois produtos da página

				Logging.printLogDebug(logger, session, "Página contém múltiplas variações de um produto.");					

				for(int i = 0; i < elementsProductOptions.size(); i++) {

					Element currentOption = elementsProductOptions.get(i);

					String variationInternalId = internalIDFirstPiece + "-" + currentOption.attr("value");
					String variationName = name + " - " + currentOption.text().trim();

					// Pegando marketplace
					JSONArray marketplace = new JSONArray();
					String partnersIdVector[] = currentOption.attr("data-partners").split(","); // pegando os ids dos parceiros que vendem essa variação
					ArrayList<String> partnersId = new ArrayList<String>();
					for (int k = 0; k < partnersIdVector.length; k++) {
						partnersId.add(partnersIdVector[k]);
					}

					Logging.printLogDebug(logger, session, "Pegando o marketplace da variação " + variationName + " ...");						

					Elements partnerLines = docMarketplaceInfo.select(".panel.nospacing .partners ul li[data-partner-id]");
					for(Element partnerLine : partnerLines) {
						String partnerName = null;
						Float partnerPrice = null;

						String dataPartnerId = partnerLine.attr("data-partner-id").trim();

						if ( this.isShoptimeId(dataPartnerId) ) {
							available = true;
							preco = Float.parseFloat(partnerLine.attr("data-partner-value").trim());
						}

						else {

							if (partnersId.contains(dataPartnerId)) { // se esse parceiro faz parte da lista de parceiros dessa variação de produto

								// nome do parceiro
								Element elementPartnerName = partnerLine.select(".partner-name .partner-logo [alt]").first();

								if(elementPartnerName != null) {
									partnerName = elementPartnerName.attr("alt");
								}

								// se o parceiro não for o Shoptime, então pegar o preço e inserir o parceiro no JSONArray
								partnerPrice = Float.parseFloat(partnerLine.attr("data-partner-value"));
								JSONObject partner = new JSONObject();
								partner.put("name", partnerName);
								partner.put("price", partnerPrice);

								marketplace.put(partner);
							}
						}

					}

					partnersId.remove("01");

					if (marketplace.length() < partnersId.size()) {

						String remainingPartner = null;

						if (soldAndDeliveredBy != null) {
							remainingPartner = soldAndDeliveredBy.text();
						}

						if (remainingPartner != null && !remainingPartner.equals("Shoptime")) {
							Float partnerPrice = this.extractMainProductPagePrice(doc);

							JSONObject partner = new JSONObject();
							partner.put("name", remainingPartner);
							partner.put("price", partnerPrice);

							marketplace.put(partner);
						}
					}


					Product product = new Product();
					product.setUrl(this.session.getUrl());
					product.setSeedId(this.session.getSeedId());
					product.setInternalId(variationInternalId);
					product.setInternalPid(internalPid);
					product.setName(variationName);
					product.setPrice(preco);
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

			}

			// inserir o único produto da página
			else {

				// montando restante do internalId
				Element secondPartInternalId = doc.select(".toggle-container[data-sku]").first();
				if (secondPartInternalId != null) {
					internalID = internalID + "-" + secondPartInternalId.attr("data-sku").trim();
				}

				// Pegando o marketplace
				Logging.printLogDebug(logger, session, "Pegando marketplace de produto único...");

				JSONArray marketplace = new JSONArray();

				// capturar o marketplace no caso de produto sem variação
				Elements partnerLines = docMarketplaceInfo.select(".panel.nospacing .partners ul li[data-partner-id]");
				for(Element partnerLine : partnerLines) {
					String partnerName = null;
					Float partnerPrice = null;

					String dataPartnerId = partnerLine.attr("data-partner-id").trim();

					if ( dataPartnerId != null && (dataPartnerId.equals("01") || dataPartnerId.isEmpty()) ) {
						available = true;
						preco = Float.parseFloat(partnerLine.attr("data-partner-value").trim());
					}

					else {

						// nome do parceiro
						Element elementPartnerName = partnerLine.select(".partner-name .partner-logo [alt]").first();

						if(elementPartnerName != null) {
							partnerName = elementPartnerName.attr("alt");
						}

						// se o parceiro não for o Shoptime, então pegar o preço e inserir o parceiro no JSONArray
						if(partnerName != null) {
							if( dataPartnerId != null && !dataPartnerId.equals("01") ) {
								partnerPrice = Float.parseFloat(partnerLine.attr("data-partner-value"));
								JSONObject partner = new JSONObject();
								partner.put("name", partnerName);
								partner.put("price", partnerPrice);

								marketplace.put(partner);
							}
						}
					}

				}

				// parceiro da página principal
				String remainingPartner = null;

				if (soldAndDeliveredBy != null) {
					remainingPartner = soldAndDeliveredBy.text();
				}

				if (remainingPartner != null && !remainingPartner.equals("Shoptime")) {
					Float partnerPrice = this.extractMainProductPagePrice(doc);

					JSONObject partner = new JSONObject();
					partner.put("name", remainingPartner);
					partner.put("price", partnerPrice);

					marketplace.put(partner);
				}

				Product product = new Product();
				product.setUrl(this.session.getUrl());
				product.setSeedId(this.session.getSeedId());
				product.setInternalId(internalID);
				product.setInternalPid(internalPid);
				product.setName(name);
				product.setPrice(preco);
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

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}

		return products;
	}


	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return url.startsWith("http://www.shoptime.com.br/produto/");
	}

	private Float extractMainProductPagePrice(Document doc) {
		Element elementPriceFirst = doc.select("div .p-prices .p-price").first();
		Float preco = null;
		if (elementPriceFirst != null) {
			Element elementPrice = elementPriceFirst.select(".value").first();
			preco = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

			return preco;
		}

		return null;
	}


	private boolean hasMoreProducts(Document doc) {
		if ( doc.select(".pure-select option").size() > 0 ) return true;

		return false;
	}

	//	private String extractPrimaryImage(Document doc) {
	//		Element elementPrimaryImage = doc.select(".p-link").first();
	//		if (elementPrimaryImage != null) {
	//			return elementPrimaryImage.attr("href").trim();
	//		}
	//
	//		return null;
	//	}

	private boolean isShoptimeId(String id) {
		if (id == null) return true;
		if (id.isEmpty()) return true;
		if (id.equals("01")) return true;

		return false;
	}

}
