package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.TreeViewer;

import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.PinModel;

public class PinBooleanEditingSupport extends PinPropertyEditingSupport {

   private PinBooleanEditingSupport(ColumnViewer viewer, long mask, long offset) {
      super(viewer, mask, offset);
   }

   @Override
   protected CellEditor getCellEditor(Object model) {
      if (!(model instanceof PinModel)) {
         return null;
      }
      CheckboxCellEditor editor = new CheckboxCellEditor(((TreeViewer)getViewer()).getTree());
      return editor;
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
   protected Object getValue(Object model) {
      return (getValueAsLong(model)!=0);
   }

   @Override
   protected void setValue(Object model, Object value) {
      setValueAsLong(model, ((Boolean)value)?1L:0L);
   }
   
   public static PinBooleanEditingSupport getPolarity(ColumnViewer viewer) {
      return new PinBooleanEditingSupport(viewer, Pin.PORT_POLARITY_MASK, Pin.PORT_POLARITY_SHIFT);
   }
   
   public static PinBooleanEditingSupport getLk(ColumnViewer viewer) {
      return new PinBooleanEditingSupport(viewer, Pin.PORT_PCR_LK_MASK, Pin.PORT_PCR_LK_SHIFT);
   }
   
   public static PinBooleanEditingSupport getDse(ColumnViewer viewer) {
      return new PinBooleanEditingSupport(viewer, Pin.PORT_PCR_DSE_MASK, Pin.PORT_PCR_DSE_SHIFT);
   }
   
   public static PinBooleanEditingSupport getOde(ColumnViewer viewer) {
      return new PinBooleanEditingSupport(viewer, Pin.PORT_PCR_ODE_MASK, Pin.PORT_PCR_ODE_SHIFT);
   }
   
   public static PinBooleanEditingSupport getPfe(ColumnViewer viewer) {
      return new PinBooleanEditingSupport(viewer, Pin.PORT_PCR_PFE_MASK, Pin.PORT_PCR_PFE_SHIFT);
   }
   
   public static PinBooleanEditingSupport getSre(ColumnViewer viewer) {
      return new PinBooleanEditingSupport(viewer, Pin.PORT_PCR_SRE_MASK, Pin.PORT_PCR_SRE_SHIFT);
   }
}
