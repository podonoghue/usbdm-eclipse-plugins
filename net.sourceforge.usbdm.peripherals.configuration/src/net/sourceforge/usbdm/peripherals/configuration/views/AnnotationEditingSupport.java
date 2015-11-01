package net.sourceforge.usbdm.peripherals.configuration.views;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;

public class AnnotationEditingSupport extends EditingSupport {

   public AnnotationEditingSupport(ColumnViewer viewer) {
      super(viewer);
      // TODO Auto-generated constructor stub
   }

   @Override
   protected boolean canEdit(Object arg0) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   protected CellEditor getCellEditor(Object arg0) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   protected Object getValue(Object arg0) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   protected void setValue(Object arg0, Object arg1) {
      // TODO Auto-generated method stub

   }

}
