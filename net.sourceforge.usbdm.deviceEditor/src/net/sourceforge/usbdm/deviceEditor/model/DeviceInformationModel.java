package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import net.sourceforge.usbdm.deviceEditor.editor.DescriptionColumnEditingSupport;
import net.sourceforge.usbdm.deviceEditor.editor.DescriptionColumnLabelProvider;
import net.sourceforge.usbdm.deviceEditor.editor.NameColumnLabelProvider;
import net.sourceforge.usbdm.deviceEditor.editor.TreeEditor;
import net.sourceforge.usbdm.deviceEditor.editor.ValueColumnEditingSupport;
import net.sourceforge.usbdm.deviceEditor.editor.ValueColumnLabelProvider;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;

/**
 * Model describing the Device
 */
public final class DeviceInformationModel extends TreeViewModel implements IPage {

   /**
    * Constructor
    * 
    * @param columnLabels  Labels to use for columns
    * @param title 
    * @param toolTip 
    */
   public DeviceInformationModel(BaseModel parent, DeviceInfo deviceInfo) {
      super(parent, "Project", "Project Settings");

      new ConstantModel(this, "Device", "", deviceInfo.getDeviceName());
      new ConstantModel(this, "Hardware File", "", deviceInfo.getSourceFilename());
      new DeviceVariantModel(this, deviceInfo);
      new DevicePackageModel(this, deviceInfo);
   }

   @Override
   public IEditorPage createEditorPage() {
      return new TreeEditorPage() {
         @Override
         public Control createComposite(Composite parent) {
            if (getEditor() == null) {
               setEditor(new TreeEditor() {
                  @Override
                  protected TreeColumnInformation[] getColumnInformation(TreeViewer viewer) {
                     final TreeColumnInformation[] fColumnInformation = {
                           new TreeColumnInformation("Property Name", 300, new NameColumnLabelProvider(),        null),
                           new TreeColumnInformation("Value",         400, new ValueColumnLabelProvider(),       new ValueColumnEditingSupport(viewer)),
                           new TreeColumnInformation("Description",   500, new DescriptionColumnLabelProvider(), new DescriptionColumnEditingSupport(viewer)),
                     };
                     return fColumnInformation;
                  }
               });
            }
            return getEditor().createControl(parent);
         }
      };
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
