package net.sourceforge.usbdm.deviceEditor.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.swt.widgets.Display;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.PeripheralFunction;
import net.sourceforge.usbdm.deviceEditor.information.PinInformation;
import net.sourceforge.usbdm.deviceEditor.parser.ParseFamilyCSV;
import net.sourceforge.usbdm.deviceEditor.xmlParser.ParseFamilyXML;

public class ModelFactory {

   static class PinCategory {
      final String               name;
      final Pattern              pattern;
      ArrayList<PinInformation>  pins =  new ArrayList<PinInformation>();

      public PinCategory(String name, String pattern) {
         this.name      = name;
         this.pattern   = Pattern.compile(pattern);
      }

      boolean tryAdd(PinInformation pinInformation) {
         boolean rv = pattern.matcher(pinInformation.getName()).matches();
         if (rv) {
            pins.add(pinInformation);
         }
         return rv;
      }
      String getName() {
         return name;
      }
      ArrayList<PinInformation> getPins() {
         return pins;
      }
   }

   static final private String[] PIN_COLUMN_LABELS         = {"Category.Pin",      "Mux:Signals", "Description"};
   static final private String[] PERIPHERAL_COLUMN_LABELS  = {"Peripheral.Signal", "Mux:Pin",     "Description"};

   /*
    * =============================================================================================
    */
   /** Peripheral based model */
   private final DeviceModel fPeripheralModel;

   /** Pin based model */
   private final DeviceModel fPinModel;

   /** Data use to construct models */
   private final DeviceInfo  fDeviceInfo;

   /** List of pin mapping entries */
   protected final ArrayList<MappingInfo> fMappingInfos = new ArrayList<MappingInfo>();

   /**
    * Create model organised by pin
    * 
    * @return
    */
   private DeviceModel createPinModel() {
      final ArrayList<PinCategory> categories = new ArrayList<PinCategory>();

      // Construct categories
      for (char c='A'; c<='I'; c++) {
         categories.add(new PinCategory("Port "+c, "PT"+c+".*"));
      }
      categories.add(new PinCategory("Misc ", ".*"));

      // Group pins into categories
      for (String pName:fDeviceInfo.getPins().keySet()) {
         PinInformation pinInformation = fDeviceInfo.getPins().get(pName);
         for (PinCategory category:categories) {
            if (category.tryAdd(pinInformation)) {
               break;
            }
         }
      }
      // Construct model
      DeviceModel deviceModel = new DeviceModel(this, PIN_COLUMN_LABELS, "Pin View", "Pin mapping organized by pin");
      for (PinCategory pinCategory:categories) {
         if (pinCategory.getPins().isEmpty()) {
            continue;
         }
         CategoryModel catergoryModel = new CategoryModel(deviceModel, pinCategory.getName(), "");
         for(PinInformation pinInformation:pinCategory.getPins()) {
            new PinModel(catergoryModel, pinInformation);
            for (MappingInfo mappingInfo:pinInformation.getMappedFunctions().values()) {
               if (mappingInfo.getMux() == MuxSelection.fixed) {
                  continue;
               }
               if (mappingInfo.getMux() == MuxSelection.disabled) {
                  continue;
               }
               if (mappingInfo.getFunctions().get(0) == PeripheralFunction.DISABLED) {
                  continue;
               }
               fMappingInfos.add(mappingInfo);
            }
         }
      }
      return deviceModel;
   }

   /**
    * Create model organised by peripheral
    * 
    * @return
    */
   private DeviceModel createPeripheralModel() {
      DeviceModel deviceModel = new DeviceModel(this, PERIPHERAL_COLUMN_LABELS, "Peripheral View", "Pin mapping organized by peripheral");
      for (String pName:fDeviceInfo.getPeripherals().keySet()) {
         Peripheral peripheral = fDeviceInfo.getPeripherals().get(pName);
         if (peripheral.getFunctions().size() == 0) {
            // Ignore peripherals without pins
            continue;
         }
         PeripheralModel.createPeripheralModel(deviceModel, peripheral);
      }
      return deviceModel;
   }

   /**
    * @return the PeripheralModel
    */
   public DeviceModel getPeripheralModel() {
      return fPeripheralModel;
   }

   /**
    * @return the PinModel
    */
   public DeviceModel getPinModel() {
      return fPinModel;
   }

   /**
    * @return the DeviceInfo
    */
   public DeviceInfo getDeviceInfo() {
      return fDeviceInfo;
   }
   
   /** Flag used to prevent multiple checks */
   boolean checkPending = false;

   /**
    * Sets conflict check as pending<br>
    * Returns original pending state
    * 
    * @return Whether a check was already pending.
    */
   synchronized boolean testAndSetCheckPending(boolean state) {
      boolean rv = checkPending;
      checkPending = state;
      return rv;
   }
   
   /**
    * Check for mapping conflicts<br>
    * This is done on a delayed thread
    */
   public synchronized void checkConflicts() {
//      System.err.println("checkConflicts() ===================");
      if (!testAndSetCheckPending(true)) {
         // Start new check
         Runnable runnable = new Runnable() {
            public void run() {
               try {
                  Thread.sleep(100);
               } catch (InterruptedException e) {
               }
               Display.getDefault().syncExec(new Runnable() {
                 public void run() {
                    checkConflictsJob();
                 }
               });
            }
         };
         new Thread(runnable).start();
      }
   }

   /**
    * Check for mapping conflicts
    */
   private void checkConflictsJob() {
//      System.err.println("checkConflictsJob() ===================");
      testAndSetCheckPending(false);
      Map<String, List<MappingInfo>> mappedNodes = new HashMap<String, List<MappingInfo>>();
      for (MappingInfo mapping:fMappingInfos) {
         mapping.setMessage("");
         if (!mapping.isSelected()) {
            continue;
         }
         List<MappingInfo> list = mappedNodes.get(mapping.getPin().getName());
         if (list == null) {
            list = new ArrayList<MappingInfo>();
            mappedNodes.put(mapping.getPin().getName(), list);
            list.add(mapping);
         }
         else {
            StringBuffer sb = null;
            for (MappingInfo other:list) {
               // Check for conflicts
               if (!mapping.getFunctionList().equals(other.getFunctionList())) {
                  if (sb == null) {
                     sb = new StringBuffer();
                     sb.append("Conflict(");
                     sb.append(mapping.getFunctionList());
                  }
                  sb.append(", ");
                  sb.append(other.getFunctionList());
               }
            }
            list.add(mapping);
            if (sb != null) {
               // Multiple functions mapped to pin
               sb.append(")");
               System.err.println(sb.toString());
               mapping.setMessage(sb.toString());
               // Mark all conflicting nodes
               for (MappingInfo other:list) {
                  System.err.println("Marking: "+other);
                  other.setMessage(sb.toString());
               }
            }
         }
      }
      for (MappingInfo mapping:fMappingInfos) {
         if (mapping.isRefreshPending()) {
            mapping.notifyListeners();
         }
      }
   }

   /**
    * Construct factory that hold models
    * 
    * @param deviceInfo
    */
   public ModelFactory(DeviceInfo deviceInfo) {
      fDeviceInfo      = deviceInfo;
      fPeripheralModel = createPeripheralModel();
      fPinModel        = createPinModel();
   }

   /**
    * Construct factory that hold models
    * 
    * @param path Path to load model from
    * 
    * @return Model created
    * 
    * @throws Exception 
    */
   public static ModelFactory createModel(Path path) throws Exception {
      
      DeviceInfo deviceInfo = null;

      System.err.println("ModelFactory.createModel("+path.toAbsolutePath()+")");
      
      if (path.getFileName().toString().endsWith("csv")) {
//         System.err.println("DeviceEditor(), Opening as CSV" + path);
         ParseFamilyCSV parser = new ParseFamilyCSV();
         deviceInfo = parser.parseFile(path);
      }
      else if ((path.getFileName().toString().endsWith("xml"))||(path.getFileName().toString().endsWith("hardware"))) {
//         System.err.println("DeviceEditor(), Opening as XML = " + path);
         ParseFamilyXML parser = new ParseFamilyXML();
         deviceInfo = parser.parseFile(path);
      }
      else {
         throw new Exception("Unknown file type");
      }
      return new ModelFactory(deviceInfo);
   }

}
