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
import net.sourceforge.usbdm.deviceEditor.xmlParser.ParseMenuXML.MenuData;

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

      new ConstantModel(this, "Device", "", deviceInfo.getVariantName());
      new ConstantModel(this, "Hardware File", "", deviceInfo.getSourceFilename());
      new DeviceVariantModel(this, deviceInfo);
      new DevicePackageModel(this, deviceInfo);
      MenuData menuData = deviceInfo.getData();

      BaseModel model = menuData.getRootModel();
      if (model != null) {
         model.setParent(this);
      }
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
   public void updatePage() {
      update();
   }

   @Override
   public BaseModel getModel() {
      return this;
   }
}
