package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

/**
 * Represents the Peripheral Parameters main tab<br>
 * This will contain a sub-tab for each peripheral
 */
public class PeripheralParametersEditor extends BaseModel implements IPage {
   
   final boolean fHasPCR;
   
   /**
    * Creates the Peripheral Parameters main tab<br>
    * This will contain a sub-tab for each peripheral
    * @param hasPCR
    */
   public PeripheralParametersEditor(Boolean hasPCR) {

      super(null, "Peripheral Parameters");
      setToolTip(
            "Peripheral signal declarations\n" +
            "Interrupt handling and\ndefault settings used by defaultConfigure()");
      fHasPCR = hasPCR;
   }

   @Override
   protected void removeMyListeners() {
   }

   @Override
   public IEditorPage createEditorPage() {
      return new IEditorPage() {

         private PeripheralParametersEditorTab fEditor = null;
         
         @Override
         public Control createComposite(Composite parent) {
            if (fEditor == null) {
               fEditor = new PeripheralParametersEditorTab(fHasPCR);
            }
            return fEditor.createControl(parent);
         }

         @Override
         public void update(IPage peripheralPageModel) {
            fEditor.setModel((PeripheralParametersEditor) peripheralPageModel);
         }
      };
   }

   @Override
   public void updatePage() {
   }

   @Override
   public PeripheralParametersEditor clone(BaseModel parentModel, VariableProvider provider, int index) throws CloneNotSupportedException {
      return (PeripheralParametersEditor) super.clone(parentModel, provider, index);
   }

   @Override
   public BaseModel getModel() {
      return this;
   }
   
}
