package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.BitmaskVariableModel;

public class BitmaskVariable extends LongVariable {

   // Permitted bits 
   private long fPermittedBits = 0xFFFFFFFF;
   
   // List of bit names (lsb-msb)
   private String fBitList;
   

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
   public String getValueAsString(long value) {
      return "0x"+Long.toHexString(value);
   }

   @Override
   public String getSubstitutionValue() {
      return getValueAsString();
   }

   /**
    * Set list of bit names (lsb-msb)
    * 
    * @param bitList e.g. "bit0,bit1"
    */
   public void setBitList(String bitList) {
      fBitList = bitList;
   }

   /**
    * Get list of bit names (lsb-msb)
    */
   public String getBitList() {
      return fBitList;
   }

   
}
