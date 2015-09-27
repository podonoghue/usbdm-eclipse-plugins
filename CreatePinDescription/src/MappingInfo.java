/**
 * Describes how a peripheral function is mapped to a pin<br>
 * e.g. FTM0_CH6(fn4) => MappingInfo(PeripheralFunction(FTM, 0, 6), 4)<br>
 * e.g. PTA3(fn1) => MappingInfo(PeripheralFunction(PT, A, 3), 1)
 */
public class MappingInfo {
   /** Peripheral function that is mapped */
   public PeripheralFunction function;
   
   /** Pin multiplexor setting to map the function on the pin */
   public int mux;
   
   /**
    * Create peripheral function for mapping to a pin<br>
    * e.g. FTM0_CH6(fn4) => <i>MappingInfo</i>(<i>PeripheralFunction</i>(FTM, 0, 6), 4)
    * 
    * @param function   Peripheral function to map e.g. <i>PeripheralFunction</i>(PT, A, 3)
    * @param mux        Pin multiplexor setting e.g. 4
    */
   public MappingInfo(PeripheralFunction function, int mux) {
      System.err.println(String.format("p=%s, mux=%s", function.toString(), mux));
      this.function = function;
      this.mux      = mux;
   }
};
