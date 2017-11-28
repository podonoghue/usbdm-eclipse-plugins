package net.sourceforge.usbdm.peripherals.model;

import java.util.ArrayList;

import net.sourceforge.usbdm.peripheralDatabase.Enumeration;
import net.sourceforge.usbdm.peripheralDatabase.Field;

/**
 * Model for a field within a register within a peripheral
 *
 */
public class FieldModel extends BaseModel implements UpdateInterface {
   protected  int                    fSize;
   private    int                    fBitOffset;
   private    int                    fBitWidth;
   private    ArrayList<Enumeration> fEnumerations;
   private    String                 fAccessMode;
   private    boolean                fReadable;
   private    boolean                fWriteable;
   
   void init(RegisterModel parent, Field field) {
      assert(parent != null) : "parent can't be null";
      setEnumeratedDescription(field.getEnumerations());
      fEnumerations = null;
      fAccessMode   = field.getAccessType().getAbbreviatedName();
      fSize         = (int)field.getBitwidth();
      fBitOffset    = (int)field.getBitOffset();
      fBitWidth     = (int)field.getBitwidth();
      fReadable     = field.getAccessType().isReadable();
      fWriteable    = field.getAccessType().isWriteable();      
   }
   
   public FieldModel(RegisterModel parent, Field field, ModelInformation information) {
      super(parent, information.getFieldName(), information.getDescription());
      init(parent, field);
   }
   
   /**
    * Gets offset of bit field within register
    * 
    * @return offset (from right)
    */
   public int getBitOffset() {
      return fBitOffset;
   }

   /**
    * Gets width of bit field within register
    * 
    * @return width in bits
    */
   public int getBitWidth() {
      return fBitWidth;
   }

   /**
    * Get value of register
    * 
    * @return Value
    * @throws MemoryException 
    */
   public long getValue() throws MemoryException {
      RegisterModel parent = (RegisterModel) fParent;
      return ((1l<<fSize)-1)&(parent.getValue()>>fBitOffset);
   }
   
   /**
    * Get last value of register
    * 
    * @return Value
    * @throws MemoryException 
    */
   public long getLastValue() throws MemoryException {
      RegisterModel parent = (RegisterModel) fParent;
      return ((1l<<fSize)-1)&(parent.getLastValue()>>fBitOffset);
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
      Long mask         = ((1l<<fSize)-1)<<fBitOffset;
      reg.setValue((currentValue&~mask)|((bitField<<fBitOffset)&mask));
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
         return super.getValueAsBinaryString(getValue(), fSize);
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
         return super.getValueAsHexString(getValue(), fSize);
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
      if ((fEnumerations != null) || (fSize<9)) {
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
      this.fEnumerations = enumeratedDescription;
   }

   /**
    * Gets list describing the various meanings of this field
    * 
    * @return description
    */
   public ArrayList<Enumeration> getEnumeratedDescription() {
      return fEnumerations;
   }

   /**
    * @return the readable
    */
   public boolean isReadable() {
      return fReadable;
   }

   /**
    * Check if field is writable
    * 
    * @return true is writable
    */
   public boolean isWritable() {
      return fWriteable;
   }

   /* (non-Javadoc)
    * @see net.sourceforge.usbdm.peripherals.ui.UsbdmDevicePeripheralsModel.BaseModel#getAddressAsString()
    */
   @Override
   public String getAddressAsString() {
      if (fSize == 1) {
         return String.format("[%d]", fBitOffset);
      }
      else {
         return String.format("[%d:%d]", fBitOffset+fSize-1, fBitOffset);
      }
   }

   /* (non-Javadoc)
    * @see net.sourceforge.usbdm.peripherals.ui.UsbdmDevicePeripheralsModel.BaseModel#getAccessMode()
    */
   @Override
   public String getAccessMode() {
      return fAccessMode;
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

   /**
    * Gets description of currently selected field value
    * 
    * @return Description as string or "" if none
    */
   public String getFieldValueDescription() {
      long fieldValue;
      try {
         fieldValue = getValue();
         for (Enumeration enumeration : getEnumeratedDescription()) {
            if (enumeration.isSelected(fieldValue)) {
               return BaseModel.makeShortDescription(enumeration.getCDescription());
            }
         }
      } catch (MemoryException e) {
      }
      return "";
   }

}