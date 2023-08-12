package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Tree;

import net.sourceforge.usbdm.deviceEditor.information.PeripheralSignalsVariable;
import net.sourceforge.usbdm.deviceEditor.peripherals.Peripheral;

/**
 * Model representing a peripheral and associated signals
 * 
 * <pre>
 *    PeripheralModel<br>
 *        +-----Signal Model
 *        +-----Signal Model
 * </pre>
 */
public final class PeripheralSignalsModel extends VariableModel implements IModelChangeListener {

   private Peripheral fPeripheral;
   
   /**
    * Constructs model for peripheral and associated signals
    * 
    * <pre>
    *    PeripheralModel<br>
    *        +-----Signal Model
    *        +-----Signal Model
    * </pre>
    * 
    * @param parent        Parent
    * @param peripheral    Peripheral used to locate signals model
    */
   public PeripheralSignalsModel(BaseModel parent, PeripheralSignalsVariable var) {
      super(parent, var);
      
      fPeripheral = (Peripheral) var.getProvider();
      fPeripheral.createSignalModels(this);
      watchChildren();
   }
   
   /**
    * Add listeners to watch changes in children(signals)
    */
   public void watchChildren() {
      if (fChildren == null) {
         return;
      }
      for (Object child : fChildren) {
         if (child instanceof SignalModel ) {
            SignalModel s = (SignalModel)child;
            s.getSignal().addListener(this);
         }
      }
   }
   
   /**
    * Get peripheral associated with this model
    * 
    * @return
    */
   public Peripheral getPeripheral() {
      return fPeripheral;
   }

   @Override
   public String getSimpleDescription() {
      String description = fPeripheral.getUserDescription();
      if ((description == null) || description.isBlank()) {
         description = fPeripheral.getDescription();
      }
      return description;
   }

   @Override
   public void modelElementChanged(ObservableModel observableModel) {
      setStatus(fPeripheral.getStatus());
      update();
   }

   @Override
   public void modelStructureChanged(ObservableModel observableModel) {
   }

   @Override
   public void elementStatusChanged(ObservableModel observableModel) {
   }

   @Override
   public void setValueAsString(String value) {
      // No data
   }

   @Override
   public CellEditor createCellEditor(Tree tree) {
      // TODO Auto-generated method stub
      return null;
   }
}
