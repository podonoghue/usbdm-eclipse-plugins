package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import net.sourceforge.usbdm.deviceEditor.editor.BooleanColumnLabelProvider;
import net.sourceforge.usbdm.deviceEditor.editor.BooleanEditingSupport;
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
 * Model representing all peripherals along with their associated signals
 * 
 * <pre>
 * Peripheral View Page Model<br>
 *    +---- Peripheral Model...<br>
 *             +-----Signal Model...
 * </pre>
 */
public final class PeripheralViewPageModel extends TreeViewModel implements IPage {

   /**
    * Constructs model representing all peripherals along with their associated signals
    * 
    * <pre>
    * Peripheral View Page Model<br>
    *    +---- Peripheral Model...<br>
    *             +-----Signal Model...
    * </pre>
    * 
    * @param parent        Parent to attache models to
    * @param fDeviceInfo   Device to obtain information from
    */
   public PeripheralViewPageModel(BaseModel parent, DeviceInfo fDeviceInfo) {
      super(parent, "Peripheral View", "Pin mapping organized by peripheral");

      for (String pName:fDeviceInfo.getPeripherals().keySet()) {
         Peripheral peripheral = fDeviceInfo.getPeripherals().get(pName);
         if (peripheral.hasMappableSignals()) {
            peripheral.createPeripheralSignalsModel(this);
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
                           new TreeColumnInformation("Peripheral.Signal", 200, new NameColumnLabelProvider(),              null),
                           new TreeColumnInformation("Mux:Pin",           200, new ValueColumnLabelProvider(),             new ValueColumnEditingSupport(viewer)),
                           new TreeColumnInformation("Code Identifier",   200, new CodeIdentifierColumnLabelProvider(),    new CodeIdentifierColumnEditingSupport(viewer)),
                           new TreeColumnInformation("Polarity",          100, BooleanColumnLabelProvider.getPolarity(),   BooleanEditingSupport.getPolarity(viewer)),
                           new TreeColumnInformation("Description",       600, new DescriptionColumnLabelProvider(),       new DescriptionColumnEditingSupport(viewer)),
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
