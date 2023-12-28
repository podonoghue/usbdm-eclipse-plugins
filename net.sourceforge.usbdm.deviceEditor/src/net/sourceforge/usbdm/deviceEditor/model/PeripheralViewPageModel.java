package net.sourceforge.usbdm.deviceEditor.model;

import java.util.Arrays;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import net.sourceforge.usbdm.deviceEditor.editor.CodeIdentifierColumnEditingSupport;
import net.sourceforge.usbdm.deviceEditor.editor.CodeIdentifierColumnLabelProvider;
import net.sourceforge.usbdm.deviceEditor.editor.DescriptionColumnEditingSupport;
import net.sourceforge.usbdm.deviceEditor.editor.DescriptionColumnLabelProvider;
import net.sourceforge.usbdm.deviceEditor.editor.InstanceColumnLabelProvider;
import net.sourceforge.usbdm.deviceEditor.editor.InstanceEditingSupport;
import net.sourceforge.usbdm.deviceEditor.editor.ModifierColumnLabelProvider;
import net.sourceforge.usbdm.deviceEditor.editor.ModifierEditingSupport;
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
import net.sourceforge.usbdm.deviceEditor.peripherals.Peripheral;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Model representing all peripherals along with their associated signals
 * 
 * <pre>
 * Peripheral View Page Model<br>
 *    +---- Peripheral Model...<br>
 *             +-----Signal Model...
 * </pre>
 */
public final class PeripheralViewPageModel extends TreeViewModel implements IPage {

   private final boolean fHasPCR;
   
   /**
    * Constructs model representing all peripherals along with their associated signals
    * 
    * <pre>
    * Peripheral View Page Model<br>
    *    +---- Peripheral Model...<br>
    *             +-----Signal Model...
    * </pre>
    * 
    * @param parent       Parent to attache models to
    * @param deviceInfo   Device to obtain information from
    */
   public PeripheralViewPageModel(BaseModel parent, DeviceInfo deviceInfo) {
      super(parent, "Peripheral View", "Pin mapping organized by peripheral");
      
      fHasPCR = deviceInfo.safeGetVariable("/PCR/_present") != null;

      for (String pName:deviceInfo.getPeripherals().keySet()) {
         Peripheral peripheral = deviceInfo.getPeripherals().get(pName);
         if (peripheral instanceof PeripheralWithState) {
            if (peripheral.hasMappableSignals()) {
               ((PeripheralWithState)peripheral).createPeripheralSignalsModel(this);
            }
         }
      }
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
                     final TreeColumnInformation[] columnInformation1 = {
                           new TreeColumnInformation("Peripherals and Signals", 160, new NameColumnLabelProvider(),              null,
                                 "Signals grouped by peripheral"),
                           new TreeColumnInformation("Mux:Pin",                 120, new ValueColumnLabelProvider(),             new ValueColumnEditingSupport(viewer),
                                 "Mapping of peripheral signal\n"+
                                 "Blue bold text is used if multiple incompatible signals are mapped to a pin"),
                           new TreeColumnInformation("Code Identifier",         120, new CodeIdentifierColumnLabelProvider(),    new CodeIdentifierColumnEditingSupport(viewer),
                                 CodeIdentifierColumnLabelProvider.getColumnToolTipText()),
                           new TreeColumnInformation("Modifier",                100, new ModifierColumnLabelProvider(),          new ModifierEditingSupport(viewer),
                                 ModifierColumnLabelProvider.getColumnToolTipText()),
                           new TreeColumnInformation("Instance",                 80, new InstanceColumnLabelProvider(),          new InstanceEditingSupport(viewer),
                                 InstanceColumnLabelProvider.getColumnToolTipText()),
                           new TreeColumnInformation("Description",             600, new DescriptionColumnLabelProvider(),        new DescriptionColumnEditingSupport(viewer),
                                 DescriptionColumnLabelProvider.getColumnToolTipText()),
                     };
                     final TreeColumnInformation[] columnInformation2 = {
                           new TreeColumnInformation("Interrupt/DMA",           115, new PinInterruptDmaColumnLabelProvider(),    new PinInterruptDmaEditingSupport(viewer),
                                 PinInterruptDmaColumnLabelProvider.getColumnToolTipText()),
                           new TreeColumnInformation("LK",                       35, PinBooleanColumnLabelProvider.getLk(),       PinBooleanEditingSupport.getLk(viewer),
                                 "Lock PCR register after 1st write"),
                           new TreeColumnInformation("DSE",                      40, PinBooleanColumnLabelProvider.getDse(),      PinBooleanEditingSupport.getDse(viewer),
                                 "High drive strength enable"),
                           new TreeColumnInformation("ODE",                      40, PinBooleanColumnLabelProvider.getOde(),      PinBooleanEditingSupport.getOde(viewer),
                                 "Open Drain enable"),
                           new TreeColumnInformation("PFE",                      40, PinBooleanColumnLabelProvider.getPfe(),      PinBooleanEditingSupport.getPfe(viewer),
                                 "Pin filter enable"),
                           new TreeColumnInformation("SRE",                      40, PinBooleanColumnLabelProvider.getSre(),      PinBooleanEditingSupport.getSre(viewer),
                                 "Slew rate limit enable"),
                           new TreeColumnInformation("Pull",                     45, new PinPullColumnLabelProvider(),            new PinPullEditingSupport(viewer),
                                 PinPullColumnLabelProvider.getColumnToolTipText()),
                     };
                     if (fHasPCR) {
                        TreeColumnInformation[] res = Arrays.copyOf(columnInformation1, columnInformation1.length+columnInformation2.length);
                        System.arraycopy(columnInformation2, 0, res, columnInformation1.length, columnInformation2.length);
                        return res;
                     }
                     else {
                        return columnInformation1;
                     }
                  }
               });
            }
            return getEditor().createControl(parent);
         }
      };
   }

   @Override
   public void updatePage() {
      update(null);
   }
   
   @Override
   public BaseModel getModel() {
      return this;
   }
}
