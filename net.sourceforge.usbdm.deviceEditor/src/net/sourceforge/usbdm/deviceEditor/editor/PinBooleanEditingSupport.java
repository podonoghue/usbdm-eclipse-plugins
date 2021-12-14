package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.TreeViewer;

import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;

public class PinBooleanEditingSupport extends PinPropertyEditingSupport {

   private PinBooleanEditingSupport(ColumnViewer viewer, long mask, long offset) {
      super(viewer, mask, offset);
   }

   @Override
   protected CellEditor getCellEditor(Object model) {
      CheckboxCellEditor editor = new CheckboxCellEditor(((TreeViewer)getViewer()).getTree());
      return editor;
   }

   @Override
   protected Object getValue(Object model) {
      Long value = getValueAsLong(model);
      return (value != null)?(value != 0):null;
   }

   @Override
   protected void setValue(Object model, Object value) {
      setValueAsLong(model, ((Boolean)value)?1L:0L);
   }
   
   public static PinBooleanEditingSupport getLk(ColumnViewer viewer) {
      return new PinBooleanEditingSupport(viewer, MappingInfo.PORT_PCR_LK_MASK, MappingInfo.PORT_PCR_LK_SHIFT);
   }
   
   public static PinBooleanEditingSupport getDse(ColumnViewer viewer) {
      return new PinBooleanEditingSupport(viewer, MappingInfo.PORT_PCR_DSE_MASK, MappingInfo.PORT_PCR_DSE_SHIFT);
   }
   
   public static PinBooleanEditingSupport getOde(ColumnViewer viewer) {
      return new PinBooleanEditingSupport(viewer, MappingInfo.PORT_PCR_ODE_MASK, MappingInfo.PORT_PCR_ODE_SHIFT);
   }
   
   public static PinBooleanEditingSupport getPfe(ColumnViewer viewer) {
      return new PinBooleanEditingSupport(viewer, MappingInfo.PORT_PCR_PFE_MASK, MappingInfo.PORT_PCR_PFE_SHIFT);
   }
   
   public static PinBooleanEditingSupport getSre(ColumnViewer viewer) {
      return new PinBooleanEditingSupport(viewer, MappingInfo.PORT_PCR_SRE_MASK, MappingInfo.PORT_PCR_SRE_SHIFT);
   }
}
