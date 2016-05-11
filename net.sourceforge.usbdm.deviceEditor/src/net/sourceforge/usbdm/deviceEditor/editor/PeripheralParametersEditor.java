package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.EditorPage;
import net.sourceforge.usbdm.deviceEditor.model.TreeEditorPage;
import net.sourceforge.usbdm.deviceEditor.model.TreeViewModel;

public class PeripheralParametersEditor {
   
   private CTabFolder      fTabFolder = null;
   
   public PeripheralParametersEditor(Composite parent) {
      fTabFolder = new CTabFolder(parent, SWT.NONE);
   }

   public Control getControl() {
      return fTabFolder;
   }

   public void setModel(TreeViewModel peripheralPageModel) {
      for (CTabItem c:fTabFolder.getItems()) {
         c.dispose();
      }
      for (Object child:peripheralPageModel.getChildren()) {
         BaseModel pageModel = (BaseModel) child;
         CTabItem tabItem = new CTabItem(fTabFolder, SWT.NONE);
         tabItem.setText(pageModel.getName());
         tabItem.setToolTipText(pageModel.getToolTip());               
         TreeEditor treeEditor = new TreeEditor();
         tabItem.setControl(treeEditor.createControls(fTabFolder).getControl());
         if (pageModel instanceof TreeViewModel) {
            treeEditor.setModel((TreeViewModel) pageModel);
         }
         else {
            TreeViewModel rootModel = new TreeViewModel(peripheralPageModel.getColumnLabels(), pageModel.getName(), pageModel.getToolTip()) {
               @Override
               protected void removeMyListeners() {
               }
               @Override
               public EditorPage createEditorPage() {
                  return new TreeEditorPage();
               }
            };
            rootModel.addChild(pageModel);
            treeEditor.setModel(rootModel);
         }
      }
   }
}
