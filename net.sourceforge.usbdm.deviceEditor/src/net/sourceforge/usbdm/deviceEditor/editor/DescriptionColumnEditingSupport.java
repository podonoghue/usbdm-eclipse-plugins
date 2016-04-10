package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import net.sourceforge.usbdm.deviceEditor.model.PinModel;

public class DescriptionColumnEditingSupport extends EditingSupport {

   private TreeViewer viewer;

   public DescriptionColumnEditingSupport(TreeViewer viewer) {
      super(viewer);
      this.viewer = viewer;
   }

   @Override
   protected boolean canEdit(Object element) {
      if (element instanceof PinModel) {
         return true;
      }
      return false;
   }

   @Override
   protected CellEditor getCellEditor(Object element) {
      if (!(element instanceof PinModel)) {
         return null;
      }
      return new StringCellEditor(viewer.getTree());
   }

   @Override
   protected Object getValue(Object element) {
      if (!(element instanceof PinModel)) {
         return null;
      }
      PinModel pinModel = (PinModel)element;
      return pinModel.getPinUseDescription();
   }

   @Override
   protected void setValue(Object element, Object value) {
      if (!(element instanceof PinModel)) {
         return;
      }
      PinModel pinModel = (PinModel)element;
      pinModel.setPinUseDescription((String) value);
      
      viewer.update(element, null);
      }

   public class StringCellEditor extends TextCellEditor {

      public StringCellEditor(Composite parent, int style) {
         super(parent, style);
         super.setValueValid(true);
      }

      public StringCellEditor(Composite parent) {
         this(parent, SWT.SINGLE);
      }

      @Override
      protected Object doGetValue() {
         Object item = super.doGetValue();
//         System.err.println("StringCellEditor.doGetValue value = " + item + ", " + item.getClass());
         return item;
      }

      @Override
      protected void doSetValue(Object value) {
//         System.err.println("StringCellEditor.doSetValue value = " + value + ", " + value.getClass());
         super.doSetValue(value);
      }
   }
  

}
