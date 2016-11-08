package br.com.lett.crawlernode.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MathCommonsMethods {
	
	/**
	 * Parses a Float from an input String. It will parse
	 * only the first match. If there is more than one float in the string,
	 * the others occurrences after the first will be disconsidered.
	 * 
	 * e.g:
	 * R$ 2.779,20 returns the Float 2779.2
	 * 
	 * @param input
	 * @return
	 */
	public static Float parseFloat(String input) {
		return Float.parseFloat( input.replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
	}
	
	/**
	 * Parse all numbers from a string and returns a list containing
	 * all the found numbers.
	 * 
	 * @param s
	 * @return
	 */
	public static List<String> parseNumbers(String s) {
		List<String> numbers = new ArrayList<String>();
		Pattern p = Pattern.compile("-?\\d+");
		Matcher m = p.matcher(s);
		while (m.find()) {
		  numbers.add(m.group());
		}
		return numbers;
	}
	
	/**
	 * Parse all numbers from a string and returns a list containing
	 * all the found numbers.
	 * 
	 * @param s
	 * @return
	 */
	public static List<String> parsePositiveNumbers(String s) {
		List<String> numbers = new ArrayList<String>();
		Pattern p = Pattern.compile("\\d+");
		Matcher m = p.matcher(s);
		while (m.find()) {
		  numbers.add(m.group());
		}
		return numbers;
	}
	
	/**
	 * Generates a random integer in the interval between min and max
	 * @param min
	 * @param max
	 * @return
	 */
	public static int randInt(int min, int max) {

		// NOTE: Usually this should be a field rather than a method
		// variable so that it is not re-seeded every call.
		Random rand = new Random();

		// nextInt is normally exclusive of the top value,
		// so add 1 to make it inclusive
		int randomNum = rand.nextInt((max - min) + 1) + min;

		return randomNum;
	}

}
