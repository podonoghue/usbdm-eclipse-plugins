package net.sourceforge.usbdm.deviceEditor.model;

import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.StringVariable;

public class NumericListVariable extends StringVariable {

   protected long fMin      = Long.MIN_VALUE;
   protected long fMax      = Long.MAX_VALUE;
   private   int  fListSize = 0;

   static final String  fDelimeter    = "[, ]+";
   static final Pattern fValuePattern = Pattern.compile("(\\d+[ ,])*(\\d+)[ ,]?");
   
   public NumericListVariable(String name, String key) {
      super(name, key);
   }

   @Override
   public String isValid(String value) {
      if (!fValuePattern.matcher(value).matches()) {
         return "Illegal list";
      }
      try {
         String values[] = value.split("[, ]+");
         if ((fListSize>0) && (values.length != fListSize)) {
            return "Illegal number of elements, Requires " + fListSize;
         }
         for (String s:values) {
            int iValue = Integer.parseInt(s);
            if ((iValue<fMin) || (iValue>fMax)) {
               return "Illegal integer in list, "+iValue;
            }
         }
         return null;
      } catch (NumberFormatException e) {
         return "Illegal integer in list";
      }
   }

   @Override
   public String isValidKey(char character) {
      Character.isWhitespace(character);
      if ("0123456789, ".indexOf(character)>=0) {
         return null;
      }
      else {
         return "Invalid character";
      }
   }

   @Override
   public String isValid() {
      return isValid(fValue);
   }

   @Override
   public boolean setValue(Object value) {
      StringBuilder sb = new StringBuilder();
      for (String s:value.toString().split(fDelimeter)) {
         sb.append(s);
         sb.append(",");
      }
      String s = sb.toString();
      if (fValue.equalsIgnoreCase(s)) {
         return false;
      }
      fValue = s;
      return true;
   }

   /**
    * Set minimum value for entry
    *  
    * @param min
    */
   public void setMin(long min) {
      fMin = min;
   }

   /**
    * Set maximum value for entry
    *  
    * @param max
    */
   public void setMax(long max) {
      fMax = max;
   }

   /**
    * Set number of entries in list<br>
    * A value of 0 sets no limit
    * 
    * @param size
    */
   public void setListSize(int size) {
      if (fListSize == size) {
         return;
      }
      fListSize = size;
      notifyListeners();
   }
   
}
