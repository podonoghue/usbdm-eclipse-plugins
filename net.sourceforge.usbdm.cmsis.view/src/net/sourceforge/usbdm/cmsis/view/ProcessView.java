package net.sourceforge.usbdm.cmsis.view;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.part.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.*;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.SWT;

public class ProcessView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "net.sourceforge.usbdm.cmsis.view.ProcessView";

	private TableViewer viewer;
	private Action action1;
	private Action action2;
	private Action doubleClickAction;
	 
	ProcessModel model = new ProcessModel();
	
	class ViewContentProvider implements IStructuredContentProvider {
	   ProcessModel model = null;
	   
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		   if (model != null) {
		      // Unhook listeners
		   }
		   model = (ProcessModel) newInput;
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {
			return model.getChildren();
		}
	}
	
	class ViewLabelProvider implements ITableLabelProvider {
      @Override
      public void addListener(ILabelProviderListener listener) {
      }
      @Override
      public void removeListener(ILabelProviderListener listener) {
      }
      @Override
      public void dispose() {
      }
      @Override
      public boolean isLabelProperty(Object element, String property) {
         return true;
      }
      @Override
      public Image getColumnImage(Object element, int columnIndex) {
         return null;
      }
      @Override
      public String getColumnText(Object element, int columnIndex) {
         if (element instanceof ProcessModel) {
            ProcessModel el = (ProcessModel)element;
            switch (columnIndex) {
            case 0:  return String.format("%d", el.cb_type);
            case 1:  return String.format("0x%X", el.stack);
            case 2:  return String.format("%d", el.prio);
            default: return "???";
            }
         }
         else {
            return "Opps";
         }
      }
	}
	
	class NameSorter extends ViewerSorter {
	}

	/**
	 * The constructor.
	 */
	public ProcessView() {
	}

	String wrapNull(long ptr) {
	   if (ptr == 0) {
	      return "null";
	   }
	   else {
	      return String.format("0x08X", ptr);
	   }
	}
	
	String wrapEvents(int events) {
	   StringBuffer buffer = new StringBuffer();
	   int width = 7;
	   if ((events & 0xFF000000) != 0) {
	      width = 31;
	   }
      else if ((events & 0xFFFF0000) != 0) {
         width = 23;
      }
      else if ((events & 0xFFFFFF00) != 0) {
         width = 15;
      }
	   for (int index=width; index>=0; index--) {
	      buffer.append(((events&(1<<index))!= 0)?'1':'0');
	   }
	   return buffer.toString();
	}
	
	/**
	 * Create a table column with given name & width
	 * 
	 * @param title
	 * @param width
	 * @return
	 */
	TableViewerColumn createColumn(String title, int width) {
	   TableViewerColumn column;
	   column = new TableViewerColumn(viewer, SWT.NONE);
      column.getColumn().setWidth(width);
      column.getColumn().setText(title);
      column.getColumn().setResizable(true);
      return column;
	}
	
	/**
	 * Callback to create Table control
	 */
	public void createPartControl(Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL| SWT.FULL_SELECTION | SWT.BORDER);

		final Table table = viewer.getTable();
      table.setHeaderVisible(true);
      table.setLinesVisible(true); 
		
      TableViewerColumn column;
      
      column = createColumn("state", 50);
      column.setLabelProvider(new ColumnLabelProvider() {
         @Override
         public String getText(Object element) {
            return ((ProcessModel)element).state.name;
         }
      });
      
      column = createColumn("priority", 50);
      column.setLabelProvider(new ColumnLabelProvider() {
         @Override
         public String getText(Object element) {
            return String.format("%d", ((ProcessModel)element).prio);
            }
      });

      column = createColumn("ID", 50);
      column.setLabelProvider(new ColumnLabelProvider() {
         @Override
         public String getText(Object element) {
            return String.format("%d", ((ProcessModel)element).task_id);
            }
      });

      column = createColumn("delta", 50);
      column.setLabelProvider(new ColumnLabelProvider() {
         @Override
         public String getText(Object element) {
            return String.format("%d", ((ProcessModel)element).delta_time);
            }
      });

      column = createColumn("interval", 50);
      column.setLabelProvider(new ColumnLabelProvider() {
         @Override
         public String getText(Object element) {
            return String.format("%d", ((ProcessModel)element).interval_time);
            }
      });

      column = createColumn("events", 80);
      column.setLabelProvider(new ColumnLabelProvider() {
         @Override
         public String getText(Object element) {
            return wrapEvents(((ProcessModel)element).events);
            }
      });

      column = createColumn("waits", 80);
      column.setLabelProvider(new ColumnLabelProvider() {
         @Override
         public String getText(Object element) {
            return wrapEvents(((ProcessModel)element).waits);
            }
      });

      column = createColumn("private stack", 70);
      column.setLabelProvider(new ColumnLabelProvider() {
         @Override
         public String getText(Object element) {
            return wrapNull(((ProcessModel)element).priv_stack);
            }
      });

      column = createColumn("stack ptr", 70);
      column.setLabelProvider(new ColumnLabelProvider() {
         @Override
         public String getText(Object element) {
            return wrapNull(((ProcessModel)element).tsk_stack);
            }
      });

      column = createColumn("stack", 50);
      column.setLabelProvider(new ColumnLabelProvider() {
         @Override
         public String getText(Object element) {
            return wrapNull(((ProcessModel)element).stack);
            }
      });

      viewer.setContentProvider(new ViewContentProvider());
//    viewer.setLabelProvider(new ViewLabelProvider());
//    viewer.setSorter(new NameSorter());
//    viewer.setInput(getViewSite());
      viewer.setInput(new ProcessModel());
      
      IWorkbenchPartSite site = getSite();
      if (site != null) {
         site.setSelectionProvider(viewer);
      }
      
		// Create the help context id for the viewer's control
//		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), "net.sourceforge.usbdm.cmsis.view.viewer");
      
//		makeActions();
//		hookContextMenu();
//		hookDoubleClickAction();
//		contributeToActionBars();
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				ProcessView.this.fillContextMenu(manager);
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
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				showMessage("Double-click detected on "+obj.toString());
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
			"ProcessView",
			message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
   /**
    * Test main
    * 
    * @param args
    */
   public static void main(String[] args) {
      Display display = new Display();

      Shell shell = new Shell(display);
      shell.setText("Task List - TableViewer Example");
      shell.setLayout(new FillLayout());
      
      Composite composite = new Composite(shell, SWT.NONE);
      composite.setBackground(new Color(display, 255, 0, 0 ));
      composite.setLayout(new FillLayout());

      ProcessView view = new ProcessView();
      
      view.createPartControl(composite);

      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }
      display.dispose();
   }


}