package net.sourceforge.usbdm.peripherals.configuration.views;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.TreeViewerEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import net.sourceforge.usbdm.peripherals.configuration.Activator;

public class PeripheralConfigurationEditor extends EditorPart implements IDocumentListener {
   
   public PeripheralConfigurationEditor() {
   }
   
   /**
    * The ID of the view as specified by the extension.
    */
   public static final String ID = "net.sourceforge.usbdm.peripherals.configuration.views.PeripheralConfigurationView";
   
   private Composite panel;
//   private MyAction pinViewAction;
//   private MyAction peripheralViewAction;

   private TabFolder tabFolder;

   class MyAction extends Action {

      MyAction(String text, String toolTip, int style, String imageId) {
         super(text, style);

         setText(text);
         setToolTipText(toolTip);
         if ((imageId!= null) && (Activator.getDefault() != null)) {
            ImageDescriptor imageDescriptor = Activator.getDefault().getImageDescriptor(imageId);
            setImageDescriptor(imageDescriptor);
         }
      }

      MyAction(String text, int style, String imageId) {
         this(text, text, style, imageId);
      }

      MyAction(String text, int style) {
         this(text, text, style, null);
      }
   }

//   class SelectViewAction extends MyAction {
//      private final Control fPanel;
//      
//      SelectViewAction(String text, String toolTip, int style, String imageId, Control panel) {
//         super(text, style);
//         fPanel = panel;
//      }
//
//      @Override
//      public void run() {
//         stackLayout.topControl = fPanel;
//         panel.layout();
//      }
//   }
   
   TabItem createPeripheralsTab(TabFolder parent) {
      TabItem tabItem = new TabItem(parent, SWT.NONE);
      tabItem.setText("Peripherals");
      Composite composite = new Composite(tabFolder, SWT.NONE);
      composite.setLayout(new FillLayout());
      tabItem.setControl(composite);
      
      TreeViewer viewer = new TreeViewer(composite, SWT.BORDER|SWT.FULL_SELECTION);
      
      Tree tree = viewer.getTree();
      tree.setLinesVisible(true);
      tree.setHeaderVisible(true);
      ColumnViewerToolTipSupport.enableFor(viewer);
      
//      viewer.setContentProvider(new ViewContentProvider());

      ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(viewer) {
         @Override
         protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {
            return event.eventType == ColumnViewerEditorActivationEvent.MOUSE_CLICK_SELECTION;
         }
      };

      TreeViewerEditor.create(viewer, actSupport, ColumnViewerEditor.DEFAULT);

      /*
       * Do columns
       */
      TreeViewerColumn column;
      column = new TreeViewerColumn(viewer, SWT.NONE);
      column.getColumn().setWidth(400);
      column.getColumn().setText("Option Name");
      column.getColumn().setResizable(true);
      column.setLabelProvider(new MyNameColumnLabelProvider());

      column = new TreeViewerColumn(viewer, SWT.NONE);
      column.getColumn().setWidth(300);
      column.getColumn().setText("Option Value");
      column.getColumn().setResizable(true);
//      column.setEditingSupport(new AnnotationEditingSupport(viewer));
//      column.setLabelProvider(new MyValueColumnLabelProvider());

      return tabItem;
   }

   TabItem createPinsTab(TabFolder parent) {
      TabItem tabItem = new TabItem(parent, SWT.NONE);
      tabItem.setText("Pins");
      Composite composite = new Composite(tabFolder, SWT.NONE);
      composite.setLayout(new FillLayout());
      tabItem.setControl(composite);
      Label label = new Label(composite, SWT.NONE);
      label.setText("Hello");
      return tabItem;
   }

   @Override
   public void createPartControl(Composite parent) {
      panel = parent;
      panel.setLayoutData(new GridData(GridData.FILL_BOTH));
      
      tabFolder = new TabFolder(panel, SWT.NONE);
      tabFolder.setLayout(new FillLayout());
      createPeripheralsTab(tabFolder);
      createPinsTab(tabFolder);
   }

//   /**
//    * Add menu manager for right click pop-up menu
//    */
//   private void hookContextMenu(Composite panel) {
//      MenuManager menuMgr = new MenuManager("#PopupMenu");
//      menuMgr.setRemoveAllWhenShown(true);
//      menuMgr.addMenuListener(new IMenuListener() {
//         public void menuAboutToShow(IMenuManager manager) {
//            PeripheralConfigurationEditor.this.fillContextMenu(manager);
//         }
//      });
//      Menu menu = menuMgr.createContextMenu(panel);
//      panel.setMenu(menu);
//   }
//
//   /**
//    * Fill Context menu
//    * 
//    * @param manager
//    */
//   private void fillContextMenu(IMenuManager manager) {
//      manager.add(pinViewAction);
//      manager.add(peripheralViewAction);
//   }

   @Override
   public void setFocus() {
      panel.setFocus();}

   @Override
   public void documentAboutToBeChanged(DocumentEvent arg0) {
   }

   @Override
   public void documentChanged(DocumentEvent arg0) {
   }

   @Override
   public void doSave(IProgressMonitor arg0) {
   }

   @Override
   public void doSaveAs() {
   }

   @Override
   public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
      if (!(editorInput instanceof IFileEditorInput)) {
         throw new PartInitException("Invalid Input: Must be IFileEditorInput");
      }
      setSite(site);
      setInput(editorInput);
      setPartName(editorInput.getName());
      setContentDescription("A configuration editor");
   }

   @Override
   public boolean isDirty() {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean isSaveAsAllowed() {
      // TODO Auto-generated method stub
      return false;
   }

}
