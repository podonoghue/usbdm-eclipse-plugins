package net.sourceforge.usbdm.annotationEditor;

import java.util.ArrayList;

import net.sourceforge.usbdm.annotationEditor.AnnotationModel.*;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;

public class AnnotationEditingSupport  extends EditingSupport {
   TreeViewer viewer;
   
   public class BooleanCellEditor extends CheckboxCellEditor {
      public BooleanCellEditor(Tree tree) {
         super(tree);
         setValueValid(true);
      }

      @Override
      protected Object doGetValue() {
         Boolean value = (Boolean) super.doGetValue();
         return value;
      }

      @Override
      protected void doSetValue(Object value) {
         super.doSetValue(value);
      }
   }
   
   public class ChoiceCellEditor extends ComboBoxCellEditor {

      public ChoiceCellEditor(Tree tree, String[] choices) {
         super(tree, choices, SWT.READ_ONLY);
         setValueValid(true);
      }
      
      @Override
      protected Object doGetValue() {
         String item = getItems()[(Integer) super.doGetValue()];
         return item;
      }
      
      @Override
      protected void doSetValue(Object value) {
         String[] items = getItems();
         for (int index=0; index<items.length; index++) {
            if (items[index].equalsIgnoreCase(value.toString())) {
               super.doSetValue(index);
               return;
            }
         }
         super.doSetValue(0);
      }
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
   
   public AnnotationEditingSupport(TreeViewer viewer) {
      super(viewer);
      this.viewer = viewer;
   }

   protected boolean canEdit(Object element) {
      if (!(element instanceof AnnotationModelNode)) {
         return false;
      }
//      System.err.println("MyEditingSupport.canEdit() => "+ ((WizardModelNode)element).canModify());
      return ((AnnotationModelNode)element).canModify();
   }

   protected CellEditor getCellEditor(Object element) {
      if (element instanceof BinaryOptionModelNode) {
         return new BooleanCellEditor(viewer.getTree());
      }
      if (element instanceof EnumeratedOptionModelNode) {
         ArrayList<EnumValue> t = ((EnumeratedOptionModelNode) element).getEnumerationValues();
         String choices[] = new String[t.size()];
         for (int index=0; index<t.size(); index++) {
            choices[index] = t.get(index).getName();
         }
         return new ChoiceCellEditor(viewer.getTree(), choices);
      }
      if (element instanceof NumericOptionModelNode) {
         BitField field = ((NumericOptionModelNode)element).getBitField();
         if ((field != null) && (field.getStart() == field.getEnd())) {
            return new BooleanCellEditor(viewer.getTree());
         }
         return new StringCellEditor(viewer.getTree());
      }
      if (element instanceof StringOptionModelNode) {
         return new StringCellEditor(viewer.getTree());
      }
      return null;
   }

   protected Object getValue(Object element) {
//      System.err.println("MyEditingSupport.getValue() element = "+ element + ", " + element.getClass());
      if (element instanceof AnnotationModelNode) {
         try {
            if (element instanceof BinaryOptionModelNode) {
               return ((BinaryOptionModelNode)element).getValue();
            }
            if (element instanceof NumericOptionModelNode) {
               BitField field = ((NumericOptionModelNode)element).getBitField();
               if ((field != null) && (field.getStart() == field.getEnd())) {
                  return new Boolean(((NumericOptionModelNode)element).getValueAsLong()!=0);
               }
            }
            return ((AnnotationModelNode) element).getValueAsString();
         } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
         }
      }
      return null;
   }

   protected void setValue(Object element, Object value) {
      try {
         //      System.err.println("MyEditingSupport.setValue() value = "+ value + ", " + value.getClass());
         if (element instanceof AnnotationModelNode) {
            if (value instanceof String) {
               // If a string get model to convert value
               ((AnnotationModelNode)element).setValueAsString(value.toString());
            }
            else {
               // Otherwise just pass the value
               ((AnnotationModelNode)element).setValue(value);
            }
            getViewer().refresh(element);
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}