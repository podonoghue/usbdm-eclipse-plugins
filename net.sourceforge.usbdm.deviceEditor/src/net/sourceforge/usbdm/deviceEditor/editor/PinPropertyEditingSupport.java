package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;

import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.model.SignalModel;

public abstract class PinPropertyEditingSupport extends EditingSupport {

   final long fOffset;
   final long fMask;

   public PinPropertyEditingSupport(ColumnViewer viewer, long mask, long offset) {
      super(viewer);
      fOffset = offset;
      fMask   = mask;
   }

   @Override
   protected boolean canEdit(Object model) {
      if (model instanceof SignalModel) {
         Signal signal = ((SignalModel) model).getSignal();
         return (signal.getMappedPin() != Pin.UNASSIGNED_PIN) && signal.hasDigitalFeatures();
      }
      return false;
   }

   protected Long getValueAsLong(Object model) {
      Signal signal = ((SignalModel) model).getSignal();
      return signal.getProperty(fMask, fOffset);
   }

   protected void setValueAsLong(Object model, Long value) {
      Signal signal = ((SignalModel) model).getSignal();
      signal.setProperty(fMask, fOffset, value.longValue());
      getViewer().update(model, null);
   }
}
