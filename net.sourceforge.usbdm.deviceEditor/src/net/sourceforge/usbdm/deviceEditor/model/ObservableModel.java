package net.sourceforge.usbdm.deviceEditor.model;

import java.util.ArrayList;

/**
 * Implements a change listener interface
 */
public class ObservableModel implements ObservableModelInterface {

   private boolean fRefreshPending  = false;
   
   private ArrayList<IModelChangeListener> fListeners = new ArrayList<IModelChangeListener>();

   /**
    * Add a model change listener to listen for changes
    * 
    * @param listener Listener to add
    */
   @Override
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
   @Override
   public void removeAllListeners() {
      // Done this way so clearing cloned variables doesn't affect original
      fListeners = new ArrayList<IModelChangeListener>();
   }

   /**
    * Remove the model change listener (if present)
    * 
    * @param listener to remove
    */
   @Override
   public void removeListener(IModelChangeListener listener) {
      fListeners.remove(listener);
   }

   /**
    * Notify all listeners<br>
    * Clears RefreshPending
    */
   @Override
   public void notifyListeners() {
      for (IModelChangeListener listener:fListeners) {
         listener.modelElementChanged(this);
      }
      fRefreshPending = false;
   }
   
   @Override
   public void notifyStatusListeners() {
      for (IModelChangeListener listener:fListeners) {
         listener.elementStatusChanged(this);
      }
      fRefreshPending = false;
   }

   /**
    * Notify structure change listeners
    * 
    */
   @Override
   public void notifyStructureChangeListeners() {
      for (IModelChangeListener listener:fListeners) {
         listener.modelStructureChanged(this);
      }
   }

   @Override
   public boolean isRefreshPending() {
      return fRefreshPending;
   }

   /**
    * Set whether the node has changed since last refresh
    * 
    * @param updatePending
    */
   @Override
   public void setRefreshPending(boolean refreshPending) {
      fRefreshPending = refreshPending;
   }
}
