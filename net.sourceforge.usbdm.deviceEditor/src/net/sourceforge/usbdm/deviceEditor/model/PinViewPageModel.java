package net.sourceforge.usbdm.deviceEditor.model;

import java.util.ArrayList;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import net.sourceforge.usbdm.deviceEditor.editor.CodeIdentifierColumnEditingSupport;
import net.sourceforge.usbdm.deviceEditor.editor.CodeIdentifierColumnLabelProvider;
import net.sourceforge.usbdm.deviceEditor.editor.DescriptionColumnEditingSupport;
import net.sourceforge.usbdm.deviceEditor.editor.DescriptionColumnLabelProvider;
import net.sourceforge.usbdm.deviceEditor.editor.NameColumnLabelProvider;
import net.sourceforge.usbdm.deviceEditor.editor.PinBooleanColumnLabelProvider;
import net.sourceforge.usbdm.deviceEditor.editor.PinBooleanEditingSupport;
import net.sourceforge.usbdm.deviceEditor.editor.PinInterruptDmaColumnLabelProvider;
import net.sourceforge.usbdm.deviceEditor.editor.PinInterruptDmaEditingSupport;
import net.sourceforge.usbdm.deviceEditor.editor.PinPullColumnLabelProvider;
import net.sourceforge.usbdm.deviceEditor.editor.PinPullEditingSupport;
import net.sourceforge.usbdm.deviceEditor.editor.TreeEditor;
import net.sourceforge.usbdm.deviceEditor.editor.ValueColumnEditingSupport;
import net.sourceforge.usbdm.deviceEditor.editor.ValueColumnLabelProvider;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.model.ModelFactory.PinCategory;

/**
 * Model representing pins organised into categories (PORTA, Power, Misc.)
 * <pre>
 * Device Signals Model
 *    +--- Category Model ...
 *             +----Pin Model ...
 * </pre> 
 */
public final class PinViewPageModel extends TreeViewModel implements IPage {

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
    * Constructs model representing pins organised into categories (PORTA, Power, Misc.)
    * <pre>
    * Device Signals Model
    *    +--- Category Model ...
    *             +----Pin Model ...
    * </pre> 
    * @param parent        Owning model
    * @param fDeviceInfo   Device information needed to create models
    */
   public PinViewPageModel(BaseModel parent, DeviceInfo deviceInfo) {
      super(parent, "Pin View", "Pin mapping organized by pin");
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
      return new TreeEditorPage() {
         @Override
         public Control createComposite(Composite parent) {
            if (getEditor() == null) {
               setEditor(new TreeEditor() {
                  @Override
                  protected TreeColumnInformation[] getColumnInformation(TreeViewer viewer) {
                     final TreeColumnInformation[] fColumnInformation = {
                           new TreeColumnInformation("Category.Pin",        150, new NameColumnLabelProvider(),               null),
                           new TreeColumnInformation("Code Identifier",     200, new CodeIdentifierColumnLabelProvider(),     new CodeIdentifierColumnEditingSupport(viewer)),
                           new TreeColumnInformation("Pin Use Description", 200, new DescriptionColumnLabelProvider(),        new DescriptionColumnEditingSupport(viewer)),
                           new TreeColumnInformation("Mux:Signals",         180, new ValueColumnLabelProvider(),              new ValueColumnEditingSupport(viewer)),
                           new TreeColumnInformation("Interrupt/DMA",       120, new PinInterruptDmaColumnLabelProvider(),    new PinInterruptDmaEditingSupport(viewer)),
                           new TreeColumnInformation("LK",                   40, PinBooleanColumnLabelProvider.getLk(),       PinBooleanEditingSupport.getLk(viewer)),
                           new TreeColumnInformation("DSE",                  40, PinBooleanColumnLabelProvider.getDse(),      PinBooleanEditingSupport.getDse(viewer)),
                           new TreeColumnInformation("ODE",                  40, PinBooleanColumnLabelProvider.getOde(),      PinBooleanEditingSupport.getOde(viewer)),
                           new TreeColumnInformation("PFE",                  40, PinBooleanColumnLabelProvider.getPfe(),      PinBooleanEditingSupport.getPfe(viewer)),
                           new TreeColumnInformation("SRE",                  40, PinBooleanColumnLabelProvider.getSre(),      PinBooleanEditingSupport.getSre(viewer)),
                           new TreeColumnInformation("Pull",                 60, new PinPullColumnLabelProvider(),            new PinPullEditingSupport(viewer)),
                     };
                     return fColumnInformation;
                  }
               });
            }
            return getEditor().createControl(parent);
         }
      };
   }

   /**
    * Create model representing pins organised into categories (PORTA, Power, Misc.)
    * <pre>
    * Device Signals Model
    *    +--- Category Model ...
    *             +----Pin Model ...
    * </pre>             
    * @param fDeviceInfo   Device information needed to create models
    */
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
      // Construct the models within model categories (discarding empty categories)
      for (PinCategory pinCategory:categories) {
         if (pinCategory.getPins().isEmpty()) {
            continue;
         }
         // Construct category
         CategoryModel categoryModel = new CategoryModel(this, pinCategory.getName());
         
         // Populate category with pin models
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
   public void updatePage() {
      update();
   }

   @Override
   public BaseModel getModel() {
      return this;
   }
}
