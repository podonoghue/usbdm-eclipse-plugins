
public class WriterForLpuart extends WriterForUart {

   public WriterForLpuart(boolean deviceIsMKE) {
      super(deviceIsMKE);
   }

   @Override
   String getGroupName() {
      return "Uart_Group";
   }

   @Override
   String getGroupTitle() {
      return "LPUART, Low Power Universal Asynchonous Receiver/Transmitter";
   }

   @Override
   String getGroupBriefDescription() {
      return "Pins used for LPUART functions";
   }
}
