package net.sourceforge.usbdm.deviceEditor.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Based on http://www.labbookpages.co.uk/software/java/engNotation.html
 */
public class EngineeringNotation {

   /** Offset of no-suffix in table i.e. unit multiplier */
   private final static int      PREFIX_OFFSET = 5;
   
   /** Suffixes for different power of 10 */
   private final static String[] PREFIX_ARRAY = {"f", "p", "n", "u", "m", "", "k", "M", "G", "T"};

   private final static long divisors[] = {1,10,100,1000,10000,100000,1000000,10000000,100000000};

   /**
    * Converts a number into Engineering notation
    * 
    * @param value      Number to convert
    * @param sigDigits  Number of significant digits in result
    * 
    * @return
    */
   public static String convert(double value, int sigDigits) {
      if (value == 0) {
         // Always return 0.0
         return "0.0";
      }
      int    pow10 = (int)Math.floor(Math.log10(value));

      String suffix = "";
      int index = ((pow10+3*PREFIX_OFFSET)/3);
//      System.err.println("index = "+index);
      if ((index<0) || (index>=PREFIX_ARRAY.length)) {
         return Double.toString(value);
      }
      suffix = PREFIX_ARRAY[index];
      
      // Convert number to long with required number significant digits
      long res = Math.round((value/Math.pow(10, pow10))*Math.pow(10,sigDigits-1));
      
      String result = "???";
      index = sigDigits-1-((24+pow10)%3);
      
      if (index < 1) {
         long factor = divisors[-index];
         result = Long.toString(res*factor);
      }
      else if (index > 0) {
         long divisor = divisors[index];
         result = Long.toString(res/divisor) + "." + String.format("%0"+index+"d", res%divisor);
      }
      else {
         result = Long.toString(res);
      }
      return result + suffix;
   }
   
   private static final String  BINARY_PATTERN   = "(0b([0-1]+))";
   private static final String  HEX_PATTERN      = "(0x([0-9|a-f|A-F]+))";
   private static final String  DEC_PATTERN      = "([0-9]*\\.?[0-9]*(E-?[0-9]*)?)";
   private static final String  SUFFIX_PATTERN   = "(f|p|n|u|m|k|M|G|T)?";
   private static final String  UNIT_PATTERN     = "(Hz|hz|s)?";
   private static final Pattern NUMBER_PATTERN   = Pattern.compile(
      "^(-)?("+
      BINARY_PATTERN+"|"+
      HEX_PATTERN+"|"+
      DEC_PATTERN+")"+
      "\\s*"+
      SUFFIX_PATTERN+
      UNIT_PATTERN+"$");

   private static final String suffixes      = "fpnumkMGT";
   private static final double suffixPower[] = {1.0e-15D, 1.0e-12D, 1.0e-9D, 1.0e-6D, 1.0e-3D, 1.0e3D, 1.0e6D, 1.0e9D, 1.0e12D, 1.0D};
   
   /**
    * Parse a number including Engineering notation e.g. 120MHz
    * 
    * @param str String to parse
    * 
    * @return Parsed value of string
    */
   public static double parse(String num) {
      double value = 0.0;
      
      if (num.equalsIgnoreCase("Infinity")) {
         System.err.println("WARNING: EngineeringNotation.parse() - loading infinite double");
         return Double.POSITIVE_INFINITY;
      }
      Matcher matcher = NUMBER_PATTERN.matcher(num);
      if (!matcher.matches()) {
         throw new NumberFormatException("Illegal number: "+num);
      }
      boolean negative = matcher.group(1) != null;
      String binaryNum = matcher.group(4);
      String hexNum    = matcher.group(6);
      String decNum    = matcher.group(7);
      String suffix    = matcher.group(9);
      if (decNum != null) {
         value = Double.parseDouble('0'+decNum);
      }
      else if (hexNum != null) {
         value = Long.parseLong(hexNum, 16);
      }
      else if (binaryNum != null) {
         value = Long.parseLong(binaryNum, 2);
      }
      if (negative) {
         value = - value;
      }
      if (suffix != null) {
         try {
            value *= suffixPower[suffixes.indexOf(suffix)];
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
//      System.err.println(num + "=>");
//      System.err.println("  bin = " + binaryNum);
//      System.err.println("  hex = " + hexNum);
//      System.err.println("  dec = " + decNum);
//      System.err.println();
      return value;
   }
   
   /**
    * Parse a number including Engineering notation e.g. 120MHz<br>
    * The number is rounded to a Long
    * 
    * @param str String to parse
    * 
    * @return Parsed value of string
    */
   public static long parseAsLong(String num) {
      double value = parse(num);
      if ((value<Long.MIN_VALUE) || (value>Long.MAX_VALUE)) {
         throw new NumberFormatException("Illegal number as Long: " + num);
      }
      return Math.round(value);
   } 
 }
