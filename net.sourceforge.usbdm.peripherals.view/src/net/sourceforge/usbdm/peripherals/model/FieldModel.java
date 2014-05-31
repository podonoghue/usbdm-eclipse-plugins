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
   private    final String           accessMode;

   public FieldModel(RegisterModel parent, Field field) {
      super(parent, field.getName(), field.getCDescription());
      assert(parent != null) : "parent can't be null";
      size                  = (int)field.getBitwidth();
      bitOffset             = (int)field.getBitOffset();
      enumerations          = null;
      accessMode            = field.getAccessType().getAbbreviatedName();
      setEnumeratedDescription(field.getEnumerations());
   }
   
   public FieldModel(RegisterModel parent, Field field, int index) throws Exception {
      super(parent, field.getName(index), field.getCDescription(index));
      assert(parent != null) : "parent can't be null";
      size                  = (int)field.getBitwidth();
      bitOffset             = (int)field.getBitOffset();
      enumerations          = null;
      accessMode            = field.getAccessType().getAbbreviatedName();
      setEnumeratedDescription(field.getEnumerations());
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
    */
   public long getValue() {
      RegisterModel parent = (RegisterModel) this.parent;
      return ((1l<<size)-1)&(parent.getValue()>>bitOffset);
   }
   
   /**
    * Get last value of register
    * 
    * @return Value
    */
   public long getLastValue() {
      RegisterModel parent = (RegisterModel) this.parent;
      return ((1l<<size)-1)&(parent.getLastValue()>>bitOffset);
   }

   /* (non-Javadoc)
    * @see net.sourceforge.usbdm.peripherals.ui.UsbdmDevicePeripheralsModel.BaseModel#isChanged()
    */
   @Override
   public boolean isChanged() {
      return getValue() != getLastValue();
   }

   /* (non-Javadoc)
    * @see net.sourceforge.usbdm.peripherals.ui.UsbdmDevicePeripheralsModel.BaseModel#getValueAsString()
    */
   @Override
   public String getValueAsString() {
      if ((enumerations != null) || (size<9)) {
         return super.getValueAsBinaryString(getValue(), size);
      }
      else {
         return super.getValueAsHexString(getValue(), size);
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
    * Sets description of the various meanings of this field
    * 
    * @return description
    */
   public ArrayList<Enumeration> getEnumeratedDescription() {
      return enumerations;
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

   /**
    * Sets the value of this bit-field within the register
    * Note this actually modifies the owning register
    * May trigger view updates
    * 
    * @param bitField New bit-field value
    * @throws Exception 
    */
   public void setValue(Long bitField) {
      Long currentValue = ((RegisterModel)(this.parent)).getValue();
      Long mask         = ((1l<<size)-1)<<bitOffset;
      ((RegisterModel)(this.parent)).setValue((currentValue&~mask)|((bitField<<bitOffset)&mask));
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
      return parent.isNeedsUpdate();
   }

   @Override
   public void forceUpdate() {
      // Pass to parent - entire register is updated
      ((RegisterModel)parent).forceUpdate();
   }

}