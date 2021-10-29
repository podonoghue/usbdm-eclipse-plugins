package net.sourceforge.usbdm.deviceEditor.editor;

import java.util.ArrayList;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
//import org.eclipse.swt.widgets.Event;
//import org.eclipse.swt.widgets.Listener;
//import org.eclipse.swt.widgets.Tree;
//import org.eclipse.swt.widgets.TreeItem;
//import org.eclipse.swt.widgets.Widget;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.IEditor;
import net.sourceforge.usbdm.deviceEditor.model.SectionModel;
import net.sourceforge.usbdm.deviceEditor.model.TabModel;
import net.sourceforge.usbdm.deviceEditor.model.TreeViewModel;

public class TabbedEditor implements IEditor {
   
   private CTabFolder  fTabFolder           = null;
   private TabModel    fPeripheralPageModel = null;
   
   public TabbedEditor() {
   }

   public Control createControl(Composite parent, int style) {
      // Create the containing tab folder
      Display display = Display.getCurrent();
      fTabFolder = new CTabFolder(parent, style);
      fTabFolder.setBorderVisible(true);
      fTabFolder.setSimple(false);
      fTabFolder.setBackground(new Color[]{
            display.getSystemColor(SWT.COLOR_TITLE_BACKGROUND_GRADIENT),
            display.getSystemColor(SWT.COLOR_WHITE)}, 
            new int[]{100}, true);
      fTabFolder.setSelectionBackground(new Color[]{
            display.getSystemColor(SWT.COLOR_WHITE),
            display.getSystemColor(SWT.COLOR_WHITE)}, 
            new int[]{100}, true);
//      fTabFolder.addListener(SWT.Selection, new Listener() {
//         
//         @Override
//         public void handleEvent(Event event) {
//            Widget item = event.item;
//            item.
//            Control c = fTabFolder.getSelection().getControl();
//            if (c instanceof Tree) {
//               Tree tree = (Tree) c;
//               TreeItem item = tree.getItem(0);
//               fPeripheralPageModel.updatePage();
//               System.err.println("Selection event" + fTabFolder.getSelectionIndex() + ", c = " + c);
//            }
//         }
//      });
      return fTabFolder;
   }

   public Control getControl() {
      return fTabFolder;
   }

   public void setModel(BaseModel model) {
      if (fPeripheralPageModel == model) {
         return;
      }
      fPeripheralPageModel = (TabModel) model;
      fTabFolder.setToolTipText(fPeripheralPageModel.getToolTip());
      for (CTabItem c:fTabFolder.getItems()) {
         c.dispose();
      }
      ArrayList<Object> children = fPeripheralPageModel.getChildren();
      if ((children.size() == 1) && (children.get(0) instanceof TreeViewModel)) {
      }
      for (Object child:fPeripheralPageModel.getChildren()) {
         BaseModel pageModel = (BaseModel) child;
         CTabItem tabItem = new CTabItem(fTabFolder, SWT.NONE);
         tabItem.setText(pageModel.getName());
         tabItem.setToolTipText(pageModel.getToolTip()); 
         if (pageModel instanceof TreeViewModel) {
            TreeEditor treeEditor = new TreeEditor() {
               @Override
               protected TreeColumnInformation[] getColumnInformation(TreeViewer viewer) {
                  final TreeColumnInformation[] fColumnInformation = {
                        new TreeColumnInformation("Property",        200, new NameColumnLabelProvider(),           null),
                        new TreeColumnInformation("Value",           300, new ValueColumnLabelProvider(),          new ValueColumnEditingSupport(viewer)),
                        new TreeColumnInformation("Code Identifier", 120, new CodeIdentifierColumnLabelProvider(), new CodeIdentifierColumnEditingSupport(viewer)),
                        new TreeColumnInformation("Description",     500, new DescriptionColumnLabelProvider(),    new DescriptionColumnEditingSupport(viewer)),
                  };
                  return fColumnInformation;
               }
            };
            tabItem.setControl(treeEditor.createControl(fTabFolder));
            treeEditor.setModel((TreeViewModel) pageModel);
         }
         else if (pageModel instanceof SectionModel) {
            SectionEditor sectionEditor = new SectionEditor();
            tabItem.setControl(sectionEditor.createControl(fTabFolder));
            sectionEditor.setModel(pageModel);
         }
         else {
            System.err.println("other");
         }
      }
      fTabFolder.setSelection(0);
   }

   @Override
   public void refresh() {
      
   }

   public Control createControl(Composite parent) {
      return createControl(parent, SWT.NONE);
   }
}
