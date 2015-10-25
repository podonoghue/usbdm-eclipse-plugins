
public enum MuxSelection {
   Fixed(-3),
   Reset(-2),
   Disabled(-1),
   mux0(0),
   mux1(1),
   mux2(2),
   mux3(3),
   mux4(4),
   mux5(5),
   mux6(6),
   mux7(7);

   public final int value;

   private MuxSelection(int value) {
      this.value = value;
   }
   public static MuxSelection valueOf(int value) throws Exception {
      switch(value) {
      case -3 : return Fixed;
      case -2 : return Reset;
      case -1 : return Disabled;
      case 0  : return mux0;
      case 1  : return mux1;
      case 2  : return mux2;
      case 3  : return mux3;
      case 4  : return mux4;
      case 5  : return mux5;
      case 6  : return mux6;
      case 7  : return mux7;
      }
      throw new Exception("No such enum, value = " + Integer.toString(value));
   }
   
   public String toString() {
      return super.toString()+"("+Integer.toString(value)+")";
   }
}