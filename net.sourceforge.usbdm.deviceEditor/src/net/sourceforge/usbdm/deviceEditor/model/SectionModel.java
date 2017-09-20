package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import net.sourceforge.usbdm.deviceEditor.editor.SectionEditor;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

public class SectionModel extends BaseModel implements IPage {

   public SectionModel(BaseModel parent, String title, String toolTip) {
      super(parent, title, toolTip);
   }

   @Override
   protected void removeMyListeners() {
   }

   @Override
   public IEditorPage createEditorPage() {
      return new IEditorPage() {

         private SectionEditor fEditor = null;

         @Override
         public Control createComposite(Composite parent) {
            if (fEditor == null) {
               fEditor = new SectionEditor();
            }
            return fEditor.createControl(parent);
         }

         @Override
         public void update(IPage peripheralPageModel) {
            fEditor.setModel((TabModel) peripheralPageModel);
         }
      };
   }

   @Override
   public String getPageName() {
      return getName();
   }

   @Override
   public void updatePage() {
   }

   @Override
   public BaseModel getModel() {
      return this;
   }

   @Override
   public SectionModel clone(BaseModel parentModel, VariableProvider provider, int index) throws CloneNotSupportedException {
      return (SectionModel) super.clone(parentModel, provider, index);
   }
   
   
}
