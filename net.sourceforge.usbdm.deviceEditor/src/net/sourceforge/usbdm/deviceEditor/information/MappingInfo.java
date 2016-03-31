package net.sourceforge.usbdm.deviceEditor.information;
import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.model.Message;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModel;

/**
 * Describes how a peripheral function is mapped to a pin<br>
 */
public class MappingInfo extends ObservableModel {
   
   /** Source of change */
   public static enum Origin {pin, function};
   
   /** List of peripheral functions that are mapped  by this selection */
   private final ArrayList<PeripheralFunction> fFunctions = new ArrayList<PeripheralFunction>();;
   
   /** Pin that functions are mapped to */
   private final PinInformation fPin;
   
   /** Pin multiplexor setting to map these functions on the pin */
   private final MuxSelection fMux;

   /** Indicates if the current mapping is selected */
   private boolean fSelected;

   /** Message associated with mapping e.g. conflict etc*/
   private Message fMessage = null;
   
   private boolean fBusy = false;
   
   private Origin fOrigin = null;
   
   public boolean marked = false;
   
   /**
    * Associates a peripheral function and a pin<br>
    * 
    * @param function   Peripheral function
    * @param pin        Pin
    * @param functionSelector        Pin multiplexor setting to select associated function on the pin
    */
   MappingInfo(PinInformation pin, MuxSelection functionSelector)  {
      fPin       = pin;
      fMux       = functionSelector;
   }
   
   /**
    * Get description of mux setting e.g. "PTA1 =>  GPIOC_6/LLWU_P10 @ mux5
    * 
    * @return Description
    */
   String getDescription() {
      return String.format("%s => %s @ %s", fPin.getName(), getFunctionList(), fMux);
   }
   
   /**
    * Returns a list of mapped functions as a string e.g. <b><i>GPIOC_6/LLWU_P10</b></i>
    * 
    * @return List of mapped functions as string
    */
   public String getFunctionList() {
      StringBuffer name = new StringBuffer();
      for (PeripheralFunction function:fFunctions) {
         if (name.length() != 0) {
            name.append("/");
         }
         name.append(function.getName());
      }
      return name.toString();
   }

   /**
    * Get list of peripheral functions that are mapped by this selection 
    * 
    * @return List of mapped functions
    */
   public ArrayList<PeripheralFunction> getFunctions() {
      return fFunctions;
   }

   /**
    * Get pin that functions are mapped to 
    * 
    * @return Associated pin
    */
   public PinInformation getPin() {
      return fPin;
   }

   /**
    * Get pin multiplexor setting to map these functions on the pin 
    * 
    * @return Mux value
    */
   public MuxSelection getMux() {
      return fMux;
   }

   public Origin getOrigin() {
      return fOrigin;
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
      fOrigin = origin;
      if (fSelected != selected) {
         setRefreshPending(true);
         fSelected = selected;
//         System.err.println(String.format("%-60s => Changed   => %s", toString(), (selected?"selected":"unselected")));
         notifyListeners();
      }
//      else {
//         System.err.println(String.format("%-60s => No change == %s", toString(), (selected?"selected":"unselected")));
//      }
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
      return fMux.hashCode()^fPin.hashCode()^fFunctions.hashCode();
   }

   /**
    * Indicates if the mapping has a conflict
    * 
    * @return 
    */
   public boolean isConflicted() {
      return (fMessage!= null) && (fMessage.greaterThan(Message.Severity.OK));
   }

   public void setMessage(String msg) {
      Message oldMsg = fMessage;
      if ((msg==null) || msg.isEmpty()) {
         fMessage = null;
      }
      else {
         fMessage = new Message(msg, Message.Severity.ERROR);
      }
      if (fMessage != oldMsg) {
//         System.err.println("setMessage() Changed: "+this+"==>"+msg);
         setRefreshPending(true);
      }
   }

   public Message getMessage() {
      return fMessage;
   }
   
};
