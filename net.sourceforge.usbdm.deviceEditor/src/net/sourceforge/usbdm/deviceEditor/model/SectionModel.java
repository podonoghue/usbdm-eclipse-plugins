package net.sourceforge.usbdm.deviceEditor.model;

import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

public class SectionModel extends BaseModel {

   public SectionModel(BaseModel parent, String title, String toolTip) {
      super(parent, title);
      setToolTip(toolTip);
   }

   @Override
   protected void removeMyListeners() {
   }

//   @Override
//   public IEditorPage createEditorPage() {
//      return new IEditorPage() {
//
//         private SectionEditor fEditor = null;
//
//         @Override
//         public Control createComposite(Composite parent) {
//            if (fEditor == null) {
//               fEditor = new SectionEditor();
//            }
//            return fEditor.createControl(parent);
//         }
//
//         @Override
//         public void update(IPage peripheralPageModel) {
//            fEditor.setModel((TabModel) peripheralPageModel);
//         }
//      };
//   }
//
//   @Override
//   public void updatePage() {
//   }

   @Override
   public SectionModel clone(BaseModel parentModel, VariableProvider provider, int index) throws CloneNotSupportedException {
      return (SectionModel) super.clone(parentModel, provider, index);
   }
   
   
}
