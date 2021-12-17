package net.sourceforge.usbdm.deviceEditor.model;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

public class PinModel extends SelectionModel implements IModelChangeListener {

   static final Pattern pinPattern = Pattern.compile("\\d+: (.*)");
   
   /** Associated pin */
   private final Pin fPin;

   /** Mappings corresponding to selections */
   private final MappingInfo[] fMappingInfos;

   public PinModel(BaseModel parent, Pin pin) {
      super(parent, pin.getNameWithLocation());

      fPin = pin;
      fPin.addListener(this);
 
      Map<MuxSelection, MappingInfo> mappingInfoMap = fPin.getMappableSignals();

      MappingInfo fixedMapping = mappingInfoMap.get(MuxSelection.fixed);
      if (fixedMapping != null) {
         // Fixed mapping for pin
         fChoices      = new String[] {fixedMapping.getSignalNames()};
         fMappingInfos = null;
         return;
      }
      
      // List of values to choose from
      ArrayList<String>      values       = new ArrayList<String>();
      ArrayList<MappingInfo> mappingInfos = new ArrayList<MappingInfo>();
      
      values.add(MuxSelection.unassigned.getShortName()+": Unassigned");
      mappingInfos.add(MappingInfo.UNASSIGNED_MAPPING);
      
      fSelection        = 0;
      for (MuxSelection muxSelection:mappingInfoMap.keySet()) {
         MappingInfo mappingInfo = mappingInfoMap.get(muxSelection);
         mappingInfos.add(mappingInfo);
         values.add(muxSelection.getShortName()+": "+mappingInfo.getSignalNames());
         if (mappingInfo.isSelected()) {
            fSelection = values.size()-1;
         }
      }
      fChoices      = values.toArray(new String[values.size()]);
      fMappingInfos = mappingInfos.toArray(new MappingInfo[mappingInfos.size()]);
   }

   /**
    * Get String describing the signals available for mapping to this pin
    * 
    * @return
    */
   public String getAvailableSignals() {
      Map<MuxSelection, MappingInfo> mappingInfoMap = fPin.getMappableSignals();

      StringBuilder sb = new StringBuilder("[");
      boolean isFirst = true;
      for (MuxSelection muxSelection:mappingInfoMap.keySet()) {
         MappingInfo mappingInfo = mappingInfoMap.get(muxSelection);
         if (!isFirst) {
            sb.append("/");
         }
         isFirst = false;
         ArrayList<Signal> signals = mappingInfo.getSignals();
         String signalList         = mappingInfo.getSignalNames();
         if (signals.get(0).getMappedPin() == Pin.UNASSIGNED_PIN) {
            signalList = signalList.replaceAll("\\/", "*/") + "*";
         }
         sb.append(signalList);
      }
      sb.append("]");
      
      return sb.toString();
   }
   
   @Override
   public String getValueAsString() {
      if (fMappingInfos == null) {
         // Fixed mapping
         return fChoices[0];
      }
      ArrayList<MappingInfo> mappedSignals = fPin.getActiveMappings();
      if (mappedSignals.size() == 0) {
         // No signals mapped
         return fChoices[0];
      }
      if (mappedSignals.size() > 1) {
         // Multiple signals mapped
         return "Multiple";
      }
      // Find mapped signal in table
      for (int index=0; index<fMappingInfos.length; index++) {
         if (fMappingInfos[index] == mappedSignals.get(0)) {
            return fChoices[index];
         }
      }
      return "Broken";
   }

   @Override
   public void setValueAsString(String value) {
      super.setValueAsString(value);
      fPin.setMuxSelection(fMappingInfos[fSelection].getMux());
      checkConflicts();
   }

   @Override
   public void modelElementChanged(ObservableModel model) {
      
      // Update status
      setStatus(fPin.getStatus());
      update();
   }

   @Override
   public String getSimpleDescription() {
      return fPin.getMappedSignalsUserDescriptions();
   }

   /**
    * Get associated pin
    * 
    * @return Associated pin
    */
   public Pin getPin() {
      return fPin;
   }
   
   @Override
   public boolean isInactive() {
      return fPin.getActiveMappings().isEmpty();
   }

   /**
    * Get Message for model
    * 
    * May return an error message from the pin mapping instead.
    */
   @Override
   Status getStatus() {
      Status rv = fPin.getStatus();
      if (rv != null) {
         return rv;
      }
      return super.getStatus();
   }

   @Override
   public String getToolTip() {
      String tip = super.getToolTip();
      if (tip==null) {
         tip = getAvailableSignals();
      }
      return tip;
   }

   @Override
   protected void removeMyListeners() {
      fPin.removeListener(this);
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("PinModel("+fName+") => ");
      if (fPin.checkMappingConflicted() != null) {
         sb.append("** ");
      }
      for (MuxSelection muxSelection:fPin.getMappableSignals().keySet()) {
         MappingInfo mappingInfo = fPin.getMappableSignals().get(muxSelection);
         if (mappingInfo.isSelected()) {
            sb.append(mappingInfo.getSignalNames() + " ");
         }
      }
      return sb.toString();
   }

   @Override
   public void elementStatusChanged(ObservableModel observableModel) {
      updateAncestors();
   }

   @Override
   public void modelStructureChanged(ObservableModel model) {
      // Not used
   }
}
