package net.sourceforge.usbdm.deviceEditor.model;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;

/**
 * Model describing the Device
 */
public final class DeviceInformationModel extends TreeViewModel implements IPage {

   static final private String[] PACKAGE_COLUMN_LABELS = {"Name", "Value", "Description"};

   /**
    * Constructor
    * 
    * @param columnLabels  Labels to use for columns
    * @param title 
    * @param toolTip 
    */
   public DeviceInformationModel(BaseModel parent, DeviceInfo deviceInfo) {
      super(parent, "Project", "Project Settings", PACKAGE_COLUMN_LABELS);

      new ConstantModel(this, "Device", "", deviceInfo.getDeviceName());
      new ConstantModel(this, "Hardware File", "", deviceInfo.getSourceFilename());
      new DeviceVariantModel(this, deviceInfo);
      new DevicePackageModel(this, deviceInfo);
   }

   @Override
   public IEditorPage createEditorPage() {
      return new TreeEditorPage();
   }

   @Override
   public String getPageName() {
      return getName();
   }

   @Override
   public void updatePage() {
      update();
   }

   @Override
   public TreeViewModel getModel() {
      return this;
   }

}
