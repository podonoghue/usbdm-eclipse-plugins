package net.sourceforge.usbdm.deviceEditor.model;

public abstract class PeripheralPageModel extends BaseModel {
   /**
    * Create model
    * 
    * @param parent        Parent model
    * @param name          Display name
    * @param description   Display description
    * 
    * @note Added as child of parent if not null
    */
   public PeripheralPageModel(BaseModel parent, String name, String description) {
      super(parent, name, description);
   }

   /**
    * Create default editor page to edit the model
    * 
    * @return
    */
   public abstract EditorPage createEditorPage();
}
