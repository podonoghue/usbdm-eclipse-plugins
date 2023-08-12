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

   @Override
   public Control getControl() {
      return fTabFolder;
   }

   @Override
   public void setModel(BaseModel model) {
      if (fPeripheralPageModel == model) {
         return;
      }
      fPeripheralPageModel = (TabModel) model;
      fTabFolder.setToolTipText(fPeripheralPageModel.getToolTip());
      for (CTabItem c:fTabFolder.getItems()) {
         c.dispose();
      }
      ArrayList<BaseModel> children = fPeripheralPageModel.getChildren();
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
                        new TreeColumnInformation("Property",        250, new NameColumnLabelProvider(),           null,
                              "Name of property"),
                        new TreeColumnInformation("Value",           300, new ValueColumnLabelProvider(),          new ValueColumnEditingSupport(viewer),
                              "Value of property"),
                        new TreeColumnInformation("Code Identifier", 200, new CodeIdentifierColumnLabelProvider(), new CodeIdentifierColumnEditingSupport(viewer),
                              "C Identifier for code generation\n"+
                              "If not blank code will be generated for the signal or peripheral"),
                        new TreeColumnInformation("Modifier",        100, new ModifierColumnLabelProvider(),       new ModifierEditingSupport(viewer),
                              ModifierColumnLabelProvider.getColumnToolTipText()),
                        new TreeColumnInformation("Instance",         80,  new InstanceColumnLabelProvider(),      new InstanceEditingSupport(viewer),
                              InstanceColumnLabelProvider.getColumnToolTipText()),
                        new TreeColumnInformation("Description",     500, new DescriptionColumnLabelProvider(),    new DescriptionColumnEditingSupport(viewer),
                              DescriptionColumnLabelProvider.getColumnToolTipText()),
                        new TreeColumnInformation("Interrupt/DMA",       120, new PinInterruptDmaColumnLabelProvider(),    new PinInterruptDmaEditingSupport(viewer),
                              PinInterruptDmaColumnLabelProvider.getColumnToolTipText()),
                        new TreeColumnInformation("LK",                   40, PinBooleanColumnLabelProvider.getLk(),       PinBooleanEditingSupport.getLk(viewer)),
                        new TreeColumnInformation("DSE",                  40, PinBooleanColumnLabelProvider.getDse(),      PinBooleanEditingSupport.getDse(viewer)),
                        new TreeColumnInformation("ODE",                  40, PinBooleanColumnLabelProvider.getOde(),      PinBooleanEditingSupport.getOde(viewer)),
                        new TreeColumnInformation("PFE",                  40, PinBooleanColumnLabelProvider.getPfe(),      PinBooleanEditingSupport.getPfe(viewer)),
                        new TreeColumnInformation("SRE",                  40, PinBooleanColumnLabelProvider.getSre(),      PinBooleanEditingSupport.getSre(viewer)),
                        new TreeColumnInformation("Pull",                 60, new PinPullColumnLabelProvider(),            new PinPullEditingSupport(viewer),
                              PinPullColumnLabelProvider.getColumnToolTipText()),
                  };
                  return fColumnInformation;
               }
            };
            tabItem.setControl(treeEditor.createControl(fTabFolder));
            treeEditor.setModel(pageModel);
         }
         else if (pageModel instanceof SectionModel) {
            SectionEditor sectionEditor = new SectionEditor();
            tabItem.setControl(sectionEditor.createControl(fTabFolder));
            sectionEditor.setModel(pageModel);
         }
      }
      fTabFolder.setSelection(0);
   }

   @Override
   public void refresh() {
      
   }

   @Override
   public Control createControl(Composite parent) {
      return createControl(parent, SWT.NONE);
   }
}
