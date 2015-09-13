package net.sourceforge.usbdm.peripherals.model;

import java.util.ArrayList;

/**
 * Implements a change listener interface
 *
 */
public class ObservableModel {
   
   private boolean refreshPending = false;
   
   private ArrayList<IModelChangeListener> fListener = new ArrayList<IModelChangeListener>();

   /**
    * Add the model change listener
    * 
    * @param listener
    */
   public void addListener(IModelChangeListener listener) {
//      System.err.println("ObservableModel.addListener(), this = " + this);
//      System.err.println("ObservableModel.addListener(), listener = " + listener);
      if (!fListener.contains(listener)) {
         fListener.add(listener);
      }
   }
   
   /**
    * Remove the model change listener (if present)
    * 
    * @param listener to remove
    */
   public void removeListener(IModelChangeListener listener) {
      fListener.remove(listener);
   }

   protected void notifyListeners() {
//      System.err.println("ObservableModel.notifyListeners(), this = " + this);
      for (IModelChangeListener listener:fListener) {
//         System.err.println("notify: " + listener.getClass());
         listener.modelElementChanged(this);
      }
   }

   protected void notifyStructureChangeListeners() {
//    System.err.println("ObservableModel.notifyListeners(), this = " + this);
    for (IModelChangeListener listener:fListener) {
//       System.err.println("notify: " + listener.getClass());
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
