package tests.internal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.annotationEditor.AnnotationParser;

public class TestParsing {

   public static void main(String[] args) {
      String test[] = new String[] {
            //            "<validate=net.sourceforge.usbdm.annotationEditor.validators.ClockValidate_MK64M12  >",
            //            "<validate=net.sourceforge.usbdm.annotationEditor.validators.ClockValidate_MK64M12 (0x1234)  >",
            //            "<validate=net.sourceforge.usbdm.annotationEditor.validators.ClockValidate_MK64M12(1,2,3,4)  >",
            //            "<name=oscclk_clock  >", 
            //            "<0-50000000>",
            //            "<i> hello there */",
//            "<o> VOUT33 [VOUT33]",
//            "<info> VOUT33 [VOUT33]",
            //            "<selection=GPIOA_1_PIN_SEL,PTA1>",
            //            "<selection=GPIOA_1_PIN_SEL,PTA1 (reset default) >",
            //            "<selection=JTAG_TDO_PIN_SEL,PTA2 (Alias:D3, LED_GREEN)>"
            //            "<1=> this is an enumeration",
            "<0x7FFFFFFF=> this is an enumeration",
            "<0xFFFFFFFF=> this is an enumeration",
            "<-1=> this is an enumeration",
            "<0x1=> this is an enumeration",
            "<-1=> this is an enumeration",
            //            "<-0x12=> this is an enumeration",
      };
      Pattern wizardPattern = Pattern.compile(AnnotationParser.wizardPatternString);
      for (String s:test) {
         System.out.println("\nTesting \""+s+"\"");
         Matcher wizardMatcher = wizardPattern.matcher(s);
         if (!wizardMatcher.matches()) {
            System.err.println("No match");
            continue;
         }
         String validateGroup    = wizardMatcher.group("validate");
         String nameGroup        = wizardMatcher.group("name");
         String annotationGroup  = wizardMatcher.group("annotation");
         String selectionGroup   = wizardMatcher.group("selection");
         String enumerationGroup = wizardMatcher.group("enumeration");

         if (validateGroup != null) {
            System.out.println("validateGroup = \'"+validateGroup+"\'");
            String validateBody = wizardMatcher.group("validateBody");
            System.out.println("validateBody = \'"+validateBody+"\'");
            Pattern p = Pattern.compile(AnnotationParser.CLASS_NAME_GROUP+"\\s*("+AnnotationParser.ARGS_GROUP+")?");
            Matcher m = p.matcher(validateBody);
            if (!m.matches()) {
               System.err.println("No match to \'validateBody\'");
               continue;
            }
            String className = m.group("className");
            String arguments = m.group("args");
            System.out.println(String.format("className = \"%s\"", className));
            System.out.println(String.format("arguments = \"%s\"", arguments));
            System.out.println("\n");
         }
         else if (nameGroup != null) {
            System.out.println("nameGroup = \'"+nameGroup+"\'");
            String nameBody = wizardMatcher.group("nameBody");
            System.out.println("nameBody = \'"+nameBody+"\'");
            Pattern p = Pattern.compile(AnnotationParser.VARIABLENAME_GROUP);
            Matcher m = p.matcher(nameBody);
            if (!m.matches()) {
               System.err.println("No match to \'nameBody\'");
               continue;
            }
            String variableName = m.group("variableName");
            System.out.println(String.format("variableName = \"%s\"", variableName));
            System.out.println("\n");
         }
         else if (annotationGroup != null) {
            System.out.println("annotationGroup = \'"+annotationGroup+"\'");
            String text = wizardMatcher.group("text");
            System.out.println("text = \'"+text+"\'");

            String control = wizardMatcher.group("control").trim();
            System.out.println("control = \'"+control+"\'");

            Pattern p = Pattern.compile("(.*?)\\s*\\*/");
            Matcher m = p.matcher(text);
            if (m.matches()) {
               text = m.group(1);
            }
            System.out.println("text = \'"+text+"\'");

            //            Pattern p = Pattern.compile(VARIABLENAME_GROUP);
            //            Matcher m = p.matcher(nameBody);
            //            if (!m.matches()) {
            //               System.out.println("No match to \'nameBody\'");
            //               continue;
            //            }
            //            String variableName = m.group("variableName");
            //            System.out.println(String.format("variableName = \"%s\"", variableName));
            //            System.out.println("\n");
         }
         else if (enumerationGroup != null) {
            // "<\\s*(?<enumValue>\\d+)\\s*=\\s*>(?<enumName>[^<]*)"
            System.out.println("enumerationGroup = \'"+enumerationGroup+"\'");

            long   value = (Long.decode(wizardMatcher.group("enumValue"))&0xFFFFFFFFL);
            String name  = wizardMatcher.group("enumName").trim();
            System.out.println("value = \'0x"+Long.toHexString(value)+"\'");
            System.out.println("value = \'"+value+"\'");
            System.out.println("name = \'"+name+"\'");
         }
         else if (selectionGroup != null) {
            System.out.println("selectionGroup = \'"+selectionGroup+"\'");
            String selectionBody = wizardMatcher.group("selectionBody");
            System.out.println("selectionBody = \'"+selectionBody+"\'");
            System.out.println("SELECTIONNAME_GROUP = \'"+AnnotationParser.SELECTIONNAME_GROUP+"\'");

            // e.g. <selection=i2c0_sda,2>
            //"(?<selectionName>.*))";
            Pattern p = Pattern.compile(AnnotationParser.SELECTIONNAME_GROUP);
            Matcher nm = p.matcher(selectionBody);
            if (!nm.matches()) {
               System.err.println("Fails match");
               continue;
            }
            String selectionTarget = nm.group(1);
            String selectionValue = nm.group(2);
            System.out.println(String.format("selectionTarget = \"%s\"", selectionTarget));
            System.out.println(String.format("selectionValue  = \"%s\"", selectionValue));

         }
         else {
            System.err.println("No match");            
         }
      }
   }

}
