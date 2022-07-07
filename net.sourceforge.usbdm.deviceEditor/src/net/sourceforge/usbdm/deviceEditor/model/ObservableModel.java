package net.sourceforge.usbdm.deviceEditor.model;

import java.util.ArrayList;

/**
 * Implements a change listener interface
 */
public class ObservableModel implements ObservableModelInterface {
 
   private boolean fRefreshPending  = false;
   
   private ArrayList<IModelChangeListener> fListeners = new ArrayList<IModelChangeListener>();

   @Override
   public void addListener(IModelChangeListener listener) {
      if (!fListeners.contains(listener)) {
         fListeners.add(listener);
      }
//      else {
//         System.err.println("Listener "+ listener + " already in list");
//      }
   }

   @Override
   public void removeAllListeners() {
      // Done this way so clearing cloned variables doesn't affect original
      fListeners = new ArrayList<IModelChangeListener>();
   }

   @Override
   public void removeListener(IModelChangeListener listener) {
      fListeners.remove(listener);
   }

   @Override
   public void notifyModelListeners() {
      for (IModelChangeListener listener:fListeners) {
         if (listener instanceof BaseModel) {
            listener.modelElementChanged(this);
         }
      }
      fRefreshPending = false;
   }
   
   @Override
   public void notifyListeners() {
      for (IModelChangeListener listener:fListeners) {
         listener.modelElementChanged(this);
      }
      fRefreshPending = false;
   }
   
   @Override
   public void notifyListeners(Object origin) {
      for (IModelChangeListener listener:fListeners) {
         if (listener != origin) {
            listener.modelElementChanged(this);
         }
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

   @Override
   public void setRefreshPending(boolean refreshPending) {
      fRefreshPending = refreshPending;
   }
}
