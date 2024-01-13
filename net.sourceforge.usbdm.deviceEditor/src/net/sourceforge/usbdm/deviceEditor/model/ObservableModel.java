package net.sourceforge.usbdm.deviceEditor.model;

import java.util.ArrayList;

/**
 * Implements a change listener interface
 */
public class ObservableModel implements ObservableModelInterface {
 
   private boolean fRefreshPending  = false;
   
   protected ArrayList<IModelChangeListener> fListeners = new ArrayList<IModelChangeListener>();

   @Override
   public boolean addListener(IModelChangeListener listener) {
      if (listener == this) {
         return false;
      }
      if (!fListeners.contains(listener)) {
         fListeners.add(listener);
         return true;
      }
      return false;
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
            listener.modelElementChanged(this, ObservableModelInterface.PROP_VALUE);
         }
      }
      fRefreshPending = false;
   }
   
   @Override
   public void notifyListeners(IModelChangeListener exclude, String[] properties) {
      
      ArrayList<IModelChangeListener> cp = new ArrayList<IModelChangeListener>();
      cp.addAll(fListeners);
      for (IModelChangeListener listener:cp) {
         if (listener != exclude) {
            listener.modelElementChanged(this, properties);
         }
      }
      fRefreshPending = false;
   }
   
   @Override
   final public void notifyListeners(String[] properties) {
      notifyListeners(null, properties);
   }
   
   /**
    * Notify all listeners apart from exclude (properties = "value")
    * 
    * @param origin Listener to exclude
    */
   final public void notifyListeners(IModelChangeListener exclude) {
      notifyListeners(exclude, ObservableModelInterface.PROP_VALUE);
   }
   
   /**
    * Notify all listeners (properties = "value")
    * 
    * @param origin Listener to exclude
    */
   final public void notifyListeners() {
      notifyListeners(ObservableModelInterface.PROP_VALUE);
   }
   
   @Override
   public void notifyStatusListeners() {
      for (IModelChangeListener listener:fListeners) {
         listener.modelElementChanged(this, new String[] {"Status"} );
      }
      fRefreshPending = false;
   }

   @Override
   public void notifyStructureChangeListeners() {
      for (IModelChangeListener listener:fListeners) {
         listener.modelElementChanged(this, new String[] {"Structure"} );
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
