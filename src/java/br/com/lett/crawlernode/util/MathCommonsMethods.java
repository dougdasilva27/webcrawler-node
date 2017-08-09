package br.com.lett.crawlernode.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MathCommonsMethods {
	
	public static final String PRICE_REGEX = "[^\\d.]+"; // get anything that is not either a digit or a period 
	
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
		String floatText =  input.replaceAll("[^0-9,]+", "").replace(".", "").replace(",", ".");
		
		if(!floatText.isEmpty()) {
			return Float.parseFloat(floatText);
		}
		
		return null;
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
	 * Takes a String representing a non formated Float, and inserts a '.'.
	 * This method is mostly used to parse sku prices, when they are in a non
	 * formated form in Json. We always use two decimal places to format the Float,
	 * as we are representing prices.
	 * 
	 * e.g:
	 * 11999 -> 119.99
	 * 
	 * @param input
	 * @return
	 */
	public static Float formatStringToFloat(String input) {
		int index = input.length() - 2;
		String firstPart = input.substring(0, index);
		String secondPart = input.substring(index, input.length());
		String formatedValue = firstPart + "." + secondPart;
		return new Float(formatedValue);
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

	/**
     * Round and normalize Double to have only two decimal places
     * eg: 23.45123 --> 23.45
     * If number is null, the method returns null.
     * 
     * @param number
     * @return A rounded Double with only two decimal places
     */
    public static Float normalizeTwoDecimalPlaces(Float number) {
        if (number == null) return null;
        
        BigDecimal big = new BigDecimal(number);
        String rounded = big.setScale(2, BigDecimal.ROUND_HALF_EVEN).toString();
        
        return Float.parseFloat(rounded);
    }
    
    /**
     * Round and normalize Double to have only two decimal places
     * eg: 23.45123 --> 23.45
     * If number is null, the method returns null.
     * 
     * @param number
     * @return A rounded Double with only two decimal places
     */
    public static Double normalizeTwoDecimalPlaces(Double number) {
        if (number == null) return null;
        
        BigDecimal big = new BigDecimal(number);
        String rounded = big.setScale(2, BigDecimal.ROUND_HALF_EVEN).toString();
        
        return Double.parseDouble(rounded);
    }
    
    /**
     * Round and normalize Double to have only two decimal places
     * eg: 23.45123 --> 23.45
     * If number is null, the method returns null.
     * 
     * @param number
     * @return A rounded Double with only two decimal places
     */
    public static Float normalizeTwoDecimalPlacesUp(Float number) {
        if (number == null) return null;
        
        BigDecimal big = new BigDecimal(number);
        String rounded = big.setScale(2, BigDecimal.ROUND_UP).toString();
        
        return Float.parseFloat(rounded);
    }
    
    /**
     * Round and normalize Double to have only two decimal places
     * eg: 23.41123 --> 23.40
     * If number is null, the method returns null.
     * 
     * @param number
     * @return A rounded Double with only two decimal places
     */
    public static Float normalizeTwoDecimalPlacesDown(Float number) {
        if (number == null) return null;
        
        BigDecimal big = new BigDecimal(number);
        String rounded = big.setScale(2, BigDecimal.ROUND_DOWN).toString();
        
        return Float.parseFloat(rounded);
    }
    
    /**
	 * Given a list of ordered disjoint intervals, select the interval where
	 * the number n fits, using a binary search algorithm.
	 * 
	 * @param n
	 * @param intervals
	 * @return
	 */
	public static Interval<Integer> findInterval(List<Interval<Integer>> intervals, Integer n) {
		if (intervals == null) return null;
		
		int beg = 0;
		int end = intervals.size() - 1;
		
		while(beg <= end) {
			int mid = (beg + end)/2;
			if (intervals.get(mid).getStart() <= n && intervals.get(mid).getEnd() >= n) {
				return intervals.get(mid);
			} else if (intervals.get(mid).getStart() <= n) {
				beg = mid + 1;
			} else {
				end = mid - 1;
			}
		}
		
		return null;
	}
}
