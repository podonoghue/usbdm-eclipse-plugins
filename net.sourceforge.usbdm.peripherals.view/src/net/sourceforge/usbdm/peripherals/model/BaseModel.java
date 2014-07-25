package net.sourceforge.usbdm.peripherals.model;

import java.util.ArrayList;

/**
 * Base Model for tree item
 */
public abstract class BaseModel extends ObservableModel {
   protected final BaseModel         parent;
   protected final ArrayList<Object> children = new ArrayList<Object>();
   protected String                  name;
   protected final String            description;
   protected       long              address;
   private static  boolean           littleEndian = true;

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
      this.parent      = parent;
      this.name        = name;
      this.description = description;
      this.address     = 0x00000000;
      if (parent != null) {
         parent.children.add(this);
      }
   }

   /**
    * @return the parent
    */
   public BaseModel getParent() {
      return parent;
   }

   /**
    * @return the children
    */
   public ArrayList<Object> getChildren() {
      return children;
   }

   /**
    * Gets the name of the tree item
    * 
    * @return String name
    */
   public String getName() {
      return name;
   }

   /**
    * Returns a string representing the value in HEX
    * 
    * @param value Value to process
    * @param size  Number of bits in number (<= 32)
    * @return      String representation in form "0xhhhhh.."
    */
   public static String  getValueAsHexString(long value, int size) {
      String format;
      switch ((int)((size+3)/4)) {
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
    * @param value Value to process
    * @param size  Number of bits in number
    * @return      String representation in form "0bbbbbb..."
    */
   public static String  getValueAsBinaryString(long value, int size) {
      String result = "0b";
      for (int index=size-1; index>=0; index--) {
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
      return "";
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
         return "-- invalid --";
      }
   }

   /**
    * Gets the memory address of the element
    * 
    * @return Address
    */
   public long getAddress() {
      return address;
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
      if (name != null) {
         return name;
      }
      if (description != null) {
         return description;
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
    * @return String wimilar to "RW", "RO" etc
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
      return description;
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
    * Indicates if the target is littleEndian
    * 
    * @return
    */
   public static boolean isLittleEndian() {
      return littleEndian;
   }

   /**
    * Set the endianess of the target
    * 
    * @param littleEndian
    */
   public static void setLittleEndian(boolean littleEndian) {
      BaseModel.littleEndian = littleEndian;
   }

   /**
    * Returns the value shifted by offset
    * @param value
    * @param offset
    * @return
    */
   static long unsignedShift(byte value, int offset) {
      
      return (((long)value) & 0xFFL)<<offset;
   }
   
   /**
    * Calculates a 32-bit unsigned value from the 2st four element of a byte[]
    * 
    * @param bytes to process
    * 
    * @return converted value
    */
   public static long getValue32bit(byte[] bytes) {
      return getValue32bit(bytes, 0);
   }
   
   /**
    * Calculates a 16-bit unsigned value from the 1st two element of a byte[]
    * 
    * @param bytes to process
    * 
    * @return converted value
    */
   public static long getValue16bit(byte[] bytes) {
      return getValue16bit(bytes, 0);
   }

   /**
    * Calculates a 32-bit unsigned value from the 1st element of a byte[]
    * 
    * @param bytes to process
    * 
    * @return converted value
    */
   public static long getValue8bit(byte[] bytes) {
      return getValue8bit(bytes, 0);
   }
   
   /**
    * Calculates a 32-bit unsigned value from the 1st four elements of a byte[]
    * 
    * @param bytes to process
    * 
    * @return converted value
    */
   public static long getValue32bit(byte[] bytes, int offset) {
//      System.err.println("BaseModel.getValue32bit("+bytes+")");
      long value = 0x0BADF00D;
      if (bytes != null) {
         if (isLittleEndian()) {
            value = unsignedShift(bytes[offset+0], 0)+unsignedShift(bytes[offset+1], 8)+unsignedShift(bytes[offset+2], 16)+unsignedShift(bytes[offset+3], 24);
//            System.err.println(String.format("BaseModel.getValue32bit(), littleEndian => 0x%08X",value));
         }
         else {
            value = unsignedShift(bytes[offset+0], 24)+unsignedShift(bytes[offset+1], 16)+unsignedShift(bytes[offset+2], 8)+unsignedShift(bytes[offset+3], 0);
//            System.err.println(String.format("BaseModel.getValue32bit(), bigEndian => 0x%08X",value));
         }
      }
      return value;
   }
   
   /**
    * Calculates a 16-bit unsigned value from the 1st two element of a byte[]
    * 
    * @param bytes to process
    * 
    * @return converted value
    */
   public static long getValue16bit(byte[] bytes, int offset) {
      long value = 0x0BADF00D;
      if (bytes != null) {
         if (isLittleEndian()) {
            value = unsignedShift(bytes[offset+0], 0)+unsignedShift(bytes[offset+1], 8);
         }
         else {
            value = unsignedShift(bytes[offset+0], 8)+unsignedShift(bytes[offset+1], 0);
         }
      }
      return value;
   }

   /**
    * Calculates a 32-bit unsigned value from the 1st element of a byte[]
    * 
    * @param bytes to process
    * 
    * @return converted value
    */
   public static long getValue8bit(byte[] bytes, int offset) {
      long value = 0x0BADF00D;
      if (bytes != null) {
         value = unsignedShift(bytes[offset], 0);
      }
      return (value);
   }


   /**
    * Creates a byte[1] from 8-bit unsigned value
    * 
    * @param bytes to process
    * 
    * @return converted value
    */
   public byte[] convertValue8bit(long value) {
      byte[] data = new byte[1];
      data[0] = (byte)value;
      return data;
   }

   /**
    * Creates a byte[2] from 16-bit unsigned value
    * 
    * @param bytes to process
    * 
    * @return converted value
    */
   public byte[] convertValue16bit(long value) {
      byte[] data = new byte[2];
      data[0] = (byte)value;
      data[1] = (byte)(value>>8);
      return data;
   }

   /**
    * Creates a byte[4] from 32-bit unsigned value
    * 
    * @param bytes to process
    * 
    * @return converted value
    */
   public byte[] convertValue32bit(long value) {
      byte[] data = new byte[4];
      data[0] = (byte)value;
      data[1] = (byte)(value>>8);
      data[2] = (byte)(value>>16);
      data[3] = (byte)(value>>24);
      return data;
   }

}