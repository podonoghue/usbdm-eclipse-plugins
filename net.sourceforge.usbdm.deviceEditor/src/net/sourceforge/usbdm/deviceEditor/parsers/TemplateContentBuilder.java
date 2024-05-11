package net.sourceforge.usbdm.deviceEditor.parsers;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import net.sourceforge.usbdm.deviceEditor.information.BitmaskVariable;
import net.sourceforge.usbdm.deviceEditor.information.BitmaskVariable.BitInformation;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceData;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.IrqVariable;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.information.Variable.MaskPair;
import net.sourceforge.usbdm.deviceEditor.information.VariableWithChoices;
import net.sourceforge.usbdm.deviceEditor.parsers.ParseMenuXML.StringPair;
import net.sourceforge.usbdm.deviceEditor.parsers.ParseMenuXML.TemplateSubstitutionInfo;
import net.sourceforge.usbdm.deviceEditor.peripherals.Peripheral;

abstract class TemplateContentBuilder {
   /**
    * Build the text of the template for delayed template evaluation
    * 
    * @return Text built
    * 
    * @throws Exception
    */
   public abstract String build() throws Exception;
   
   static class TemplateBuilder extends TemplateContentBuilder {

      TemplateSubstitutionInfo info;
      private String body;

      public TemplateBuilder(TemplateSubstitutionInfo tsi, String text) {
         info = tsi;
         body = text;
      }

      @Override
      public String build() throws Exception {
         
         List<StringPair> substitutions = getTemplateSubstitutions(info);
         
         // Apply substitutions
         return doTemplateSubstitutions(body, substitutions);
      }
   }

   static class ClockTemplateBuilder extends TemplateContentBuilder {

      TemplateSubstitutionInfo info;
      private String body;

      public ClockTemplateBuilder(TemplateSubstitutionInfo tsi, String text) {
         info = tsi;
         body = text;
      }

      @Override
      public String build() throws Exception {
         
         List<StringPair> substitutions = getTemplateSubstitutions(info);
         
         // Add body substitution
         String caseBody = getTemplateCaseStatement(info.variableList.get(0));
         substitutions.add(0, new StringPair("%body", caseBody));
         
         // Apply substitutions
         return doTemplateSubstitutions(body, substitutions);
      }
   }

   /**
    * Construct template substitutions<br><br>
    * <ul>
    * <li>%baseType[index]                Underlying type for enum

    * <li>%constructorBitSet              Expression of form '%registerName |= %paramExpression
    * <li>%configFieldAssignment          Expression of form '%register      = (%register & ~%mask)|%registerName
    * <li>%configRegAssignment            Expression of form '%register      = %registerName
    * <li>%constructorFieldAssignment     Expression of form '%registerName  = (%registerName & ~%mask)|%paramExpression
    * <li>%constructorRegAssignment       Expression of form '%registerName  = %paramExpression

    * <li>%defaultValue[index]            Default value of variable
    * <li>%description[index]             Description from variable e.g. Compare Function Enable

    * <li>%fieldExtract                   Expression of form '(%register & %mask)
    * <li>%fieldAssignment                Expression of form '%register     <= (%register & ~%mask)|%paramExpression

    * <li>%initExpression                 List of initialisation values from variables (current values)
    * <li>%initNonDefaultExpression       List of initialisation values from variables (current values if not equal to default)
    
    * <li>%macro[index]                   From &lt;mask&gt; or deduced from &lt;controlVarName&gt; e.g. "SIM_SOPT_REG"
    * <li>%mask[index]                    From &lt;mask&gt; or deduced from &lt;controlVarName&gt; e.g. "SIM_SOPT_REG_MASK" (_MASK is added)
    * <li>%maskingExpression              Based on variable etc. Similar to (%register&%mask)
    * <li>%multilineDescription           Brief description of all variables

    * <li>%param[index]                   Formatted parameter for function
    * <li>%paramDescription[index]        Tool-tip from controlVar formatted as param description @param ...
    * <li>%paramExpression                Parameters ORed together e.g. adcPretrigger|adcRefSel
    * <li>%paramName[index]               Based on typeName with lower-case first letter adcCompare
    * <li>%params                         Formatted parameter list for function
    * <li>%paramType[index]               Based on typeName e.g. AdcCompare (or uint32_t)

    * <li>%regAssignment                  Expression of form '%register     <= %paramExpression
    * <li>%register[index]                Register associated with variable e.g. adc->APCTL1
    * <li>%registerName[index]            Name of corresponding register (lower-case for Init()) e.g. apctl1
    * <li>%registerNAME[index]            Name of corresponding register (upper-case for Init()) e.g. APCTL1
    * <li>%returnType[index]              Based on typeName e.g. AdcCompare (or uint32_t) (references and const stripped)

    * <li>%shortDescription[index]        Short description from controlVar e.g. Compare Function Enable
    * <li>%symbolicExpression[index]      Symbolic formatted value e.g. AdcCompare_Disabled

    * <li>%tooltip[index]                 Tool-tip from controlVar e.g. Each bit disables the GPIO function

    * <li>%valueExpression                Numeric variable value e.g. 0x3
    * <li>%variable[index]                Variable name e.g. /ADC0/adc_sc2_acfe
    * </ul>
    * 
    * @param element                 Element
    * @param variableAttributeName   Control var to obtain information from
    * 
    * @return  List of substitutions or null if variables requested but none found
    * 
    * @throws  Exception
    */
   private static List<StringPair> getTemplateSubstitutions(TemplateSubstitutionInfo info) throws Exception {

      if (info.variableNeededButNoneFound) {
         // Discard template as no variables found
         return null;
      }
      if (info.variableList == null) {
         // Return empty list to indicate keep template but no processing
         return new ArrayList<StringPair>();
      }
      boolean        multipleParamsOnNewline    = info.multipleParamsOnNewline;
      List<String>   paramOverride              = info.paramOverride;
      List<String>   paramTypesOverride         = info.paramTypesOverride;
      List<String>   defaultValueOverride       = info.defaultValueOverride;
      Long           numberOfNonDefaultParams   = info.numberOfNonDefaultParams;
      
      // List of variables to process - may be trimmed if variable doesn't exist
      ArrayList<Variable> variableList = info.variableList;
      
      StringBuilder variableKeys = new StringBuilder();
      
      ArrayList<StringPair> substitutions = new ArrayList<StringPair>();
      
      boolean allowEmptyParameterList = false;
      
      if (variableList != null) {
         
         // Process variables found
         StringBuilder maskSb               = new StringBuilder();  // Combined mask e.g. MASK1|MASK2
         StringBuilder valueExpressionSb    = new StringBuilder();  // Combined values $(var1)|$(var2)
         StringBuilder symbolicExpressionSb = new StringBuilder();  // Combined values $(var1.enum[])|$(var2.enum[])
         StringBuilder initExpressionSb     = new StringBuilder();  // Combined values $(var1.enum[])|, // comment ...
         StringBuilder initNonDefaultExprSb = new StringBuilder();  // Combined values $(var1.enum[])|, // comment ...
         StringBuilder paramExprSb          = new StringBuilder();  // Combined expression param1|param2
         StringBuilder paramsSb             = new StringBuilder();  // Parameter list with defaults etc.
         StringBuilder paramDescriptionSb   = new StringBuilder();  // @param style comments for parameters
         StringBuilder descriptionSb        = new StringBuilder();  // @param style comments for parameters

         // Accumulates the description for all parameters as block comment
         StringBuilder multilineDescription = new StringBuilder();

         boolean       parmsOnNewLine       = false;                 // Indicates there are multiple parameters
          
         // Padding applied to comments (before * @param)
         String linePadding    = info.linePadding;
         String tooltipPadding = info.tooltipPadding;

         // Terminator for initExpression
         String terminator     = info.terminator;

         // Separator for initExpression
         String separator     = info.separator;

         // No newline before initExpression (suitable for a single initialisation value)
         boolean initExpressionOnSameLine = info.initExpressionOnSameLine;

         // Find maximum name length
         int maxTypeNameLength = 4;
         for (int index=0; index<variableList.size(); index++) {
            if ((paramOverride.size()>index) && (paramOverride.get(index).equals("*"))) {
               continue;
            }
            String typeName = variableList.get(index).getTypeName();
            if (typeName == null) {
               continue;
            }
            maxTypeNameLength = Math.max(maxTypeNameLength, typeName.length());
         }
         String  register           = null;
         String  registerName       = null;
         boolean registeNameChanged = false;

         // Padding applied to parameters
         String paramPadding = (variableList.size()<=1)?"":"\\t      "+linePadding;
         
         // Used to differentiate 'nameless' params
         int valueSuffix = 0;
         
         for (int index=0; index<variableList.size(); index++) {
            StringBuilder currentInitExpressionSb     = new StringBuilder();  // Combined values $(var1.enum[])|, // comment ...

            Variable variable    = variableList.get(index);
            String   variableKey = variable.getKey();
            
            if (index > 0) {
               valueExpressionSb.append(separator);
               symbolicExpressionSb.append(separator);
               currentInitExpressionSb.append("\n");
            }

            // Mask created from variable name e.g. MACRO_MASK or deduced from valueFormat attribute
            String mask  = null;
            String macro = null;

            MaskPair maskPair = variable.generateMask();
            mask = maskPair.mask;
            macro = maskPair.macro;
            
            if (mask == null) {
               mask  = "";
               macro = "";
            }
            else if (!mask.isBlank()) {
               
               if (maskSb.length()>0) {
                  maskSb.append('|');
               }
               maskSb.append(mask);
               
               boolean bracketsRequired = !mask.matches("[a-zA-Z0-9_]*");
               if (bracketsRequired) {
                  mask = '('+mask+')';
               }
            }

            String baseType = "'%baseType' is not valid here";
            
            String baseTypeValue = variable.getBaseType();
            String typeNameValue = variable.getTypeName();
            if (baseTypeValue != null) {
               baseType = baseTypeValue;
            }
            else if (typeNameValue != null) {
               baseType = typeNameValue;
            }
            
            // Type from variable with upper-case 1st letter
            String paramName  = variable.getParamName();
            String paramType  = variable.getParamType();
            String returnType = variable.getReturnType();
            
            if (Variable.isIntegerTypeInC(paramType)) {
               // Integer parameters get a name of 'value' by default
               paramName = "value";
               if (valueSuffix != 0) {
                  paramName = paramName+valueSuffix;
               }
               valueSuffix++;
            }
            
            if ((paramOverride.size()>index) && !paramOverride.get(index).isBlank()) {
               if (paramOverride.get(index).equals("*")) {
                  // Exclude variable from parameter list
                  paramName               = null;
                  allowEmptyParameterList = true;
               }
               else {
                  paramName = paramOverride.get(index);
               }
            }

            if ((paramTypesOverride.size()>index) && !paramTypesOverride.get(index).isBlank()) {
               paramType = paramTypesOverride.get(index);
            }

            // $(variableKey)
            String valueExpression = variable.getSubstitutionValue();
//            String valueExpression = "$("+variableKey+")";
            valueExpressionSb.append(valueExpression);

//            String symbolicExpression = "$("+variableKey+".usageValue)";
//            symbolicExpressionSb.append("$("+variableKey+".usageValue)");

            String symbolicExpression = variable.getUsageValue();
            symbolicExpressionSb.append(symbolicExpression);

            // Description from variable
            String description = "'%description' not available in this template";
            String temp = variable.getDescription();
            if (temp != null) {
               description = temp;
               if (!descriptionSb.isEmpty()) {
                  if ((index+1)==variableList.size()) {
                     descriptionSb.append(" and ");
                  }
                  else {
                     descriptionSb.append(", ");
                  }
               }
               descriptionSb.append(description);
            }
            if (temp == null) {
               System.err.println("Warning: no description for '"+variable.getName()+"'");
            }

            // Short description from variable
            String shortDescription = "'%shortDescription' not available in this template";
            temp = variable.getShortDescription();
            if (temp != null) {
               shortDescription = temp;
            }
            String pad = "\\t   // ";
            if (!multilineDescription.isEmpty()) {
               multilineDescription.append("\\n");
               pad = "\\t"+linePadding+"// ";
            }
            multilineDescription.append(pad + shortDescription);
            multilineDescription.append(" ("+variable.getName()+")");
            
            // Tool-tip from variable
            String tooltip = "'%tooltip' not available in this template";
            temp = variable.getToolTipAsCode("\\t"+linePadding+tooltipPadding);
            if (temp != null) {
               tooltip = temp;
            }
            if (index == 0) {
               // 1st Expression
               if (!initExpressionOnSameLine) {
                  currentInitExpressionSb.append("\n\\t   "+linePadding);
               }
            }
            else {
               currentInitExpressionSb.append("\\t   "+linePadding);
            }
            String t = symbolicExpression.replace("\\t", "\\t   "+linePadding);
            currentInitExpressionSb.append(t);
            if (index+1 == variableList.size()) {
               // Last Expression
               currentInitExpressionSb.append(terminator+"  ");
            }
            else {
               currentInitExpressionSb.append(" "+separator+" ");
            }
            if (info.padToComments != 0) {
               int sol = t.lastIndexOf("\\t");
               if (sol<0) {
                  sol = 0;
               }
               else {
                  sol += 5;
               }
               int eol = t.length()-sol;
               int padding = info.padToComments - eol;
               if (padding>0) {
                  currentInitExpressionSb.append(String.format("%"+padding+"s",  ""));
               }
            }
            currentInitExpressionSb.append(String.format("%-30s","// ("+variable.getName()+") "));
            
            currentInitExpressionSb.append(variable.getShortDescription());

            if (variable instanceof VariableWithChoices) {
               VariableWithChoices vwc = (VariableWithChoices) variable;
               currentInitExpressionSb.append(" - "+vwc.getEffectiveChoice().getName());
            }

            String defaultParamV = variable.getDefaultParameterValue();
            if ((defaultValueOverride.size()>index) && !defaultValueOverride.get(index).isBlank()) {
               defaultParamV = defaultValueOverride.get(index);
            }
            if (paramName != null) {
               if (paramExprSb.length()>0) {
                  paramExprSb.append(separator);
               }
               paramExprSb.append(variable.formatValueForRegister(paramName));
            }
            
            String defaultValue = "%defaultValue"+index+" not available";
            if (defaultParamV != null) {
               defaultValue = defaultParamV;
            }
            String paramDescriptionN = "%paramDescription not available";
            if (paramName != null) {
               paramDescriptionN = String.format("\\t"+linePadding+" * @param %"+(-maxTypeNameLength)+"s %s", paramName, tooltip);
               if (paramDescriptionSb.length()>0) {
                  paramDescriptionSb.append("\n");
               }
               paramDescriptionSb.append(paramDescriptionN);
            }
            
            String param = "%param"+index+" not available";
            if (paramName != null) {
               if (index<numberOfNonDefaultParams) {
                  param = String.format("%"+(-maxTypeNameLength)+"s %s", paramType, paramName);
               }
               else {
                  param = String.format("%"+(-maxTypeNameLength)+"s %"+(-maxTypeNameLength)+"s = %s", paramType, paramName, defaultParamV);
               }
               if (paramsSb.length()>0) {
                  paramsSb.append(",");
                  
                  // Indicates newline is needed as newline on multi-params was requested
                  parmsOnNewLine = multipleParamsOnNewline;
                  
                  if (multipleParamsOnNewline) {
                     paramsSb.append("\n"+paramPadding);
                  }
               }
               paramsSb.append(param);
            }
            String registerN     = "'register' is not valid here";
            String registerNameN = "'registerName' is not valid here";
            String registerNAMEN = "'registerNAME' is not valid here";

            {
               // Try to deduce register
               String tempRegister     = deduceCRegister(info.peripheral, variable, info.context);
               String tempRegisterName = deduceRegisterName(info.peripheral, variable, info.context);
               if (tempRegister != null) {
                  registerN     = tempRegister;
//                  registerNameN = tempRegister.replaceAll("([a-zA-Z0-9]*)->", "").toLowerCase();
                  registerNameN = tempRegisterName.toLowerCase();
                  registerNAMEN = tempRegisterName.toUpperCase();
                  if (!registeNameChanged) {
                     if (register == null) {
                        register     = registerN;
                        registerName = registerNameN;
                     }
                     else if (!tempRegister.equals(register)) {
                        registeNameChanged = true;
                        register     = "'register' is conflicted";
                        registerName = "'registerName' is conflicted";
                     }
                  }
               }
            }
            String fieldExtractN = variable.fieldExtractFromRegister(registerN);

            if (paramName == null) {
               paramName = "%paramName"+index+" not available";
            }

            String fieldAssignmentN            = "";
            String constructorFieldAssignmentN = "";
            String configFieldAssignmentN      = "";
            
            if (!mask.isBlank()) {
               //  %register = (%register&~%mask) | %paramExpression;
               fieldAssignmentN    = registerN+" = "+"("+registerN+"&~"+mask+")"+" | %paramName"+index;
               
               //  %registerName = (%registerName&~%mask) | %paramExpression;
               constructorFieldAssignmentN = registerNameN+" = ("+registerNameN+"&~"+mask+") | %paramName"+index;
               
               //  %register = (%register&~%mask) | %registerName;
               configFieldAssignmentN = registerN+" = ("+registerN+"&~"+mask+") | "+"init."+registerNameN;
            }
            else {
               
               //  %register = %paramExpression;
               fieldAssignmentN       = registerN+" = %paramName"+index;
               
               //  %registerName = %paramExpression;
               constructorFieldAssignmentN = registerNameN+" =  %paramName"+index;
               
               //  %registerName = %paramExpression;
               configFieldAssignmentN = registerN+" = "+"init."+registerNameN;
            }

            substitutions.add(0, new StringPair("%baseType"+index,                baseType));
            substitutions.add(0, new StringPair("%defaultValue"+index,            defaultValue));
            substitutions.add(0, new StringPair("%description"+index,             description));
            substitutions.add(0, new StringPair("%macro"+index,                   macro));
            substitutions.add(0, new StringPair("%mask"+index,                    mask));
            substitutions.add(0, new StringPair("%paramDescription"+index,        paramDescriptionN));
            substitutions.add(0, new StringPair("%paramName"+index,               paramName));
            substitutions.add(0, new StringPair("%paramType"+index,               paramType));
            substitutions.add(0, new StringPair("%param"+index,                   param));
            substitutions.add(0, new StringPair("%registerName"+index,            registerNameN));
            substitutions.add(0, new StringPair("%registerNAME"+index,            registerNAMEN));
            substitutions.add(0, new StringPair("%register"+index,                registerN));
            substitutions.add(0, new StringPair("%returnType"+index,              returnType));
            substitutions.add(0, new StringPair("%shortDescription"+index,        shortDescription));
            substitutions.add(0, new StringPair("%symbolicExpression"+index,      symbolicExpression));
            substitutions.add(0, new StringPair("%tooltip"+index,                 tooltip));
            substitutions.add(0, new StringPair("%valueExpression"+index,         valueExpression));
            substitutions.add(0, new StringPair("%variable"+index,                variableKey));
            substitutions.add(0, new StringPair("%fieldExtract"+index,            fieldExtractN));
            
            substitutions.add(0, new StringPair("%fieldAssignment"+index,            fieldAssignmentN));
            substitutions.add(0, new StringPair("%constructorFieldAssignment"+index, constructorFieldAssignmentN));
            substitutions.add(0, new StringPair("%configFieldAssignment"+index,      configFieldAssignmentN));
            
            if (!variableKeys.isEmpty()) {
               variableKeys.append(",");
            }
            variableKeys.append(Variable.getBaseNameFromKey(variableKey));
            if (index == 0) {
               substitutions.add(new StringPair("%baseType",                baseType));
               substitutions.add(new StringPair("%defaultValue",            defaultValue));
               substitutions.add(new StringPair("%paramName",               paramName));
               substitutions.add(new StringPair("%paramType",               paramType));
               substitutions.add(new StringPair("%returnType",              returnType));
               substitutions.add(new StringPair("%registerName",            registerNameN));
               substitutions.add(new StringPair("%registerNAME",            registerNAMEN));
               substitutions.add(new StringPair("%shortDescription",        shortDescription));
               substitutions.add(new StringPair("%tooltip",                 tooltip));
            }
            
            initExpressionSb.append(currentInitExpressionSb.toString());
            
            if (!variable.isDefault()) {
               initNonDefaultExprSb.append(currentInitExpressionSb.toString());
            }
         }
         substitutions.add(new StringPair("%multilineDescription",             multilineDescription.toString()));

         String mask = null;
         if (maskSb.length() > 0) {
            mask = maskSb.toString();
            // If not a simple name/number add brackets
            boolean bracketsRequired = !mask.matches("[a-zA-Z0-9_]*");
            if (bracketsRequired) {
               mask = '('+mask+')';
            }
         }
         String paramExpr = "'paramExpr' is not valid here";
         if (paramExprSb.length()>0) {
            paramExpr = paramExprSb.toString();
         }

         String maskingExpression            = "'maskingExpression' is not valid here";
         String fieldExtract                 = "'fieldExtract' is not valid here";
         String fieldAssignment              = "'fieldAssignment' is not valid here";
         String constructorFieldAssignment   = "'constructorFieldAssignment' is not valid here";
         String configFieldAssignment        = "'constructorFieldAssignment' is not valid here";
         String regAssignment                = "'regAssignment' is not valid here";
         String constructorRegAssignment     = "'constructorRegAssignment' is not valid here";
         String configRegAssignment          = "'configRegAssignment' is not valid here";
         String constructorBitSet            = "'constructorBitSet' is not valid here";

         if (register != null) {
            if (variableList.size()==1) {
               // LongVariable   => ((SIM_SCG_DEL_MASK&<b>registerValue</b>)>>SIM_SCG_DEL_SHIFT)
               // ChoiceVariable => (SIM_SCG_DEL_MASK&<b>registerValue</b>)
               fieldExtract       = variableList.get(0).fieldExtractFromRegister(register);
            }
            //  %register = %paramExpression;
            regAssignment       = register+" = "+paramExpr;
            
            //  %register = init.%registerName (e.g. ftm->PWMLOAD = pwmload;)
            configRegAssignment = register+" = "+"init."+registerName;
            
            //  %registerName = %paramExpression; (e.g. pwmload |= ftmLoadPoint;)
            constructorRegAssignment = registerName+" = "+paramExpr;
            
            //  %registerName |= %paramExpression; (e.g. pwmload |= ftmLoadPoint;)
            constructorBitSet = registerName+" |= "+paramExpr;
            

            if (mask != null) {
               maskingExpression = register+"&"+mask;
               
               //  %register = (%register&~%mask) | %paramExpression;
               fieldAssignment    = register+" = "+"("+register+"&~"+mask+")"+" | "+paramExpr;
               
               //  %registerName = (%registerName&~%mask) | %paramExpression;
               constructorFieldAssignment = registerName+" = ("+registerName+"&~"+mask+") | "+paramExpr;
               
               //  %register = (%register&~%mask) | %registerName;
               configFieldAssignment = register+" = ("+register+"&~"+mask+") | "+"init."+registerName;
            }
            else {
               
               //  %register = %paramExpression;
               fieldAssignment       = register+" = "+paramExpr;
               
               //  %registerName = %paramExpression;
               constructorFieldAssignment = registerName+" = "+paramExpr;
               
               //  %registerName = %paramExpression;
               configFieldAssignment = register+" = "+"init."+registerName;
               
            }
         }
         if (register == null) {
            register     = "'%register' is not valid here";
            registerName = "'%registerName' is not valid here";
         }
         if (mask == null) {
            mask = "'%mask' not available in this template";
         }
         String params = "'%params' is not valid here";
         if (paramsSb.length()>0) {
            if (parmsOnNewLine) {
               paramsSb.insert(0,"\n"+paramPadding);
            }
            params = paramsSb.toString();
         }
         else if (allowEmptyParameterList) {
            params="";
         }
         String paramDescription = "'%comments' is not valid here";
         if (paramDescriptionSb.length()>0) {
            paramDescription = paramDescriptionSb.toString();
         }
         else if (allowEmptyParameterList) {
            paramDescription = "";
         }

         String initExpression = "'%initExpression' is not valid here";
         if (initExpressionSb.length()>0) {
            initExpression = initExpressionSb.toString();
         }

         String initNonDefaultExpression = initNonDefaultExprSb.toString();

         String description = "'%description' is not valid here";
         if (descriptionSb.length()>0) {
            description = descriptionSb.toString();
         }
         substitutions.add(new StringPair("%constructorBitSet",          constructorBitSet));
         substitutions.add(new StringPair("%configFieldAssignment",      configFieldAssignment));
         substitutions.add(new StringPair("%configRegAssignment",        configRegAssignment));
         substitutions.add(new StringPair("%constructorFieldAssignment", constructorFieldAssignment));
         substitutions.add(new StringPair("%constructorRegAssignment",   constructorRegAssignment));
         
         substitutions.add(new StringPair("%fieldExtract",               fieldExtract));
         substitutions.add(new StringPair("%fieldAssignment",            fieldAssignment));
         
         substitutions.add(new StringPair("%description",                description));
         
         substitutions.add(new StringPair("%initExpression",             initExpression));
         substitutions.add(new StringPair("%initNonDefaultExpression",   initNonDefaultExpression));
         substitutions.add(new StringPair("%maskingExpression",          maskingExpression));
         substitutions.add(new StringPair("%mask",                       mask));
         substitutions.add(new StringPair("%paramDescription",           paramDescription));
         substitutions.add(new StringPair("%paramExpression",            paramExpr));
         substitutions.add(new StringPair("%params",                     params));
         
         substitutions.add(new StringPair("%registerName",               registerName));
         substitutions.add(new StringPair("%regAssignment",              regAssignment));
         substitutions.add(new StringPair("%register",                   register));
         
         substitutions.add(new StringPair("%symbolicExpression",         symbolicExpressionSb.toString()));
         
         substitutions.add(new StringPair("%valueExpression",            valueExpressionSb.toString()));
         substitutions.add(new StringPair("%variables",                  variableKeys.toString()));
      }
      
      return substitutions;
   }

   /**
    * Apply a set of template substitutions of form <b>%name</b> in template text
    * 
    * @param text          Text to modify
    * @param substitutions Substitutions to do
    * 
    * @return Modified test
    */
   public static String doTemplateSubstitutions(String text, List<StringPair> substitutions) {
      
      if (text == null) {
         return null;
      }
      if (substitutions == null) {
         return text;
      }
      for (StringPair p:substitutions) {
         if (p.key==null) {
            System.err.println("key is null, value = "+p.value);
         }
         if (p.value==null) {
            System.err.println("value is null, res = "+p.key);
         }
         String pattern     = Pattern.quote(p.key)+"(\\W|_)";
         String replacement = Matcher.quoteReplacement(p.value)+"$1";
         Matcher m = Pattern.compile(pattern).matcher(text);
         boolean doneReplacement = false;
         StringBuffer sb = new StringBuffer();
         while (m.find()) {
             m.appendReplacement(sb, replacement);
             doneReplacement = true;
         }
         if (doneReplacement) {
            m.appendTail(sb);
            text = sb.toString();
         }
      }
      return text;
   }

   private static String getTemplateCaseStatement(Variable var) throws Exception {

      String returnFormat = "%s";
      String caseBody = "%body (case statement) not available here";

      if (var == null) {
         return caseBody + "(var not present)";
      }
      if (!(var instanceof VariableWithChoices)) {
         return caseBody + "(Var not of correct type)";
      }
      VariableWithChoices choiceVar = (VariableWithChoices) var;

      StringBuilder caseBodySb = new StringBuilder();
      String typeName = choiceVar.getTypeName();
      if ((typeName==null)||typeName.isBlank()) {
         return caseBody + "(No typeName)";
      }
      ChoiceData[] choiceData = choiceVar.getChoiceData();

      String[] enumNames      = new String[choiceData.length];
      String[] returnValues   = new String[choiceData.length];

      String   comment;
      int enumNameMax    = 0;
      int returnValueMax = 0;

      // Create body for case statement
      for (int index=0; index<choiceData.length; index++) {

         String enumName  = choiceData[index].getEnumName();
         String codeValue = choiceData[index].getCodeValue();
         if ((enumName == null) || (codeValue == null)) {
            throw new Exception("Choice '"+choiceData[index].getName()+"' is missing enum/code value in "+choiceVar);
         }
         enumNames[index]     = typeName+"_"+enumName;
         enumNameMax          = Math.max(enumNameMax, enumNames[index].length());
         returnValues[index]  = String.format(returnFormat+";", codeValue);
         returnValueMax       = Math.max(returnValueMax, returnValues[index].length());
      }
      final String format = "\\t      case %-"+enumNameMax+"s : return %-"+returnValueMax+"s %s\n";
      for (int index=0; index<choiceData.length; index++) {
         comment  = "///< "+choiceData[index].getName();
         caseBodySb.append(String.format(format, enumNames[index], returnValues[index], comment));
      }
      return caseBodySb.toString();
   }
   
   /**
    * Try to determine entire register for use in C code e.g. sim->SOPT1
    * 
    * @param currentPeripheral   Current peripheral
    * @param controlVar          Variable to obtain information from
    * @param context             Context for the field
    * 
    * @note The controlVar is used to obtain an (optional) register name.<br>
    *       The register attribute name may be necessary as some registers have '_' as part of their<br>
    *       name and slicing on '_' is ambiguous.  <br>
    *       If not provided, the register name is assumed to not contain '_'.
    * 
    * @return Full register or null if not deduced e.g. SIM->SOPT4
    * 
    * @throws Exception if unable to deduce
    */
   private static String deduceCRegister(Peripheral currentPeripheral, Variable controlVar, String context) throws Exception {

      if (controlVar instanceof IrqVariable) {
         return "callbackFunction";
      }
      if (context == null) {
         // No context - return unchanged
         context = "%s";
      }
      String register = null;
      String variableKey  = controlVar.getBaseNameFromKey();
      String registerName = controlVar.getRegister();

      if (registerName != null) {
         Pattern p = Pattern.compile("(.+)_"+registerName+"_(.+)");
         Matcher m = p.matcher(variableKey);
         if (m.matches()) {
            register = m.group(1)+"->"+String.format(context, registerName.toUpperCase());
         }
         else {
            throw new Exception("Unable to match register name "+registerName+" against "+variableKey);
         }
      }
      else {
         // Try some likely candidates
         String peripherals[] = {
               "port",
               "nvic",
               currentPeripheral.getName().toLowerCase(),      // e.g. FTM2
               currentPeripheral.getBaseName().toLowerCase()}; // e.g. FTM0 => FTM, PTA => PT
         for (String peripheral:peripherals) {
            Pattern p = Pattern.compile("^"+peripheral+"_([a-zA-Z0-9]*)(_(.+))?$");
            Matcher m = p.matcher(variableKey);
            if (m.matches()) {
               register = peripheral+"->"+String.format(context, m.group(1).toUpperCase());
               break;
            }
         }
      }
      return register;
   }
   
   /**
    * Try to deduce the name of the register associated with a variable (field)
    * 
    * @param currentPeripheral Current peripheral
    * @param controlVar        The variable being examined
    * @param context           Context for the field
    * 
    * @return                  Name of register e.g. SOPT4
    * 
    * @throws Exception
    */
   private static String deduceRegisterName(Peripheral currentPeripheral, Variable controlVar, String context) throws Exception {

      if (controlVar instanceof IrqVariable) {
         return "callbackFunction";
      }
      if (context == null) {
         // No context - return unchanged
         context = "%s";
      }
      String register = null;
      String variableKey  = controlVar.getBaseNameFromKey();
      String registerName = controlVar.getRegister();

      if (registerName != null) {
         Pattern p = Pattern.compile("(.+)_"+registerName+"_(.+)");
         Matcher m = p.matcher(variableKey);
         if (m.matches()) {
            register = registerName.toUpperCase();
         }
         else {
            throw new Exception("Unable to match register name "+registerName+" against "+variableKey);
         }
      }
      else {
         // Try some likely candidates
         String peripherals[] = {
               "port",
               "nvic",
               currentPeripheral.getName().toLowerCase(),      // e.g. FTM2
               currentPeripheral.getBaseName().toLowerCase()}; // e.g. FTM0 => FTM, PTA => PT
         for (String peripheral:peripherals) {
            Pattern p = Pattern.compile("^"+peripheral+"_([a-zA-Z0-9]*)(_(.+))?$");
            Matcher m = p.matcher(variableKey);
            if (m.matches()) {
               register = m.group(1);
               break;
            }
         }
      }
      return register;
   }
   static class ChoiceEnumBuilder extends TemplateContentBuilder {
      
      /// Format string with parameters: description, tool-tip, enumClass, body
      public final static String fullEnumTemplate = ""
          /*                  */ + " \\t/**\n"
          /*  Description     */ + " \\t * %s\n"
          /*  Variable names  */ + " \\t * (%s)\n"
          /*                  */ + " \\t *\n"
          /*  Tooltip         */ + " \\t * %s\n"
          /*                  */ + " \\t */\n"
          /*  type,enumtype   */ + " \\tenum %s%s {\n"
          /*  body            */ + " %s"
          /*                  */ + " \\t};\\n\\n\n";

      /// Format string with parameters: description, tool-tip, enumClass, body
      public final static String prefixTemplate = ""
          /*                  */ + " \\t/**\n"
          /*  Description     */ + " \\t * %s\n"
          /*  Variable names  */ + " \\t * (%s)\n"
          /*                  */ + " \\t *\n"
          /*  Tooltip         */ + " \\t * %s\n"
          /*                  */ + " \\t */\n";

      /// Format string with parameters: description, tool-tip, enumClass, body
      public final static String enumTemplate = ""
          /*  type,enumtype   */ + " \\tenum %s%s {\n"
          /*  body            */ + " %s"
          /*                  */ + " \\t};\\n\\n\n";

      /// Format string with parameters: description, tool-tip, enumClass, body
      public final static String constantTemplate = ""
            + "      \\t/**\n"
            + "      \\t * %s\n"
            + "      \\t *\n"
            + "      \\t * %s\n"
            + "      \\t */\n"
            + "      %s\\n\\n\n";
      
      public final static String guardedEnumTemplate = ""
            + "#if %s\n"
            + "%s"
            + "#endif\\n\\n\n";

      
      protected final Variable            fVariable;
      protected final String              fBaseType;
      protected final String              fValueFormat;
      protected final String              fEnumText;
      protected final String              fEnumGuard;
      protected final DeviceInfo          fDeviceInfo;
      protected final boolean             fGenerateAsConstants;

      public ChoiceEnumBuilder(ParseMenuXML parser, Element varElement, Variable variable) throws Exception {
         
         fGenerateAsConstants = parser.getAttributeAsBoolean(varElement, "generateAsConstants", false);
         fBaseType            = parser.getAttributeAsString(varElement,  "baseType");
         fEnumText            = parser.getAttributeAsString(varElement,  "enumText", null);
         fEnumGuard           = parser.getAttributeAsString(varElement,  "enumGuard");
         fVariable            = variable;
         fDeviceInfo          = parser.getDeviceInfo();

//         String macroName = Variable.getBaseNameFromKey(variable.getKey()).toUpperCase();
         fValueFormat = variable.getValueFormat();// parser.getAttributeAsString(varElement, "valueFormat", macroName+"(%s)");
      }
      
      static class CheckRepeats {
         
         static class Info {
            
         final String fName;
         final String fValue;
         final String fComment;
            
            Info(String name, String value, String comment) {
               fName    = name;
               fValue   = value;
               fComment = comment;
            }
         };
         
         ArrayList<Info> namesList = new ArrayList<Info>();
         
         private int valueWidth    = 0;
         private int nameWidth     = 0;
         
         /**
          * Add entry to list of not already present
          * 
          * @param name       Name of  enum/constant
          * @param value      Value for enum/constant
          * @param comment    Comment for enum/constant
          * 
          * @return  TRUE => Added to list, FALSE => Entry with same name already present
          */
         Info add(String name, String value, String comment) {
            for (Info existingName:namesList) {
               if (existingName.fName.equalsIgnoreCase(name)) {
                  return null;
               }
            }
            nameWidth  = Math.max(nameWidth,  name.length()+2);
            valueWidth = Math.max(valueWidth, value.length()+2);
            
            Info entry  = new Info(name, value, comment);
            namesList.add(entry);
            return entry;
         }

         public Info get(int index) {
            return namesList.get(index);
         }

         public int size() {
            return namesList.size();
         }
      };
      
      @Override
      public String build() throws Exception {
         
         String typeName   = fVariable.getTypeName();
         String enumClass  = Character.toUpperCase(typeName.charAt(0)) + typeName.substring(1);

         VariableWithChoices varWithChoices = (VariableWithChoices) fVariable;
         
         ArrayList<ChoiceData[]> lists = new ArrayList<ChoiceData[]>();
         lists.add(varWithChoices.getChoiceData());
         ChoiceData[] hiddenChoices = varWithChoices.getHiddenChoiceData();
         if (hiddenChoices != null) {
            lists.add(hiddenChoices);
         }
         
         // Accumulate enum information
         CheckRepeats enumNamesList = new CheckRepeats();
         
         for (ChoiceData[] choiceData:lists) {
            if (choiceData == null) {
               continue;
            }
            for (int index=0; index<choiceData.length; index++) {
               ChoiceData choice = choiceData[index];
               String enumName = choice.getEnumName();
               if ((enumName == null) || enumName.isBlank()) {
                  throw new Exception("enumTemplate - enum data is incomplete in choice '" + choice.getName() + "' ='"+varWithChoices+"'");
               }
               if (enumName.equals("-deleted-")) {
                  continue;
               }
               String completeEnumName = enumClass+"_"+enumName;
               String completeValue = varWithChoices.getDefinitionValue(choice);
               
               CheckRepeats.Info entry  = enumNamesList.add(completeEnumName, completeValue, choice.getName());
               if (entry == null) {
                  throw new Exception("Repeated base enum!");
               }
               String hardwareName = choice.getAssociatedHardware();
               if (hardwareName != null) {
                  Object hardware = null;
                  
                  if ((hardwareName != null)&&(!hardwareName.isBlank())) {
                     // Try Signal
                     hardware = fDeviceInfo.getSignal(hardwareName);
                     if (hardware == null) {
                        // Try Peripheral
                        hardware = fDeviceInfo.getPeripheral(hardwareName);
                     }
                     if (hardware == null) {
                        throw new Exception("Unable to find signal or peripheral '"+hardwareName+
                              "' associated with choice '" + varWithChoices);
                     }
                  }
                  if (hardware instanceof Signal) {
                     Signal signal = (Signal) hardware;
                     if (signal != null) {
                        String codeNames = signal.getCodeIdentifier();
                        if ((codeNames != null)&&!codeNames.isBlank()) {
                           for (String codeName:codeNames.split("/")) {
                              String newEnumName = enumClass+"_"+codeName;
                              String desc = signal.getUserDescription();
                              if ((desc == null)||desc.isBlank()) {
                                 desc = choice.getName();
                              }
                              entry = enumNamesList.add(newEnumName, completeValue, desc);
                           }
                        }
                        Pin pin = signal.getMappedPin();
                        String pinName = null;
                        if ((pin != null)&&(pin != Pin.UNASSIGNED_PIN)) {
                           pinName = pin.getName();
                        }
                        if ((pinName != null)&&!pinName.isBlank()) {
                           String newEnumName = enumClass+"_"+pinName;
                           String desc = "Pin "+pinName;
                           entry = enumNamesList.add(newEnumName, completeValue, desc);
                        }
                     }
                  }
                  else if (hardware instanceof Peripheral) {
                     Peripheral peripheral = (Peripheral) hardware;
                     if (peripheral != null) {
                        String codeNames = peripheral.getCodeIdentifier();
                        if ((codeNames != null)&&!codeNames.isBlank()) {
                           for(String codeName:codeNames.split("/")) {
                              String newEnumName = enumClass+"_"+codeName;
                              String desc = choice.getName();
                              entry = enumNamesList.add(newEnumName, completeEnumName, desc);
                           }
                        }
                     }
                  }
                  else {
                     throw new Exception("Unexpected hardware type for '"+hardwareName+
                           "' associated with choice '" + varWithChoices);
                  }
               }
            }
         }
         // Create enum body
         StringBuilder body = new StringBuilder();

         for (int index=0; index<enumNamesList.size(); index++) {
            CheckRepeats.Info entry = enumNamesList.get(index);
            if (fGenerateAsConstants) {
               body.append(String.format("\\tstatic constexpr %s %-"+enumNamesList.nameWidth+"s = %-"+enumNamesList.valueWidth+"s ///< %s\n",
                     fBaseType, entry.fName, entry.fValue+";", entry.fComment));
            }
            else {
               body.append(String.format("\\t   %-"+enumNamesList.nameWidth+"s = %-"+enumNamesList.valueWidth+"s ///< %s\n",
                     entry.fName, entry.fValue+",", entry.fComment));
            }
         }
         // Add enum text
         if (fEnumText != null) {
            
            String enumText = fEnumText;
            enumText = enumText.replace("%(typeName)",  typeName);
            enumText = enumText.replaceAll("\\\\n",  "XXXX");
            
            body.append(enumText+"\n");
         }
         String baseType = "";
         if (fBaseType != null) {
            baseType = " : "+fBaseType;
         }
         StringBuilder sb = new StringBuilder();
         
         String description   = XML_BaseParser.escapeString(varWithChoices.getDescriptionAsCode());
         String tooltip       = XML_BaseParser.escapeString(varWithChoices.getToolTipAsCode());

         // Prefix with comments
         sb.append(String.format(prefixTemplate, description, varWithChoices.getName(), tooltip));

         Boolean useEnumClass = fVariable.useEnumClass();

         if (fGenerateAsConstants) {
            sb.append(body);
            sb.append("\\n\\n");
         }
         else {
            sb.append(String.format(enumTemplate, (useEnumClass?"class ":"")+enumClass, baseType, body.toString()));
         }
         
         // Create entire declaration
         String entireDeclaration = sb.toString();
         
         if (fEnumGuard != null) {
            // Surround with guard
            entireDeclaration = String.format(guardedEnumTemplate, fEnumGuard, entireDeclaration);
         }
         return entireDeclaration;
      }
   };
   
   static class BitmaskEnumBuilder extends ChoiceEnumBuilder {

      private final boolean fEmptyEnum;

      public BitmaskEnumBuilder(ParseMenuXML parser, Element varElement, Variable variable)
            throws Exception {
         super(parser, varElement, variable);
         
         String doEnum = parser.getAttributeAsString(varElement, "generateEnum", "true");
         fEmptyEnum = "empty".equalsIgnoreCase(doEnum);
      }

      @Override
      public String build() throws Exception {
         
         BitmaskVariable bmv = (BitmaskVariable) fVariable;
         String typeName   = bmv.getTypeName();
         String enumClass  = Character.toUpperCase(typeName.charAt(0)) + typeName.substring(1);

         BitmaskVariable variable = bmv;
         BitInformation bitInformation = variable.getFinalBitInformation();

         Boolean useEnumClass = bmv.useEnumClass();

         // Accumulate enum information
         CheckRepeats enumNamesList = new CheckRepeats();

//         CheckRepeats.Info entry = enumNamesList.add((useEnumClass?"":enumClass+"_")+"None", "0", "None");
         
         for (int index=0; index<bitInformation.bits.length; index++) {
//            String completeValue    = bmv.formatUsageValue(Integer.toString(1<<bitMap[index]));
            String completeEnumName = (useEnumClass?"":enumClass+"_")+ParseMenuXML.makeSafeIdentifierName(bitInformation.bits[index].bitName);
            String completeValue    = bmv.getDefinitionValue(bitInformation.bits[index]);
            String description      = bitInformation.bits[index].description;
            CheckRepeats.Info entry = enumNamesList.add(completeEnumName, completeValue, description);
            if (entry == null) {
               throw new Exception("Repeated enum name '"+completeEnumName+"'");
            }
         }
         // Create enum body
         StringBuilder body = new StringBuilder();
         
         if (!fEmptyEnum) {
            for (int index=0; index<enumNamesList.size(); index++) {
               CheckRepeats.Info entry = enumNamesList.get(index);
               if (fGenerateAsConstants) {
                  body.append(String.format("\\tstatic constexpr %s %-"+enumNamesList.nameWidth+"s = %-"+enumNamesList.valueWidth+"s ///< %s\n",
                        fBaseType, entry.fName, entry.fValue+";", entry.fComment));
               }
               else {
                  body.append(String.format("\\t   %-"+enumNamesList.nameWidth+"s = %-"+enumNamesList.valueWidth+"s ///< %s\n",
                        entry.fName, entry.fValue+",", entry.fComment));
               }
            }
         }
       
         // Add enum text
         if (fEnumText != null) {
            String enumText = fEnumText;
            enumText = enumText.replace("%(typeName)",  typeName);
            enumText = enumText.replaceAll("\\\\n[ ]*\\\\t",  "\n\\\\t");
            
            body.append(enumText+"\n");
         }
         String baseType = "";
         if (fBaseType != null) {
            baseType = " : "+fBaseType;
         }
         String description   = XML_BaseParser.escapeString(variable.getDescriptionAsCode());
         String tooltip       = XML_BaseParser.escapeString(variable.getToolTipAsCode());

         StringBuilder sb = new StringBuilder();
         
         // Prefix with comments
         sb.append(String.format(prefixTemplate, description, variable.getName(), tooltip));
         
         if (fGenerateAsConstants) {
            sb.append(body);
            sb.append("\\n\\n");
         }
         else {
            sb.append(String.format(enumTemplate, (useEnumClass?"class ":"")+enumClass, baseType, body.toString()));
         }
         
         if (useEnumClass) {
            String typeOperators = ""
                  + "\\t/**\\n"
                  + "\\t * Combines two %typename values (by ORing)\\n"
                  + "\\t * Used to create new %typename mask\\n"
                  + "\\t * \\n"
                  + "\\t * @param left    Left operand\\n"
                  + "\\t * @param right   Right operand\\n"
                  + "\\t * \\n"
                  + "\\t * @return  Combined value\\n"
                  + "\\t */\\n"
                  + "\\tconstexpr %typename operator|(%typename left, %typename right) {\\n"
                  + "\\t   return %typename(long(left)|long(right));\\n"
                  + "\\t}\\n"
                  + "\\t\\n"
                  + "\\t/**\\n"
                  + "\\t * Combines two %typename values (by ANDing) to produce a bool result\\n"
                  + "\\t * Used to check a value against a %typename mask\\n"
                  + "\\t * \\n"
                  + "\\t * @param left    Left operand\\n"
                  + "\\t * @param right   Right operand\\n"
                  + "\\t * \\n"
                  + "\\t * @return boolean value indicating if the result is non-zero\\n"
                  + "\\t */\\n"
                  + "\\tconstexpr bool operator&(%typename left, %typename right) {\\n"
                  + "\\t   return bool(long(left)&long(right));\\n"
                  + "\\t}\\n"
                  + "\\t\\n";
            typeOperators = typeOperators.replace("%typename", typeName);
            sb.append(typeOperators);
         }
         // Create entire declaration
         String entireDeclaration = sb.toString();
         
         if (fEnumGuard != null) {
            // Surround with guard
            entireDeclaration = String.format(guardedEnumTemplate, fEnumGuard, entireDeclaration);
         }
         return entireDeclaration;
      }
   }
   
}