package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TreeViewer;

import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.model.SignalModel;

public class ModifierEditingSupport extends EditingSupport {

   public ModifierEditingSupport(ColumnViewer viewer) {
      super(viewer);
   }

   public static ModifierEditorInterface getModifierEditor(Object baseModel) {
      
      if (!(baseModel instanceof SignalModel)) {
         return null;
      }
      Signal signal = ((SignalModel)baseModel).getSignal();
      if (signal.getMappedPin() == Pin.UNASSIGNED_PIN) {
         return null;
      }
      return signal.getModifierEditor();
   }
   
   @Override
   protected CellEditor getCellEditor(Object model) {

      ModifierEditorInterface me = getModifierEditor(model);
      if (me == null) {
         return null;
      }
      return me.getCellEditor((TreeViewer)getViewer());
   }

   @Override
   protected boolean canEdit(Object model) {

      ModifierEditorInterface me = getModifierEditor(model);
      if (me == null) {
         return false;
      }
      return me.canEdit((SignalModel)model);
   }

   @Override
   protected Object getValue(Object model) {

      ModifierEditorInterface me = getModifierEditor(model);
      if (me == null) {
         return false;
      }
      return me.getValue((SignalModel) model);
   }

   @Override
   protected void setValue(Object model, Object value) {

      ModifierEditorInterface me = getModifierEditor(model);
      if (me == null) {
         return;
      }
      if (me.setValue((SignalModel) model, value)) {
         getViewer().update(model, null);
      }
   }
}
