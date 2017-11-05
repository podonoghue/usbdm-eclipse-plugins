package net.sourceforge.usbdm.peripherals.model;

import java.util.ArrayList;

/**
 * Implements a change listener interface
 *
 */
public class ObservableModel {

   private boolean refreshPending = false;

   private ArrayList<IModelChangeListener> fListeners = new ArrayList<IModelChangeListener>();

   /**
    * Add the model change listener
    * 
    * @param listener
    */
   public void addListener(IModelChangeListener listener) {
      if (!fListeners.contains(listener)) {
         fListeners.add(listener);
      }
   }

   /**
    * Remove the model change listener (if present)
    * 
    * @param listener to remove
    */
   public void removeListener(IModelChangeListener listener) {
      fListeners.remove(listener);
   }

   /**
    * Notify all listeners
    */
   protected void notifyListeners() {
      for (IModelChangeListener listener:fListeners) {
         listener.modelElementChanged(this);
      }
   }

   /**
    * Notify structure change listeners
    */
   protected void notifyStructureChangeListeners() {
      for (IModelChangeListener listener:fListeners) {
         listener.modelStructureChanged(this);
      }
   }

   public boolean isRefreshPending() {
      return refreshPending;
   }

   public void setRefreshPending(boolean updatePending) {
      this.refreshPending = updatePending;
   }
}
