package net.sourceforge.usbdm.deviceEditor.editor;

import java.util.HashMap;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.TreeViewerEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;

import net.sourceforge.usbdm.deviceEditor.Activator;
import net.sourceforge.usbdm.deviceEditor.model.RootModel;

public class TreeEditor {

   TreeViewer       fViewer      = null;
   RootModel        fDeviceModel = null;
   TreeViewerColumn fColumns[];
   
   public TreeViewer getViewer() {
      return fViewer;
   }
   
   public TreeEditor() {
   }

   public void setModel(RootModel deviceModel) {
      fDeviceModel = deviceModel;
      fViewer.setInput(deviceModel);
      deviceModel.addViewer(fViewer);
      String[] columnLabels = deviceModel.getColumnLabels();
      for (int index=0; index<columnLabels.length; index++) {
         fColumns[index].getColumn().setText(columnLabels[index]);
      }
   }

   public void refresh() {
      fViewer.refresh();
   }
   
   private LocalResourceManager     resManager = null;
   private HashMap<String, Image>   imageCache = new HashMap<String,Image>();

   public Image getMyImage(String imageId) {
      Image image = imageCache.get(imageId);
      if ((Activator.getDefault() != null) && (image == null)) {
         ImageDescriptor imageDescriptor  = Activator.getDefault().getImageDescriptor(imageId);
         image = resManager.createImage(imageDescriptor);
         imageCache.put(imageId, image);
      }
      return image;
   }

   public TreeViewer createControls(Composite parent) {
      parent.setLayoutData(new FillLayout());
      fViewer = new TreeViewer(parent, SWT.BORDER|SWT.FULL_SELECTION);
//      viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
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

      return fViewer;
   }

}