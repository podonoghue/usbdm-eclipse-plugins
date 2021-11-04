package net.sourceforge.usbdm.deviceEditor.information;
import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.model.ObservableModel;

/**
 * Describes the set of peripheral signals that are mapped to a pin for a particular mux value<br>
 */
public class MappingInfo extends ObservableModel {
   
   /** Source of change */
   public static enum Origin {pin, signal};
   
   /** List of peripheral signals that are mapped  by this selection */
   private final ArrayList<Signal> fSignals = new ArrayList<Signal>();
   
   /** Pin that signals are mapped to */
   private final Pin fPin;
   
   /** Pin multiplexor setting to map these signals on the pin */
   private final MuxSelection fMuxValue;

   /** Indicates if the current mapping is selected */
   private boolean fSelected;

   private boolean fBusy = false;
   
   public boolean marked = false;
   
   /** Indicate mapping does not correspond to a pin i.e. unassigned */
   public static MappingInfo UNASSIGNED_MAPPING = new MappingInfo(Pin.UNASSIGNED_PIN, MuxSelection.unassigned);
   /**
    * Associates a peripheral signal and a pin<br>
    * 
    * @param signal     Peripheral signal
    * @param pin        Pin
    * @param muxValue   Pin multiplexor setting to select associated signal on the pin
    */
   public MappingInfo(Pin pin, MuxSelection muxValue)  {
      fPin       = pin;
      fMuxValue  = muxValue;
      addListener(pin);
   }
   
   /**
    * Get brief description e.g. "PTA1 =>  GPIOC_6/LLWU_P10 @ mux5
    * 
    * @return Description
    */
   String getDescription() {
      return String.format("%s => %s @ %s", fPin.getName(), getSignalList(), fMuxValue);
   }
   
   /**
    * Returns a list of mapped signals as a string e.g. <b><i>GPIOC_6/LLWU_P10</b></i>
    * 
    * @return List of mapped signals as string
    */
   public String getSignalList() {
      StringBuffer name = new StringBuffer();
      for (Signal signal:fSignals) {
         if (name.length() != 0) {
            name.append("/");
         }
         name.append(signal.getName());
      }
      return name.toString();
   }

   /**
    * Get list of peripheral signals that are mapped by this selection 
    * 
    * @return List of mapped signals
    */
   public ArrayList<Signal> getSignals() {
      return fSignals;
   }

   /** 
    * Add signal to the list of signals in this mapping
    * 
    * @param signal
    */
   public void addSignal(Signal signal) {
      fSignals.add(signal);
      addListener(signal);
   }
   
   
   /**
    * Get pin that signals are mapped to 
    * 
    * @return Associated pin
    */
   public Pin getPin() {
      return fPin;
   }

   /**
    * Get pin multiplexor setting to map these signals on the pin 
    * 
    * @return Mux value
    */
   public MuxSelection getMux() {
      return fMuxValue;
   }

   /**
    * Sets selection state i.e. whether mux setting is current for a pin<br>
    * If changed then listeners are notified
    * 
    * @param selected
    */
   public void select(Origin origin, boolean selected) {
      if (this == MappingInfo.UNASSIGNED_MAPPING) {
         return;
      }
//      System.err.println("MappingIofo.select("+selected+") == "+fSelected);
      if (fSelected == selected) {
//         System.err.println(toString() + " No Change");
         return;
      }
      fSelected = selected;
      if (fBusy) {
         throw new RuntimeException("Loop!!!");
      }
      fPin.modelElementChanged(this);
      for (Signal signal:getSignals()) {
         signal.modelElementChanged(this);
      }
   }

   /**
    * Indicates if the current mapping is selected (or fixed)
    * 
    * @return
    */
   public boolean isSelected() {
      return fSelected || (fMuxValue == MuxSelection.fixed);
   }
   
   @Override
   public String toString() {
      return "Mapping("+getDescription() + ", " + fSelected+")";
   }

   @Override
   public int hashCode() {
      return fMuxValue.hashCode()^fPin.hashCode()^fSignals.hashCode();
   }

   /**
    * Notifies associated signals that the pin properties have changed
    * 
    * @param pin  Pin with changes
    */
   public void pinPropertiesChanged(Pin pin) {
      for (Signal signal:fSignals) {
         signal.notifyListeners();
      }
   }

};
