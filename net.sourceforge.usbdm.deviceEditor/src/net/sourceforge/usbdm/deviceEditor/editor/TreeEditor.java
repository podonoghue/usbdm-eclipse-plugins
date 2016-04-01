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
import net.sourceforge.usbdm.deviceEditor.model.DeviceModel;

public class TreeEditor {

   TreeViewer       viewer      = null;
   DeviceModel      deviceModel = null;
   TreeViewerColumn columns[];
   
   public TreeViewer getViewer() {
      return viewer;
   }
   
   public TreeEditor() {
   }

   public void setModel(DeviceModel deviceModel) {
      this.deviceModel = deviceModel;
      viewer.setInput(deviceModel);
      deviceModel.addViewer(viewer);
      String[] columnLabels = deviceModel.getColumnLabels();
      for (int index=0; index<columnLabels.length; index++) {
         columns[index].getColumn().setText(columnLabels[index]);
      }
   }

   public void refresh() {
      viewer.refresh();
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
      viewer = new TreeViewer(parent, SWT.BORDER|SWT.FULL_SELECTION);
//      viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
      ColumnViewerToolTipSupport.enableFor(viewer);
      
      Tree tree = viewer.getTree();
      tree.setLinesVisible(true);
      tree.setHeaderVisible(true);
//      ColumnViewerToolTipSupport.enableFor(viewer);

      viewer.setContentProvider(new ViewContentProvider());

      ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(viewer) {
         @Override
         protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {
            return event.eventType == ColumnViewerEditorActivationEvent.MOUSE_CLICK_SELECTION;
         }
      };
      
      TreeViewerEditor.create(viewer, actSupport, ColumnViewerEditor.DEFAULT);

      columns = new TreeViewerColumn[4];
      
      columns[0] = new TreeViewerColumn(viewer, SWT.NONE);
      columns[0].getColumn().setWidth(300);
      columns[0].setLabelProvider(new NameColumnLabelProvider());

      columns[1] = new TreeViewerColumn(viewer, SWT.NONE);
      columns[1].getColumn().setWidth(300);
      columns[1].setEditingSupport(new PinMappingEditingSupport(viewer));
      columns[1].setLabelProvider(new ValueColumnLabelProvider());
      
      columns[2] = new TreeViewerColumn(viewer, SWT.NONE);
      columns[2].getColumn().setWidth(300);
      columns[2].setEditingSupport(new PinUseDescriptionEditingSupport(viewer));
      columns[2].setLabelProvider(new DelegatingStyledCellLabelProvider(new DescriptionLabelProvider(this)));

//      columns[3] = new TreeViewerColumn(viewer, SWT.NONE);
//      columns[3].getColumn().setText("Description");
//      columns[3].getColumn().setWidth(300);
//      columns[3].setLabelProvider(new DelegatingStyledCellLabelProvider(new DescriptionLabelProvider(this)));

      return viewer;
   }

}