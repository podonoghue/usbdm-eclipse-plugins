package net.sourceforge.usbdm.deviceEditor.model;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.Variable;

/**
 * Implements a change listener interface
 */
public class ObservableModel implements ObservableModelInterface {
   
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
   public void notifyBaseModelListeners() {
      
      for (IModelChangeListener listener:fListeners) {
         if (listener instanceof BaseModel) {
            listener.modelElementChanged(this, IModelChangeListener.PROPERTY_VALUE);
         }
      }
   }
   
   @Override
   public void notifyListeners(int properties) {
      boolean logging = (this instanceof Variable) && ((Variable)this).isLogging();
      if (logging) {
         System.err.println(this.toString()+".notifyListeners("+IModelChangeListener.getPropertyNames(properties)+") ========");
      }
      ArrayList<IModelChangeListener> cp = new ArrayList<IModelChangeListener>();
      cp.addAll(fListeners);
      for (IModelChangeListener listener:cp) {
         boolean logThisNotice = logging || ((listener instanceof Variable) && ((Variable)listener).isLogging());
         if (logThisNotice) {
            System.err.println(this.toString()+".notifyListeners("+IModelChangeListener.getPropertyNames(properties)+") =>"+
                  listener.toString());
         }
//         if (listener instanceof Expression) {
//            Expression exp = (Expression) listener;
//            if (exp.getExpressionStr().startsWith("(/OSC0/osc_clock==12_MHz)?")) {
//               System.err.println("exp.getExpressionStr());
//            }
//         }
         listener.modelElementChanged(this, properties);
      }
   }

   @Override
   public void notifyStructureChangeListeners() {
      for (IModelChangeListener listener:fListeners) {
         listener.modelElementChanged(this, IModelChangeListener.PROPERTY_STRUCTURE);
      }
   }
   
   /**
    * Notify all listeners (properties = "value")
    * 
    * @param origin Listener to exclude
    */
   final public void notifyListeners() {
      notifyListeners(IModelChangeListener.PROPERTY_VALUE);
   }
   
}
