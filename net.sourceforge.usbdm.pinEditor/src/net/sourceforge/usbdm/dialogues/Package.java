package net.sourceforge.usbdm.dialogues;

import java.io.PrintStream;
import java.util.HashMap;

public class Package extends Common {

   static HashMap<String, HashMap<String, Package>> packages = new HashMap<String, HashMap<String, Package>>();
   
   String fName;
   String fPin;
   
   static public Package factory(String pkgd, String pin) {
      
      HashMap<String, Package> pkgList = packages.get(pkgd);
      if (pkgList == null) {
         pkgList = new HashMap<String, Package>();
         packages.put(pkgd, pkgList);
      }
      Package pkg = pkgList.get(pin);
      if (pkg == null) {
         pkg = new Package(pkgd, pin);
         pkgList.put(pin, pkg);
      }
      return pkg;
   }
   
   static void list(PrintStream out) {
      for( String key:packages.keySet()) {
         out.println("==== "+key+" ==================================");
         HashMap<String, Package> signal = packages.get(key);
         for(String pinKey:signal.keySet()) {
            Package pkg = signal.get(pinKey);
            pkg.writeSVD(out);
         }
      }
   }

   private Package(String name, String pin) {
      fName = name;
      fPin  = pin;
   }
   
   public void writeSVD(PrintStream out) {
      out.println(String.format("%s<package pkgd=\"%s\" pin=\"%s\"/>", getIndentFill(), fName, fPin));
   }

}
