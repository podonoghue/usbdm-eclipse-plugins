package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import net.sourceforge.usbdm.deviceEditor.editor.ImageCanvas;
import net.sourceforge.usbdm.deviceEditor.information.DeviceVariantInformation;
import net.sourceforge.usbdm.jni.Usbdm;

public class PackageImageModel extends PeripheralPageModel implements EditorPage {

   private ImageCanvas        fImageCanvas = null;
   private PackageImageModel  fPackageImageModel;
   private final ModelFactory fModelFactory;

   public PackageImageModel(ModelFactory modelFactory) {
      super(null, "Package Image", "");
      fModelFactory = modelFactory;
   }

   @Override
   protected void removeMyListeners() {
   }

   @Override
   public Control createComposite(Composite parent) {
      fImageCanvas = new ImageCanvas(parent, fPackageImageModel);
      return fImageCanvas;
   }

   @Override
   public void update(PeripheralPageModel model) {
      if (model != this) {
         // Only supports updating the image - not the entire model
         throw new RuntimeException("Model differs from this!");
      }
      fImageCanvas.setImage(createImage());
   }

   /**
    * Creates the image representing the package
    * 
    * @return Image created (transfers ownership!)
    */
   private Image createImage() {
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
   public EditorPage createEditorPage() {
      return this;
   }
}
