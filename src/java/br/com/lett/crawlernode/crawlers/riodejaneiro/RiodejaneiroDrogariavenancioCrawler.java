package br.com.lett.crawlernode.crawlers.riodejaneiro;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;

public class RiodejaneiroDrogariavenancioCrawler extends Crawler {

	public RiodejaneiroDrogariavenancioCrawler(CrawlerSession session) {
		super(session);
	}


	@Override
	public boolean shouldVisit() {
		String href = session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && href.startsWith("http://www.drogariavenancio.com.br/");
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if( session.getOriginalURL().contains("/produto/") ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

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

			// Categorias
			String category1 = "";
			String category2 = "";
			String category3 = "";
			ArrayList<String> categories = new ArrayList<String>();
			
			Elements categ = doc.select("#miolo .breadCrumbs a");
			
			if(categ.size() > 1) categories.add(categ.get(1).text());
			Elements subCategoriesElements = doc.select("#miolo .breadCrumbs h3 p");
			for(Element e : subCategoriesElements) {
				categories.add(e.text());
			}
			for(String category : categories) {
				if(category1.isEmpty()) {
					category1 = category;
				} 
				else if(category2.isEmpty()) {
					category2 = category;
				} 
				else if(category3.isEmpty()) {
					category3 = category;
				}
			}

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
				String internalId = null;
				if(internalIdElement != null){
					internalId = preInternalId + "-" + internalIdElement.attr("value");
				}

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
					Float priceVariation = normalizeTwoDecimalPlaces(price + Float.valueOf(plus));
					

					// Disponibilidade
					boolean available = true;
					if(stock == 0) {
						available = false;
					}

					// Imagens

					// Requisição POST para conseguir dados da imagem
					String response = DataFetcher.fetchString(DataFetcher.POST_REQUEST, session, "http://www.drogariavenancio.com.br/ajax/gradesku_imagem_ajax.asp", assembleUrlParameters(session.getOriginalURL().split("/")[4], posInternalId), null);
					
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
					
					product.setUrl(session.getOriginalURL());
					product.setInternalId(internalId);
					product.setInternalPid(internalPid);
					product.setName(name);
					product.setPrice(priceVariation);
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
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return products;
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

	/**
	 * Round and normalize Double to have only two decimal places
	 * eg: 23.45123 --> 23.45
	 * @param number
	 * @return A rounded Double with only two decimal places
	 */
	public static Float normalizeTwoDecimalPlaces(Float number) {
		BigDecimal big = new BigDecimal(number);
		String rounded = big.setScale(2, BigDecimal.ROUND_HALF_EVEN).toString();
		
		return Float.parseFloat(rounded);
	}
}