package net.sourceforge.usbdm.deviceEditor.model;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.model.ModelFactory.PinCategory;

/**
 * Model describing the device signals organised by peripheral
 */
public final class DeviceSignalsModel extends TreeViewModel implements IPage {

   static final private String[] PIN_COLUMN_LABELS = {"Category.Pin", "Mux:Signals", "Description"};

   /** List of all pin mapping entries to scan for mapping conflicts */
   private ArrayList<MappingInfo> fMappingInfos;

   /** Get list of all pin mapping entries to scan for mapping conflicts
    * 
    * @return List
    */
   public ArrayList<MappingInfo> getMappingInfos() {
      return fMappingInfos;
   }

   /**
    * Constructor
    * @param parent 
    * 
    * @param columnLabels  Labels to use for columns
    * @param title 
    * @param toolTip 
    */
   public DeviceSignalsModel(BaseModel parent, DeviceInfo deviceInfo) {
      super(parent, "Pin View", "Pin mapping organized by pin", PIN_COLUMN_LABELS);
      createModels(deviceInfo);
   }

   @Override
   public String getValueAsString() {
      return "";
   }

   @Override
   protected void removeMyListeners() {
   }

   @Override
   public IEditorPage createEditorPage() {
      return new TreeEditorPage();
   }

   private void createModels(DeviceInfo fDeviceInfo) {      
      
      fMappingInfos = new ArrayList<MappingInfo>();

      final ArrayList<PinCategory> categories = new ArrayList<PinCategory>();

      // Construct categories
      for (char c='A'; c<='I'; c++) {
         categories.add(new PinCategory("Port "+c, "PT"+c+".*"));
      }
      categories.add(new PinCategory("Power", "((VDD|VSS|VREGIN|VBAT|VOUT|(VREF(H|L)))).*"));
      categories.add(new PinCategory("Miscellaneous", ".*"));

      // Group pins into categories
      for (String pName:fDeviceInfo.getPins().keySet()) {
         Pin pinInformation = fDeviceInfo.getPins().get(pName);
         if (pinInformation.isAvailableInPackage()) {
            // Only add if available in package
            for (PinCategory category:categories) {
               if (category.tryAdd(pinInformation)) {
                  break;
               }
            }
         }
      }
      for (PinCategory pinCategory:categories) {
         if (pinCategory.getPins().isEmpty()) {
            continue;
         }
         CategoryModel categoryModel = new CategoryModel(this, pinCategory.getName(), "");
         for(Pin pinInformation:pinCategory.getPins()) {
            new PinModel(categoryModel, pinInformation);
            for (MappingInfo mappingInfo:pinInformation.getMappableSignals().values()) {
               if (mappingInfo.getMux() == MuxSelection.fixed) {
                  continue;
               }
               if (mappingInfo.getMux() == MuxSelection.unassigned) {
                  continue;
               }
               if (mappingInfo.getSignals().get(0) == Signal.DISABLED_SIGNAL) {
                  continue;
               }
               fMappingInfos.add(mappingInfo);
            }
         }
      }
}

   @Override
   public String getPageName() {
      return "Pin View";
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
