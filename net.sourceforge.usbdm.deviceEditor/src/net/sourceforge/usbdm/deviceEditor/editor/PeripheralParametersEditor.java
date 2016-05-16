package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.EditorPage;
import net.sourceforge.usbdm.deviceEditor.model.TreeEditorPage;
import net.sourceforge.usbdm.deviceEditor.model.TreeViewModel;

public class PeripheralParametersEditor {
   
   private CTabFolder      fTabFolder = null;
   private TreeViewModel   fPeripheralPageModel = null;
   
   public PeripheralParametersEditor(Composite parent) {

      // Create the containing tab folder
      Display display = Display.getCurrent();
      fTabFolder   = new CTabFolder(parent, SWT.NONE);
      fTabFolder.setSimple(false);
      fTabFolder.setBackground(new Color[]{
            display.getSystemColor(SWT.COLOR_WHITE),
            display.getSystemColor(SWT.COLOR_CYAN)}, 
            new int[]{100}, true);
      fTabFolder.setSelectionBackground(new Color[]{
            display.getSystemColor(SWT.COLOR_WHITE),
            display.getSystemColor(SWT.COLOR_WHITE)}, 
            new int[]{100}, true);
   }

   public Control getControl() {
      return fTabFolder;
   }

   public void setModel(TreeViewModel peripheralPageModel) {
      if (fPeripheralPageModel == peripheralPageModel) {
         return;
      }
      fPeripheralPageModel = peripheralPageModel;
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
      fTabFolder.setSelection(0);
   }
}
