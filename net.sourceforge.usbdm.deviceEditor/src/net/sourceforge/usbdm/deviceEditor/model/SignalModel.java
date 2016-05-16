package net.sourceforge.usbdm.deviceEditor.model;

import java.util.ArrayList;
import java.util.TreeSet;

import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

public class SignalModel extends SelectionModel implements IModelChangeListener {

   /** Associated signal */
   private final Signal fSignal;
   
   /** List of mapping info choices - Used to map selections to mappingInfo value for signal */
   private final MappingInfo[] fMappingInfos;
   
   public SignalModel(PeripheralModel parent, Signal signal) {
      super(parent, signal.getName(), "");

      fSignal = signal;
      setDescription(fSignal.getMappedPin().getPin().getPinUseDescription());

      TreeSet<MappingInfo> mappingInfoSet = signal.getPinMapping();
      MappingInfo firstMapping = mappingInfoSet.first();
      if (firstMapping.getMux() == MuxSelection.fixed) {
         // Fixed mapping for function
         fChoices      = new String[] {signal.getPinMapping().first().getPin().getName()};
         fMappingInfos = null;
         return;
      }

      // Create list of values to choose from and corresponding mappingInfos
      ArrayList<String> values            = new ArrayList<String>();
      ArrayList<MappingInfo> mappingInfos = new ArrayList<MappingInfo>();
      for (MappingInfo mappingInfo:mappingInfoSet) {
         MuxSelection muxSelection = mappingInfo.getMux();
         if (!mappingInfo.getPin().isAvailableInPackage()) {
            // Discard pins without package location
            continue;
         }
         if (muxSelection == MuxSelection.reset) {
            values.add(muxSelection.getShortName()+": ("+mappingInfo.getPin().getNameWithLocation()+")");
         }
         else {
            values.add(muxSelection.getShortName()+": "+mappingInfo.getPin().getNameWithLocation());
         }
         mappingInfos.add(mappingInfo);
      }
      fChoices = values.toArray(new String[values.size()]);
      fMappingInfos = mappingInfos.toArray(new MappingInfo[mappingInfos.size()]);
      
      fSignal.addListener(this);

      fSignal.connectListeners();
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
      return fChoices[findValueIndex(fSignal.getMappedPin())];
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
   public void modelElementChanged(ObservableModel model) {
      if (model instanceof Signal) {
         viewerUpdate(this, null);
      }
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
   Message getMessage() {
      Message rv = null;
      MappingInfo currentMapping = fSignal.getMappedPin();
      if (currentMapping != null) {
         rv = currentMapping.getMessage();
      }
      if (rv != null) {
         return rv;
      }
      return super.getMessage();
   }

   @Override
   public String getDescription() {
      MappingInfo currentMapping = fSignal.getMappedPin();
      if (currentMapping != null) {
         setDescription(currentMapping.getPin().getPinUseDescription());
      }
      return super.getDescription();
   }

   @Override
   public void modelStructureChanged(ObservableModel observableModel) {
      // Not used
   }

   @Override
   public boolean isReset() {
      MappingInfo currentMapping = fSignal.getMappedPin();
      return ((currentMapping == null) || 
              (currentMapping.getMux() == MuxSelection.reset) ||
              (currentMapping.getMux() == MuxSelection.disabled));
   }

   @Override
   protected void removeMyListeners() {
      fSignal.removeListener(this);
      fSignal.disconnectListeners();
   }

}