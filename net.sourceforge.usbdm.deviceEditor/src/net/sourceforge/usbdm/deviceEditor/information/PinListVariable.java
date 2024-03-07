package net.sourceforge.usbdm.deviceEditor.information;

import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.PinListVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

public class PinListVariable extends StringVariable {

   protected long fMin         = Long.MIN_VALUE;
   protected long fMax         = Long.MAX_VALUE;
   protected int  fMinListSize = 0;
   protected int  fMaxListSize = 0;
   
   private PeripheralWithState fPeripheral;

   public PeripheralWithState getPeripheral() {
      return fPeripheral;
   }

   public void setPeripheral(PeripheralWithState peripheral) {
      fPeripheral = peripheral;
   }

   static final String  fDelimeter    = "[, ]+";
   static final Pattern fValuePattern = Pattern.compile("((\\d+[ ,]+)*\\d+[ ]+),?");
   
   /**
    * Creates a numeric list where the order of values is significant
    * @param fProvider
    * 
    * @param name
    * @param key
    */
   public PinListVariable(VariableProvider provider, String name, String key) {
      super(provider, name, key);
   }

   @Override
   protected VariableModel privateCreateModel(BaseModel parent) {
      return new PinListVariableModel(parent, this);
   }

   public int[] getValues(String value) {
      String values[];
      if (value.isEmpty()) {
         values = new String[0];
      }
      else {
         values = value.split(fDelimeter);
      }
      int returnValues[] = new int[values.length];
      for (int index=0; index<values.length; index++) {
         returnValues[index] = Integer.parseInt(values[index]);
      }
      return returnValues;
   }
   
   public int[] getValues() {
      return getValues(getValueAsString());
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
         if ((fMaxListSize == fMinListSize) && (values.length != fMaxListSize)) {
            return "Illegal number of elements, Requires == " + fMaxListSize;
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
      return isValid(getPersistentValue());
   }

   /**
    * Set value as String
    * 
    * @param value Value to set
    * 
    * @return True if variable actually changed value and listeners notified
    */
   @Override
   public boolean setValueQuietly(String value) {
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
      return super.setValueQuietly(s);
   }

   @Override
   public boolean setValueQuietly(Object value) {
      return setValueQuietly(value.toString());
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
    * Get minimum list size
    * 
    * @return
    */
   public int getMinListSize() {
      return fMinListSize;
   }

   /**
    * Get minimum list size
    * 
    * @return
    */
   public int getMaxListSize() {
      return fMaxListSize;
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
