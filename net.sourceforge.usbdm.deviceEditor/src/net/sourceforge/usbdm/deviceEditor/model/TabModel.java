package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import net.sourceforge.usbdm.deviceEditor.editor.TabbedEditor;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

/**
 * Represents a model containing tabs.
 * Used within a page.
 * 
 */
public class TabModel extends BaseModel implements IPage {

   /**
    * 
    * @param parent     Owning model
    * @param title      Title for tab
    * @param toolTip    Tool-tip for tab
    */
   public TabModel(BaseModel parent, String title, String toolTip) {
      super(parent, title);
      setToolTip(toolTip);
   }

   @Override
   protected void removeMyListeners() {
   }

   @Override
   public IEditorPage createEditorPage() {
      return new IEditorPage() {

         private TabbedEditor fEditor = null;
         
         @Override
         public Control createComposite(Composite parent) {
            if (fEditor == null) {
               fEditor = new TabbedEditor();
            }
            return fEditor.createControl(parent);
         }

         @Override
         public void update(IPage peripheralPageModel) {
            fEditor.setModel((TabModel) peripheralPageModel);
         }
      };
   }

   @Override
   public void updatePage() {
   }

   @Override
   public TabModel clone(BaseModel parentModel, VariableProvider provider, int index) throws CloneNotSupportedException {
      return (TabModel) super.clone(parentModel, provider, index);
   }

   @Override
   public BaseModel getModel() {
      return this;
   }
   
}
