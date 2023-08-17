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
    */
   void expressionChanged(Expression expression);
}
