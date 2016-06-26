package net.sourceforge.usbdm.deviceEditor.model;

public interface IPage {
   
   /**
    * Create default editor page to edit the model
    * 
    * @return
    */
   public abstract IEditorPage createEditorPage();

   public abstract String getPageName();

   public abstract void updatePage();

   public abstract void removeListeners();

   public abstract BaseModel getModel();
   
}
