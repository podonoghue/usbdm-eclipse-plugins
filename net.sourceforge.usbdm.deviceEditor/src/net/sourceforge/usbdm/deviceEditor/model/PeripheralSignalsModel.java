package net.sourceforge.usbdm.deviceEditor.model;

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
public final class PeripheralSignalsModel extends EditableModel implements IModelChangeListener {

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
   public PeripheralSignalsModel(BaseModel parent, Peripheral peripheral) {
      super(parent, peripheral.getName());
      
      fPeripheral = peripheral;
      fPeripheral.createSignalModels(this);
      watchChildren();
      fPeripheral.addListener(this);
   }
   
   /**
    * Add listeners to watch changes in children(signals)
    */
   void watchChildren() {
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
}