package net.sourceforge.usbdm.deviceEditor.information;

import java.util.regex.Pattern;

public class NumericListVariable extends StringVariable {

   protected long fMin         = Long.MIN_VALUE;
   protected long fMax         = Long.MAX_VALUE;
   private   int  fMinListSize = 0;
   private   int  fMaxListSize = 0;

   static final String  fDelimeter    = "[, ]+";
   static final Pattern fValuePattern = Pattern.compile("((\\d+[ ,]+)*\\d+[ ]+),?");
   
   public NumericListVariable(String name, String key) {
      super(name, key);
   }

   @Override
   public String isValid(String value) {
//      Matcher m = fValuePattern.matcher(value);
//      if (!m.matches()) {
//         return "Illegal list";
//      }
//      value = m.group(1);
      try {
         String values[];
         if (value.isEmpty()) {
            values = new String[0];
         }
         else {
            values = value.split(fDelimeter);
         }
         if ((fMaxListSize>0) && (values.length > fMaxListSize)) {
            return "Illegal number of elements, Requires <= " + fMaxListSize;
         }
         if ((fMinListSize>0) && (values.length < fMinListSize)) {
            return "Illegal number of elements, Requires >= " + fMinListSize;
         }
         for (String s:values) {
            int iValue = Integer.parseInt(s);
            if ((iValue<fMin) || (iValue>fMax)) {
               return "Illegal integer in list, "+iValue+", range ["+fMin+","+fMax+"]";
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

   /**
    * Set value as String
    * 
    * @param value Value to set
    * 
    * @return True if variable actually changed value and listeners notified
    */
   @Override
   public boolean setValue(String value) {
      StringBuilder sb = new StringBuilder();
      boolean isFirst = true;
      for (String s:value.toString().split(fDelimeter)) {
         if (!isFirst) {
            sb.append(",");
         }
         isFirst = false;
         sb.append(s);
      }
      String s = sb.toString();
      return super.setValue(s);
   }

   @Override
   public boolean setValue(Object value) {
      return setValue(value.toString());
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
    * Set Minimum number of entries in list<br>
    * 
    * @param l
    */
   public void setMinListLength(long l) {
      fMinListSize = (int) l;
   }
   
   /**
    * Set maximum number of entries in list<br>
    * A value of 0 sets no limit
    * 
    * @param l
    */
   public void setMaxListLength(long l) {
      fMaxListSize = (int) l;
      if (fMinListSize>l) {
         fMinListSize = (int)l;
      }
   }
   
   /**
    * Set number of entries in list<br>
    * A value of 0 sets no limit
    * 
    * @param l
    */
   public void setListLength(long l) {
      if (fMaxListSize == l) {
         return;
      }
      fMaxListSize = (int) l;
      fMinListSize = (int) l;
      notifyListeners();
   }
   
}
