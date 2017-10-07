package net.sourceforge.usbdm.deviceEditor.model;

public interface IPage {
   
   /**
    * Create default editor page to edit the model
    * 
    * @return
    */
   public abstract IEditorPage createEditorPage();

   public abstract String getName();

   public abstract String getToolTip();

   public abstract void updatePage();

   public abstract BaseModel getModel();
}
