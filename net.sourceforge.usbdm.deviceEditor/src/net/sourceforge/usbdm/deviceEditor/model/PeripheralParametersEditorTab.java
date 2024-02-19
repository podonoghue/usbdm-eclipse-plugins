package net.sourceforge.usbdm.deviceEditor.model;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import net.sourceforge.usbdm.deviceEditor.editor.CodeIdentifierColumnEditingSupport;
import net.sourceforge.usbdm.deviceEditor.editor.CodeIdentifierColumnLabelProvider;
import net.sourceforge.usbdm.deviceEditor.editor.DescriptionColumnEditingSupport;
import net.sourceforge.usbdm.deviceEditor.editor.DescriptionColumnLabelProvider;
import net.sourceforge.usbdm.deviceEditor.editor.InstanceColumnLabelProvider;
import net.sourceforge.usbdm.deviceEditor.editor.InstanceEditingSupport;
import net.sourceforge.usbdm.deviceEditor.editor.ModifierColumnLabelProvider;
import net.sourceforge.usbdm.deviceEditor.editor.ModifierEditingSupport;
import net.sourceforge.usbdm.deviceEditor.editor.NameColumnLabelProvider;
import net.sourceforge.usbdm.deviceEditor.editor.PinBooleanColumnLabelProvider;
import net.sourceforge.usbdm.deviceEditor.editor.PinBooleanEditingSupport;
import net.sourceforge.usbdm.deviceEditor.editor.PinInterruptDmaColumnLabelProvider;
import net.sourceforge.usbdm.deviceEditor.editor.PinInterruptDmaEditingSupport;
import net.sourceforge.usbdm.deviceEditor.editor.PinPullColumnLabelProvider;
import net.sourceforge.usbdm.deviceEditor.editor.PinPullEditingSupport;
import net.sourceforge.usbdm.deviceEditor.editor.TreeEditor;
import net.sourceforge.usbdm.deviceEditor.editor.ValueColumnEditingSupport;
import net.sourceforge.usbdm.deviceEditor.editor.ValueColumnLabelProvider;

/**
 * Represents a editor tab containing information for a peripheral
 */
public class PeripheralParametersEditorTab implements IEditor {
   
   private CTabFolder  fTabFolder           = null;
   private PeripheralParametersEditor    fPeripheralPageModel = null;
   private final boolean fHasPCR;

   public PeripheralParametersEditorTab(boolean hasPCR) {
      fHasPCR = hasPCR;
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
      fPeripheralPageModel = (PeripheralParametersEditor) model;
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
                  final TreeColumnInformation[] columnInformation1 = {
                        new TreeColumnInformation("Description",     300, new DescriptionColumnLabelProvider(),    new DescriptionColumnEditingSupport(viewer),
                              DescriptionColumnLabelProvider.getColumnToolTipText()),
                        new TreeColumnInformation("Value",           300, new ValueColumnLabelProvider(),          new ValueColumnEditingSupport(viewer),
                              "Value of property"),
                        new TreeColumnInformation("Code Identifier", 160, new CodeIdentifierColumnLabelProvider(), new CodeIdentifierColumnEditingSupport(viewer), ""),
                        new TreeColumnInformation("Modifier",        100, new ModifierColumnLabelProvider(),       new ModifierEditingSupport(viewer),
                              ModifierColumnLabelProvider.getColumnToolTipText()),
                        new TreeColumnInformation("Instance",         80,  new InstanceColumnLabelProvider(),      new InstanceEditingSupport(viewer),
                              InstanceColumnLabelProvider.getColumnToolTipText()),
                        new TreeColumnInformation("Property",        250, new NameColumnLabelProvider(),           null,
                              "Name of property"),
                  };
                  
                  final TreeColumnInformation[] columnInformation2 = {
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
                  if (fHasPCR) {
                     TreeColumnInformation[] res = Arrays.copyOf(columnInformation1, columnInformation1.length+columnInformation2.length);
                     System.arraycopy(columnInformation2, 0, res, columnInformation1.length, columnInformation2.length);
                     return res;
                  }
                  else {
                     return columnInformation1;
                  }
               }
            };
            tabItem.setControl(treeEditor.createControl(fTabFolder));
            treeEditor.setModel(pageModel);
            treeEditor.getViewer().addFilter(new MyViewFilter());
         }
         else {
            System.err.println("Unexpected page mode type");
         }
//         else if (pageModel instanceof SectionModel) {
//            SectionEditor sectionEditor = new SectionEditor();
//            tabItem.setControl(sectionEditor.createControl(fTabFolder));
//            sectionEditor.setModel(pageModel);
//         }
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
