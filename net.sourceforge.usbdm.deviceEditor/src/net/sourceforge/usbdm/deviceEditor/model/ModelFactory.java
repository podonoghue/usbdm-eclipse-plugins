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
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

public class ModelFactory extends ObservableModel implements IModelChangeListener {

   TabModel fParameterModels = null;
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

   /*
    * =============================================================================================
    */
   /** Data use to construct models */
   private final DeviceInfo   fDeviceInfo;

   /** List of all models */
   ArrayList<IPage> fModels = new ArrayList<IPage>();

   /**
    * Create model organised by pin<br>
    * Also updates fMappingInfos
    * 
    * @return Model
    */
   private DeviceSignalsModel createPinModel() {
      return new DeviceSignalsModel(null, fDeviceInfo);
   }

   /**
    * Create model organised by peripheral
    * 
    * @return Model
    */
   private DevicePinsModel createPeripheralModel() {
      return new DevicePinsModel(null, fDeviceInfo);
   }
   
   /**
    * Create model for Device provided elements usually parameters
    * 
    * @return
    */
   private TabModel createParameterModels() {
//      System.err.println("createParameterModels()");
      //XXX Working here
//      if (fParameterModels != null) {
////         for (Object m:fParameterModels.getChildren()) {
////            BaseModel model = (BaseModel) m;
////         }
//         return fParameterModels;
//      }
      fParameterModels = new TabModel(null, "Peripheral Parameters", "Interrupt handling and\ndefault settings used by defaultConfigure()");
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
   
   /** Models representing all pins */
   private DeviceSignalsModel fPinModel;

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

      for (MappingInfo mapping:fPinModel.getMappingInfos()) {
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
      for (IPage model:fModels) {
         model.updatePage();
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

      fPackageModel      = (DeviceInformationModel)fDeviceInfo.getModel(null);
      
      fPackageImageModel = new PackageImageModel(this, null);

      createModels();
      
      underConstruction     = false;
      
      BaseModel.setFactory(this);
   }

   void report() {
      for (String key:fDeviceInfo.getPins().keySet()) {
         Pin pin= fDeviceInfo.getPins().get(key);
         System.err.println(String.format("Pin:%-15s => %-15s %s", pin.getName(), pin.getMuxValue()+",", pin.getMappableSignals().get(pin.getMuxValue())));
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
      for (IPage page:fModels) {
         if ((page != null)&&(page != fPackageModel)) {
            page.getModel().removeListeners();
         }
      }
      fModels = new ArrayList<IPage>();
      fModels.add(fPackageModel);
      fModels.add(createPeripheralModel());
      fPinModel = createPinModel();
      fModels.add(fPinModel);
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
   public ArrayList<IPage> getModels() {
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
