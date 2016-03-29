package net.sourceforge.usbdm.deviceEditor.information;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

   /**
    * Comparator for port names (e.g. PTA3) or peripheral functions e.g. (FTM3_CH2)
    * Treats the number separately as a number.
    */
   public static Comparator<String> comparator = new Comparator<String>() {
      @Override
      public int compare(String arg0, String arg1) {
         if ((arg0.length()==0) && (arg1.length()==0)) {
            return 0;
         }
         if (arg0.length()==0) {
            return -1;
         }
         if (arg1.length()==0) {
            return 1;
         }
         Pattern p = Pattern.compile("([^\\d]*)(\\d*)(.*)");
         Matcher m0 = p.matcher(arg0);
         Matcher m1 = p.matcher(arg1);
         if (m0.matches() && m1.matches()) {
            String t0 = m0.group(1);
            String t1 = m1.group(1);
            int r = t0.compareTo(t1);
            if (r == 0) {
               // Treat as numbers
               String n0 = m0.group(2);
               String n1 = m1.group(2);
               int no0 = -1, no1 = -1;
               if (n0.length() > 0) {
                  no0 = Integer.parseInt(n0);
               }
               if (n1.length() > 0) {
                  no1 = Integer.parseInt(n1);
               }
               r = -no1 + no0;

               if (r == 0) {
                  String s0 = m0.group(3);
                  String s1 = m1.group(3);
                  r = compare(s0, s1);
               }
            }
            return r;
         }
         return arg0.compareTo(arg1);
      }
   };

}
