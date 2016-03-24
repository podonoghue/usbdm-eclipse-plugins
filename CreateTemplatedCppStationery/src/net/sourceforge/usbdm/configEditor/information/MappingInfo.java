package net.sourceforge.usbdm.configEditor.information;
import java.util.ArrayList;

/**
 * Describes how a peripheral function is mapped to a pin<br>
 */
public class MappingInfo {
   
   /** Peripheral functions that are mapped */
   public ArrayList<PeripheralFunction> functions;
   
   /** Pin that functions are mapped to */
   public PinInformation pin;
   
   /** Pin multiplexor setting to map these functions on the pin */
   public MuxSelection mux;

   /**
    * Associates a peripheral function and a pin<br>
    * 
    * @param function   Peripheral function
    * @param pin        Pin
    * @param functionSelector        Pin multiplexor setting to select associated function on the pin
    */
   MappingInfo(PinInformation pin, MuxSelection functionSelector)  {
//      System.err.println(String.format("f=%s, p=%s, mux=%s", function.getName(), pin.getName(), mux));
      this.functions = new ArrayList<PeripheralFunction>();
      this.pin       = pin;
      this.mux       = functionSelector;
   }
   
   @Override
   public String toString() {
      return String.format("Mapping(%s => %s @ %s)", pin.getName(), functions.toString(), mux);
   }

   /**
    * Returns a list of mapped functions as a string e.g. <b><i>GPIOC_6/LLWU_P10</b></i>
    * 
    * @return list as string
    */
   public String getFunctionList() {
      StringBuffer name = new StringBuffer();
      for (PeripheralFunction function:functions) {
         if (name.length() != 0) {
            name.append("/");
         }
         name.append(function.getName());
      }
      return name.toString();
   }

};
