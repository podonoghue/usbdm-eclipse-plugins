package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TreeViewer;

import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.SignalModel;

public class BooleanEditingSupport extends EditingSupport {

   String  fTrueText;
   String  fFalseText;
   
   public BooleanEditingSupport(ColumnViewer viewer) {
      super(viewer);
   }

   public BooleanEditingSupport(ColumnViewer viewer, String trueText, String falseText) {
      super(viewer);
   }

   @Override
   protected boolean canEdit(Object model) {
      if (!(model instanceof BaseModel)) {
         return false;
      }
      BaseModel baseModel = (BaseModel)model;
      return baseModel.canEdit();
   }

   @Override
   protected CellEditor getCellEditor(Object model) {
      if (!(model instanceof SignalModel)) {
         return null;
      }
      CheckboxCellEditor editor = new CheckboxCellEditor(((TreeViewer)getViewer()).getTree());
      return editor;
   }

   @Override
   protected Object getValue(Object model) {
      if (!(model instanceof SignalModel)) {
         return null;
      }
      Signal signal = ((SignalModel)model).getSignal();
      return signal.isActiveLow();
   }

   @Override
   protected void setValue(Object model, Object value) {
      if (!(model instanceof SignalModel)) {
         return;
      }
      Signal signal = ((SignalModel)model).getSignal();
      signal.setActiveLow((Boolean)value);
      getViewer().update(model, null);
   }

   public static EditingSupport getPolarity(TreeViewer viewer) {
      BooleanEditingSupport t = new BooleanEditingSupport(viewer) {
      };
      return t;
   }

}
