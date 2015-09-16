/*
===============================================================================================================
| History                                                                                                      
---------------------------------------------------------------------------------------------------------------
| 19 Jan 2015 | Removed byte gender code                                                          | V4.10.6.250
===============================================================================================================
*/
package net.sourceforge.usbdm.peripherals.model;

import java.util.ArrayList;

/**
 * Base Model for tree item
 */
public abstract class BaseModel extends ObservableModel {
   protected final BaseModel         fParent;
   protected final ArrayList<Object> fChildren = new ArrayList<Object>();
   private         String            fName;
   protected final String            fDescription;
   protected       long              fAddress;

   /**
    * Constructor
    * 
    * @param parent Parent of this element in tree
    * @param name   Name of this element in tree
    * 
    * @note Added as child of parent if not null
    */
   public BaseModel(BaseModel parent, String name, String description) {
      if (name == null) {
         name = "No device";
      }
      if (description == null) {
         description = "No description";
      }
      fParent      = parent;
      fName        = name;
      fDescription = description;
      fAddress     = 0x00000000;
      if (parent != null) {
         parent.fChildren.add(this);
      }
   }

   /**
    * @return the parent
    */
   public BaseModel getParent() {
      return fParent;
   }

   /**
    * @return the children
    */
   public ArrayList<Object> getChildren() {
      return fChildren;
   }

   /**
    * Gets the name of the tree item
    * 
    * @return String name
    */
   public String getName() {
      return fName;
   }

   /**
    * Sets name of model
    * 
    * @param name
    */
   protected void setName(String name) {
      this.fName = name;
   }
   
   /**
    * Returns a string representing the value in HEX
    * 
    * @param value        Value to process
    * @param sizeInBits  Number of bits in number (<= 32)
    * 
    * @return      String representation in form "0xhhhhh.."
    */
   public static String  getValueAsHexString(long value, int sizeInBits) {
      String format;
      switch ((int)((sizeInBits+3)/4)) {
      case 1  : format = "0x%01X"; break;
      case 2  : format = "0x%02X"; break;
      case 3  : format = "0x%03X"; break;
      case 4  : format = "0x%04X"; break;
      case 5  : format = "0x%05X"; break;
      case 6  : format = "0x%06X"; break;
      case 7  : format = "0x%07X"; break;
      case 8  : format = "0x%08X"; break;
      default : format = "0x%X";   break;
      }
      return String.format(format, value);  
   }

   /**
    * Returns a string representing the value in BINARY
    * 
    * @param value       Value to process
    * @param sizeInBits  Number of bits in number
    * 
    * @return     String representation in form "0bbbbbb..."
    */
   public static String  getValueAsBinaryString(long value, int sizeInBits) {
      String result = "0b";
      for (int index=sizeInBits-1; index>=0; index--) {
         result += ((value&(1<<index)) !=0 )?"1":"0";
      }
      return result;
   }

   /**
    * Returns a string representing the value in an appropriate form for model
    * 
    * @return String representation e.g. "0xhhh"
    * @throws MemoryException 
    */
   public String getValueAsString() throws MemoryException {
      return "<invalid>";
   }

   /**
    * Returns a string representing the value in an appropriate form for model
    * 
    * @return String representation e.g. "0xhhh"
    * @throws MemoryException 
    */
   public String safeGetValueAsString() {
      try {
         return getValueAsString();
      } catch (MemoryException e) {
         e.printStackTrace();
         return "<invalid>";
      }
   }

   /**
    * Gets the memory address of the element
    * 
    * @return Address
    */
   public long getAddress() {
      return fAddress;
   }

   /**
    * Returns a string representing the memory address
    * 
    * @return String representation e.g. "0xhhh"
    */
   public String  getAddressAsString() {
      return String.format("0x%08X", getAddress());
   }

   /**
    * Gets a string representation of the name suitable for tree view  
    *
    * @return string
    */
   public String toString() {
      if (fName != null) {
         return fName;
      }
      if (fDescription != null) {
         return fDescription;
      }
      return super.toString();
   }
   
   /**
    * Indicates if the value has changed compared to the reference value  
    *
    * @return true/false
    */
   public boolean isChanged() throws Exception {
      return false;
   }

   /**
    * Indicates the access available to the register/field etc  
    *
    * @return String similar to "RW", "RO" etc
    */
   public String getAccessMode() {
      return "";
   }

   /**
    * 
    * Gets description of element
    * 
    * @return string
    */
   public String getDescription() {
      return fDescription;
   }

   /**
    * 
    * Gets description of element
    * 
    * @return string
    */
   public static String makeShortDescription(String description) {
      // Truncate at newline if present
      int newlineIndex = description.indexOf("\n");
      if (newlineIndex > 0) {
         description = description.substring(0, newlineIndex);
      }
      newlineIndex = description.indexOf("\t");
      if (newlineIndex > 0) {
         description = description.substring(0, newlineIndex);
      }
      return description;
   }

   /**
    * 
    * Gets description of element
    * 
    * @return string
    */
   public String getShortDescription() {
      return makeShortDescription(getDescription());
   }

   /**
    * Indicates if the value needs to be updated from target
    *
    * @return true/false
    */
   public boolean isNeedsUpdate() {
      return false;
   }

   /**
    * Returns the register value as a binary string of form 0b001100...
    */
   public String getValueAsBinaryString() {
      return "<invalid>";
   }

   /**
    * Returns the register value as a hex string of form 0x12AB03......
    */
   public String getValueAsHexString() {
      return "<invalid>";
   }
   
   /**
    * Returns the register value as a decimal string of form 12345......
    */
   public String getValueAsDecimalString() {
      return "<invalid>";
   }
}