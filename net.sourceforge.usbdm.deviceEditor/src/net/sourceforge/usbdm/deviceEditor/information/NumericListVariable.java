package net.sourceforge.usbdm.deviceEditor.information;

import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.NumericListVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;

public class NumericListVariable extends StringVariable {

   protected long fMin         = Long.MIN_VALUE;
   protected long fMax         = Long.MAX_VALUE;
   protected int  fMinListLength = 0;
   protected int  fMaxListLength = 0;

   static final String  fDelimeter    = "[, ]+";
   static final Pattern fValuePattern = Pattern.compile("((\\d+[ ,]+)*\\d+[ ]+),?");

   /**
    * Creates a numeric list where the order of values is significant
    * 
    * @param name
    * @param key
    */
   public NumericListVariable(String name, String key) {
      super(name, key);
   }

   @Override
   public VariableModel createModel(BaseModel parent) {
      return new NumericListVariableModel(parent, this);
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
         if ((fMaxListLength>0) && (values.length > fMaxListLength)) {
            return "Illegal number of elements, Requires <= " + fMaxListLength;
         }
         if ((fMinListLength>0) && (values.length < fMinListLength)) {
            return "Illegal number of elements, Requires >= " + fMinListLength;
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
      return isValid(getValueAsString());
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
    * Get minimum permitted value
    * 
    * @return
    */
   public long getMin() {
      return fMin;
   }

   /**
    * Get maximum permitted value
    * 
    * @return
    */
   public long getMax() {
      return fMax;
   }

   /**
    * Set Minimum number of entries in list<br>
    * 
    * @param l
    */
   public void setMinListLength(long l) {
      fMinListLength = (int) l;
   }
   
   /**
    * Set maximum number of entries in list<br>
    * A value of 0 sets no limit
    * 
    * @param l
    */
   public void setMaxListLength(long l) {
      fMaxListLength = (int) l;
      if (fMinListLength>l) {
         fMinListLength = (int)l;
      }
   }
   
   /**
    * Get minimum list size
    * 
    * @return
    */
   public int getMinListLength() {
      return fMinListLength;
   }

   /**
    * Get maximum list size
    * 
    * @return
    */
   public int getMaxListLength() {
      return fMaxListLength;
   }

   /**
    * Set number of entries in list<br>
    * A value of 0 sets no limit
    * 
    * @param l
    */
   public void setListLength(long l) {
      if (fMaxListLength == l) {
         return;
      }
      fMaxListLength = (int) l;
      fMinListLength = (int) l;
      notifyListeners();
   }
   
}
