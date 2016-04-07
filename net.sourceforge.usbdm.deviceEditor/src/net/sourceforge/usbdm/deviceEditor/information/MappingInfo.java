package net.sourceforge.usbdm.deviceEditor.information;
import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.model.Message;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModel;

/**
 * Describes how a peripheral signal is mapped to a pin<br>
 */
public class MappingInfo extends ObservableModel {
   
   /** Source of change */
   public static enum Origin {pin, signal};
   
   /** List of peripheral signals that are mapped  by this selection */
   private final ArrayList<Signal> fSignals = new ArrayList<Signal>();;
   
   /** Pin that signals are mapped to */
   private final Pin fPin;
   
   /** Pin multiplexor setting to map these signals on the pin */
   private final MuxSelection fMuxValue;

   /** Indicates if the current mapping is selected */
   private boolean fSelected;

   /** Message associated with mapping e.g. conflict etc*/
   private Message fMessage = null;
   
   private boolean fBusy = false;
   
   public boolean marked = false;
   
   public static MappingInfo DISABLED_MAPPING = new MappingInfo(Pin.DISABLED_PIN, MuxSelection.disabled);
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
      if (fBusy) {
         throw new RuntimeException("Loop!!!");
      }
      fBusy = true;
      if (fSelected != selected) {
         setRefreshPending(true);
         fSelected = selected;
         System.err.println(String.format("%-60s => Changed   => %s", toString(), (selected?"selected":"unselected")));
         notifyListeners();
      }
      else {
         System.err.println(String.format("%-60s => No change == %s", toString(), (selected?"selected":"unselected")));
      }
      fBusy = false;   
   }

   /**
    * Indicates if the current mapping is selected 
    * 
    * @return
    */
   public boolean isSelected() {
      return fSelected;
   }
   
   @Override
   public String toString() {
      return String.format("Mapping(%s)", getDescription());
   }

   @Override
   public int hashCode() {
      return fMuxValue.hashCode()^fPin.hashCode()^fSignals.hashCode();
   }

   /**
    * Indicates if the mapping has a conflict
    * 
    * @return 
    */
   public boolean isConflicted() {
      return (fMessage!= null) && (fMessage.greaterThan(Message.Severity.OK));
   }

   /**
    * Get message to display
    * 
    * @return Message to display
    */
   public void setMessage(String msg) {
      if ((msg==null) || msg.isEmpty()) {
         fMessage = null;
      }
      else {
         fMessage = new Message(msg, Message.Severity.ERROR);
      }
   }

   /**
    * Get current message
    * 
    * @return Message or null if none
    */
   public Message getMessage() {
      return fMessage;
   }

};
