package net.sourceforge.usbdm.deviceEditor.model;

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.jface.viewers.TreeViewer;

import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.PinInformation;

public class PinModel extends SelectionModel implements IModelChangeListener {

   /** Default selection index */
   protected int fDefaultSelection = 0;

   protected final ArrayList<MappingInfo> fMappingInfos = new ArrayList<MappingInfo>();

   public PinModel(BaseModel parent, PinInformation pinInformation) {
      super(parent, pinInformation.getName(), pinInformation.getDescription());

      Map<MuxSelection, MappingInfo> mappingInfoMap = pinInformation.getMappedFunctions();
      MuxSelection defaultMuxValue  = pinInformation.getDefaultValue();

      // List of values to choose from
      ArrayList<String> values = new ArrayList<String>();
      for (MuxSelection muxSelection:mappingInfoMap.keySet()) {
         if (muxSelection == MuxSelection.fixed) {
            // Uses icon so no prefix
            values.add(mappingInfoMap.get(muxSelection).getFunctionList());
            fMappingInfos.add(mappingInfoMap.get(muxSelection));
            break;
         }
         MappingInfo mappingInfo = mappingInfoMap.get(muxSelection);
         mappingInfo.addListener(this);
         if (muxSelection == MuxSelection.reset) {
            values.add(muxSelection.getShortName()+": ("+mappingInfo.getFunctionList()+")");
         }
         else {
            values.add(muxSelection.getShortName()+": "+mappingInfo.getFunctionList());
         }
         if (muxSelection == defaultMuxValue) {
            fDefaultSelection = values.size()-1;
         }
         fMappingInfos.add(mappingInfoMap.get(muxSelection));
      }
      if (values.size()>1) {
         // Add default entry
         values.add(values.get(fDefaultSelection).replaceFirst(".:", "D:"));
      }
      fValues     = values.toArray(new String[values.size()]);
      fSelection  = fDefaultSelection;
      MappingInfo mapping = fMappingInfos.get(fSelection);
      mapping.select(MappingInfo.Origin.pin, true);
   }

   @Override
   public void setValue(String value) {
      super.setValue(value);
      if (fSelection == fValues.length-1) {
         // Last entry is the default
         fSelection = fDefaultSelection;
      }
      MappingInfo  mappedFunction  = fMappingInfos.get(fSelection);
      MuxSelection muxValue = MuxSelection.disabled;         
      if (fMappingInfos.get(fSelection) != null) {
         muxValue = fMappingInfos.get(fSelection).getMux();
      }
      System.err.println("===================================================================");
      System.err.println("Pin("+fName+")::setValue("+fSelection+") => "+muxValue+", "+mappedFunction);
      for (MappingInfo mappingInfo:fMappingInfos) {
         if ((mappingInfo != null) && (mappingInfo != mappedFunction)) {
            mappingInfo.select(MappingInfo.Origin.pin, false);
         }
      }
      notifyMuxChange();
   }

   void notifyMuxChange() {
      MappingInfo mappedFunction = fMappingInfos.get(fSelection);
      for (MappingInfo mappingInfo:fMappingInfos) {
         if (mappingInfo != null) {
            mappingInfo.select(MappingInfo.Origin.pin, mappingInfo == mappedFunction);
         }
      }
   }

   @Override
   public void modelElementChanged(ObservableModel model) {
      if (model instanceof MappingInfo) {
         MappingInfo mappingInfo = (MappingInfo)model;
         if (fMessage != mappingInfo.getMessage()) {
            setMessage(mappingInfo.getMessage());
            for (TreeViewer viewer:getViewers()) {
               viewer.update(this,  null);
               viewer.update(this.fParent,  null);
            }
         }
         boolean selected     = mappingInfo.isSelected();
         int changedSelection = fMappingInfos.indexOf(mappingInfo);
         if (selected) {
            // A function has been mapped to this pin - do update if needed
            if (fSelection != changedSelection) {
               System.err.println(String.format("Pin(%s)::modelElementChanged(%s) = %s", fName, mappingInfo, selected?"selected":"unselected"));
               fSelection = changedSelection;
               //               notifyMuxChange();
               for (TreeViewer viewer:getViewers()) {
                  viewer.update(this,  null);
               }
            }
         }
         else {
            if (fSelection == changedSelection) {
               // The current function has been unmapped from this pin 
               System.err.println(String.format("Pin(%s)::modelElementChanged(%s) = %s", fName, mappingInfo, selected?"selected":"unselected"));
               fSelection = 0;
               //               notifyMuxChange();
               for (TreeViewer viewer:getViewers()) {
                  viewer.update(this,  null);
               }
            }
         }
      }
   }

   @Override
   public void modelStructureChanged(ObservableModel model) {
      // Not used
   }

}
