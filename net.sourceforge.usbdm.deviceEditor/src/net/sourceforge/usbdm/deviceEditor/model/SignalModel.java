package net.sourceforge.usbdm.deviceEditor.model;

import java.util.ArrayList;
import java.util.TreeSet;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

public class SignalModel extends SelectionModel implements IModelChangeListener {
   
   static final Pattern signalPattern = Pattern.compile("\\d+: (.*)");

   /** Associated signal */
   private final Signal fSignal;
   
   /** List of mapping info choices - Used to map selections to mappingInfo value for signal */
   private final MappingInfo[] fMappingInfos;
   
   public Signal getSignal() {
      return fSignal;
   }
   
   public SignalModel(BaseModel parent, Signal signal) {
      super(parent, signal.getName());
//      if (getName().contains(watchedName)) {
//         System.err.println("SignalModel(" + getName() + "):"+this.hashCode() + ", parent = " + parent + ": " + parent.hashCode() );
//         System.err.flush();
//      }

      fSignal = signal;

      TreeSet<MappingInfo> mappingInfoSet = fSignal.getPinMapping();
//      MappingInfo firstMapping = mappingInfoSet.first();
      //      if (firstMapping.getMux() == MuxSelection.fixed) {
      //         // Fixed mapping for function
      //         fChoices      = new String[] {fSignal.getPinMapping().first().getPin().getNameWithLocation()};
      //         fMappingInfos = null;
      //      }
      // Create list of values to choose from and corresponding mappingInfos
      ArrayList<String> values            = new ArrayList<String>();
      ArrayList<MappingInfo> mappingInfos = new ArrayList<MappingInfo>();
      for (MappingInfo mappingInfo:mappingInfoSet) {
         MuxSelection muxSelection = mappingInfo.getMux();
         if (!mappingInfo.getPin().isAvailableInPackage()) {
            // Discard pins without package location
            continue;
         }
         if (muxSelection == MuxSelection.unassigned) {
            values.add(muxSelection.getShortName()+": ("+mappingInfo.getPin().getNameWithLocation()+")");
         }
         else {
            values.add(muxSelection.getShortName()+": "+mappingInfo.getPin().getNameWithLocation());
         }
         mappingInfos.add(mappingInfo);
      }
      fChoices = values.toArray(new String[values.size()]);
      fSelection = fSignal.getFirstMappedPinInformation().getMux().getMuxValue();
      if (fSelection<0) {
         fSelection = 0;
      }
      fMappingInfos = mappingInfos.toArray(new MappingInfo[mappingInfos.size()]);

      fSignal.addListener(this);
   }
   
   /**
    * Gets pins that this signal may be mapped to.
    * 
    * @return String listing mappable pins
    */
   public String getAvailablePins() {
      TreeSet<MappingInfo> mappingInfoSet = fSignal.getPinMapping();
      
      // Create list of values to choose from and corresponding mappingInfos
      StringBuilder sb = new StringBuilder("[");
      boolean isFirst = true;
      for (MappingInfo mappingInfo:mappingInfoSet) {
//         System.err.print("SignalModel.getAvailableSignals mappingInfo = (" + mappingInfo.hashCode() + ")" + mappingInfo + "\n");
         Pin pin = mappingInfo.getPin();
         if (pin == Pin.UNASSIGNED_PIN) {
            continue;
         }
         if (!pin.isAvailableInPackage()) {
            // Discard pins without package location
            continue;
         }
         if (!isFirst) {
            sb.append("/");
         }
         isFirst = false;
         sb.append(pin.getName());
         if (pin.getActiveMappings().isEmpty()) {
            // Pin is not currently mapped
            sb.append("*");
         }
      }
      sb.append("]");
      return sb.toString();
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
      if (fSignal.checkMappingConflicted() != null) {
         return "Multiple";
      }
//      int index = fSignal.getFirstMappedPinInformation().getMux().getMuxValue();
//      if (index<0) {
//         return "Unmapped";
//      }
      int index = findValueIndex(fSignal.getFirstMappedPinInformation());
      if (index<0) {
         index = 0;
      }
      return fChoices[index];
   }

   @Override
   public void setValueAsString(String value) {
//      System.err.println("===================================================================");
//      System.err.println("SignalModel("+fName+")::setValue("+value+")");
      super.setValueAsString(value);
      fSignal.setMappedPin(fMappingInfos[fSelection]);
//      System.err.println("===================================================================");
   }
   
   @Override
   public String toString() {
      return "SignalModel("+fName+") => "+getValueAsString();
   }

   /**
    * Get Message for model
    * 
    * May return an error message from the pin mapping instead.
    */
   @Override
   Status getStatus() {
      Status rv = fSignal.getStatus();
      if (rv != null) {
         return rv;
      }
      return super.getStatus();
   }

   @Override
   public String getToolTip() {
      String tip = super.getToolTip();
      if (tip==null) {
         MappingInfo currentMapping = fSignal.getFirstMappedPinInformation();
         if ((currentMapping != null) && (currentMapping != MappingInfo.UNASSIGNED_MAPPING)) {
            tip = "Available mappings: " + getAvailablePins();
         }
         else {
            tip = "Select pin mapping: " + getAvailablePins();
         }
      }
      return tip;
   }

   @Override
   public String getSimpleDescription() {
      String description = fSignal.getUserDescription();
      if (description.isBlank()) {
         description = getAvailablePins();
      }
      return description;
      
//      List<MappingInfo> currentMapping = fSignal.getMappedPinInformation();
//
//      // Try to get description from currently mapped pin
//      if (!currentMapping.isEmpty()) {
//         description = currentMapping.get(0).getPin().getUserDescription();
//      }
//      else {
//         String pinListDescription = getAvailablePins();
//         if (pinListDescription != null) {
//            description = pinListDescription;
//         }
//      }
//      return description;
   }

   @Override
   public boolean isInactive() {
      MappingInfo currentMapping = fSignal.getFirstMappedPinInformation();
      return ((currentMapping == null) ||
              (currentMapping.getMux() == MuxSelection.unassigned));
   }

   @Override
   protected void removeMyListeners() {
      fSignal.removeListener(this);
      fSignal.disconnectListeners();
   }

   /**
    * Enable this signal
    * 
    * @param enable
    */
   public void enable(boolean enable) {
      fSignal.enable(enable);
   }
   
   @Override
   public boolean isEnabled() {
      return fParent.isEnabled() && fSignal.isEnabled();
   }

   @Override
   public boolean canEdit() {
      boolean canedit = super.canEdit();
      if (fParent instanceof PeripheralSignalsModel) {
         canedit = canedit && !((PeripheralSignalsModel)fParent).areChildrenLocked();
      }
      return canedit;
   }

   @Override
   public int getSelection() {
      MappingInfo mapping = fSignal.getFirstMappedPinInformation();
      fSelection = mapping.getMux().ordinal() - MuxSelection.unassigned.ordinal();
      if (fSelection<0) {
         fSelection = 0;
      }
      if (fSelection>=fChoices.length) {
         fSelection = 0;
      }
      return fSelection;
   }

   @Override
   public void modelElementChanged(ObservableModelInterface model, int properties) {
      
      if ((properties & PROPERTY_VALUE) != 0) {
         // Update status
         Status status = fSignal.checkMappingConflicted();
         if (status == null) {
            for (MappingInfo mappingInfo: fSignal.getMappedPinInformation()) {
               status = mappingInfo.getPin().checkMappingConflicted();
               if (status != null) {
                  break;
               }
            }
         }
         setStatus(status);
         
         if (model instanceof Signal) {
            update(null);
         }
         if (model instanceof Pin) {
            update(null);
         }
      }
      if ((properties & PROPERTY_STATUS) != 0) {
         updateAncestors();
      }
   }

}