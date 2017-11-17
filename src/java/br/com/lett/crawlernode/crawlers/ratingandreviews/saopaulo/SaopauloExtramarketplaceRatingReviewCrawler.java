package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import java.util.ArrayList;
import java.util.List;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

/**
 * Date: 14/12/16
 * 
 * @author gabriel
 *
 */
public class SaopauloExtramarketplaceRatingReviewCrawler extends RatingReviewCrawler {

	public SaopauloExtramarketplaceRatingReviewCrawler(Session session) {
		super(session);
	}

	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
		RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

		if (isProductPage(document)) {
			Logging.printLogDebug(logger, session,
					"Product page identified: " + this.session.getOriginalURL());

			RatingsReviews ratingReviews = new RatingsReviews();
			ratingReviews.setDate(session.getDate());

			String internalPid = crawlInternalPid(document);

			// To get reviews
			// JSONArray ratings = crawlScriptWithRating(internalPid);
			//
			// Integer totalNumOfEvaluations = ratings.length();
			// Double avgRating = getTotalAvgRating(ratings, totalNumOfEvaluations);

			ratingReviews.setTotalRating(getTotalRating(document));
			ratingReviews.setAverageOverallRating(getTotalAvgRating(document));

			List<String> idList = crawlInternalIds(document, internalPid);
			for (String internalId : idList) {
				RatingsReviews clonedRatingReviews = (RatingsReviews) ratingReviews.clone();
				clonedRatingReviews.setInternalId(internalId);
				ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
			}
		}

		return ratingReviewsCollection;

	}

	// /**
	// * Para esse market foi necessario fazer uma engenharia reversa para conseguir capturar 2
	// * parametros da url de rating que sao baseados no considerado "internalPid" do produto, com
	// isso
	// * ao realizar essa engenharia foi notado que no arquivo full.js tem uma função que monta esses
	// * parâmetros baseados em uma string, no caso o internalPid. Essa função em js foi traduzida
	// para
	// * java para podermos montar a url de rating dos produtos.
	// *
	// * Abaixo segue a função traduzida para java, que tem o objetivo de retornar 5 caracteres sendo
	// * eles ++/++ onde o + seriam números, esses números são retirados de um charcode do
	// internalPid,
	// * depois pega o módulo de 255 - o charcode de um caracter do mesmo e tudo isso sendo somado em
	// * uma variavel DA. Após isso é pego o resto da divisão desse DA por 1023 e transformado em
	// * string. Caso esse resto não dê 4 carcateres, é importante que coloque 0's no inicio da string
	// * até completar os 4 caracteres. Depois damos um split na metade dessa string e colocamos uma
	// * barra no meio e assim teremos os 2 parâmetros para a url de rating.
	// *
	// * O arquivo full.js se encontra em: http://www.extra-imagens.com.br/Js/pwr/engine/js/full.js
	// *
	// * @param internalPid
	// * @return
	// */
	// private String CNOVAFunction(String internalPid) {
	// String DB = internalPid + "_2";
	// int DA = 0;
	//
	// for (int DZ = 0; DZ < DB.length(); DZ++) {
	// int De = DB.charAt(DZ);
	// De = De * Math.abs(255 - De);
	// DA += De;
	// }
	//
	// DA = DA % 1023;
	// String DAString = DA + "";
	//
	// int DW = DAString.length();
	//
	// switch (DW) {
	// case 0: {
	// DAString = "0000";
	// break;
	// }
	// case 1: {
	// DAString = "000" + DAString;
	// break;
	// }
	// case 2: {
	// DAString = "00" + DAString;
	// break;
	// }
	// case 3: {
	// DAString = "0" + DAString;
	// break;
	// }
	// default: {
	// break;
	// }
	// }
	//
	// DW = DAString.length();;
	//
	// return DAString.substring(0, DW / 2) + "/" + DAString.substring(DW / 2, DW);
	//
	// }

	// /**
	// * The rating of this market is in a js script like this:
	// *
	// * POWERREVIEWS.common.gResult['content/06/80/7931784_2-pt_BR-1-reviews.js'] =
	// * [{r:{id:220121082,si:2,pi:27515520,dp:true,mu:13070544,v:0,t:0,r:5,h:"A fralda &eacute;
	// * &oacute;tima para sair, n&atilde;o vaza, grandinha atr&aacute;s", n:"DIDI",l:"pt_BR",
	// w:"Campos
	// * dos Goytacazes\/RJ",fc:0,b:{n:"Avalia&ccedil;&atilde;o Geral",k:"Yes"},
	// * o:"e",d:"9\/6\/2017",db:"2017-06-09T07:29:14",p:"MUITO BOM!"}}];
	// *
	// * @param internalPid
	// * @return
	// */
	// private JSONArray crawlScriptWithReviews(String internalPid) {
	// JSONArray ratings = new JSONArray();
	//
	// String parametersBasedOnInternalPid = CNOVAFunction(internalPid);
	//
	// String url = "http://www.extra-imagens.com.br/Js/pwr/content/" + parametersBasedOnInternalPid
	// + "/" + internalPid + "_2-pt_BR-1-reviews.js";
	// String response = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null,
	// cookies);
	//
	// if (response.contains("=") && response.contains(";")) {
	// int x = response.indexOf("=") + 1;
	// int y = response.indexOf("];", x);
	//
	// String json = response.substring(x, y + 1).trim();
	//
	// if (json.startsWith("[") && json.endsWith("]")) {
	// ratings = new JSONArray(json);
	// }
	// }
	//
	// return ratings;
	// }

	/**
	 * 
	 * @param doc
	 * @return
	 */
	private Integer getTotalRating(Document doc) {
		Integer total = null;

		Element rating = doc.select(".rating .rating-count").first();

		if (rating != null) {
			total = Integer.parseInt(rating.ownText().replaceAll("[^0-9]", ""));
		}

		return total;
	}

	/**
	 * @param Double
	 * @return
	 */
	private Double getTotalAvgRating(Document doc) {
		Double avgRating = null;

		Element avg = doc.select(".rating .rating-value").first();

		if (avg != null) {
			avgRating = Double.parseDouble(avg.ownText().replace(",", "."));
		}

		return avgRating;
	}

	private List<String> crawlInternalIds(Document doc, String internalPid) {
		List<String> ids = new ArrayList<>();
		if (hasProductVariations(doc)) {
			Elements skuOptions = doc.select(".produtoSku option[value]:not([value=\"\"])");

			for (Element e : skuOptions) {
				ids.add(internalPid + "-" + e.attr("value"));
			}

		} else {
			Element elementDataSku = doc.select("#ctl00_Conteudo_hdnIdSkuSelecionado").first();

			if (elementDataSku != null) {
				ids.add(internalPid + "-" + elementDataSku.attr("value"));
			}
		}

		return ids;
	}

	private String crawlInternalPid(Document document) {
		String internalPid = null;
		Elements elementInternalId = document.select("script[type=text/javascript]");

		String idenfyId = "idProduct";

		for (Element e : elementInternalId) {
			String script = e.outerHtml();

			if (script.contains(idenfyId)) {
				script = script.replaceAll("\"", "");

				int x = script.indexOf(idenfyId);
				int y = script.indexOf(',', x + idenfyId.length());

				internalPid = script.substring(x + idenfyId.length(), y).replaceAll("[^0-9]", "").trim();
			}
		}


		return internalPid;
	}

	private boolean hasProductVariations(Document document) {
		Elements skuChooser = document.select(".produtoSku option[value]:not([value=\"\"])");

		if (skuChooser.size() > 1) {
			if (skuChooser.size() == 2) {
				String prodOne = skuChooser.get(0).text();
				if (prodOne.contains("|")) {
					prodOne = prodOne.split("\\|")[0].trim();
				}

				String prodTwo = skuChooser.get(1).text();
				if (prodTwo.contains("|")) {
					prodTwo = prodTwo.split("\\|")[0].trim();
				}


				if (prodOne.equals(prodTwo)) {
					return false;
				}
			}
			return true;
		}

		return false;

	}

	private boolean isProductPage(Document doc) {
		Element productElement = doc.select(".produtoNome").first();

		if (productElement != null) {
			return true;
		}
		return false;
	}

}
