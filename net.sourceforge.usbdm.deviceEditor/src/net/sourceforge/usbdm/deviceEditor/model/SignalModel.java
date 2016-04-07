package net.sourceforge.usbdm.deviceEditor.model;

import java.util.ArrayList;
import java.util.TreeSet;

import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

public class SignalModel extends SelectionModel implements IModelChangeListener {

   /** List of possible mappings for this signal */
   protected final ArrayList<MappingInfo> fMappingInfos = new ArrayList<MappingInfo>();
   
   /** Current mapping (may be null) */
   private MappingInfo fCurrentMapping;
   
   private Signal fSignal = null;
   
//   /** Peripheral associated with this model */
//   private PeripheralFunction fPeripheralFunction;
   
   public SignalModel(PeripheralModel parent, Signal signal) {
      super(parent, signal.getName(), "");
      //      fPeripheralFunction = peripheralFunction;

      TreeSet<MappingInfo> mappingInfoSet = signal.getPinMapping();
      MappingInfo firstMapping = signal.getPinMapping().first();
      if (firstMapping.getMux() == MuxSelection.fixed) {
         // Fixed mapping for function
         fValues = new String[] {signal.getPinMapping().first().getPin().getName()};
         return;
      }

      fSignal = signal;
      signal.addListener(this);

      // List of values to choose from
      ArrayList<String> values = new ArrayList<String>();

//      if ((mappingInfoSet.size()>1) || (mappingInfoSet.first().getMux() != MuxSelection.fixed)) {
//         // Add disabled option */
//         values.add(Pin.DISABLED_PIN.getName());
//         fMappingInfos.add(MappingInfo.DISABLED_MAPPING);
//      }
      for (MappingInfo mappingInfo:mappingInfoSet) {
         MuxSelection muxSelection = mappingInfo.getMux();
         if (!mappingInfo.getPin().isAvailableInPackage()) {
            // Discard pins without package location
            continue;
         }
         if (muxSelection == MuxSelection.fixed) {
            // Uses icon so no prefix
            values.add(mappingInfo.getPin().getName());
         }
         else {
            if (muxSelection == MuxSelection.reset) {
               values.add(muxSelection.getShortName()+": ("+mappingInfo.getPin().getName()+")");
            }
            else {
               values.add(muxSelection.getShortName()+": "+mappingInfo.getPin().getName());
            }
         }
         fMappingInfos.add(mappingInfo);
//         mappingInfo.addListener(this);
      }
      fValues = values.toArray(new String[values.size()]);
      fSignal.connectListeners();
   }

   @Override
   public void setValue(String value) {
      super.setValue(value);
      MuxSelection muxValue = MuxSelection.disabled;         
      fCurrentMapping = fMappingInfos.get(fSelection);
      if (fCurrentMapping != null) {
         muxValue = fCurrentMapping.getMux();
      }
      System.err.println("===================================================================");
      System.err.println("Signal("+fName+")::setValue("+fSelection+") => "+muxValue+", "+fCurrentMapping);
      
      fSignal.setPin(fCurrentMapping);
      ((DeviceModel)getRoot()).checkConflicts();
   }
   
//   void notifyMuxChange() {
//      fCurrentMapping = fMappingInfos.get(fSelection);
//      for (MappingInfo mappingInfo:fMappingInfos) {
//         if (mappingInfo != null) {
//            mappingInfo.select(MappingInfo.Origin.signal, mappingInfo == fCurrentMapping);
//         }
//      }
//   }
   
   @Override
   public void modelElementChanged(ObservableModel model) {
      if (model instanceof Signal) {
         Signal signal = (Signal) model;
         System.err.println("SignalModel("+fName+")::modelElementChanged("+signal+") => "+fSelection+" => "+fCurrentMapping);
         fCurrentMapping = signal.getMappedPin();
         fSelection = fMappingInfos.indexOf(fCurrentMapping);
         if (fSelection<0) {
            fSelection = 0;
         }
         System.err.println("SignalModel("+fName+")::modelElementChanged("+fSelection+") => "+fSelection+" => "+fCurrentMapping);
         
      }
      
//      if (model instanceof MappingInfo) {
//         MappingInfo mappingInfo = (MappingInfo)model;
//         boolean selected = mappingInfo.isSelected();
//
////         System.err.println(String.format("Function(%s)::modelElementChanged(%s) = %s", fName, mappingInfo, selected?"selected":"unselected"));
//
//         // Get change (either disabled or correct selection)
//         int changedSelection = fMappingInfos.indexOf(mappingInfo);
//         if (selected) {
//            // A function has been mapped to a pin 
//            if (fSelection != changedSelection) {
//               // Not already mapped
////               System.err.println(String.format("Function(%s)::modelElementChanged(%s) = %s", fName, mappingInfo, selected?"selected":"unselected"));
//               fSelection = changedSelection;
//               fCurrentMapping = fMappingInfos.get(fSelection);
//               ((DeviceModel)getRoot()).checkConflicts();
//            }
//         }
//         else {
//            // A function has been unmapped
//            if (fSelection == changedSelection) {
//               // Currently mapped to this pin - unmap
////               System.err.println(String.format("Function(%s)::modelElementChanged(%s) = %s", fName, mappingInfo, selected?"selected":"unselected"));
//               fSelection = 0;
//               fCurrentMapping = fMappingInfos.get(fSelection);
//               ((DeviceModel)getRoot()).checkConflicts();
////               notifyMuxChange();
//            }
//         }
//         viewerUpdate(this,  null);
//      }
   }

   /**
    * Get Message for model
    * 
    * May return an error message from the pin mapping instead.
    */
   @Override
   Message getMessage() {
      Message rv = null;
      if (fCurrentMapping != null) {
         rv = fCurrentMapping.getMessage();
      }
      if (rv != null) {
         return rv;
      }
      return super.getMessage();
   }

   @Override
   public void modelStructureChanged(ObservableModel observableModel) {
      // Not used
   }

   @Override
   public boolean isReset() {
      return ((fCurrentMapping == null) || 
              (fCurrentMapping.getMux() == MuxSelection.reset) ||
              (fCurrentMapping.getMux() == MuxSelection.disabled));
   }

}