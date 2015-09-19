package net.sourceforge.usbdm.peripherals.model;

import java.util.ArrayList;

import net.sourceforge.usbdm.peripheralDatabase.Enumeration;
import net.sourceforge.usbdm.peripheralDatabase.Field;

/**
 * Model for a field within a register within a peripheral
 *
 */
public class FieldModel extends BaseModel implements UpdateInterface {
   protected  int                    size;
   private    int                    bitOffset;
   private    ArrayList<Enumeration> enumerations;
   private    String                 accessMode;
   private    boolean                readable;
   private    boolean                writeable;
   
   void init(RegisterModel parent, Field field) {
      assert(parent != null) : "parent can't be null";
      setEnumeratedDescription(field.getEnumerations());
      enumerations = null;
      accessMode   = field.getAccessType().getAbbreviatedName();
      size         = (int)field.getBitwidth();
      bitOffset    = (int)field.getBitOffset();
      readable     = field.getAccessType().isReadable();
      writeable    = field.getAccessType().isWriteable();      
   }
   
   public FieldModel(RegisterModel parent, Field field, ModelInformation information) {
      super(parent, information.getFieldName(), information.getDescription());
      init(parent, field);
   }
   
   /**
    * Gets offset of bit field with register
    * 
    * @return offset (from right)
    */
   public int getBitOffset() {
      return bitOffset;
   }

   /**
    * Get value of register
    * 
    * @return Value
    * @throws MemoryException 
    */
   public long getValue() throws MemoryException {
      RegisterModel parent = (RegisterModel) fParent;
      return ((1l<<size)-1)&(parent.getValue()>>bitOffset);
   }
   
   /**
    * Get last value of register
    * 
    * @return Value
    * @throws MemoryException 
    */
   public long getLastValue() throws MemoryException {
      RegisterModel parent = (RegisterModel) fParent;
      return ((1l<<size)-1)&(parent.getLastValue()>>bitOffset);
   }

   /**
    * Sets the value of this bit-field within the register
    * Note this actually modifies the owning register
    * May trigger view updates
    * 
    * @param bitField New bit-field value
    */
   public void setValue(Long bitField) {
      Long currentValue = 0L;
      RegisterModel reg = (RegisterModel) fParent;
      try {
         currentValue = reg.getValue();
      } catch (MemoryException e) {
      }
      Long mask         = ((1l<<size)-1)<<bitOffset;
      reg.setValue((currentValue&~mask)|((bitField<<bitOffset)&mask));
   }

   /* (non-Javadoc)
    * @see net.sourceforge.usbdm.peripherals.ui.UsbdmDevicePeripheralsModel.BaseModel#isChanged()
    */
   @Override
   public boolean isChanged() {
      try {
         return getValue() != getLastValue();
      } catch (MemoryException e) {
         // Quietly ignore
      }
      return false;
   }

   /* (non-Javadoc)
    * @see net.sourceforge.usbdm.peripherals.ui.UsbdmDevicePeripheralsModel.BaseModel#getValueAsString()
    */
   @Override
   public String getValueAsBinaryString() {
      RegisterModel parent = (RegisterModel) this.fParent;
      String rv = parent.getStatus();
      if (rv != null) {
         return rv;
      }
      try {
         return super.getValueAsBinaryString(getValue(), size);
      } catch (MemoryException e) {
         return "<invalid>";
      }
   }

   /* (non-Javadoc)
    * @see net.sourceforge.usbdm.peripherals.ui.UsbdmDevicePeripheralsModel.BaseModel#getValueAsString()
    */
   @Override
   public String getValueAsHexString() {
      RegisterModel parent = (RegisterModel) fParent;
      String rv = parent.getStatus();
      if (rv != null) {
         return rv;
      }
      try {
         return super.getValueAsHexString(getValue(), size);
      } catch (MemoryException e) {
         return "<invalid>";
      }
   }


   /* (non-Javadoc)
    * @see net.sourceforge.usbdm.peripherals.model.BaseModel#getValueAsDecimalString()
    */
   @Override
   public String getValueAsDecimalString() {
      RegisterModel parent = (RegisterModel) fParent;
      String rv = parent.getStatus();
      if (rv != null) {
         return rv;
      }
      try {
         return String.format("%d", getValue());
      } catch (MemoryException e) {
         return "<invalid>";
      }
   }
   
   /* (non-Javadoc)
    * @see net.sourceforge.usbdm.peripherals.ui.UsbdmDevicePeripheralsModel.BaseModel#getValueAsString()
    */
   @Override
   public String getValueAsString() {
      if ((enumerations != null) || (size<9)) {
         return getValueAsBinaryString();
      }
      else {
         return getValueAsHexString();
      }
   }

   /**
    * Sets description of the various meanings of this field
    * 
    * @param enumeratedDescription description to set
    */
   public void setEnumeratedDescription(ArrayList<Enumeration> enumeratedDescription) {
      this.enumerations = enumeratedDescription;
   }

   /**
    * Gets list describing the various meanings of this field
    * 
    * @return description
    */
   public ArrayList<Enumeration> getEnumeratedDescription() {
      return enumerations;
   }

   /**
    * @return the readable
    */
   public boolean isReadable() {
      return readable;
   }

   /**
    * Check if field is writable
    * 
    * @return true is writable
    */
   public boolean isWritable() {
      return writeable;
   }

   /* (non-Javadoc)
    * @see net.sourceforge.usbdm.peripherals.ui.UsbdmDevicePeripheralsModel.BaseModel#getAddressAsString()
    */
   @Override
   public String getAddressAsString() {
      if (size == 1) {
         return String.format("[%d]", bitOffset);
      }
      else {
         return String.format("[%d:%d]", bitOffset+size-1, bitOffset);
      }
   }

   /* (non-Javadoc)
    * @see net.sourceforge.usbdm.peripherals.ui.UsbdmDevicePeripheralsModel.BaseModel#getAccessMode()
    */
   @Override
   public String getAccessMode() {
      return accessMode;
   }

   /* (non-Javadoc)
    * @see net.sourceforge.usbdm.peripherals.ui.UsbdmDevicePeripheralsModel.BaseModel#hasChanged()
    */
   @Override
   public boolean isNeedsUpdate() {
      RegisterModel reg = (RegisterModel) fParent;
      return reg.isNeedsUpdate();
   }

   /* (non-Javadoc)
    * @see net.sourceforge.usbdm.peripherals.model.UpdateInterface#forceUpdate()
    */
   @Override
   public void forceUpdate() {
         // Pass to parent - entire register is updated
         RegisterModel reg = (RegisterModel) fParent;
         reg.forceUpdate();
   }

}