package net.sourceforge.usbdm.peripherals.model;

public abstract class RegisterHolder extends BaseModel {

   public RegisterHolder(BaseModel parent, String name, String description) {
      super(parent, name, description);
   }

   public abstract MemoryBlockCache findAddressBlock(long address, long sizeInBytes);

   public abstract void registerChanged(RegisterModel child);
}
