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
      fPin.addListener(this);

      Map<MuxSelection, MappingInfo> mappingInfoMap = fPin.getMappedSignals();

      MuxSelection defaultMuxValue  = fPin.getDefaultValue();
      MuxSelection currentMuxValue  = fPin.getMuxValue();

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

      fChoices = values.toArray(new String[values.size()]);

      fPin.connectListeners();
      fPin.setMuxSelection(currentMuxValue);
   }

   @Override
   public void setValueAsString(String value) {

      super.setValueAsString(value);
      if (fSelection == fChoices.length-1) {
         // Last entry is the default
         fSelection = fDefaultSelection;
      }
      MuxSelection  currentMuxValue = fMappingInfos.get(fSelection);
      if (currentMuxValue == null) {
         currentMuxValue = MuxSelection.disabled;
      }
//      System.err.println("===================================================================");
//      System.err.println("PinModel("+fName+")::setValue("+fSelection+") => "+currentMuxValue+", ");

      fPin.setMuxSelection(currentMuxValue);
      ((DeviceModel)getRoot()).checkConflicts();
   }

   @Override
   public void modelElementChanged(ObservableModel model) {
      if (model instanceof Pin) {
         Pin pin = (Pin) model;
         MuxSelection muxValue = pin.getMuxValue();
         fSelection = fMappingInfos.indexOf(muxValue);
         if (fSelection<0) {
            throw new RuntimeException("Impossible pin");
         }
//         System.err.println("PinModel("+fName+")::modelElementChanged(M:"+muxValue+", S:"+fSelection+")");
         viewerUpdate(this, null);
         ((DeviceModel)getRoot()).checkConflicts();
      }
   }

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

   /**
    * Get Message for model
    * 
    * May return an error message from the pin mapping instead.
    */
   @Override
   Message getMessage() {
//      String msg = fPin.isValid();
//      if (msg != null) {
//         return new Message(msg, this);
//      }
      Message rv = fPin.getMappedSignal().getMessage();
      if (rv != null) {
         return rv;
      }
      return super.getMessage();
   }

   @Override
   protected void removeMyListeners() {
      fPin.removeListener(this);
      fPin.disconnectListeners();
   }

   @Override
   public String toString() {
      return "PinModel("+fName+")";
   }

}
