package net.sourceforge.usbdm.peripheralDatabase;

import java.util.regex.Pattern;

public class Utiltity {

   static Pattern pattern = Pattern.compile("[a-zA-Z_]\\w*");
   
   static boolean isCIdentifier(String name) {
      return pattern.matcher(name).matches();
   }
}
