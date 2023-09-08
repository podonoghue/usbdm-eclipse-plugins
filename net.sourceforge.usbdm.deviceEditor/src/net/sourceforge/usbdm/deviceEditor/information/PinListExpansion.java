package net.sourceforge.usbdm.deviceEditor.information;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PinListExpansion {

   /**
    * Expand patterns in string<br>
    * <b>Examples:</b>
    * <li> PTA(0-6)       => PTA0,PTA1,PTA2,PTA3,PTA4,PTA5
    * <li> PT(A-B)(0-6)   => PTA0,PTA1,PTA2,PTA3,PTA4,PTA5,PTB0,PTB1,PTB2,PTB3,PTB4,PTB5
    * 
    * @param pattern Pattern to expand
    * 
    * @return  Expanded pattern
    * 
    * @throws Exception
    */
   static private List<String> expand(String pattern) throws Exception {

      final String numberRange = "\\((\\d+)-(\\d+)\\)";           // 2 groups
      final String letterRange = "\\(([a-z|A-Z])-([a-z|A-Z])\\)"; // 2 groups

      //                                  G1   G2    G3,4            G5,6       G7
      final Pattern p = Pattern.compile("^(.*?)("+numberRange+"|"+letterRange+")(.*)$");

      List<String> completedItems = new ArrayList<String>();
      List<String> itemsToProcess = new LinkedList<String>();
      itemsToProcess.add(pattern);

      while (!itemsToProcess.isEmpty()) {
         String currentPattern = itemsToProcess.remove(0);
//         System.err.println("Expanding '" + currentPattern + "'");
         Matcher m = p.matcher(currentPattern);
         if (!m.matches()) {
            // Doesn't need expansion
            completedItems.add(currentPattern);
            continue;
         }
         if ((m.group(3) != null) && (m.group(4) != null)) {
            // Number range
            int start = Integer.parseInt(m.group(3));
            int end   = Integer.parseInt(m.group(4));
            if (start < end) {
               for (int bitNum=start; bitNum<=end; bitNum++) {
                  // Add for further expansion
                  itemsToProcess.add(m.group(1)+Integer.toString(bitNum)+m.group(7));
               }
            }
            else {
               for (int bitNum=end; bitNum>=start; bitNum--) {
                  // Add for further expansion
                  itemsToProcess.add(m.group(1)+Integer.toString(bitNum)+m.group(7));
               }
            }
         }
         else if ((m.group(5) != null) && (m.group(6) != null)) {
            // Letter range
            char start = m.group(5).charAt(0);
            char end   = m.group(6).charAt(0);
            if (start < end) {
               for (char bitNum=start; bitNum<=end; bitNum++) {
                  // Add for further expansion
                  itemsToProcess.add(m.group(1)+bitNum+m.group(7));
               }
            }
            else {
               for (char bitNum=end; bitNum>=start; bitNum--) {
                  // Add for further expansion
                  itemsToProcess.add(m.group(1)+bitNum+m.group(7));
               }
            }
         }
         else {
            throw new Exception("Unexpected text for expansion '" + currentPattern + "'");
         }
      }
      return completedItems;
   }
   
   /**
    * Expand %i pattern in string<br>
    * <b>Examples:</b>
    * <li> (Pin%i, 5)       => Pin0:Pin1:Pin2:Pin3:Pin4
    * 
    * @param pattern Pattern to expand
    * 
    * @return  Expanded pattern
    * 
    * @throws Exception
    */
   static private ArrayList<String> expand(String pattern, int dimension) throws Exception {
      
      ArrayList<String> list = new ArrayList<String>();
      
      for (int dim=0; dim<dimension; dim++) {
         list.add(pattern.replaceAll("%i", Integer.toString(dim)));
      }
      return list;
   }
   
   /**
    * Expand patterns in string<br>
    * <b>Examples:</b>
    * <li> PTA(0-6),PTB(1-2)  => PTA0:PTA1:PTA2:PTA3:PTA4:PTA5:PTB1:PTB2
    * <li> PT(A-B)(0-6)       => PTA0:PTA1:PTA2:PTA3:PTA4:PTA5:PTB0:PTB1:PTB2:PTB3:PTB4:PTB5
    * 
    * @param pattern Pattern to expand
    * 
    * @return  Expanded pattern
    * 
    * @throws Exception
    */
   public static ArrayList<String> expandPinList(String pattern, String delimeter) {

      ArrayList<String> result = new ArrayList<String>();

      try {
         for(String item:pattern.split(delimeter)) {
            result.addAll(expand(item.trim()));
         }
      } catch (Exception e) {
         System.err.println("Failed to expand '" + pattern + "'");
         e.printStackTrace();
      }
      return result;
   }
   
   /**
    * Expand patterns in string<br>
    * <b>Examples:</b>
    * <li> PTA(0-6)       => PTA0:PTA1:PTA2:PTA3:PTA4:PTA5
    * <li> PT(A-B)(0-6)   => PTA0:PTA1:PTA2:PTA3:PTA4:PTA5:PTB0:PTB1:PTB2:PTB3:PTB4:PTB5
    * 
    * @param pattern Pattern to expand
    * 
    * @return  Expanded pattern
    * 
    * @throws Exception
    */
   private static ArrayList<String> expandList(String pattern) throws Exception {
      
      ArrayList<String> result = new ArrayList<String>();
      for (String s:pattern.split(";")) {
         s = s.trim();
         String[] parts = s.split(",|(\\=\\>)");
         if (parts.length != 2) {
           throw new Exception("Unmatched expansion (should be 2 elements) '" + s + "'");
         }
         ArrayList<String> froms = expandPinList(parts[0].trim(),":");
         ArrayList<String> tos   = expandPinList(parts[1].trim(),":");
         
         if ((froms.size() == 1) && froms.get(0).contains("%i") && (tos.size()>1)) {
            froms = expand(froms.get(0), tos.size());
         }
         if ((tos.size() == 1) && tos.get(0).contains("%i") && (froms.size()>1)) {
            tos = expand(tos.get(0), froms.size());
         }
         if (froms.size() != tos.size()) {
            throw new Exception("Unmatched expansion (should be matching length) '"+parts[0]+" => '"+parts[1]);
          }
         for (int index=0; index<froms.size(); index++) {
            result.add(froms.get(index)+","+tos.get(index));
         }
      }
      return result;
   }
   
   /**
    * Expand a paired list of names e.g.
    *    <li>KBI0_P%i,PT(A-B)(0-2); => [KBI0_P0=>PTA0, KBI0_P1=>PTA1, KBI0_P2=>PTA2, KBI0_P3=>PTB0, KBI0_P4=>PTB1, KBI0_P5=>PTB2]
    *    <li>ADC(0-1):w,PTA(0-1):y => [ADC0=>PTA0, ADC1=>PTA1, w=>y]
    *    <li>f:g,ff:gg => [f=>ff, g=>gg]
    *    <li>d:e,dd:ee => [d=>dd, e=>ee]
    * 
    * List must contain at least 1 semicolon to trigger expansion
    * 
    * @param pattern List to expand
    * 
    * @return Expanded list as array
    * @throws Exception
    */
   public static String[] expandNameList(String pattern) {

      if (!pattern.contains(";")) {
         // Simple list separated by commas
         return pattern.split(",");
      }
      ArrayList<String> res = null;
      try {
         res = expandList(pattern);
      } catch (Exception e) {
         e.printStackTrace();
      }
      return res.toArray(new String[res.size()]);
   }
   
   public static void main(String[] args) throws Exception {
      String[] tests = {
            "KBI0_P%i,PT(A-B)(0-2);",
            "ADC(0-1):w,PTA(0-1):y",
            "ADC(0-1),PTA(0-1);w,y",
            "ADC(0-1),PTA(0-1)",
            "a:b:c,w:x:y",
            "a,aa  ;  b,bb;  c,cc  ",
            "d:e,dd:ee",
            "f:g,ff:gg", };
      for (String test:tests) {
         String[] result = expandNameList(test);
         System.err.println(test + " => " + result.toString());
      }
   }

}
