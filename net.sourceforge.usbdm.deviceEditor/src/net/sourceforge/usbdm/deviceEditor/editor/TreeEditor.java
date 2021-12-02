package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.TreeViewerEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.IEditor;
import net.sourceforge.usbdm.deviceEditor.model.TreeViewModel;

public abstract class TreeEditor implements IEditor {

   TreeViewer       fViewer      = null;
   TreeViewModel    fDeviceModel = null;
   TreeViewerColumn fColumns[];
   
   public static class TreeColumnInformation {
      final String            name;
      final int               width;
      final BaseLabelProvider labelProvider;
      final EditingSupport    editingSupport;
      final String            tooltip;
      
      public TreeColumnInformation(
            String              name, 
            int                 width, 
            BaseLabelProvider   labelProvider, 
            EditingSupport      editingSupport,
            String              tooltip) {
         
         this.name           = name;
         this.width          = width;
         this.labelProvider  = labelProvider;
         this.editingSupport = editingSupport;
         this.tooltip        = tooltip;
      }
      public TreeColumnInformation(
            String              name, 
            int                 width, 
            BaseLabelProvider   labelProvider, 
            EditingSupport      editingSupport) {
         
         this.name           = name;
         this.width          = width;
         this.labelProvider  = labelProvider;
         this.editingSupport = editingSupport;
         this.tooltip        = null;
      }

   }
   
   public TreeEditor() {
   }

   public void setModel(BaseModel model) {
      
      fDeviceModel = (TreeViewModel) model;
      fViewer.setInput(fDeviceModel);
      fDeviceModel.setViewer(fViewer);
   }

   public void refresh() {
      fViewer.refresh();
   }
   
   public Control createControl(Composite parent) {
      return createControl(parent, SWT.BORDER);
   }

   protected abstract TreeColumnInformation[] getColumnInformation(TreeViewer viewer);

   public Control createControl(Composite parent, int style) {
//      parent.setLayoutData(new FillLayout());
      fViewer = new TreeViewer(parent, SWT.FULL_SELECTION|style);
      ColumnViewerToolTipSupport.enableFor(fViewer);
      Tree tree = fViewer.getTree();
      tree.setLinesVisible(true);
      tree.setHeaderVisible(true);

      fViewer.setContentProvider(new ViewContentProvider());

      ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(fViewer) {
         @Override
         protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {
            return event.eventType == ColumnViewerEditorActivationEvent.MOUSE_CLICK_SELECTION;
         }
      };
      TreeViewerEditor.create(fViewer, actSupport, ColumnViewerEditor.DEFAULT);
      final TreeColumnInformation[] fColumnInformation = getColumnInformation(fViewer);

      fColumns = new TreeViewerColumn[fColumnInformation.length];
      
      for (int index=0; index<fColumnInformation.length; index++) {
         fColumns[index] = new TreeViewerColumn(fViewer, SWT.NONE);
         fColumns[index].getColumn().setWidth(fColumnInformation[index].width);
         fColumns[index].setLabelProvider(new DelegatingStyledCellLabelProvider(fColumnInformation[index].labelProvider));
         fColumns[index].getColumn().setText(fColumnInformation[index].name);
         fColumns[index].getColumn().setToolTipText(fColumnInformation[index].tooltip);
         if (fColumnInformation[index].editingSupport != null) {
            fColumns[index].setEditingSupport(fColumnInformation[index].editingSupport);
         }
      }
      return fViewer.getControl();
   }

   public TreeViewer getViewer() {
      return fViewer;
   }
   
   @Override
   public Control getControl() {
      return fViewer.getControl();
   }

}