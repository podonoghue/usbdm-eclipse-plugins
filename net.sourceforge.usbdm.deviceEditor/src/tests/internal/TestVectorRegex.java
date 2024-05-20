package tests.internal;

public class TestVectorRegex {

   public static void main(String[] args) {

      String tests[] = {
            "RTC_Alarm_IRQn;RTC_Seconds_IRQn",
            "MCG_IRQn",
            "I2S0_Tx_IRQn;I2S0_Rx_IRQn",
            "LPTMR0_LPTMR1_IRQn",
            "DMA0_Ch0_16_IRQn;DMA0_Ch15_31_IRQn;DMA0_Error_IRQn",
      };
      for (String test:tests) {
         String vectors[] = test.split(";");
         if (vectors.length>1) {
            for (String vector:vectors) {
               String oldVector = vector.replaceAll("^(.*)_IRQn$", "$1_IRQHandler");
               String newVector = vector.replaceAll("^(.*?)_(.*)_IRQn$", "Class::irqHandler<Class::IrqNum_$2>");
               System.err.println(String.format("%-25s => %-30s, %s", "'"+vector+"'", "'"+oldVector+"'", "'"+newVector+"'"));
            }
         }
         else {
            String vector = vectors[0];
            String oldVector = vector.replaceAll("^(.*)_IRQn$", "$1_IRQHandler");
            String newVector = vector.replaceAll("^(.*)_IRQn$", "Class::irqHandler");
            System.err.println(String.format("%-25s => %-30s, %s", "'"+vector+"'", "'"+oldVector+"'", "'"+newVector+"'"));
         }
      }
   }

}
