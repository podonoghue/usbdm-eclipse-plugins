package net.sourceforge.usbdm.deviceEditor.editor;

import java.util.ArrayList;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;

import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.model.PinModel;
import net.sourceforge.usbdm.deviceEditor.model.SignalModel;

public abstract class PinPropertyEditingSupport extends EditingSupport {

   final long fOffset;
   final long fMask;

   public PinPropertyEditingSupport(ColumnViewer viewer, long mask, long offset) {
      super(viewer);
      fOffset = offset;
      fMask   = mask;
   }

   public static MappingInfo getMappingInfo(Object model) {
      MappingInfo mappingInfo = null;

      if (model instanceof SignalModel) {
         Signal signal = ((SignalModel) model).getSignal();
         mappingInfo = signal.getFirstMappedPinInformation();
      }
      else if (model instanceof PinModel) {
         Pin pin = ((PinModel)model).getPin();
         ArrayList<MappingInfo> mappedSignals = pin.getActiveMappings();
         if ((mappedSignals.size()==0) || (mappedSignals.size()>1)) {
            // Unmapped or multiply mapped signals
            return null;
         }
         mappingInfo = mappedSignals.get(0);
      }
      return mappingInfo;
   }
   
   @Override
   protected boolean canEdit(Object model) {
      MappingInfo mappingInfo = getMappingInfo(model);
      if (mappingInfo == null) {
         return false;
      }
      return mappingInfo.getProperty(fMask, fOffset) != null;
   }

   public Long getValueAsLong(Object model) {
      MappingInfo mappingInfo = getMappingInfo(model);
      if (mappingInfo == null) {
         return null;
      }
      return mappingInfo.getProperty(fMask, fOffset);
   }

   protected void setValueAsLong(Object model, Long value) {
      MappingInfo mappingInfo = getMappingInfo(model);
      if (mappingInfo == null) {
         return;
      }
      mappingInfo.setProperty(fMask, fOffset, value.longValue());
      getViewer().update(model, null);
   }
}
