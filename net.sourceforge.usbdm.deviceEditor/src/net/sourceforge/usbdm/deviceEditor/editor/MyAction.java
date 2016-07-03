package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;

import net.sourceforge.usbdm.deviceEditor.Activator;

/** 
 * Convenience wrapper for Action 
 */
class MyAction extends Action {

   /**
    * 
    * @param text       Text for button/menu
    * @param toolTip    Tool tip
    * @param style      Style (passed to Action)
    * @param imageId    Image ID for icon
    */
   MyAction(String text, String toolTip, int style, String imageId) {
      super(text, style);

      setText(text);
      setToolTipText(toolTip);
      if ((imageId != null) && (Activator.getDefault() != null)) {
         ImageDescriptor imageDescriptor = Activator.getDefault().getImageDescriptor(imageId);
         setImageDescriptor(imageDescriptor);
      }
   }

   MyAction(String text,int style, String imageId) {
      this(text, text, style, imageId);
   }

   MyAction(String text,int style) {
      this(text, text, style, null);
   }
}