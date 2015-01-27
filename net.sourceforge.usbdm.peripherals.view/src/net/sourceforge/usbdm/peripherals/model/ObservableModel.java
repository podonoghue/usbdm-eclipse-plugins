package net.sourceforge.usbdm.peripherals.model;

/**
 * Implements a change listener interface
 *
 */
public class ObservableModel {
   
   private boolean refreshPending = false;
   
   /**
    * Dummy listener used when there is no real listener connected
    */
   private IModelChangeListener dummyListener = new IModelChangeListener() {
      
      /* (non-Javadoc)
       * @see net.sourceforge.usbdm.peripherals.ui.IModelChangeListener#modelElementChanged(net.sourceforge.usbdm.peripherals.ui.ObservableModel)
       */
      @Override
      public void modelElementChanged(ObservableModel observableModel) {
      }
   };

   private IModelChangeListener listener = dummyListener;

   /**
    * Add the model change listener
    * Note - only a single listener at a time is supported.
    * 
    * @param listener
    */
   public void addListener(IModelChangeListener listener) {
      this.listener = listener;
   }
   
   /**
    * Remove the model change listener
    * 
    * @param listener to remove
    */
   public void removeListener(IModelChangeListener listener) {
      if (this.listener == listener) {
         this.listener = dummyListener;
      }
   }

   protected void notifyListeners() {
//      System.err.println("ObservableModel.notifyListeners(): listener = "+listener.toString());
      listener.modelElementChanged(this);
   }

   public boolean isRefreshPending() {
      return refreshPending;
   }

   public void setRefreshPending(boolean updatePending) {
      this.refreshPending = updatePending;
   }
}
