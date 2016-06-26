package net.sourceforge.usbdm.deviceEditor.model;

public class FilePathModel extends EditableModel {

   /**  Model factory associated with this model */
   ModelFactory fFactory;
   
   /**
    * Constructor
    * 
    * @param parent        Parent model
    * @param modelFactory  Model factory associated with this model
    */
   public FilePathModel(BaseModel parent, ModelFactory modelFactory) {
      super(parent, "Hardware", "Path to hardware description");
      
      fFactory = modelFactory;
   }

   @Override
   public void setValueAsString(String value) {
      fFactory.setHardwareFile(value);
   }
   
   @Override
   public String getValueAsString() {
      return fFactory.getDeviceInfo().getSourceFilename();
   }

   @Override
   protected void removeMyListeners() {
   }

}
