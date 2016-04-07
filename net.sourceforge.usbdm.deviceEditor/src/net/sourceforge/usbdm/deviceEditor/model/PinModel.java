package net.sourceforge.usbdm.deviceEditor.model;

import java.util.ArrayList;
import java.util.Map;

import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.Pin;

public class PinModel extends SelectionModel implements IModelChangeListener {

   /** Associated pin */
   protected final Pin fPin;
   
   /** Default selection index */
   protected int fDefaultSelection = 0;

   /** Mappings corresponding to selections */
   protected final ArrayList<MuxSelection> fMappingInfos = new ArrayList<MuxSelection>();

   public PinModel(BaseModel parent, Pin pin) {
      super(parent, pin.getNameWithLocation(), pin.getDescription());

      fPin = pin;
      
      pin.addListener(this);

      Map<MuxSelection, MappingInfo> mappingInfoMap = pin.getMappedSignals();
      
      MuxSelection defaultMuxValue  = pin.getDefaultValue();
      MuxSelection currentMuxValue  = pin.getMuxValue();

      fDefaultSelection = 0;
      fSelection        = 0;
      
      // List of values to choose from
      ArrayList<String> values = new ArrayList<String>();
      for (MuxSelection muxSelection:mappingInfoMap.keySet()) {
         if (muxSelection == MuxSelection.fixed) {
            // Uses icon so no prefix
            values.add(mappingInfoMap.get(muxSelection).getSignalList());
            fMappingInfos.add(muxSelection);
            break;
         }
         MappingInfo mappingInfo = mappingInfoMap.get(muxSelection);
//         mappingInfo.addListener(this);
         if (muxSelection == MuxSelection.reset) {
            values.add(muxSelection.getShortName()+": ("+mappingInfo.getSignalList()+")");
         }
         else {
            values.add(muxSelection.getShortName()+": "+mappingInfo.getSignalList());
         }
         if (muxSelection == defaultMuxValue) {
            fDefaultSelection = values.size()-1;
         }
         if (muxSelection == currentMuxValue) {
            fSelection = values.size()-1;
         }
         fMappingInfos.add(muxSelection);
      }
      if (values.size()>1) {
         // Add default entry
         values.add(values.get(fDefaultSelection).replaceFirst(".:", "D:"));
      }
      
      fValues     = values.toArray(new String[values.size()]);
//      MuxSelection mapping = fMappingInfos.get(fSelection);

      pin.connectListeners();
      pin.setMuxSelection(currentMuxValue);
//      pin.getMappedSignals().get(mapping).select(MappingInfo.Origin.pin, true);
   }

   @Override
   public void setValue(String value) {
      
      super.setValue(value);
      if (fSelection == fValues.length-1) {
         // Last entry is the default
         fSelection = fDefaultSelection;
      }
      MuxSelection  currentMuxValue = fMappingInfos.get(fSelection);
      if (currentMuxValue == null) {
         currentMuxValue = MuxSelection.disabled;
      }
      System.err.println("===================================================================");
      System.err.println("PinModel("+fName+")::setValue("+fSelection+") => "+currentMuxValue+", ");

      fPin.setMuxSelection(currentMuxValue);
      ((DeviceModel)getRoot()).checkConflicts();

//      notifyMuxChange();
   }

//   void notifyMuxChange() {
//      MuxSelection  currentMuxValue = fMappingInfos.get(fSelection);
//      
//      MappingInfo mappingInfo;
//      
//      // De-select inactive mapping
//      for (MuxSelection muxValue:fMappingInfos) {
//         mappingInfo = fPin.getMappedSignals().get(muxValue);
//         if ((mappingInfo != null) && (muxValue != currentMuxValue)) {
//            mappingInfo.select(MappingInfo.Origin.pin, false);
//         }
//      }
//      // Select active mapping
//      mappingInfo = fPin.getMappedSignals().get(currentMuxValue);
//      if (mappingInfo != null) {
//         mappingInfo.select(MappingInfo.Origin.pin, true);
//      }
//   }

   @Override
   public void modelElementChanged(ObservableModel model) {
//      if (model instanceof MappingInfo) {
//         mappingChanged((MappingInfo) model);
//      }      
//      else 
      if (model instanceof Pin) {
         Pin pin = (Pin) model;
         MuxSelection muxValue = pin.getMuxValue();
         fSelection = fMappingInfos.indexOf(muxValue);
         if (fSelection<0) {
            fSelection = fDefaultSelection;
         }
//         // X XX Delete me
//         System.err.println("PinModel.modelElementChanged("+pin.getName()+") => " + fSelection);
      }
   }

//   private void mappingChanged(MappingInfo mappingInfo) {
//      boolean selected     = mappingInfo.isSelected();
//      int changedSelection = fMappingInfos.indexOf(mappingInfo.getMux());
//      if (selected) {
//         // A function has been mapped to this pin - do update if needed
//         if (fSelection != changedSelection) {
////               System.err.println(String.format("Pin(%s)::modelElementChanged(%s) = %s", fName, mappingInfo, selected?"selected":"unselected"));
//            fSelection = changedSelection;
//            MuxSelection  muxValue = fMappingInfos.get(fSelection);
//            if (muxValue == null) {
//               muxValue = MuxSelection.disabled;
//            }
//            fPin.setMuxSelection(muxValue);
//            //               notifyMuxChange();
//            viewerUpdate(this,  null);
//         }
//      }
//      else {
//         if (fSelection == changedSelection) {
//            // The current function has been unmapped from this pin 
////               System.err.println(String.format("Pin(%s)::modelElementChanged(%s) = %s", fName, mappingInfo, selected?"selected":"unselected"));
//            fSelection = 0;
//            MuxSelection  muxValue = fMappingInfos.get(fSelection);
//            if (muxValue == null) {
//               muxValue = MuxSelection.disabled;
//            }
//            fPin.setMuxSelection(muxValue);
//            //               notifyMuxChange();
//            viewerUpdate(this,  null);
//         }
//      }
//   }

   /**
    * Get user description of pin use
    * 
    * @return
    */
   public String getPinUseDescription() {
      return fPin.getPinUseDescription();
   }
   
   /**
    * Set user description of pin use
    * 
    * @return
    */
   public void setPinUseDescription(String pinUseDescription) {
      fPin.setPinUseDescription(pinUseDescription);
      // Update watchers of active mapping
      MappingInfo mappingInfo = fPin.getMappedSignals().get(fMappingInfos.get(fSelection));
      mappingInfo.notifyListeners();
   }
   
   @Override
   public void modelStructureChanged(ObservableModel model) {
      // Not used
   }

   @Override
   public boolean isReset() {
      return (fMappingInfos.get(fSelection) == MuxSelection.reset);
   }

//   public boolean isUnmapped() {
//      MappingInfo mappingInfo = fPin.getMappedSignals().get(fMappingInfos.get(fSelection));
//      return mappingInfo.getPin() == Pin.DISABLED_PIN;
//   }

   /**
    * Get Message for model
    * 
    * May return an error message from the pin mapping instead.
    */
   @Override
   Message getMessage() {
      Message rv = fPin.getMappedSignal().getMessage();
      if (rv != null) {
         return rv;
      }
      return super.getMessage();
   }

}
