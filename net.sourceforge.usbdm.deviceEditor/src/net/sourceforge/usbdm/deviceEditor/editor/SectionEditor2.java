package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.IEditor;
import net.sourceforge.usbdm.deviceEditor.model.SectionModel;
import net.sourceforge.usbdm.deviceEditor.model.TabModel;
import net.sourceforge.usbdm.deviceEditor.model.TreeViewModel;

public class SectionEditor2 implements IEditor {

   private Composite          tabArea      = null;
   private SectionModel       fSectionModel = null;
   private ScrolledComposite  scroller = null;

   public SectionEditor2() {
   }

   @Override
   public Control createControl(Composite parent) {

      scroller = new ScrolledComposite(parent, SWT.BORDER|SWT.V_SCROLL|SWT.H_SCROLL);

      tabArea = new Composite(scroller, SWT.NONE); 
      scroller.setContent(tabArea);

      GridLayout gridLayout = new GridLayout(1, false);
      gridLayout.marginWidth = 5;
      gridLayout.marginHeight = 5;
      gridLayout.verticalSpacing = 0;
      gridLayout.horizontalSpacing = 0;
      tabArea.setLayout(gridLayout);

      scroller.setExpandVertical(true);
      scroller.setExpandHorizontal(true);
      scroller.setMinSize(tabArea.computeSize(SWT.DEFAULT, SWT.DEFAULT));

      return scroller;
   }

   public Control getControl() {
      return tabArea;
   }

   public void setModel(BaseModel model) {
      if (fSectionModel == model) {
         return;
      }
      //      if ((fControl != null) && !fControl.isDisposed()) {
      //         fControl.dispose();
      //      }
      fSectionModel = (SectionModel) model;
      for (Object child:fSectionModel.getChildren()) {
         BaseModel pageModel = (BaseModel) child;
         if (pageModel instanceof TreeViewModel) {
            TreeEditor treeEditor = new TreeEditor();
            Control treeControl = treeEditor.createControl(tabArea, SWT.NO_SCROLL);
            treeControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            treeEditor.setModel((TreeViewModel) pageModel);
            treeEditor.getViewer().addTreeListener( new ITreeViewerListener() {
               @Override
               public void treeExpanded(TreeExpansionEvent arg0) {
                  System.err.println("Expand");
                  Display.getDefault().asyncExec(new Runnable() {
                     @Override
                     public void run() {
                        arg0.getTreeViewer().getControl().update();
                        scroller.update();
                        scroller.setMinSize(tabArea.computeSize(SWT.DEFAULT, SWT.DEFAULT));
                     }
                  });
               }
               @Override
               public void treeCollapsed(TreeExpansionEvent arg0) {
                  System.err.println("Collapse");
                  Display.getDefault().asyncExec(new Runnable() {
                     @Override
                     public void run() {
                        arg0.getTreeViewer().getControl().update();
                        scroller.update();
                        scroller.setMinSize(tabArea.computeSize(SWT.DEFAULT, SWT.DEFAULT));
                     }
                  });
               }
            });
         }
         else if (pageModel instanceof TabModel) {
            TabbedEditor tabEditor = new TabbedEditor();
            Control treeControl = tabEditor.createControl(tabArea, SWT.NO_SCROLL);
            treeControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            tabEditor.setModel(pageModel);
         }
      }
      scroller.setMinSize(tabArea.computeSize(SWT.DEFAULT, SWT.DEFAULT));
   }

   @Override
   public void refresh() {
   }
}
