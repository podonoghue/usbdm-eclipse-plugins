package net.sourceforge.usbdm.deviceEditor.model;

import java.util.ArrayList;
import java.util.TreeSet;

import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

public class SignalModel extends SelectionModel implements IModelChangeListener {

   /** List of possible mappings for this signal */
   protected final ArrayList<MappingInfo> fMappingInfos = new ArrayList<MappingInfo>();
   
   /** Associated signal */
   private Signal fSignal = null;
   
   /** Current mapping */
   private MappingInfo fCurrentMapping;
   
   /** Mapping used for reset */
   private MappingInfo fResetMapping = null;
   
   public SignalModel(PeripheralModel parent, Signal signal) {
      super(parent, signal.getName(), "");

      fSignal = signal;

      TreeSet<MappingInfo> mappingInfoSet = signal.getPinMapping();
      MappingInfo firstMapping = signal.getPinMapping().first();
      if (firstMapping.getMux() == MuxSelection.fixed) {
         // Fixed mapping for function
         fChoices = new String[] {signal.getPinMapping().first().getPin().getName()};
         return;
      }

      fCurrentMapping = null;
      
      // List of values to choose from
      ArrayList<String> values = new ArrayList<String>();
      for (MappingInfo mappingInfo:mappingInfoSet) {
         MuxSelection muxSelection = mappingInfo.getMux();
         if (!mappingInfo.getPin().isAvailableInPackage()) {
            // Discard pins without package location
            continue;
         }
         if (muxSelection == MuxSelection.reset) {
            values.add(muxSelection.getShortName()+": ("+mappingInfo.getPin().getNameWithLocation()+")");
            fResetMapping = mappingInfo;
         }
         else {
            values.add(muxSelection.getShortName()+": "+mappingInfo.getPin().getNameWithLocation());
         }
         fMappingInfos.add(mappingInfo);
         if (mappingInfo.isSelected()) {
            fCurrentMapping = mappingInfo;
            fSelection = fMappingInfos.size()-1;
         }
      }
      if (fCurrentMapping == null) {
         fCurrentMapping = fMappingInfos.get(0);
      }
      if (fResetMapping == null) {
         fResetMapping = fMappingInfos.get(1);
      }
      fChoices = values.toArray(new String[values.size()]);

      setDescription(fCurrentMapping.getPin().getPinUseDescription());

      fSignal.addListener(this);

      fSignal.connectListeners();
   }

   @Override
   public void setValueAsString(String value) {
      super.setValueAsString(value);
      MuxSelection muxValue = MuxSelection.reset;         
      fCurrentMapping = fMappingInfos.get(fSelection);
      if (fCurrentMapping == null) {
         throw new RuntimeException("Illegal mapping");
      }
      muxValue = fCurrentMapping.getMux();
      if (muxValue == MuxSelection.disabled) {
         // Can't set a signal to disabled because it is unclear what to do to the unmapped pin?
         muxValue = MuxSelection.reset;
         fCurrentMapping = fResetMapping;
         fSelection = fMappingInfos.indexOf(fResetMapping);
      }
//      System.err.println("===================================================================");
//      System.err.println("SignalModel("+fName+")::setValue("+value+", "+fSelection+") => "+fCurrentMapping);
      fSignal.setPin(fCurrentMapping);
   }
   
   @Override
   public void modelElementChanged(ObservableModel model) {
      if (model instanceof Signal) {
         Signal signal = (Signal) model;
//         System.err.println(" "+toString()+"::modelElementChanged() => "+fSelection+" => C:"+fCurrentMapping);
         fCurrentMapping = signal.getCurrentMapping();
         fSelection = fMappingInfos.indexOf(fCurrentMapping);
         if (fSelection<0) {
            throw new RuntimeException(toString()+"::modelElementChanged("+fSelection+") - Impossible mapping");
         }
         setDescription(fCurrentMapping.getPin().getPinUseDescription());

//         System.err.println("*"+toString()+"::modelElementChanged() => "+fSelection+" => N:"+fCurrentMapping);
         viewerUpdate(this, null);
      }
   }

   @Override
   public String toString() {
      return "SignalModel("+fName+")";
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

   /* (non-Javadoc)
    * @see net.sourceforge.usbdm.deviceEditor.model.SelectionModel#canEdit()
    */
   @Override
   public boolean canEdit() {
      return super.canEdit() && ((fChoices.length>2) || (fCurrentMapping == MappingInfo.DISABLED_MAPPING));
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

   @Override
   protected void removeMyListeners() {
      fSignal.removeListener(this);
      fSignal.disconnectListeners();
   }

}