package br.com.lett.crawlernode.processor.base;

import java.util.LinkedHashMap;
import java.util.Map;

public class ReplacementMaps {
	
	public static Map<String, String> unitsReplaceMap = new LinkedHashMap<String, String>();
	public static Map<String, String> recipientsReplaceMap = new LinkedHashMap<String, String>();
	
	static {
		unitsReplaceMap.put("gr", "gramas");
		unitsReplaceMap.put("g", "gramas");
		unitsReplaceMap.put("kg", "kilogramas");
		unitsReplaceMap.put("kl", "kilogramas");
		unitsReplaceMap.put("k", "kilogramas");
		unitsReplaceMap.put("ml", "mililitros");
		unitsReplaceMap.put("mili", "mililitros");
		unitsReplaceMap.put("lit", "litros");
		unitsReplaceMap.put("lt", "litros");
		unitsReplaceMap.put("l", "litros");
		unitsReplaceMap.put("un", "unidades");
		unitsReplaceMap.put("cm", "centimetros");
		unitsReplaceMap.put("litros.", "litros");
		unitsReplaceMap.put("litro", "litros");
		unitsReplaceMap.put("kilograma", "kilogramas");
		unitsReplaceMap.put("unidade", "unidades");
		unitsReplaceMap.put("grama", "gramas");
		unitsReplaceMap.put("m", "metros");
		unitsReplaceMap.put("unids", "unidades");
		
		recipientsReplaceMap.put("lt", "lata");
		recipientsReplaceMap.put("vd", "vidro");
		recipientsReplaceMap.put("pc", "pacote");
		recipientsReplaceMap.put("ev", "pacote");
		recipientsReplaceMap.put("cx", "caixa");
		recipientsReplaceMap.put("fc", "frasco");
		recipientsReplaceMap.put("br", "barra");
		recipientsReplaceMap.put("ba", "bandeja");
		recipientsReplaceMap.put("bdj", "bandeja");
		recipientsReplaceMap.put("gf", "garrafa");
		recipientsReplaceMap.put("pt", "pote");
		recipientsReplaceMap.put("pet", "garrafa");
		recipientsReplaceMap.put("gfa", "garrafa");
		recipientsReplaceMap.put("sachêt", "sachet");
		recipientsReplaceMap.put("pct", "pacote");
		recipientsReplaceMap.put("doypack", "sachet");
		recipientsReplaceMap.put("bustina", "sachet");
		recipientsReplaceMap.put("ln", "long neck");
	}
	

}
