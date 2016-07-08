package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.IEditor;
import net.sourceforge.usbdm.deviceEditor.model.SectionModel;
import net.sourceforge.usbdm.deviceEditor.model.TabModel;
import net.sourceforge.usbdm.deviceEditor.model.TreeViewModel;

public class SectionEditor implements IEditor {

   private Composite          tabArea      = null;
   private SectionModel       fSectionModel = null;

   public SectionEditor() {
   }

   @Override
   public Control createControl(Composite parent) {

      tabArea = new SashForm(parent, SWT.NONE|SWT.VERTICAL); 

      GridLayout gridLayout = new GridLayout(1, false);
      gridLayout.marginWidth = 5;
      gridLayout.marginHeight = 5;
      gridLayout.verticalSpacing = 0;
      gridLayout.horizontalSpacing = 0;
      tabArea.setLayout(gridLayout);

      return tabArea;
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
            TreeEditor treeEditor = new TreeEditor() {
               @Override
               protected TreeColumnInformation[] getColumnInformation(TreeViewer viewer) {
                  final TreeColumnInformation[] fColumnInformation = {
                        new TreeColumnInformation("Property",    350, new NameColumnLabelProvider(),        null),
                        new TreeColumnInformation("Value",       450, new ValueColumnLabelProvider(),       new ValueColumnEditingSupport(viewer)),
                        new TreeColumnInformation("Description", 500, new DescriptionColumnLabelProvider(), new DescriptionColumnEditingSupport(viewer)),
                  };
                  return fColumnInformation;
               }
            };
            Control treeControl = treeEditor.createControl(tabArea);
            treeControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            treeEditor.setModel((TreeViewModel) pageModel);
         }
         else if (pageModel instanceof TabModel) {
            TabbedEditor tabEditor = new TabbedEditor();
            Control treeControl = tabEditor.createControl(tabArea);
            treeControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            tabEditor.setModel(pageModel);
         }
      }
   }

   @Override
   public void refresh() {
   }
}
