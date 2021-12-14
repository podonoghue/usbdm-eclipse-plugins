package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.swt.graphics.Image;

import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;

public abstract class PinPropertyColumnLabelProvider extends BaseLabelProvider {

   final long fOffset;
   final long fMask;

   /**
    * Display property (field extracted from getProperties())
    * 
    * @param mask    Mask to extract field 
    * @param offset  Offset to shift after extraction
    */
   public PinPropertyColumnLabelProvider(long mask, long offset) {
      super();
      fOffset = offset;
      fMask   = mask;
   }

   /**
    * Get pin property value of object associated with model
    * 
    * @param baseModel
    * 
    * @return Value as Long or null if not available or suitable model type
    */
   Long getValue(BaseModel baseModel) {
      MappingInfo mappingInfo = PinPropertyEditingSupport.getMappingInfo(baseModel);
      if (mappingInfo == null) {
         return null;
      }
      return mappingInfo.getProperty(fMask, fOffset);
   } 

   @Override
   public Image getImage(BaseModel model) {      
      return null;
   }

}
