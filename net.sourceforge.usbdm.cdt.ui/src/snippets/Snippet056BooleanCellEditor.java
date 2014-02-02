package snippets;

/*******************************************************************************
 * Copyright (c) 2006, 2010 Tom Schindl and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tom Schindl - initial API and implementation
 *******************************************************************************/

import java.util.ArrayList;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.TreeViewerEditor;
import org.eclipse.jface.viewers.TreeViewerFocusCellManager;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;

/**
 * A simple TreeViewer to demonstrate usage
 *
 * @author Tom Schindl <tom.schindl@bestsolution.at>
 *
 */
public class Snippet056BooleanCellEditor {
   
   public class BooleanCellEditor extends CheckboxCellEditor {
      public BooleanCellEditor(Tree tree) {
         super(tree);
         setValueValid(true);
      }
   }
   
   public class ChoiceCellEditor extends ComboBoxCellEditor {

      public ChoiceCellEditor(Tree tree, String[] choices) {
         super(tree, choices);
         setValueValid(true);
      }
      
      @Override
      protected Object doGetValue() {
         return getItems()[(Integer) super.doGetValue()];
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

      public StringCellEditor(Tree tree) {
         super(tree);
         setValueValid(true);
      }
   }
   
   class MyEditingSupport extends EditingSupport {
      TreeViewer viewer;
      
      public MyEditingSupport(TreeViewer viewer) {
         super(viewer);
         this.viewer = viewer;
      }

      protected boolean canEdit(Object element) {
         return true;
      }

      protected CellEditor getCellEditor(Object element) {
         if (element instanceof StringModel) {
            return new StringCellEditor(viewer.getTree());
         }
         if (element instanceof BooleanModel) {
            return new BooleanCellEditor(viewer.getTree());
         }
         if (element instanceof EnumeratedModel) {
            return new ChoiceCellEditor(viewer.getTree(), ((EnumeratedModel)element).getChoices());
         }
         
         return null;
      }

      protected Object getValue(Object element) {
         if (element instanceof MyModel) {
            return ((MyModel) element).getValue();
         }
         return null;
      }

      protected void setValue(Object element, Object value) {
         if (element instanceof MyModel) {
            ((MyModel) element).setValue(value);
         }
         getViewer().update(element, null);
      }
   }
   
   public Snippet056BooleanCellEditor(final Shell shell) {
      final TreeViewer viewer = new TreeViewer(shell, SWT.BORDER|SWT.FULL_SELECTION);
      viewer.getTree().setLinesVisible(true);
      viewer.getTree().setHeaderVisible(true);

      FocusCellOwnerDrawHighlighter h = new FocusCellOwnerDrawHighlighter(viewer) {

         protected Color getSelectedCellBackgroundColorNoFocus(
               ViewerCell cell) {
            return shell.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
         }

         protected Color getSelectedCellForegroundColorNoFocus(
               ViewerCell cell) {
            return shell.getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
         }
      };

      TreeViewerFocusCellManager focusCellManager = new TreeViewerFocusCellManager(viewer,h);
      ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(viewer);

      TreeViewerEditor.create(viewer, focusCellManager, actSupport, ColumnViewerEditor.TABBING_HORIZONTAL
            | ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR
            | ColumnViewerEditor.TABBING_VERTICAL | ColumnViewerEditor.KEYBOARD_ACTIVATION);

      TreeViewerColumn column = new TreeViewerColumn(viewer, SWT.NONE);
      column.getColumn().setWidth(200);
      column.getColumn().setMoveable(true);
      column.getColumn().setText("Column 1");
      column.setLabelProvider(new ColumnLabelProvider() {
         public String getText(Object element) {
            return ((MyModel) element).getDescription();
         }
      });
      column = new TreeViewerColumn(viewer, SWT.NONE);
      column.getColumn().setWidth(200);
      column.getColumn().setMoveable(true);
      column.getColumn().setText("Column 2");
      column.setLabelProvider(new ColumnLabelProvider() {
         public String getText(Object element) {
            return ((MyModel) element).getValue().toString();
         }
      });
      column.setEditingSupport(new MyEditingSupport(viewer));

      column = new TreeViewerColumn(viewer, SWT.NONE);
      column.getColumn().setWidth(200);
      column.getColumn().setMoveable(true);
      column.getColumn().setText("Column 3");
      column.setLabelProvider(new ColumnLabelProvider() {
         public String getText(Object element) {
            return "Column 3 => " + element.toString();
         }
      });
      
      viewer.setContentProvider(new MyContentProvider());

      viewer.setInput(createModel());
   }

   private MyModel createModel() {

      MyModel root = new MyModel(null, "Root");
      root.addChild(new BooleanModel(root, "bool 1", true));
      root.addChild(new BooleanModel(root, "bool 2", true));
      root.addChild(new BooleanModel(root, "bool 3", true));
      root.addChild(new StringModel(root, "string 1", "One"));
      root.addChild(new StringModel(root, "string 2", "Two"));
      root.addChild(new StringModel(root, "string 3", "Three"));
      root.addChild(new EnumeratedModel(root, "enum 1", new String[]{"one","two","three"}));
      root.addChild(new EnumeratedModel(root, "enum 2", new String[]{"four","five","six"}));
      MyModel node = new StringModel(root, "Expand Me", "node");
      root.addChild(node);
      node.addChild(new EnumeratedModel(node, "enum 3", new String[]{"four","five","six"}));
      node.addChild(new EnumeratedModel(node, "enum 4", new String[]{"four","five","six"}));
      
      return root;
   }

   public static void main(String[] args) {
      Display display = new Display();

      Shell shell = new Shell(display);
      shell.setLayout(new FillLayout());
      new Snippet056BooleanCellEditor(shell);
      shell.open();

      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }

      display.dispose();
   }

   private class MyContentProvider implements ITreeContentProvider {

      public Object[] getElements(Object inputElement) {
         return ((MyModel) inputElement).children.toArray();
      }

      public void dispose() {
      }

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }

      public Object[] getChildren(Object parentElement) {
         return getElements(parentElement);
      }

      public Object getParent(Object element) {
         if ((element == null) || !(element instanceof MyModel)) {
            return null;
         }
         return ((MyModel) element).parent;
      }

      public boolean hasChildren(Object element) {
         if ((element == null) || !(element instanceof MyModel)) {
            return false;
         }
         return ((MyModel) element).children.size() > 0;
      }
   }

   public class MyModel {
      private MyModel            parent;
      private String             description;
      private Object             value;
      private ArrayList<MyModel> children = new ArrayList<MyModel>();

      public MyModel(MyModel parent, String description) {
         this.parent = parent;
         this.setValue(null);
         this.setDescription(description);
      }

      public void addChild(MyModel child) {
         this.children.add(child);
      }
      
      public ArrayList<MyModel> getChildren() {
         return this.children;
      }
      
      public MyModel getParent () {
         return this.parent;
      }
      
      public Object getValue() {
         return this.value;
      }
      
      public void setValue(Object value) {
         this.value = value;
      }

      public String getDescription() {
         return description;
      }

      public void setDescription(String description) {
         this.description = description;
      }

      public String toString() {
         return this.value.toString();
      }
      
      public boolean canEdit() {
         return false;
      }
   }
   
   public class BooleanModel extends MyModel {
      
      public BooleanModel(MyModel parent, String description, boolean value) {
         super(parent, description);
         setValue(value);
      }

      @Override
      public Boolean getValue() {
         return (Boolean) super.getValue();
      }
      
      @Override
      public boolean canEdit() {
         return true;
      }
   }
   
   public class StringModel extends MyModel {

      public StringModel(MyModel parent, String description, String value) {
         super(parent, description);
         setValue(value);
      }

      @Override
      public String getValue() {
         return (String) super.getValue();
      }
      
      @Override
      public boolean canEdit() {
         return true;
      }
   }
   
   public class EnumeratedModel extends MyModel {
      String[] choices;
      
      public EnumeratedModel(MyModel parent, String description, String[] values) {
         super(parent, description);
         setValue(values[0]);
         this.choices = values;
      }

      public String[] getChoices() {
         return choices;
      }

      @Override
      public String getValue() {
         return (String) super.getValue();
      }
      
      @Override
      public boolean canEdit() {
         return true;
      }
   }
   
}
