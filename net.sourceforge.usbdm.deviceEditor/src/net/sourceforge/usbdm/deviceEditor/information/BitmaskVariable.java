package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.information.PinListExpansion.PinMap;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.BitmaskVariableModel;
import net.sourceforge.usbdm.deviceEditor.parsers.Expression;

public class BitmaskVariable extends LongVariable {

   // Permitted bits
   private long fPermittedBits = 0xFFFFFFFF;
   
   // List of bit names (lsb-msb)
   private String fBitList;

   // Pin mapping information
   private String fPinMap;

   public BitmaskVariable(String name, String key) {
      super(name, key);
   }

   /**
    * Sets the bit-mask of permitted bit values
    * 
    * @param permittedBits
    */
   public void setPermittedBits(long permittedBits) {
      fPermittedBits = permittedBits;
   }

   /**
    * Gets the bit-mask of permitted bit values
    * 
    * @return
    */
   public long getPermittedBits() {
      return fPermittedBits;
   }

   @Override
   protected BitmaskVariableModel privateCreateModel(BaseModel parent) {
      return new BitmaskVariableModel(parent, this);
   }

   @Override
   public String formatValueAsString(long value) {
      return "0x"+Long.toHexString(value);
   }

   @Override
   public String getSubstitutionValue() {
      return getValueAsString();
   }

   /**
    * Set list of bit names (lsb-msb).  The list is expanded when used:<br>
    *    <li>A1-3;BB5-7    => A1,A2,A3,BB5,BB6,BB7 and then split on commas
    *    <li>PT(A-D)(0-7); => PTA0,PTA1,PTA2...PTD5,PTD6,PTD7<br>
    * After expansion %i is replace with the index of the name in the list<br>
    * 
    * @param bitList e.g. "bit0,bit1"
    */
   public void setBitList(String bitList) {
      fBitList = bitList;
   }

   /**
    * Get list of bit names (unexpanded)
    */
   public String getBitList() {
      return fBitList;
   }

   /**
    * Set pin map<br>
    * This indicates the Signal to Pin mapping for each bit in the bitmask value
    * 
    * @param pinMap
    */
   public void setPinMap(String pinMap) {
      fPinMap = pinMap;
   }

   /**
    * Update pin mapping based on the bitmap provided
    * 
    * @param bitmap Bitmap where '1' => map pin to signal, '0' => unmap the pin
    * @throws Exception
    */
   private void updatePinMap(long bitmap) throws Exception {
      String disablePinMap = getDisabledPinMap();
      if (!isEnabled() && (disablePinMap != null)) {
         // Disabled and special map provided
         setActivePinMappings(getDisabledPinMap());
      }
      else {
         // Use map associated with choice (even if variable disabled)
         if (fPinMap != null) {
            PinMap[] pinMaps = PinListExpansion.expandNameList(fPinMap);

            for (int index=0; index<pinMaps.length; index++) {
               PinMap pinMapEntry = pinMaps[index];
               try {
                  if ((bitmap & (1L<<index)) != 0) {
                     // Map these
                     setActivePinMapping(pinMapEntry.signal, pinMapEntry.pin);
                  }
                  else {
                     // Unmap these
                     setActivePinMapping(pinMapEntry.signal, null);
                  }
               } catch (Exception e) {
                  System.err.println("Signal mapping change failed for " + pinMapEntry);
               }
            }
         }
      }
   }
   
   @Override
   public boolean enable(boolean enabled) {
      boolean rv = super.enable(enabled);
      if (rv) {
         try {
            updatePinMap(getValueAsLong());
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      return rv;
   }
   
   @Override
   public boolean setValueQuietly(Long value) {
      boolean rv = super.setValueQuietly(value);
      if (rv) {
         try {
            updatePinMap(value);
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      return rv;
   }

   @Override
   protected Object clone() throws CloneNotSupportedException {
      // TODO Auto-generated method stub
      return super.clone();
   }

   @Override
   public void expressionChanged(Expression expression) {
      super.expressionChanged(expression);
      try {
         updatePinMap(getValueAsLong());
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

}
