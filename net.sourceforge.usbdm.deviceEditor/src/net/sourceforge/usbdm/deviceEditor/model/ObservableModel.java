package net.sourceforge.usbdm.deviceEditor.model;

import java.util.ArrayList;

/**
 * Implements a change listener interface
 *
 */
public class ObservableModel {

   private boolean fRefreshPending = false;

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
    * Notify all listeners<br>
    * Clears RefreshPending
    */
   public void notifyListeners() {
      for (IModelChangeListener listener:fListeners) {
         listener.modelElementChanged(this);
      }
      fRefreshPending = false;
   }

   /**
    * Notify structure change listeners
    * 
    */
   public void notifyStructureChangeListeners() {
      for (IModelChangeListener listener:fListeners) {
         listener.modelStructureChanged(this);
      }
   }

   public boolean isRefreshPending() {
      return fRefreshPending;
   }

   /**
    * Set whether the node has changed since last refresh
    * 
    * @param updatePending
    */
   public void setRefreshPending(boolean refreshPending) {
      fRefreshPending = refreshPending;
   }
}
