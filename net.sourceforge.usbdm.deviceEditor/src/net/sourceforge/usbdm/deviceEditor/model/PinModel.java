package net.sourceforge.usbdm.deviceEditor.model;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Pin.PinIntDmaValue;
import net.sourceforge.usbdm.deviceEditor.information.Pin.PinPullValue;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

public class PinModel extends SelectionModel implements IModelChangeListener {

   static final Pattern pinPattern = Pattern.compile("\\d+: (.*)");
   
   /** Associated pin */
   private final Pin fPin;

   /** Mappings corresponding to selections */
   private final MappingInfo[] fMappingInfos;

   public PinModel(BaseModel parent, Pin pin) {
      super(parent, pin.getNameWithLocation(), pin.getPinUseDescription());

      fPin = pin;
      fPin.addListener(this);

      MuxSelection currentMuxValue  = fPin.getMuxValue();

      fSelection        = 0;

      Map<MuxSelection, MappingInfo> mappingInfoMap = fPin.getMappableSignals();

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
      
      values.add(MuxSelection.unassigned.getShortName()+": Unassigned");
      mappingInfos.add(MappingInfo.UNASSIGNED_MAPPING);
      
      for (MuxSelection muxSelection:mappingInfoMap.keySet()) {
         MappingInfo mappingInfo = mappingInfoMap.get(muxSelection);

         mappingInfos.add(mappingInfo);
         if (muxSelection == MuxSelection.unassigned) {
            values.add(muxSelection.getShortName()+": ("+mappingInfo.getSignalList()+")");
         }
         else {
            values.add(muxSelection.getShortName()+": "+mappingInfo.getSignalList());
         }
         if (muxSelection == currentMuxValue) {
            fSelection = values.size()-1;
         }
      }
      fChoices      = values.toArray(new String[values.size()]);
      fMappingInfos = mappingInfos.toArray(new MappingInfo[mappingInfos.size()]);
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
      if (fPin.checkMappingConflicted() != null) {
         return "Conflict"; 
      }
      return fChoices[findValueIndex(fPin.getMappedSignals())];
   }

   @Override
   public void setValueAsString(String value) {
      super.setValueAsString(value);
      fPin.setMappedSignal(fMappingInfos[fSelection]);
      checkConflicts();
   }

   @Override
   public void modelElementChanged(ObservableModel model) {
      
      // Update status
      Status status = fPin.checkMappingConflicted();
      if (status == null) {
         for (Signal signal : fPin.getMappedSignals().getSignals()) {
            status = signal.checkMappingConflicted();
            if (status != null) {
               break;
            }
         }
      }
      setStatus(status);
      
      if (model instanceof Pin) {
         // XXX Delete me!
         Pin pin = (Pin)model;
         System.err.println("PinModel("+fPin.getName()+").modelElementChanged(Pin("+pin.getName()+"))");
         update();
         checkConflicts();
      }
   }

   @Override
   public String getSimpleDescription() {
//      if (fPin.isMappingConflicted()) {
//         return "Conflict";
//      }
//      for (Signal signal : fPin.getMappedSignals().getSignals()) {
//         if (signal.isMappingConflicted()) {
//            return "Conflict";
//         }
//      }
      return getPinUseDescription();
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

   /**
    * Get identifier to use in code generation
    * 
    * @return
    */
   public String getCodeIdentifier() {
      return fPin.getCodeIdentifier();
   }

   /**
    * Set identifier to use in code generation
    * 
    * @return
    */
   public void setCodeIdentifier(String pinUseDescription) {
      fPin.setCodeIdentifier(pinUseDescription);
   }

   @Override
   public boolean isUnassigned() {
      return (fPin.getMuxValue() == MuxSelection.unassigned);
   }

   /**
    * Get Message for model
    * 
    * May return an error message from the pin mapping instead.
    */
   @Override
   Status getStatus() {
      Status rv = fPin.getMappedSignals().getMessage();
      if (rv != null) {
         return rv;
      }
      return super.getStatus();
   }

   @Override
   public String getToolTip() {
      String tip = super.getToolTip();
      if (tip==null) {
         StringBuilder sb = new StringBuilder();
         boolean isFirst = true;
         for (String pin:getChoices()) {
            if (pin.startsWith("U")) {
               continue;
            }
            Matcher m = pinPattern.matcher(pin);
            if (!m.matches()) {
               continue;
            }
            if (!isFirst) {
               sb.append("/");
            }
            isFirst = false;
            sb.append(m.group(1));
         }
         if (sb.length()>0) {
            tip = sb.toString();
         }
      }
      return tip;
   }

   @Override
   protected void removeMyListeners() {
      fPin.removeListener(this);
      fPin.disconnectListeners();
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
            sb.append(mappingInfo.getSignalList() + " ");
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
   
   public long getProperty(long mask, long offset) {
      return fPin.getProperty(mask, offset);
   }
   
   public boolean setProperty(long mask, long offset, long property) {
      return fPin.setProperty(mask, offset, property);
   }

   public PinPullValue getPullSetting() {
      return fPin.getPullSetting();
   }

   public void setPullSetting(PinPullValue pinPullValue) {
      fPin.setPullSetting(pinPullValue);
   }

   public PinIntDmaValue getInterruptDmaSetting() {
      return fPin.getInterruptDmaSetting();
   }

   public void setInterruptDmaSetting(PinIntDmaValue pinIntDmaValue) {
      fPin.setInterruptDmaSetting(pinIntDmaValue);
   }
}
