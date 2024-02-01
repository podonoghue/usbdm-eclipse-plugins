package net.sourceforge.usbdm.deviceEditor.model;

import java.util.ArrayList;

/**
 * Used to communicate changes in the model element.
 *
 */
public interface IModelChangeListener {
   
   public static final int PROPERTY_VALUE     = 0b00001;
   public static final int PROPERTY_STATUS    = 0b00010;
   public static final int PROPERTY_MAPPING   = 0b00100;
   public static final int PROPERTY_STRUCTURE = 0b01000;
   public static final int PROPERTY_HIDDEN    = 0b10000;
   
   final String names[] = {
         "Value",
         "Status",
         "Mapping",
         "Structure",
         "Hidden",
   };
   
   /**
    * Convert properties to array of property strings
    * 
    * @param properties
    * 
    * @return array of properties or null if none
    */
   public static String[] getProperties(int properties) {
      
      if (properties == 0) {
         return null;
      }
      
      ArrayList<String> props = new ArrayList<String>();
      for (int bitNum=0; bitNum<names.length; bitNum++) {
         if (((1<<bitNum)&properties) != 0) {
            props.add(names[bitNum]);
         }
      }
      return props.toArray(new String[props.size()]);
   }

   /**
    * Get names of the properties for debugging
    * 
    * @param properties
    * 
    * @return
    */
   public static String getPropertyNames(int properties) {
      StringBuilder sb = new StringBuilder();
      for (int bitNum=0; bitNum<names.length; bitNum++) {
         if (((1<<bitNum)&properties) != 0) {
            if (!sb.isEmpty()) {
               sb.append(",");
            }
            sb.append(names[bitNum]);
         }
      }
      return sb.toString();
   }
   
   /**
    * Called when the model changes.
    * 
    * @param observableModel - The model element that has changed
    * @param properties      - Indicates properties changed (bitmask)
    */
   void modelElementChanged(ObservableModelInterface observableModel, int properties);
   
}
