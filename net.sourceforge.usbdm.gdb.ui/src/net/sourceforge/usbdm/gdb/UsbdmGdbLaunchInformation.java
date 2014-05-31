package net.sourceforge.usbdm.gdb;

import java.util.HashMap;

public class UsbdmGdbLaunchInformation {

   private HashMap<String, String> map = new HashMap<String, String>();
   
   public UsbdmGdbLaunchInformation() {
   }
   
   public String getValue(String key) {
      return map.get(key);
   }
   
   public void addValue(String key, String value) {
      map.put(key, value);
   }
   
}
