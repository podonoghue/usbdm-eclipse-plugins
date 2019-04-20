package net.sourceforge.usbdm.not.used.internal;

/*
 * 
 * This is an experiment that didn't work!!!
 * 
 */



import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.cdt.managedbuilder.core.IManagedConfigElement;
import org.eclipse.cdt.managedbuilder.core.IManagedConfigElementProvider;

public class UsbdmManagedConfigElementProvider implements
      IManagedConfigElementProvider {

   public static IManagedConfigElement[] makeArray(ArrayList<IManagedConfigElement> arrayList) {
      IManagedConfigElement[] array = new IManagedConfigElement[arrayList.size()];
      int index = 0;
      for (IManagedConfigElement element:arrayList) {
         array[index++] = element;
      }
      return array;
   }
   private static int indent = 0;
   
  
      public class UsbdmManagedConfigElement implements IManagedConfigElement {
         
      final String name;
      ArrayList<UsbdmManagedConfigElement> children = null;
      HashMap<String,String> attributes = null;
      
      UsbdmManagedConfigElement(String name) {
         this.name       = name;   
         this.children   = new ArrayList<UsbdmManagedConfigElement>(20);
         this.attributes = new HashMap<String, String>(20);
      }
      
      @Override
      public String getName() {
//         System.err.println("UsbdmManagedConfigElementProvider.usbdmManagedConfigElement.getName() => \'"+name+"\'");
         return name;
      }
      
      @Override
      public IManagedConfigElement[] getChildren(String elementName) {
         ArrayList<IManagedConfigElement> selectedChildren = new ArrayList<IManagedConfigElement>(20);
         for (IManagedConfigElement element:children) {
            if (element.getName().equals(elementName)) {
               selectedChildren.add(element);
            }
         }
//         if (selectedChildren.size()>0) {
//            System.err.println("UsbdmManagedConfigElementProvider.usbdmManagedConfigElement.getChildren("+elementName+") ==> #"+selectedChildren.size());
//         }
         IManagedConfigElement[] childArray = new IManagedConfigElement[0];
         return selectedChildren.toArray(childArray);
      }
      
      @Override
      public IManagedConfigElement[] getChildren() {
//         if (children.size()>0) {
//            System.err.println("UsbdmManagedConfigElementProvider.usbdmManagedConfigElement.getChildren() ==> #"+children.size());
//         }
         IManagedConfigElement[] childArray = new IManagedConfigElement[0];
         return children.toArray(childArray);
      }
      
      @Override
      public String getAttribute(String name) {
         String attributeValue = attributes.get(name);
         if (attributeValue == null) {
            return "";
         }
//         System.err.println("UsbdmManagedConfigElementProvider.usbdmManagedConfigElement.getAttribute("+name+") ==> " + attributeValue);
         return attributeValue;
      }
      
      public void addAttribute(String key, String value) {
         attributes.put(key, value);
      }

      public void addChild(UsbdmManagedConfigElement child) {
         children.add(child);
      }
      
      public String toString() {
         
         indent = 0;
         String rv = toStringHelper();
         
         return rv;         
      }
      private String toStringHelper() {
         String rv = "";
         
         for(int index=indent; index-->0;) {
            rv += " ";
         }
         rv += "<" + name;
         Iterator<Entry<String, String>> it = attributes.entrySet().iterator();
         while (it.hasNext()) {
             Map.Entry<String, String> pairs = it.next();
             rv += "\n";
             for(int index=indent+6; index-->0;) {
                rv += " ";
             }
             rv += pairs.getKey() + "=\"" + pairs.getValue() + "\"";
         }
         rv += ">\n";
         for(UsbdmManagedConfigElement child:children) {
            indent += 3;
            rv += child.toStringHelper();
            indent -= 3;
         }
         for(int index=indent; index-->0;) {
            rv += " ";
         }
         rv += "</" + name + ">\n";
         return rv;         
      }
   };
   

//<toolChain
//     id="net.sourceforge.usbdm.cdt.wizard.toolChain.dynamic>
//     superClass="cdt.managedbuild.toolchain.gnu.cross.base"
//   <optionCategory
//      id="net.sourceforge.usbdm.cdt.toolchain.category.dynamic
//      name="USBDM Dynamic Options
//      owner="net.sourceforge.usbdm.cdt.wizard.toolChain.dynamic>
//   </optionCategory>
//   <option
//      id="net.sourceforge.usbdm.cdt.toolchain.gcc.mcpu.dynamic
//      category="net.sourceforge.usbdm.cdt.toolchain.category.dynamic
//      name="Cpu (-mcpu=)dynamic
//      command="-mcpu={value}
//      valueType="string
//      defaultValue="dynamic value>
//   </option>
//</toolChain>
   
   public class armCpuOptions extends UsbdmManagedConfigElement {

      armCpuOptions() {
         super("toolChain");
         addAttribute("id",         "net.sourceforge.usbdm.cdt.wizard.toolChain.dynamic");
         addAttribute("superClass", "cdt.managedbuild.toolchain.gnu.cross.base");
         
         UsbdmManagedConfigElement categoryElement;
         categoryElement = new UsbdmManagedConfigElement("optionCategory");
         categoryElement.addAttribute("id",           "net.sourceforge.usbdm.cdt.toolchain.category.dynamic");
         categoryElement.addAttribute("name",         "USBDM Dynamic Options");
         categoryElement.addAttribute("owner",        "net.sourceforge.usbdm.cdt.wizard.toolChain.dynamic");
         addChild(categoryElement);
         
         UsbdmManagedConfigElement optionElement = new UsbdmManagedConfigElement("option");
         optionElement.addAttribute("id",           "net.sourceforge.usbdm.cdt.toolchain.gcc.mcpu.dynamic");
         optionElement.addAttribute("category",     "net.sourceforge.usbdm.cdt.toolchain.category.dynamic");
         optionElement.addAttribute("valueType",    "string");         
         optionElement.addAttribute("defaultValue", "dynamic value");         
         optionElement.addAttribute("command",      "-mcpu={value}");
         optionElement.addAttribute("name",         "Cpu (-mcpu=)dynamic");
         addChild(optionElement);
      }
      
   }
   
   public class LinuxToolChain extends UsbdmManagedConfigElement {

//  <toolChain
//      id="net.sourceforge.usbdm.cdt.toolchain.linux"
//      isAbstract="false"
//      name="Cross GCC - USBDM (Linux)"
//      osList="linux"
//      superClass="net.sourceforge.usbdm.cdt.toolchain">
//    <builder
//         id="net.sourceforge.usbdm.cdt.toolchain.linux.builder"
//         isAbstract="false"
//         isVariableCaseSensitive="false"
//         superClass="cdt.managedbuild.builder.gnu.cross">
//    </builder>
//  </toolChain>
      
//  <toolChain
//      id="net.sourceforge.usbdm.cdt.toolchain.linux"
//      name="Cross GCC - USBDM (Linux)"
//      superClass="net.sourceforge.usbdm.cdt.toolchain">
//      isAbstract="false"
//      osList="linux"
//   <builder
//         id="net.sourceforge.usbdm.cdt.toolchain.linux.builder"
//         isVariableCaseSensitive="false"
//         superClass="cdt.managedbuild.builder.gnu.cross">
//   </builder>
//</toolChain>

      LinuxToolChain() {
         super("toolChain");
         addAttribute("id",         "net.sourceforge.usbdm.cdt.toolchain.linux");
         addAttribute("name",       "Linux Dynamic");
         addAttribute("superClass", "net.sourceforge.usbdm.cdt.toolchain");
         addAttribute("isAbstract", "false");
         addAttribute("osList",     "linux");
                 
         UsbdmManagedConfigElement builderElement;
         builderElement = new UsbdmManagedConfigElement("builder");
         builderElement.addAttribute("id",                        "net.sourceforge.usbdm.cdt.toolchain.linux.builder");
         builderElement.addAttribute("isAbstract",                "false");
         builderElement.addAttribute("isVariableCaseSensitive",   "false");
         builderElement.addAttribute("superClass",                "cdt.managedbuild.builder.gnu.cross");
         addChild(builderElement);


//         UsbdmManagedConfigElement categoryElement;
//         categoryElement = new UsbdmManagedConfigElement("optionCategory");
//         categoryElement.addAttribute("id",           "net.sourceforge.usbdm.cdt.toolchain.category.dynamic");
//         categoryElement.addAttribute("name",         "USBDM Dynamic Options");
//         categoryElement.addAttribute("owner",        "net.sourceforge.usbdm.cdt.toolchain.linux.dynamic");
//         addChild(categoryElement);
//         
//         UsbdmManagedConfigElement optionElement = new UsbdmManagedConfigElement("option");
//         optionElement.addAttribute("id",           "net.sourceforge.usbdm.cdt.toolchain.gcc.mcpu.dynamic");
//         optionElement.addAttribute("category",     "net.sourceforge.usbdm.cdt.toolchain.category.dynamic");
//         optionElement.addAttribute("valueType",    "string");         
//         optionElement.addAttribute("defaultValue", "dynamic value");         
//         optionElement.addAttribute("command",      "-mcpu={value}");
//         optionElement.addAttribute("name",         "Cpu (-mcpu=)dynamic");
//         addChild(optionElement);
      }
      
   }
   
   @Override
   public IManagedConfigElement[] getConfigElements() {
      
//      System.err.println("UsbdmManagedConfigElementProvider.getConfigElements()");
      
      IManagedConfigElement[] element = {
//            new armCpuOptions(),
//            new LinuxToolChain(),
            } ;
//      System.err.println("getConfigElements ==>\n" + element[0].toString());
      
      return element;
   }

}
