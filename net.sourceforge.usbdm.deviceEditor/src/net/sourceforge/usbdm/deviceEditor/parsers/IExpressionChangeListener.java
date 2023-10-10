package net.sourceforge.usbdm.deviceEditor.parsers;

/**
 * Used to communicate changes in the model element.
 *
 */
public interface IExpressionChangeListener {
   
   /**
    * Called when a monitored expression changes.
    * 
    * @param expression - The expression that has changed
    * 
    * @return Boolean indicating if a notification of change has been done
    */
   void expressionChanged(Expression expression);
}
