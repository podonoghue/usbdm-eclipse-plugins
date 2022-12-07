package net.sourceforge.usbdm.deviceEditor.model;

/**
 * Allows change listeners to be added to this object
s */
public interface ObservableModelInterface {

   /**
    * Add a model change listener to listen for changes
    * 
    * @param listener Listener to add
    */
   public void addListener(IModelChangeListener listener);

   /**
    * Remove the model change listener (if present)
    * 
    * @param listener to remove
    */
   public void removeAllListeners();

   /**
    * Remove the model change listener (if present)
    * 
    * @param listener to remove
    */
   public void removeListener(IModelChangeListener listener);
   
   /**
    * Notify all listeners<br>
    * Clears RefreshPending
    */
   public void notifyListeners();
   
   /**
    * Notify BaseModel listeners<br>
    * Clears RefreshPending
    */
   public void notifyModelListeners();
   
   /**
    * Notify all listeners apart from origin<br>
    * Clears RefreshPending
    * 
    * @param origin Listener to exclude
    */
   public void notifyListeners(Object origin);

   /**
    * Notify all listeners<br>
    * Clears RefreshPending
    */
   public void notifyStatusListeners();

   /**
    * Notify structure change listeners
    * 
    */
   public void notifyStructureChangeListeners();

   public boolean isRefreshPending();

   /**
    * Set whether the node has changed since last refresh
    * 
    * @param updatePending
    */
   public void setRefreshPending(boolean refreshPending);
   
}
