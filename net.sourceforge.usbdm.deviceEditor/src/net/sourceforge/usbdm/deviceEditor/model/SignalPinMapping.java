package net.sourceforge.usbdm.deviceEditor.model;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.parsers.Expression;
import net.sourceforge.usbdm.deviceEditor.parsers.IExpressionChangeListener;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

public class SignalPinMapping implements IExpressionChangeListener {

   static class Mapping {
      final Pin pin;
      final Expression expression;
      
      /**
       * Create object to hold a pin mapping for a signal and associated enabling expression
       * 
       * @param pin        Pin to map
       * @param expression Expression enabling this mapping
       */
      Mapping(Pin pin, Expression expression) {
         this.pin = pin;
         this.expression = expression;
      }
      
      @Override
      public
      String toString() {
         return "SignaPinMapping("+pin.getName()+" mapped when "+expression.getExpressionStr() + ")";
      }
   };
   
   /** Signal to be mapped */
   final Signal signal;
   
   /** Pin mappings and enabling expressions for the signal */
   ArrayList<Mapping> mappings = new ArrayList<Mapping>();

   /**
    * Create Dynamic signal mapping
    * 
    * @param signal  Signal to be dynamically mapped to a pin
    */
   public SignalPinMapping(Signal signal) {
      this.signal = signal;
   }

   @Override
   public void expressionChanged(Expression expression) {

      boolean mappingFound = false;
      // Activate enabled mapping
      for (Mapping mapping:mappings) {
         try {
            if (mapping.expression.getValueAsBoolean()) {
               signal.mapPin(mapping.pin);
//               System.err.println("Mapping "+signal.getName()+" => "+mapping.pin.getName());
               mappingFound = true;
               // Stop at first enabled mapping
               break;
            }
         } catch (Exception e) {
            new Exception("Unable to activate mapping for expression " + mapping.expression.getExpressionStr(), e).printStackTrace();
         }
      }
      if (!mappingFound) {
         // Remove current mapping
         signal.mapPin(Pin.UNASSIGNED_PIN);
      }
   }

   /**
    * Adds a dynamic mapping for the pin
    * 
    * @param pin           Pin to map to
    * @param expression    Expression activating this mapping
    * @param provider      Provider for variable used in expression
    * 
    * @throws Exception    On invalid expression
    */
   public void addMapping(Pin pin, String expression, VariableProvider provider) throws Exception {
      Expression exp = new Expression(expression, provider);
      mappings.add(new Mapping(pin, exp));
   }
   
   public void activate() {
      for( Mapping mapping:mappings) {
         mapping.expression.addListener(this);
//         System.err.println("Watching '"+mapping.expression.getExpressionStr()+"'");
      }
      expressionChanged(null);
   }
}
