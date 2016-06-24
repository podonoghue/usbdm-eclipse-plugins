package net.sourceforge.usbdm.deviceEditor.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.widgets.Display;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.DeviceVariantInformation;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

public class ModelFactory extends ObservableModel implements IModelChangeListener {

   PeripheralParametersModel fParameterModels = null;
   boolean  underConstruction;
   
   /**
    * Used to sort the pins into categories for display
    */
   static class PinCategory {
      /** Name of category */
      final String               name;
      
      /** Pattern used to select pins to include in this category */
      final Pattern              pattern;
      
      /** Pins collected that match the pattern*/
      ArrayList<Pin>  pins =  new ArrayList<Pin>();

      /**
       * Constructor <br>
       * Used to group the pins into categories for display
       * 
       * @param name       Name of category 
       * @param pattern    Pattern used to select pins to include in this category 
       */
      public PinCategory(String name, String pattern) {
         this.name      = name;
         this.pattern   = Pattern.compile(pattern);
      }

      /**
       * Add pin to category if pin name matches pattern
       * 
       * @param pinInformation
       * 
       * @return true => Added to this category
       */
      boolean tryAdd(Pin pinInformation) {
         boolean rv = pattern.matcher(pinInformation.getName()).matches();
         if (rv) {
            pins.add(pinInformation);
         }
         return rv;
      }
      
      /**
       * Get name of category
       * 
       * @return
       */
      String getName() {
         return name;
      }
      
      /**
       * Get pins in this category
       * 
       * @return
       */
      ArrayList<Pin> getPins() {
         return pins;
      }
   }

   static final private String[] PIN_COLUMN_LABELS         = {"Category.Pin",      "Mux:Signals", "Description"};
   static final private String[] PERIPHERAL_COLUMN_LABELS  = {"Peripheral.Signal", "Mux:Pin",     "Description"};
   static final private String[] PACKAGE_COLUMN_LABELS     = {"Name",              "Value",       "Description"};

   /*
    * =============================================================================================
    */
   /** Data use to construct models */
   private final DeviceInfo   fDeviceInfo;

   /** List of all models */
   ArrayList<PeripheralPageModel> fModels = new ArrayList<PeripheralPageModel>();

   /** List of all pin mapping entries to scan for mapping conflicts */
   protected ArrayList<MappingInfo> fMappingInfos = null;

   /**
    * Create model organised by pin<br>
    * Also updates fMappingInfos
    * 
    * @return Model
    */
   private DevicePinsModel createPinModel() {
      fMappingInfos = new ArrayList<MappingInfo>();

      final ArrayList<PinCategory> categories = new ArrayList<PinCategory>();

      // Construct categories
      for (char c='A'; c<='I'; c++) {
         categories.add(new PinCategory("Port "+c, "PT"+c+".*"));
      }
      categories.add(new PinCategory("Power", "((VDD|VSS|VREGIN|VBAT|VOUT|(VREF(H|L)))).*"));
      categories.add(new PinCategory("Miscellaneous", ".*"));

      // Group pins into categories
      for (String pName:fDeviceInfo.getPins().keySet()) {
         Pin pinInformation = fDeviceInfo.getPins().get(pName);
         if (pinInformation.isAvailableInPackage()) {
            // Only add if available in package
            for (PinCategory category:categories) {
               if (category.tryAdd(pinInformation)) {
                  break;
               }
            }
         }
      }
      // Construct model
      DevicePinsModel deviceModel = new DevicePinsModel(PIN_COLUMN_LABELS, "Pin View", "Pin mapping organized by pin");
      for (PinCategory pinCategory:categories) {
         if (pinCategory.getPins().isEmpty()) {
            continue;
         }
         CategoryModel categoryModel = new CategoryModel(deviceModel, pinCategory.getName(), "");
         for(Pin pinInformation:pinCategory.getPins()) {
            new PinModel(categoryModel, pinInformation);
            for (MappingInfo mappingInfo:pinInformation.getMappedSignals().values()) {
               if (mappingInfo.getMux() == MuxSelection.fixed) {
                  continue;
               }
               if (mappingInfo.getMux() == MuxSelection.disabled) {
                  continue;
               }
               if (mappingInfo.getSignals().get(0) == Signal.DISABLED_SIGNAL) {
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
    * @return Model
    */
   private DevicePinsModel createPeripheralModel() {
      DevicePinsModel deviceModel = new DevicePinsModel(PERIPHERAL_COLUMN_LABELS, "Peripheral View", "Pin mapping organized by peripheral");
      for (String pName:fDeviceInfo.getPeripherals().keySet()) {
         Peripheral peripheral = fDeviceInfo.getPeripherals().get(pName);
         if (peripheral.hasMappableSignals()) {
            new PeripheralModel(deviceModel, peripheral);
         }
      }
      return deviceModel;
   }
   
   /**
    * Create model for device selection and package information
    * 
    * @return Model
    */
   private DeviceInformationModel createPackageModel() {
      DeviceInformationModel packageModel = new DeviceInformationModel(PACKAGE_COLUMN_LABELS, "Project", "Project Settings");
      fDeviceInfo.getModels(packageModel);
      return packageModel;
   }

   /**
    * Create model for Device provided elements usually parameters
    * 
    * @return
    */
   private PeripheralParametersModel createParameterModels() {
      if (fParameterModels != null) {
         return fParameterModels;
      }
      fParameterModels = new PeripheralParametersModel(this, null, "Peripheral Parameters", "These are usually the default values for parameters");
      for (String peripheralName:fDeviceInfo.getPeripherals().keySet()) {
         Peripheral device = fDeviceInfo.getPeripherals().get(peripheralName);
         if (device instanceof IModelEntryProvider) {
            ((IModelEntryProvider) device).getModels(fParameterModels);
         }
      }
      return fParameterModels;
   }

   /**
    * @return the DeviceInfo
    */
   public DeviceInfo getDeviceInfo() {
      return fDeviceInfo;
   }
   
   /** Flag used to prevent multiple consistency checks */
   boolean conflictCheckPending = false;
   
   /** 
    * The model for the first page of the editor<br>
    * This page does not get re-generated
    */
   private DeviceInformationModel fPackageModel= null;
   
   /**
    * The model for the image page of the editor<br>
    * This page does not get re-generated
    */
   PackageImageModel fPackageImageModel ;
   
   /** The current device variant - if this changes the models are rebuilt */
   private DeviceVariantInformation fCurrentDeviceVariant = null;

   /**
    * Sets conflict check as pending<br>
    * Returns original pending state<br>
    * This is used to fold together multiple checks
    * 
    * @return Whether a check was already pending.
    */
   synchronized boolean testAndSetConflictCheckPending(boolean state) {
      boolean rv = conflictCheckPending;
      conflictCheckPending = state;
      return rv;
   }
   
   /**
    * Check for mapping conflicts<br>
    * This is done on a delayed thread for efficiency
    */
   public synchronized void checkConflicts() {
      if (!testAndSetConflictCheckPending(true)) {
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
    * Add mapping to map based on key
    * 
    * @param map           Map to add to
    * @param pinMapping    Pin mapping to add
    * @param key           Key to use in map
    * 
    * @return Existing bin that may conflict
    */
   List<MappingInfo> addToMap(Map<String, List<MappingInfo>> map, MappingInfo pinMapping, String key) {
      
      List<MappingInfo> list = map.get(key);
      if (list != null) {
         list.add(pinMapping);
         return list;
      }
      // First mapping for that pin
      list = new ArrayList<MappingInfo>();
      map.put(key, list);
      list.add(pinMapping);
      return null;
   }
   
   /**
    * Check for mapping conflicts
    * 
    * <li>Multiple signals mapped to a single pin
    * <li>Signals mapped to multiple pins
    */
   private void checkConflictsJob() {
//      System.err.println("checkConflictsJob()");
      testAndSetConflictCheckPending(false);

      /** Used to check for multiple mappings to a single pin */ 
      Map<String, List<MappingInfo>> mappedSignalsByPin      = new HashMap<String, List<MappingInfo>>();

      /** Used to check for a signal being mapped to multiple pins */
      Map<String, List<MappingInfo>> mappedPinsBySignal = new HashMap<String, List<MappingInfo>>();

      for (MappingInfo mapping:fMappingInfos) {
         mapping.setMessage("");
         if (!mapping.isSelected()) {
            continue;
         }
         /*
          * Check for multiple Signals => Pin
          */
         List<MappingInfo> signalsMappedToPin = addToMap(mappedSignalsByPin, mapping, mapping.getPin().getName());
         if (signalsMappedToPin != null) {
            // Signal previously mapped to this pin

            // Check if any conflicts between new signal mapping and existing ones
            // Note - Multiple signals may be mapped to the same pin without conflict 
            // since some signals share a mapping.
            // Need to check the signalLists not the signals
            boolean conflicting = false;
            StringBuffer sb = null;
            for (MappingInfo other:signalsMappedToPin) {
               // Check for conflicts
               if (!mapping.getSignalList().equals(other.getSignalList())) {
                  // Not shared port mapping
                  conflicting = true;
               }
            }
            if (conflicting) {
               // Construct conflict message
               for (MappingInfo other:signalsMappedToPin) {
                  if (sb == null) {
                     sb = new StringBuffer();
                     sb.append("Error: (");
                  }
                  else {
                     sb.append(", ");
                  }
                  sb.append(other.getSignalList());
                  sb.append("@"+other.getMux().name());
               }
               sb.append(") =>> ");
               sb.append(mapping.getPin().getName());
               sb.append("\nPin mapped to multiple signals");
               // Mark all conflicting nodes
               for (MappingInfo other:signalsMappedToPin) {
                  other.setMessage(sb.toString());
               }
            }
         }
         /*
          * Check for Signal => multiple Pins
          */
         List<MappingInfo>  pinsMappedToSignal = addToMap(mappedPinsBySignal, mapping, mapping.getSignalList());
//         System.err.println("checkConflictsJob(): " + mapping);
         if (pinsMappedToSignal != null) {
            // Pins previously mapped to this signal

            // Construct conflict message
            StringBuffer sb = null;
            for (MappingInfo other:pinsMappedToSignal) {
               if (sb == null) {
                  sb = new StringBuffer();
                  sb.append(mapping.getSignalList()+" =>> (");
               }
               else {
                  sb.append(", ");
               }
               sb.append(other.getPin().getName());
            }
            // Multiple signals mapped to pin
            sb.append(")");
            sb.append("\nSignal mapped to multiple pins");
            // Mark all conflicting nodes
            for (MappingInfo other:pinsMappedToSignal) {
               other.setMessage(sb.toString());
            }
         }
      }
      for (BaseModel model:fModels) {
         model.update();
      }
   }

   /**
    * Construct factory that hold models
    * 
    * @param deviceInfo
    */
   public ModelFactory(DeviceInfo deviceInfo) {
      
      underConstruction     = false;
      
      fDeviceInfo           = deviceInfo;
      fCurrentDeviceVariant = fDeviceInfo.getDeviceVariant();

      fDeviceInfo.addListener(this);

      fPackageModel      = createPackageModel();
      fPackageImageModel = new PackageImageModel(this);

      createModels();
      
      underConstruction     = false;
      
      BaseModel.setFactory(this);
   }

   void report() {
      for (String key:fDeviceInfo.getPins().keySet()) {
         Pin pin= fDeviceInfo.getPins().get(key);
         System.err.println(String.format("Pin:%-15s => %-15s %s", pin.getName(), pin.getMuxValue()+",", pin.getMappedSignals().get(pin.getMuxValue())));
      }
      for (String key:fDeviceInfo.getSignals().keySet()) {
         Signal signal = fDeviceInfo.getSignals().get(key);
         System.err.println(String.format("Signal: %-15s => %-15s", signal.getName(), signal.getMappedPin()));
      }
   }
   
   /**
    * Creates models for the variant pages and updates the model list
    */
   void createModels() {
      for (BaseModel model:fModels) {
         if (model != fPackageModel) {
            model.removeListeners();
         }
      }
      fModels = new ArrayList<PeripheralPageModel>();
      fModels.add(fPackageModel);
      fModels.add(createPeripheralModel());
      fModels.add(createPinModel());
      fModels.add(createParameterModels());
      fModels.add(fPackageImageModel);

//      report();
      
      checkConflicts();
   }
   
   /**
    * Get list of all models created
    * 
    * @return
    */
   public ArrayList<PeripheralPageModel> getModels() {
      return fModels;
   }
   
   /**
    * Construct factory that hold models
    * 
    * @param path          Path to load model from
    * @param loadSettings  Controls whether settings are loaded from associated file
    * 
    * @return Model created
    * 
    * @throws Exception 
    */
   public static ModelFactory createModels(Path path, boolean loadSettings) throws Exception {
      return new ModelFactory(DeviceInfo.create(path));
   }

   @Override
   public void modelElementChanged(ObservableModel model) {
      if (underConstruction) {
         return;
      }
      if (model instanceof DeviceInfo) {
         if (fCurrentDeviceVariant != fDeviceInfo.getDeviceVariant()) {
            // Major change
            fCurrentDeviceVariant = fDeviceInfo.getDeviceVariant();
            createModels();
            notifyStructureChangeListeners();
         }
         else {
            // Minor change
            notifyListeners();
         }
      }
   }
   
   @Override
   public void modelStructureChanged(ObservableModel model) {
   }

   public void setHardwareFile(String value) {
   }

   /** Viewers associated with this factory */
   private final ArrayList<StructuredViewer> fViewers = new ArrayList<StructuredViewer>();

   /**
    * Add viewer associated with this factory
    * 
    * @param viewer Viewer to add
    */
   void addViewer(StructuredViewer viewer) {
      fViewers.add(viewer);
   }
   
   /**
    * Remove viewer associated with this factory
    * 
    * @param viewer Viewer to remove
    */
   void removeViewer(StructuredViewer viewer) {
      fViewers.remove(viewer);
   }

   /**
    * Update viewers associated with this factory
    * 
    * @param element
    * @param properties
    */
   public void update(BaseModel element, String[] properties) {
      System.err.println("ModelFactory.update("+element+")");
      for (StructuredViewer viewer:fViewers) {
         if ((viewer != null) && !viewer.getControl().isDisposed()) {
            viewer.update(element, properties);
         };
      }
   }
   
   /**
    * Refresh viewers associated with this factory
    */
   public void refresh() {
      System.err.println("ModelFactory.refresh()");
      for (StructuredViewer viewer:fViewers) {
         if ((viewer != null) && !viewer.getControl().isDisposed()) {
            viewer.refresh();
         };
      }
   }

   @Override
   public void elementStatusChanged(ObservableModel observableModel) {
   }
}
