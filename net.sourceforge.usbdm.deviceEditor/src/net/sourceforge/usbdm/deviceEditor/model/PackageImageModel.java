package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import net.sourceforge.usbdm.deviceEditor.information.DeviceVariantInformation;
import net.sourceforge.usbdm.jni.Usbdm;

public class PackageImageModel extends RootModel {
   
   public PackageImageModel(ModelFactory modelFactory) {
      super(modelFactory, null, "Device Image", "");
   }

   /**
    * Creates the image representing the package
    * 
    * @return Image created (transfers ownership!)
    */
   public Image createImage() {
      try {
         DeviceVariantInformation deviceVariant = fModelFactory.getDeviceInfo().getDeviceVariant();
         IPath path = Usbdm.getResourcePath().append("Stationery/Packages/Images/"+deviceVariant.getPackage().getName()+".png");
         return new Image(Display.getCurrent(), path.toString());
      } catch (Exception e) {
         System.err.println("Failed to load device image, reason: )"+e.getMessage());
         return null;
      }
   }

   @Override
   protected void removeMyListeners() {
   }

   public static RootModel createModel(ModelFactory modelFactory) {
      return new PackageImageModel(modelFactory);
   }
   
}
