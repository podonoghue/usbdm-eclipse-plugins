package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import net.sourceforge.usbdm.deviceEditor.editor.CodeIdentifierColumnEditingSupport;
import net.sourceforge.usbdm.deviceEditor.editor.CodeIdentifierColumnLabelProvider;
import net.sourceforge.usbdm.deviceEditor.editor.DescriptionColumnEditingSupport;
import net.sourceforge.usbdm.deviceEditor.editor.DescriptionColumnLabelProvider;
import net.sourceforge.usbdm.deviceEditor.editor.NameColumnLabelProvider;
import net.sourceforge.usbdm.deviceEditor.editor.TreeEditor;
import net.sourceforge.usbdm.deviceEditor.editor.ValueColumnEditingSupport;
import net.sourceforge.usbdm.deviceEditor.editor.ValueColumnLabelProvider;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.peripherals.Peripheral;

/**
 * Model describing the device pins organised by pin category
 */
public final class DevicePinsModel extends TreeViewModel implements IPage {

   /**
    * Constructor
    * @param parent 
    * 
    * @param columnLabels  Labels to use for columns
    * @param title 
    * @param toolTip 
    */
   public DevicePinsModel(BaseModel parent, DeviceInfo fDeviceInfo) {
      super(parent, "Peripheral View", "Pin mapping organized by peripheral");

      for (String pName:fDeviceInfo.getPeripherals().keySet()) {
         Peripheral peripheral = fDeviceInfo.getPeripherals().get(pName);
         if (peripheral.hasMappableSignals()) {
            new PeripheralModel(this, peripheral);
         }
      }
   }
   @Override
   public String getValueAsString() {
      return "";
   }

   @Override
   protected void removeMyListeners() {
   }

   @Override
   public IEditorPage createEditorPage() {
      return new TreeEditorPage() {
         @Override
         public Control createComposite(Composite parent) {
            if (getEditor() == null) {
               setEditor(new TreeEditor() {
                  @Override
                  protected TreeColumnInformation[] getColumnInformation(TreeViewer viewer) {
                     final TreeColumnInformation[] fColumnInformation = {
                           new TreeColumnInformation("Peripheral.Signal", 200, new NameColumnLabelProvider(),        null),
                           new TreeColumnInformation("Mux:Pin",           200, new ValueColumnLabelProvider(),       new ValueColumnEditingSupport(viewer)),
                           new TreeColumnInformation("Code Identifier",   120, new CodeIdentifierColumnLabelProvider(), new CodeIdentifierColumnEditingSupport(viewer)),
                           new TreeColumnInformation("Description",       600, new DescriptionColumnLabelProvider(), new DescriptionColumnEditingSupport(viewer)),
                     };
                     return fColumnInformation;
                  }
               });
            }
            return getEditor().createControl(parent);
         }
      };
   }

   @Override
   public void updatePage() {
      update();
   }
   
   @Override
   public BaseModel getModel() {
      return this;
   }
}
