package br.com.lett.crawlernode.crawlers.riodejaneiro;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.fetcher.DataFetcher;
import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.kernel.task.Crawler;
import br.com.lett.crawlernode.kernel.task.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;


public class RiodejaneiroDrogariavenancioCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.drogariavenancio.com.br/";

	public RiodejaneiroDrogariavenancioCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if( isProductPage(this.session.getUrl()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			Element variationElement = doc.select(".variacao").first();

			// Pre id interno
			String preInternalId = null;
			Element elementPreInternalId = doc.select("#miolo .produtoPrincipal .info .codigo").first();
			if(elementPreInternalId != null) {
				preInternalId = elementPreInternalId.text().split(":")[1].trim();
			}

			// Pid
			String internalPid = null;
			Element elementInternalPid = doc.select("input[name=IdProduto]").first();
			if (elementInternalPid != null) {
				internalPid = elementInternalPid.attr("value").trim();
			}

			// Pre nome
			String preName = null;
			Element elementName = doc.select("#NomeProduto").first();
			if(elementName != null) {
				preName = elementName.text();
			}

			// Preço
			Float price = null;
			Element elementPrice = doc.select(".valores .preco .precoPor span").first();
			if(elementPrice != null) {
				price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

			// Categories
			ArrayList<String> categories = this.crawlCategories(doc);
			String category1 = getCategory(categories, 0);
			String category2 = getCategory(categories, 1);
			String category3 = getCategory(categories, 2);

			// Descricao
			String description = "";
			Element elementDescriptionTab1 = doc.select("#miolo .abas div .aba1").first();
			Element elementDescriptionTab2 = doc.select("#miolo .abas div .aba2").first();
			if(elementDescriptionTab1 != null) description = description + elementDescriptionTab1.text();
			if(elementDescriptionTab2 != null) description = description + elementDescriptionTab2.text();

			// Marketplace
			JSONArray marketplace = null;

			// Separando dois caso
			// Caso 1 - produto não possui variação
			// Caso 2 - produto possui múltiplas variações

			if(variationElement == null || (variationElement != null && variationElement.select(".optionsVariacao li").size() == 0)) { //Caso 1

				// Id interno
				Element internalIdElement = doc.select("input[name=IdProduto]").first();
				String internalId = preInternalId + "-" + internalIdElement.attr("value");

				// Nome
				String name = preName;

				// Disponibilidade
				boolean available = true;
				Element elementButtonSoldOut = doc.select(".esgotado-detalhes[style=display:block;]").first();
				if(elementButtonSoldOut != null) {
					available = false;
				}

				// Imagem primária
				String primaryImage = null;
				Element elementPrimaryImage = doc.select(".produtoPrincipal .imagem .holder .cloud-zoom .foto").first();
				if(elementPrimaryImage != null) {
					String src = elementPrimaryImage.attr("src");
					if( !src.contains("imagemindisponivel") ) {
						primaryImage = src;
					}	
				}

				// Imagens secundárias
				String secondaryImages = null;
				JSONArray secondaryImagesArray = new JSONArray();
				Elements elementsSecondaryImage = doc.select(".produtoPrincipal .imagem .multifotos ul li a .foto");

				if(elementsSecondaryImage.size() > 0) {
					for(Element secondaryImage : elementsSecondaryImage) {
						String image = secondaryImage.attr("src").replaceAll("/produto/", "/Produto/");
						if( !image.equals(primaryImage) && !image.contains("imagemindisponivel") ) {
							secondaryImagesArray.put(image);
						}
					}
				}
				if(secondaryImagesArray.length() > 0) {
					secondaryImages = secondaryImagesArray.toString();
				}

				// Estoque
				Integer stock = null;

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

			}

			else {	// Caso 2

				Elements elementsVariations = doc.select(".optionsVariacao li");
				for(Element elementVariation : elementsVariations) {

					// Id interno
					// O id interno é montado usando o código do produto appendado do id interno da variação
					String internalId = null;
					String posInternalId = null;
					posInternalId = elementVariation.attr("data-value").split("#")[3];
					internalId = preInternalId + "-" + posInternalId;

					// Nome
					String name = null;
					Element elementPosName = elementVariation.select("[title]").first();
					if(elementPosName != null) {
						name = preName + " - " + elementPosName.attr("title");
					}

					// Estoque
					Integer stock = null;
					String scriptName = elementVariation.attr("onclick");
					stock = Integer.valueOf(scriptName.split("'")[5]);

					// Preço
					String plus = scriptName.split("'")[7].replace(',', '.');
					price = price + Float.valueOf(plus);

					// Disponibilidade
					boolean available = true;
					if(stock == 0) {
						available = false;
					}

					// Imagens

					// Requisição POST para conseguir dados da imagem
					String response = DataFetcher.fetchString(DataFetcher.POST_REQUEST, session, "http://www.drogariavenancio.com.br/ajax/gradesku_imagem_ajax.asp", assembleUrlParameters(session.getUrl().split("/")[4], posInternalId), null);

					String imageId = parseImageId(response);
					Element elementPrimaryImage = doc.select(".produtoPrincipal .imagem .holder .cloud-zoom .foto").first();
					String primaryImage = null;
					if(elementPrimaryImage != null && imageId != null) {
						primaryImage = "http://static-webv8.jet.com.br/drogariavenancio/Produto/" + imageId + ".jpg";
					}

					String secondaryImages = null;
					JSONArray secondaryImagesArray = new JSONArray();
					Elements elementsSecondaryImage = doc.select(".produtoPrincipal .imagem .multifotos ul li a .foto");

					if(elementsSecondaryImage.size() > 0) {
						for(Element secondaryImage : elementsSecondaryImage) {
							String tmp = secondaryImage.attr("src").replaceAll("/produto/", "/Produto/");
							if( !tmp.equals(primaryImage) && !tmp.contains("imagemindisponivel") ) {
								secondaryImagesArray.put(secondaryImage.attr("src"));
							}
						}
					}
					if(secondaryImagesArray.length() > 0) {
						secondaryImages = secondaryImagesArray.toString();
					}

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

				}
			}

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + session.getSeedId());
		}

		return products;
	}

	/*******************************
	 * Product page identification *
	 *******************************/
	private boolean isProductPage(String url) {
		return url.contains("/produto/");
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();

		Element mainCategory = document.select(".breadCrumbs a").last();
		if (mainCategory != null) {
			categories.add(mainCategory.text().trim());
		}

		Element subCategory = document.select(".breadCrumbs a .hierarquia").first();
		if (subCategory != null) {
			categories.add(subCategory.text().trim());
		}

		return categories;
	}
	
	private String getCategory(ArrayList<String> categories, int n) {
		if (n < categories.size()) {
			return categories.get(n);
		}

		return "";
	}

	private String assembleUrlParameters(String idProduto, String variacaoCombinacao) {
		String urlParameters = "IdProduto=" + idProduto + 
				"&VariacaoCombinacao=" + variacaoCombinacao +
				"&paginaOrigem=Grade" +
				"&pIdEmpresa=" + 40 +
				"&pNomePasta=drogariavenancio" +
				"&pCaminhoDesignLoja=http%3A%2F%2Fstatic";

		return urlParameters;

	}

	private String parseImageId(String response) {
		String tmp = response.split("\\.")[0];
		if (tmp != null) {
			return tmp.substring(tmp.indexOf('=') + 1);
		}
		return null;
	}

}