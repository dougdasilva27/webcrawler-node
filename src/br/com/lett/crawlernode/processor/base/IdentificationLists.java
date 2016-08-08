package br.com.lett.crawlernode.processor.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class have two lists that where previously read from google drive.
 * These lists are passed as parameters to the Extractor.
 * @author Samir Leao
 *
 */
public class IdentificationLists {
	
	public static List<String> recipientsList = new ArrayList<String>(Arrays.asList(
			
			"aerosol",
			"bandeja",
			"barra",
			"barril",
			"caixa",
			"cápsulas",
			"copo",
			"frasco",
			"garrafa",
			"garrafão",
			"lata",
			"pacote",
			"peso",
			"pote",
			"sachet",
			"tubo",
			"unidade",
			"vacuo",
			"vidro",
			"neck"
			));
	
	public static List<String> unitsList = new ArrayList<String>(Arrays.asList(
			
			"gramas",
			"kilogramas",
			"mililitros",
			"litros",
			"unidades",
			"metros",
			"centimetros"
			));

}
