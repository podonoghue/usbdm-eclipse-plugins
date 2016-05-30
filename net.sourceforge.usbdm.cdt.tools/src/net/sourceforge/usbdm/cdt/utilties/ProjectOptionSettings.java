package net.sourceforge.usbdm.cdt.utilties;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.sourceforge.usbdm.cdt.tools.UsbdmConstants;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

public class ProjectOptionSettings {
   private Map<String,String> fVariableMap;
   private IProject           fProjectHandle;
   public static final String USBDM_PROJECT_SETTINGS = ".usbdm-settings";
   
   public ProjectOptionSettings(IProject projectHandle) throws Exception {
      fProjectHandle = projectHandle;
      loadSetting();
   }
   
   public ProjectOptionSettings(IProject projectHandle, Map<String,String> variableMap) {
      fVariableMap = new HashMap<String, String>();
      Iterator<Entry<String, String>> it = variableMap.entrySet().iterator(); 
      while (it.hasNext()) {
         Entry<String, String> pairs = it.next();
         if (pairs.getKey().startsWith(UsbdmConstants.CONDITION_PREFIX_KEY)) {
            fVariableMap.put(pairs.getKey(), pairs.getValue());
         }
      }
      fProjectHandle = projectHandle;
   }
   
   @SuppressWarnings("unchecked")
   private void loadSetting() throws Exception {
      InputStream        is  = null;
      ObjectInputStream  ois = null;
      try {
         IFile resourceFile = fProjectHandle.getFile(USBDM_PROJECT_SETTINGS);
         is = resourceFile.getContents();
         ois = new ObjectInputStream(is);
         fVariableMap = (HashMap<String, String>) ois.readObject();
      } finally {
         if (ois != null) {
            ois.close();
         }
         if (is != null) {
            is.close();
         }
      }
   }
   
   public void saveSetting() throws Exception {
      IFile resourceFile = fProjectHandle.getFile(USBDM_PROJECT_SETTINGS);
      ByteArrayOutputStream bos = null;
      ObjectOutputStream    oos = null;
      try {
         bos = new ByteArrayOutputStream(1000);
         oos = new ObjectOutputStream(bos);
         oos.writeObject(fVariableMap);
         if (resourceFile.exists()) {
            resourceFile.delete(true, null);
         }
         ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
         resourceFile.create(bis, true, null);
      } finally {
         if (bos != null) {
            bos.close();
         }
         if (oos != null) {
            oos.close();
         }
      }
   }

   /**
    * @return the variableMap
    */
   public Map<String, String> getVariableMap() {
      return fVariableMap;
   }
   
}
