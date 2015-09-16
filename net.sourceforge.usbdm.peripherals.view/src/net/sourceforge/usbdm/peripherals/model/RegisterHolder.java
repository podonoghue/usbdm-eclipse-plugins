package net.sourceforge.usbdm.peripherals.model;

public abstract class RegisterHolder extends BaseModel {

   public RegisterHolder(BaseModel parent, String name, String description) {
      super(parent, name, description);
   }

   /**
    * Finds the MemoryBlockCache containing this register
    * 
    * @param address
    * @param sizeInBytes in bytes!
    * 
    * @return null if not found, block otherwise
    */
   public abstract MemoryBlockCache findAddressBlock(long address, long sizeInBytes);

   /**
    * Receive notification that a child register has changed<br>
    * 
    * @param child
    */
   public abstract void registerChanged(RegisterModel child);
}
