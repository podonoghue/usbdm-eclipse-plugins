package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import net.sourceforge.usbdm.deviceEditor.editor.PeripheralParametersEditor;

public final class PeripheralConfigurationModel extends TreeViewModel {

   static final private String[] OTHER_COLUMN_LABELS = {"Peripheral.Parameter", "Value", "Description"};

   public PeripheralConfigurationModel(ModelFactory factory, BaseModel parent, String title, String toolTip) {
      super(OTHER_COLUMN_LABELS, title, toolTip);
   }

   @Override
   protected void removeMyListeners() {
   }

   @Override
   public EditorPage createEditorPage() {
      return new EditorPage() {

         private PeripheralParametersEditor fEditor = null;

         @Override
         public Control createComposite(Composite parent) {
            if (fEditor == null) {
               fEditor = new PeripheralParametersEditor(parent);
            }
            return fEditor.getControl();
         }

         @Override
         public void update(PeripheralPageModel peripheralPageModel) {
            fEditor.setModel((TreeViewModel) peripheralPageModel);
         }
      };
   }
}
