
public enum MuxSelection {
   Disabled(-1),
   mux0(0),
   mux1(1),
   mux2(2),
   mux3(3),
   mux4(4),
   mux5(5),
   mux6(6),
   mux7(1);

   public final int value;

   private MuxSelection(int value) {
      this.value = value;
   }
   public static MuxSelection valueOf(int value) throws Exception {
      switch(value) {
      case -1 : return Disabled;
      case 0  : return mux0;
      case 1  : return mux0;
      case 2  : return mux0;
      case 3  : return mux0;
      case 4  : return mux0;
      case 5  : return mux0;
      case 6  : return mux0;
      case 7  : return mux0;
      }
      throw new Exception("No such enum");
   }
   public String toString() {
      return Integer.toString(value);
   }
}