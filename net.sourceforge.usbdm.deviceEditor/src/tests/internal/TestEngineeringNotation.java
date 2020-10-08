package tests.internal;

import net.sourceforge.usbdm.deviceEditor.model.EngineeringNotation;

public class TestEngineeringNotation {
    
   public static void main(String[] args) {

      String tests[]   = {"0", "24.56789kHz", "100Hz", "100MHz", "100kHz", "100k", "100u", "100m", "0x23", "0b1010", "-100", "-0x23", "-0b1010kHz", };
      double results[] = { 0, 24567.89,      100,    100000000, 100000,  100000, 0.0001, 0.10,    0x23,    10, -100, -0x23, -10*1000, };
      for (int index=0; index<results.length; index++) {
         double num = EngineeringNotation.parse(tests[index]);
         if (Math.round(num*1e9) != Math.round(results[index]*1e9)) {
            System.err.println(String.format("%-15s => Expected %20s, Got %20s - failed", tests[index], String.format("%f",results[index]), String.format("%f",num)));
         }
         else {
            System.err.println(String.format("%-15s => Expected %20s, Got %20s - OK", tests[index], String.format("%f",results[index]), String.format("%f",num)));
         }
      }
      String tests3[]   = {"24.56789kHz", "900m", "100Hz", "100MHz", "100kHz", "100k", "100u", "100m", "0x23", "10", "-100", "-0x23", "-0b1010kHz", };
      long results3[] = { 24568,      1, 100,    100000000, 100000,  100000, 0, 0,    0x23,    10, -100, -0x23, -0xA*1000, };
      for (int index=0; index<results3.length; index++) {
         long num = EngineeringNotation.parseAsLong(tests3[index]);
         if (num != results3[index]) {
            System.err.println(String.format("%-15s => Expected %20s, Got %20s - failed", tests3[index], String.format("%f",results3[index]), String.format("%f",num)));
         }
         else {
            System.err.println(String.format("%-15s => Expected %20s, Got %20s - OK", tests3[index], String.format("%d",results3[index]), String.format("%d",num)));
         }
      }
      double tests2[]  = {0, .000123456789, .00123456789, .0123456789, .123456789, 1.23456789, 12.3456789, 123.456789, 1234.56789, 12345.6789,
            0, .0001, .001, .01, .1, 1.0, 10.0, 100.0, 1000.0, 10000.0};
      for (double test:tests2) {
         int    pow10 = (int)Math.floor(Math.log10(test));
         System.out.println(String.format("\ntest = %12f, pow10 = %d", test, pow10));
         for (int sigDigits=1; sigDigits<10; sigDigits++) {
            System.out.print(String.format("%12d", sigDigits));
         }
         System.out.println();
         for (int sigDigits=1; sigDigits<10; sigDigits++) {
            System.out.print(String.format("%12s", EngineeringNotation.convert(test, sigDigits)));
         }
         System.out.println();
      }
   }

}
