package net.sourceforge.usbdm.deviceEditor.model;

import java.util.ArrayList;
import java.util.Map;

import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.Pin;

public class PinModel extends SelectionModel implements IModelChangeListener {

   /** Associated pin */
   private final Pin fPin;

   /** Default selection index */
   private int fDefaultSelection = 0;

   /** Mappings corresponding to selections */
   private final MappingInfo[] fMappingInfos;

   public PinModel(BaseModel parent, Pin pin) {
      super(parent, pin.getNameWithLocation(), pin.getDescription());

      fPin = pin;
      fPin.addListener(this);

      MuxSelection defaultMuxValue  = fPin.getDefaultValue();
      MuxSelection currentMuxValue  = fPin.getMuxValue();

      fDefaultSelection = 0;
      fSelection        = 0;

      Map<MuxSelection, MappingInfo> mappingInfoMap = fPin.getMappedSignals();

      MappingInfo fixedMapping = mappingInfoMap.get(MuxSelection.fixed);
      if (fixedMapping != null) {
         // Fixed mapping for pin
         fChoices      = new String[] {fixedMapping.getSignalList()};
         fMappingInfos = null;
         return;
      }
      
      // List of values to choose from
      ArrayList<String>      values       = new ArrayList<String>();
      ArrayList<MappingInfo> mappingInfos = new ArrayList<MappingInfo>();
      for (MuxSelection muxSelection:mappingInfoMap.keySet()) {
         MappingInfo mappingInfo = mappingInfoMap.get(muxSelection);

         mappingInfos.add(mappingInfo);
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
      }
      if (values.size()>1) {
         // Add default entry
         values.add(values.get(fDefaultSelection).replaceFirst(".:", "D:"));
      }

      fChoices      = values.toArray(new String[values.size()]);
      fMappingInfos = mappingInfos.toArray(new MappingInfo[mappingInfos.size()]);
      
      fPin.connectListeners();
      fPin.setMuxSelection(currentMuxValue);
   }

   /**
    * Find selection index corresponding to mapping info from signal
    * 
    * @param value
    * @return
    */
   int findValueIndex(MappingInfo value) {
      if (fMappingInfos == null) {
         // Fixed mapping
         return 0;
      }
      for (int index=0; index<fMappingInfos.length; index++) {
         if (fMappingInfos[index] == value) {
            return index;
         }
      }
      return -1;
   }
   
   @Override
   public String getValueAsString() {
      return fChoices[findValueIndex(fPin.getMappedSignal())];
   }

   @Override
   public void setValueAsString(String value) {
      super.setValueAsString(value);
      if (fSelection == fChoices.length-1) {
         // Last entry is the default
         fSelection = fDefaultSelection;
      }
      fPin.setMappedSignal(fMappingInfos[fSelection]);
      ((DeviceModel)getRoot()).checkConflicts();
   }

   @Override
   public void modelElementChanged(ObservableModel model) {
      if (model instanceof Pin) {
         viewerUpdate(this, null);
         ((DeviceModel)getRoot()).checkConflicts();
      }
   }

   @Override
   public String getDescription() {
      setDescription(getPinUseDescription());
      return super.getDescription();
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
   }

   @Override
   public void modelStructureChanged(ObservableModel model) {
      // Not used
   }

   @Override
   public boolean isReset() {
      return (fPin.getMuxValue() == MuxSelection.reset);
   }

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

   @Override
   protected void removeMyListeners() {
      fPin.removeListener(this);
      fPin.disconnectListeners();
   }

   @Override
   public String toString() {
      return "PinModel("+fName+") => "+getValueAsString();
   }
}
