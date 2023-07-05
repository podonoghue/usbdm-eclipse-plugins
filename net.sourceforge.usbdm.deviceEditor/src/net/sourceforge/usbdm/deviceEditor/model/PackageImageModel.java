package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import net.sourceforge.usbdm.deviceEditor.editor.ImageCanvas;
import net.sourceforge.usbdm.deviceEditor.information.DeviceVariantInformation;
import net.sourceforge.usbdm.jni.Usbdm;

public class PackageImageModel extends BaseModel implements IEditorPage, IPage {

   private ImageCanvas        fImageCanvas = null;
   private PackageImageModel  fPackageImageModel;
   private final ModelFactory fModelFactory;
   private Image              fImage = null;

   public PackageImageModel(ModelFactory modelFactory, BaseModel parent) {
      super(parent, "Package");
      setToolTip("Image of chip or evaluation board");
      setSimpleDescription("Package Image");
      fModelFactory = modelFactory;
   }

   @Override
   public Control createComposite(Composite parent) {
      fImageCanvas = new ImageCanvas(parent, fPackageImageModel);
      return fImageCanvas;
   }

   @Override
   public void update(IPage model) {
      if (model != this) {
         // Only supports updating the image - not the entire model
         throw new RuntimeException("Model differs from this!");
      }
      if (fImage != null) {
         fImage.dispose();
      }
      fImage = createImage();
      fImageCanvas.setImage(fImage);
      
      fImageCanvas.addDisposeListener(new DisposeListener() {
         
         @Override
         public void widgetDisposed(DisposeEvent arg0) {
            if (fImage != null) {
               fImage.dispose();
               fImage = null;
            }
         }
      });
   }

   /**
    * Creates the image representing the package
    * 
    * @return Image created (transfers ownership!)
    */
   private Image createImage() {
      try {
         DeviceVariantInformation deviceVariant = fModelFactory.getDeviceInfo().getVariant();
         IPath path = Usbdm.getResourcePath().append("Stationery/Packages/Images/"+deviceVariant.getPackage().getName()+".png");
         return new Image(Display.getCurrent(), path.toString());
      } catch (Exception e) {
         System.err.println("Failed to load device image, reason: "+e.getMessage());
         return null;
      }
   }

   @Override
   public IEditorPage createEditorPage() {
      return this;
   }

   public String getPageName() {
      return "Package Image";
   }

   @Override
   public void updatePage() {
   }

   @Override
   protected void removeMyListeners() {
   }

   @Override
   public BaseModel getModel() {
      return this;
   }
}
