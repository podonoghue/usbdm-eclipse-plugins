package net.sourceforge.usbdm.deviceEditor.model;

import java.util.ArrayList;
import java.util.TreeSet;

import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.PeripheralFunction;
import net.sourceforge.usbdm.deviceEditor.information.PinInformation;

public class FunctionModel extends SelectionModel implements IModelChangeListener {

   /** List of possible mappings */
   protected final ArrayList<MappingInfo> fMappingInfos = new ArrayList<MappingInfo>();
   
   /** Current mapping (may be null) */
   private MappingInfo fCurrentMapping;
   
   public FunctionModel(PeripheralModel parent, PeripheralFunction peripheralFunction) {
      super(parent, peripheralFunction.getName(), "");
      
      TreeSet<MappingInfo> mappingInfoSet = peripheralFunction.getPinMapping();
      MappingInfo firstMapping = peripheralFunction.getPinMapping().first();
      if (firstMapping.getMux() == MuxSelection.fixed) {
         // Fixed mapping for function
         fValues = new String[] {peripheralFunction.getPinMapping().first().getPin().getName()};
         return;
      }
      
      // List of values to choose from
      ArrayList<String> values = new ArrayList<String>();
      
      if ((mappingInfoSet.size()>1) || (mappingInfoSet.first().getMux() != MuxSelection.fixed)) {
         values.add(PinInformation.DISABLED_PIN.getName());
         fMappingInfos.add(null);
      }
      for (MappingInfo mappingInfo:mappingInfoSet) {
         MuxSelection muxSelection = mappingInfo.getMux();
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
         mappingInfo.addListener(this);
      }
      fValues = values.toArray(new String[values.size()]);
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
      System.err.println("Function("+fName+")::setValue("+fSelection+") => "+muxValue+", "+fCurrentMapping);
      notifyMuxChange();
   }
   
   void notifyMuxChange() {
      fCurrentMapping = fMappingInfos.get(fSelection);
      for (MappingInfo mappingInfo:fMappingInfos) {
         if (mappingInfo != null) {
            mappingInfo.select(MappingInfo.Origin.function, mappingInfo == fCurrentMapping);
         }
      }
      ((DeviceModel)getRoot()).checkConflicts();
   }
   
   @Override
   public void modelElementChanged(ObservableModel model) {
      if (model instanceof MappingInfo) {
         MappingInfo mappingInfo = (MappingInfo)model;
         boolean selected = mappingInfo.isSelected();

//         System.err.println(String.format("Function(%s)::modelElementChanged(%s) = %s", fName, mappingInfo, selected?"selected":"unselected"));

         // Get change (either disabled or correct selection)
         int changedSelection = fMappingInfos.indexOf(mappingInfo);
         if (fMessage != mappingInfo.getMessage()) {
            setMessage(mappingInfo.getMessage());
         }
         if (selected) {
            // A function has been mapped to a pin 
            if (fSelection != changedSelection) {
               // Not already mapped
//               System.err.println(String.format("Function(%s)::modelElementChanged(%s) = %s", fName, mappingInfo, selected?"selected":"unselected"));
               fSelection = changedSelection;
               fCurrentMapping = fMappingInfos.get(fSelection);
               ((DeviceModel)getRoot()).checkConflicts();
            }
         }
         else {
            // A function has been unmapped
            if (fSelection == changedSelection) {
               // Currently mapped to this pin - unmap
//               System.err.println(String.format("Function(%s)::modelElementChanged(%s) = %s", fName, mappingInfo, selected?"selected":"unselected"));
               fSelection = 0;
               fCurrentMapping = fMappingInfos.get(fSelection);
               ((DeviceModel)getRoot()).checkConflicts();
//               notifyMuxChange();
            }
         }
         viewerUpdate(this,  null);
      }
   }

   @Override
   public String getDescription() {
      if (fCurrentMapping != null) {
         return fCurrentMapping.getPin().getPinUseDescription();
      }
      return "";
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