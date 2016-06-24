package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import net.sourceforge.usbdm.deviceEditor.editor.PeripheralParametersEditor;

public final class PeripheralParametersModel extends PeripheralPageModel {

   static final public String[] OTHER_COLUMN_LABELS = {"Peripheral.Parameter", "Value", "Description"};

   public PeripheralParametersModel(ModelFactory factory, BaseModel parent, String title, String toolTip) {
//      super(OTHER_COLUMN_LABELS, title, toolTip);
      super(parent, title, toolTip);
   }

   @Override
   public void addChild(BaseModel model) {
      // TODO Auto-generated method stub
      super.addChild(model);
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
            fEditor.setModel((PeripheralParametersModel) peripheralPageModel);
         }
      };
   }

   public String[] getColumnLabels() {
      return OTHER_COLUMN_LABELS;
   }
}
