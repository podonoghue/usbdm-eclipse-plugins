package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Represents a page in the editor
 */
public abstract interface IEditorPage {

   /**
    * Constructs the composite representing this editor page
    * 
    * @param   parent
    * @return
    */
   public abstract Control createComposite(Composite parent);

   /**
    * Updates the Editor with changes from the model 
    * 
    * @param peripheralPageModel 
    */
   public abstract void update(IPage peripheralPageModel);
   
}
