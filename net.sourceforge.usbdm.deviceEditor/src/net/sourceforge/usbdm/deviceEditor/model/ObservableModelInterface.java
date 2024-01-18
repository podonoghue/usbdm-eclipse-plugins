package net.sourceforge.usbdm.deviceEditor.model;

/**
 * Allows change listeners to be added to this object
s */
public interface ObservableModelInterface {

   public final static String[] PROP_MAPPING   = {"Mapping"};
   public final static String[] PROP_VALUE     = {"Value"};
   public final static String[] PROP_STATUS    = {"Status"};
   public final static String[] PROP_HIDDEN    = {"Hidden"};
   
   /**
    * Add a model change listener to listen for changes<br>
    * Duplicate listeners are discarded
    * 
    * @param listener Listener to add
    * 
    * @return true if listener actually added
    */
   public boolean addListener(IModelChangeListener listener);

   /**
    * Remove all model change listeners
    */
   public void removeAllListeners();

   /**
    * Remove the model change listener (if present)
    * 
    * @param listener to remove
    */
   public void removeListener(IModelChangeListener listener);
   
   /**
    * Notify all listeners of changes in given property types
    */
   public void notifyListeners(int properties);
   
   /**
    * Notify BaseModel listeners<br>
    * Notify all BaseModel listeners of change in property = "Status"
    */
   public void notifyBaseModelListeners();
   
   /**
    * Notify structure change listeners
    */
   public void notifyStructureChangeListeners();

}
