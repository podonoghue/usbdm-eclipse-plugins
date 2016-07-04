package testmem.views;


import java.util.concurrent.Executor;

import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.ImmediateExecutor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import testmem.dsf.GdbDsfSessionListener;


/**
 * This sample class demonstrates how to plug-in a new
 * workbench view. The view shows data obtained from the
 * model. The sample creates a dummy model on the fly,
 * but a real implementation would connect to the model
 * available either in this or another plug-in (e.g. the workspace).
 * The view is connected to the model using a content provider.
 * <p>
 * The view uses a label provider to define how model
 * objects should be presented in the view. Each
 * view can present the same model objects using
 * different labels and icons, if needed. Alternatively,
 * a single label provider can be shared between views
 * in order to ensure that objects of the same type are
 * presented in the same way everywhere.
 * <p>
 */

public class SampleView extends ViewPart {

   GdbDsfSessionListener gdbDsfSessionListener = null;

   class MemoryAccessTest {
      private final long fAddress;
      private final int  fSize;
      byte[]     data;
      boolean    pending = true;
      
      public MemoryAccessTest(long address, int size) {
         fAddress = address;
         fSize    = size;
      }
      
      public void refresh(final TableViewer viewer) {
         System.err.println("SampleView.refresh() pid = " + Thread.currentThread().getId());
         Executor executor = ImmediateExecutor.getInstance();
         pending = true;
         viewer.update(this, null);
         gdbDsfSessionListener.readMemory(fAddress, fSize, new DataRequestMonitor<byte[]>(executor, null) {
            
            private void updateGui() {
               Display.getDefault().asyncExec(new Runnable() {
                 public void run() {
                    System.err.println("SampleView.refresh().updateGui().run() pid = " + Thread.currentThread().getId());
                    pending = false;
                    viewer.update(MemoryAccessTest.this, null);
                 }
               });
            }
            @Override
            protected void handleSuccess() {
               System.err.println("SampleView.refresh().handleSuccess() pid = " + Thread.currentThread().getId());
               data = getData();
               updateGui();
            }
            @Override
            protected void handleFailure() {
               System.err.println("SampleView.refresh().handleFailure() pid = " + Thread.currentThread().getId());
               data = null;
               updateGui();
            }
         });
      }

      @Override
      public String toString() {
         System.err.println("SampleView.toString() pid = " + Thread.currentThread().getId());
         String s = "<invalid>";
         if (pending) {
            s = "<pending>";
         }
         else if (data != null) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (byte value:data) {
               if (!first) {
                  first = false;
                  sb.append(",");
               }
               sb.append(String.format("%02X", value));
            }
            s = sb.toString();
         }
         return "Mem["+Long.toString(fAddress)+"] = " + s;
      }
   };
   
   /**
    * The ID of the view as specified by the extension.
    */
   public static final String ID = "testmem.views.SampleView";

   private TableViewer viewer;
   private Action action1;
   private Action action2;
   private Action doubleClickAction;


   class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
      
      public String getColumnText(Object obj, int index) {
         return getText(obj);
      }
      public Image getColumnImage(Object obj, int index) {
         return getImage(obj);
      }
      public Image getImage(Object obj) {
         return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
      }
   }

   /**
    * The constructor.
    */
   public SampleView() {
      gdbDsfSessionListener = new GdbDsfSessionListener();
   }

   @Override
   public void dispose() {
      if (gdbDsfSessionListener != null) {
         gdbDsfSessionListener.dispose();
      }
      gdbDsfSessionListener = null;
      super.dispose();
   }

   /**
    * This is a callback that will allow us
    * to create the viewer and initialize it.
    */
   public void createPartControl(Composite parent) {
      viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);

      viewer.setContentProvider(ArrayContentProvider.getInstance());
      viewer.setInput(new MemoryAccessTest[] { 
         new MemoryAccessTest(100, 20), 
         new MemoryAccessTest(200, 20), 
         new MemoryAccessTest(300, 20),
      });
      
      viewer.setLabelProvider(new ViewLabelProvider());

      // Create the help context id for the viewer's control
      PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), "TestMem.viewer");
      getSite().setSelectionProvider(viewer);
      makeActions();
      hookContextMenu();
      hookDoubleClickAction();
      contributeToActionBars();
   }

   private void hookContextMenu() {
      MenuManager menuMgr = new MenuManager("#PopupMenu");
      menuMgr.setRemoveAllWhenShown(true);
      menuMgr.addMenuListener(new IMenuListener() {
         public void menuAboutToShow(IMenuManager manager) {
            SampleView.this.fillContextMenu(manager);
         }
      });
      Menu menu = menuMgr.createContextMenu(viewer.getControl());
      viewer.getControl().setMenu(menu);
      getSite().registerContextMenu(menuMgr, viewer);
   }

   private void contributeToActionBars() {
      IActionBars bars = getViewSite().getActionBars();
      fillLocalPullDown(bars.getMenuManager());
      fillLocalToolBar(bars.getToolBarManager());
   }

   private void fillLocalPullDown(IMenuManager manager) {
      manager.add(action1);
      manager.add(new Separator());
      manager.add(action2);
   }

   private void fillContextMenu(IMenuManager manager) {
      manager.add(action1);
      manager.add(action2);
      // Other plug-ins can contribute there actions here
      manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
   }

   private void fillLocalToolBar(IToolBarManager manager) {
      manager.add(action1);
      manager.add(action2);
   }

   private void makeActions() {
      action1 = new Action() {
         public void run() {
            showMessage("Action 1 executed");
         }
      };
      action1.setText("Action 1");
      action1.setToolTipText("Action 1 tooltip");
      action1.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
            getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));

      action2 = new Action() {
         public void run() {
            showMessage("Action 2 executed");
         }
      };
      action2.setText("Action 2");
      action2.setToolTipText("Action 2 tooltip");
      action2.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
            getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
      
      doubleClickAction = new Action() {
         public void run() {
            ISelection selection = viewer.getSelection();
            if (selection == null) {
               return;
            }
            Object obj = ((IStructuredSelection)selection).getFirstElement();
            if (!(obj instanceof MemoryAccessTest)) {
               return;
            }
            ((MemoryAccessTest)obj).refresh(viewer);
            viewer.update(obj, null);
         }
      };
   }

   private void hookDoubleClickAction() {
      viewer.addDoubleClickListener(new IDoubleClickListener() {
         public void doubleClick(DoubleClickEvent event) {
            doubleClickAction.run();
         }
      });
   }
   
   private void showMessage(String message) {
      MessageDialog.openInformation(
            viewer.getControl().getShell(),
            "Sample View",
            message);
   }

   /**
    * Passing the focus request to the viewer's control.
    */
   public void setFocus() {
      viewer.getControl().setFocus();
   }
}
