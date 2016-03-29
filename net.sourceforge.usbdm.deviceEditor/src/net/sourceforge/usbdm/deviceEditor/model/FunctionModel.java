package net.sourceforge.usbdm.deviceEditor.model;

import java.util.ArrayList;
import java.util.TreeSet;

import org.eclipse.jface.viewers.TreeViewer;

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
      
      if (peripheralFunction.getName().startsWith("FTM2_CH0")) {
         // XXX Delete me
         System.err.println("Stop here");
      }
      
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

         System.err.println(String.format("Function(%s)::modelElementChanged(%s) = %s", fName, mappingInfo, selected?"selected":"unselected"));

         // Get change (either disabled or correct selection)
         int changedSelection = fMappingInfos.indexOf(mappingInfo);
         if (fMessage != mappingInfo.getMessage()) {
            setMessage(mappingInfo.getMessage());
            for (TreeViewer viewer:getViewers()) {
               viewer.update(this,  null);
               viewer.update(this.fParent,  null);
            }
         }
         if (selected) {
            // A function has been mapped to a pin 
            if (fSelection != changedSelection) {
               // Not already mapped
               System.err.println(String.format("Function(%s)::modelElementChanged(%s) = %s", fName, mappingInfo, selected?"selected":"unselected"));
               fSelection = changedSelection;
               fCurrentMapping = fMappingInfos.get(fSelection);
               ((DeviceModel)getRoot()).checkConflicts();
//             notifyMuxChange();
               for (TreeViewer viewer:getViewers()) {
                  viewer.update(this,  null);
                  viewer.update(this.fParent,  null);
               }
            }
         }
         else {
            // A function has been unmapped
            if (fSelection == changedSelection) {
               // Currently mapped to this pin - unmap
               System.err.println(String.format("Function(%s)::modelElementChanged(%s) = %s", fName, mappingInfo, selected?"selected":"unselected"));
               fSelection = 0;
               fCurrentMapping = fMappingInfos.get(fSelection);
               ((DeviceModel)getRoot()).checkConflicts();
//               notifyMuxChange();
               for (TreeViewer viewer:getViewers()) {
                  viewer.update(this,  null);
                  viewer.update(this.fParent,  null);
               }
            }
         }
      }
   }

   @Override
   public void modelStructureChanged(ObservableModel observableModel) {
      // Not used
   }

//   @Override
//   protected Message checkConflicts(Map<String, List<MappingInfo>> mappedNodes) {
////      Message oldMessage = fMessage;
//      setMessage("");
//      if (fCurrentMapping != null) {
//         List<MappingInfo> list = mappedNodes.get(fCurrentMapping.getPin().getName());
//         if (list == null) {
//            list = new ArrayList<MappingInfo>();
//            mappedNodes.put(fCurrentMapping.getPin().getName(), list);
//            list.add(fCurrentMapping);
//         }
//         else {
//            StringBuffer sb = null;
//            for (MappingInfo other:list) {
//               // Check for conflicts
//               if (!fCurrentMapping.getFunctionList().equals(other.getFunctionList())) {
//                  if (sb == null) {
//                     sb = new StringBuffer();
//                     sb.append("Conflict(");
//                     sb.append(getName());
//                  }
//                  sb.append(", ");
//                  sb.append(other.getFunctionList());
//               }
//            }
//            if (sb != null) {
//               // Conflicts - notify all nodes
//               sb.append(")");
//               // Multiple functions mapped to pin
//               System.err.println(sb.toString());
//               setMessage(sb.toString());
//               for (MappingInfo other:list) {
//                  other.setMessage(sb.toString());
////                  for (TreeViewer viewer:getViewers()) {
////                     viewer.update(other,  null);
////                     viewer.update(other.getParent(),  null);
////                  }
//               }
//               fCurrentMapping.setMessage(sb.toString());
//            }
//            list.add(fCurrentMapping);
//         }
//      }
////      if (oldMessage != fMessage) {
////         for (TreeViewer viewer:getViewers()) {
////            viewer.update(this,  null);
////            viewer.update(getParent(),  null);
////         }
////      }
//      return fMessage;
//   }
}
