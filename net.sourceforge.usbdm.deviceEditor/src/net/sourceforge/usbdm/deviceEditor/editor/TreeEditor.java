package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
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

public class TreeEditor implements IEditor {

   TreeViewer       fViewer      = null;
   TreeViewModel    fDeviceModel = null;
   TreeViewerColumn fColumns[];
   
   public TreeEditor() {
   }

   public void setModel(BaseModel model) {
      
      fDeviceModel = (TreeViewModel) model;
      fViewer.setInput(fDeviceModel);
      fDeviceModel.setViewer(fViewer);
      String[] columnLabels = fDeviceModel.getColumnLabels();
      for (int index=0; index<columnLabels.length; index++) {
         fColumns[index].getColumn().setText(columnLabels[index]);
      }
   }

   public void refresh() {
      fViewer.refresh();
   }
   public Control createControl(Composite parent) {
      return createControl(parent, SWT.BORDER);
   }
   
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

      fColumns = new TreeViewerColumn[3];
      
      fColumns[0] = new TreeViewerColumn(fViewer, SWT.NONE);
      fColumns[0].getColumn().setWidth(350);
//      fColumns[0].setLabelProvider(new NameColumnLabelProviderX());
      fColumns[0].setLabelProvider(new DelegatingStyledCellLabelProvider(new NameColumnLabelProvider(this)));

      fColumns[1] = new TreeViewerColumn(fViewer, SWT.NONE);
      fColumns[1].getColumn().setWidth(450);
      fColumns[1].setEditingSupport(new ValueColumnEditingSupport(fViewer));
      fColumns[1].setLabelProvider(new DelegatingStyledCellLabelProvider(new ValueColumnLabelProvider(this)));
      
      fColumns[2] = new TreeViewerColumn(fViewer, SWT.NONE);
      fColumns[2].getColumn().setWidth(500);
      fColumns[2].setEditingSupport(new DescriptionColumnEditingSupport(fViewer));
      fColumns[2].setLabelProvider(new DelegatingStyledCellLabelProvider(new DescriptionColumnLabelProvider(this)));

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