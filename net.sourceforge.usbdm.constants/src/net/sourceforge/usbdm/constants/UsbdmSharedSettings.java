/*
 * Provided persistent storage for USBDM across Eclipse Workspaces
 * 
 */
package net.sourceforge.usbdm.constants;

import java.io.IOException;

import net.sourceforge.usbdm.jni.Usbdm;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.DialogSettings;

public class UsbdmSharedSettings {

   private static final String USBDM_COMMON_SETTINGS     = "UsbdmCommonSettings";
   private static final String SHARED_SETTINGS_FILENAME  = "UsbdmEclipseSharedSettings.xml";
   private static final IPath  usbdmDataPath = Usbdm.getDataPath().append(SHARED_SETTINGS_FILENAME);
   
   private static DialogSettings sharedSettings = null;
      
   /**
    *    Constructor
    */
   private UsbdmSharedSettings() {
   }
   
   /**
    *    Save settings to file
    */
   public void flush() {
      synchronized (USBDM_COMMON_SETTINGS) {
         try {
            sharedSettings.save(usbdmDataPath.toOSString());
//            System.err.println("UsbdmSharedSettings.flush() - written data to file \'"+usbdmDataPath.toOSString()+"\'\n");
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
   }

   /**
    * Save Key-Value pair
    * 
    * @param Key  Key to save value under
    * @param item String value to be saved
    */
   public void put(String Key, String item) {
      synchronized (USBDM_COMMON_SETTINGS) {
         sharedSettings.put(Key, item);
      }
   }

   /**
    * Save Key-Value pair
    * 
    * @param Key  Key to save value under
    * @param item integer value to be saved
    */
   public void put(String Key, int item) {
      synchronized (USBDM_COMMON_SETTINGS) {
         sharedSettings.put(Key, item);
      }
   }

   /**
    * Save Key-Value pair
    * 
    * @param Key  Key to save value under
    * @param item integer value to be saved
    */
   public void put(String Key, long item) {
      synchronized (USBDM_COMMON_SETTINGS) {
         sharedSettings.put(Key, item);
      }
   }

   /**
    * Save Key-Value pair
    * 
    * @param Key  Key to save value under
    * @param item boolean value to be saved
    */
   public void put(String Key, boolean item) {
      synchronized (USBDM_COMMON_SETTINGS) {
         sharedSettings.put(Key, item);
      }
   }

   /**
    * Retrieve value for key
    * 
    * @param key           Key to look under
    * @param defaultValue  Value returned if not found
    * 
    * @return  Value corresponding to key or defaultValue
    */
   public String get(String key, String defaultValue) {
      String value = null;
      synchronized (key) {
         value = sharedSettings.get(key);
      }
      if (value == null) {
         value = defaultValue;
      }
      return value;
   }
   
   /**
    * Retrieve value for key
    * 
    * @param key           Key to look under
    * @param defaultValue  Value returned if not found
    * 
    * @return  Value corresponding to key or defaultValue
    */
   public int get(String key, int defaultValue) {
      int value = defaultValue;
      String s = null;
      synchronized (key) {
         s = sharedSettings.get(key);
      }
      if (s != null) {
         value = Integer.parseInt(s);
      }
      return value;
   }
   
   /**
    * Retrieve value for key
    * 
    * @param key           Key to look under
    * @param defaultValue  Value returned if not found
    * 
    * @return  Value corresponding to key or defaultValue
    */
   public long get(String key, long defaultValue) {
      long value = defaultValue;
      String s = null;
      synchronized (key) {
         s = sharedSettings.get(key);
      }
      if (s != null) {
         value = Long.parseLong(s);
      }
      return value;
   }
  
   /**
    * Retrieve value for key
    * 
    * @param key           Key to look under
    * @param defaultValue  Value returned if not found
    * 
    * @return  Value corresponding to key or defaultValue
    */
   public boolean get(String key, boolean defaultValue) {
      boolean value = defaultValue;
      String s = null;
      synchronized (key) {
         s = sharedSettings.get(key);
      }
      if (s != null) {
         value = Boolean.parseBoolean(s);
      }
      return value;
   }
   
   /**
    * Retrieve value for key
    * 
    * @param key Key to look under
    * 
    * @return  Value corresponding to key or null if not found
    */
   public String get(String key) {
      return get(key, (String)null);
   }
   
   /**
    * @return Shared settings object
    */
   public static UsbdmSharedSettings getSharedSettings() {
      synchronized (USBDM_COMMON_SETTINGS) {
         if (sharedSettings == null) {
            sharedSettings = new DialogSettings(USBDM_COMMON_SETTINGS);
            try {
               sharedSettings.load(usbdmDataPath.toOSString());
               System.err.println("UsbdmSharedSettings.getSharedSettings() - loaded settings from file \'"+usbdmDataPath.toOSString()+"\'\n");
            } catch (IOException e) {
               // Ignore as the file may not exist yet
               System.err.println("UsbdmSharedSettings.getSharedSettings() - file doesn't exist");
            }
         }
         return new UsbdmSharedSettings();
      }
   }
}
