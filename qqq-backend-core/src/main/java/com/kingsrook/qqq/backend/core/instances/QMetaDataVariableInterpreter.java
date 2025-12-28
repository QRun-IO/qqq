/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.backend.core.instances;


import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;


/*******************************************************************************
 ** To avoid having secrets (passwords, access keys, etc) committed into meta data
 ** files, as well as to just let some meta data not be hard-coded, this class is
 ** used by the Enricher to "promote" values, such as ${env.ACCESS_KEY}
 ** to be read from the environment (or other secret providers (to be implemented)).
 **
 ** Supported syntax / value sources are:
 ** ${env.VAR} = system environment variables, e.g., export VAR=val
 ** ${prop.VAR} = properties, e.g., -DVAR=val
 ** ${literal.VAR} = get back a literal "VAR" (in case VAR matches some of the other supported syntax in here)
 *******************************************************************************/
public class QMetaDataVariableInterpreter
{
   private static final QLogger LOG = QLogger.getLogger(QMetaDataVariableInterpreter.class);

   private Map<String, String>                    environmentOverrides;
   private Map<String, Map<String, Serializable>> valueMaps;



   /*******************************************************************************
    **
    *******************************************************************************/
   public QMetaDataVariableInterpreter()
   {
      environmentOverrides = new HashMap<>();
      Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
      for(DotenvEntry e : dotenv.entries())
      {
         environmentOverrides.put(e.getKey(), e.getValue());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void interpretObject(Object o) throws QException
   {
      ///////////////////////////////////////////////////////////////////////////////////////////////////
      // get the InterpretableFields from the object's class - exiting if the annotation isn't present //
      ///////////////////////////////////////////////////////////////////////////////////////////////////
      InterpretableFields interpretableFields = o.getClass().getAnnotation(InterpretableFields.class);
      if(interpretableFields == null)
      {
         return;
      }

      //////////////////////////////////////////////////////////
      // iterate over interpretable fields, interpreting each //
      //////////////////////////////////////////////////////////
      for(String fieldName : interpretableFields.fieldNames())
      {
         try
         {
            ///////////////////////////////////////////////////////////////////////////////////////
            // get the getter & setter methods for the field (getMethod will throw if not found) //
            // enforce Strings-only at this time.                                                //
            ///////////////////////////////////////////////////////////////////////////////////////
            String   fieldNameUcFirst = fieldName.substring(0, 1).toUpperCase(Locale.ROOT) + fieldName.substring(1);
            Method   getter           = o.getClass().getMethod("get" + fieldNameUcFirst);
            Class<?> fieldType        = getter.getReturnType();
            if(!fieldType.equals(String.class))
            {
               throw new QException("Interpretable field: " + fieldName + " on class " + o.getClass() + " is not a String (which is required at this time)");
            }
            Method setter = o.getClass().getMethod("set" + fieldNameUcFirst, fieldType);

            //////////////////////////////////////////////////////////////////////////////////////////////
            // get the value - if it's null, move on, else, interpret it, and put it back in the object //
            //////////////////////////////////////////////////////////////////////////////////////////////
            Object value = getter.invoke(o);
            if(value == null)
            {
               continue;
            }
            String interpreted = interpret((String) value);
            setter.invoke(o, interpreted);
         }
         catch(Exception e)
         {
            throw (new QException("Error interpreting variables in object " + o, e));
         }
      }
   }



   /*******************************************************************************
    ** Interpret a value string, which may be a variable, into its run-time value -
    ** always as a String.
    **
    *******************************************************************************/
   public String interpret(String value)
   {
      return (ValueUtils.getValueAsString(interpretForObject(value)));
   }



   /*******************************************************************************
    ** Interpret a value string, which may be a variable, into its run-time value.
    **
    ** If input is null, output is null.
    ** If input looks like ${env.X}, then the return value is the value of the env variable 'X'
    ** If input looks like ${prop.X}, then the return value is the value of the system property 'X'
    ** If input looks like ${literal.X}, then the return value is the literal 'X'
    **  - used if you really want to get back the literal value, ${env.X}, for example.
    ** Else the output is the input.
    *******************************************************************************/
   public Serializable interpretForObject(String value)
   {
      return (interpretForObject(value, null));
   }



   /*******************************************************************************
    ** Interpret a value string, which may be a variable, into its run-time value,
    ** getting back the specified default if the string looks like a variable, but can't
    ** be found.  Where "looks like" means, for example, started with "${env." and ended
    ** with "}", but wasn't set in the environment, or, more interestingly, based on the
    ** valueMaps - only if the name to the left of the dot is an actual valueMap name.
    ** additional valueMaps can be added via @see #addValueMap. also, more than one value
    ** can be specified in the same string, separated by "??", and the first one found
    ** in the valueMaps will be used. (e.g. "${input.clientId}??${processValues.possibleValueFilterValueClientId}"
    **
    ** If input is null, output is null.
    ** If input looks like ${env.X}, then the return value is the value of the env variable 'X'
    ** If input looks like ${prop.X}, then the return value is the value of the system property 'X'
    ** If input looks like ${literal.X}, then the return value is the literal 'X'
    **  - used if you really want to get back the literal value, ${env.X}, for example.
    ** Else the output is the input.
    *******************************************************************************/
   public Serializable interpretForObject(String value, Serializable defaultIfLooksLikeVariableButNotFound)
   {
      if(value == null)
      {
         return (null);
      }

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // Support "null coalescing" with "??" (e.g., ${env.X}??${prop.X}??${input.X}), returning the first resolved //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
      if(value.contains("??"))
      {
         return interpretForObjectNullCoalescingScenario(value, defaultIfLooksLikeVariableButNotFound);
      }

      String envPrefix = "${env.";
      if(value.startsWith(envPrefix) && value.endsWith("}"))
      {
         String envVarName = value.substring(envPrefix.length()).replaceFirst("}$", "");
         String result     = getEnvironmentVariable(envVarName);
         return (result == null ? defaultIfLooksLikeVariableButNotFound : result);
      }

      String propPrefix = "${prop.";
      if(value.startsWith(propPrefix) && value.endsWith("}"))
      {
         String propertyName = value.substring(propPrefix.length()).replaceFirst("}$", "");
         String result       = System.getProperty(propertyName);
         return (result == null ? defaultIfLooksLikeVariableButNotFound : result);
      }

      String literalPrefix = "${literal.";
      if(value.startsWith(literalPrefix) && value.endsWith("}"))
      {
         return (value.substring(literalPrefix.length()).replaceFirst("}$", ""));
      }

      if(valueMaps != null)
      {
         boolean looksLikeVariable = false;
         for(Map.Entry<String, Map<String, Serializable>> entry : valueMaps.entrySet())
         {
            String                    name     = entry.getKey();
            Map<String, Serializable> valueMap = entry.getValue();

            String prefix = "${" + name + ".";
            if(value.startsWith(prefix) && value.endsWith("}"))
            {
               looksLikeVariable = true;
               String lookupName = value.substring(prefix.length()).replaceFirst("}$", "");
               if(valueMap != null && valueMap.containsKey(lookupName))
               {
                  return (valueMap.get(lookupName));
               }
            }
         }

         if(looksLikeVariable)
         {
            return (defaultIfLooksLikeVariableButNotFound);
         }
      }

      return (value);
   }



   /***************************************************************************
    * Support "null coalescing" with "??" (e.g., ${env.X}??${prop.X}??${input.X}),
    * returning the first resolved non-null value (and for Strings, the first
    * non-empty that is not the same literal variable token).
    ***************************************************************************/
   private Serializable interpretForObjectNullCoalescingScenario(String value, Serializable defaultIfLooksLikeVariableButNotFound)
   {
      String[]    split                    = value.split("\\?\\?");
      boolean     anyPartLooksLikeVariable = false;
      Set<String> allInputMaps             = new HashSet<>(Set.of("env", "prop"));
      if(valueMaps != null)
      {
         allInputMaps.addAll(valueMaps.keySet());
      }

      for(String part : split)
      {
         Serializable result = interpretForObject(part, defaultIfLooksLikeVariableButNotFound);
         if(!Objects.equals(result, defaultIfLooksLikeVariableButNotFound) && result != null)
         {
            ////////////////////////////////////////////////////////////////////////////////////////////
            // If result is a String, ensure it has content and is not the same literal we passed in. //
            ////////////////////////////////////////////////////////////////////////////////////////////
            if(result instanceof String s)
            {
               if(StringUtils.hasContent(s) && !s.equals(part))
               {
                  return (result);
               }
            }
            else
            {
               return (result);
            }
         }

         if(!anyPartLooksLikeVariable)
         {
            for(String inputMapName : allInputMaps)
            {
               String prefix = "${" + inputMapName + ".";
               if(part.startsWith(prefix) && part.endsWith("}"))
               {
                  anyPartLooksLikeVariable = true;
                  break;
               }
            }
         }
      }

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // if we make it here and haven't returned, return either the defaultIfLooksLikeVariableButNotFound, or the input value. //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      return (anyPartLooksLikeVariable ? defaultIfLooksLikeVariableButNotFound : value);
   }



   /*******************************************************************************
    ** Setter for environmentOverrides - protected - meant to be called (at least at this
    ** time), only in unit test
    **
    *******************************************************************************/
   protected void setEnvironmentOverrides(Map<String, String> environmentOverrides)
   {
      this.environmentOverrides = environmentOverrides;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String getEnvironmentVariable(String key)
   {
      if(this.environmentOverrides.containsKey(key))
      {
         return (this.environmentOverrides.get(key));
      }

      return System.getenv(key);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void addValueMap(String name, Map<String, Serializable> values)
   {
      if(valueMaps == null)
      {
         valueMaps = new LinkedHashMap<>();
      }

      valueMaps.put(name, values);
   }



   /*******************************************************************************
    ** First look for a string in the specified system property -
    ** Next look for a string in the specified env var name -
    ** Finally return the default.
    *******************************************************************************/
   public String getStringFromPropertyOrEnvironment(String systemPropertyName, String environmentVariableName, String defaultIfNotSet)
   {
      String propertyValue = System.getProperty(systemPropertyName);
      if(StringUtils.hasContent(propertyValue))
      {
         LOG.info("Read system property [" + systemPropertyName + "] as [" + propertyValue + "].");
         return (propertyValue);
      }

      String envValue = interpret("${env." + environmentVariableName + "}");
      if(StringUtils.hasContent(envValue))
      {
         LOG.info("Read env var [" + environmentVariableName + "] as [" + envValue + "].");
         return (envValue);
      }

      return defaultIfNotSet;
   }



   /*******************************************************************************
    ** First look for a boolean ("true" or "false") in the specified system property -
    ** Next look for a boolean in the specified env var name -
    ** Finally return the default.
    *******************************************************************************/
   public boolean getBooleanFromPropertyOrEnvironment(String systemPropertyName, String environmentVariableName, boolean defaultIfNotSet)
   {
      String propertyValue = System.getProperty(systemPropertyName);
      if(StringUtils.hasContent(propertyValue))
      {
         if("false".equalsIgnoreCase(propertyValue))
         {
            LOG.info("Read system property [" + systemPropertyName + "] as boolean false.");
            return (false);
         }
         else if("true".equalsIgnoreCase(propertyValue))
         {
            LOG.info("Read system property [" + systemPropertyName + "] as boolean true.");
            return (true);
         }
         else
         {
            LOG.warn("Unrecognized boolean value [" + propertyValue + "] for system property [" + systemPropertyName + "].");
         }
      }

      String envValue = interpret("${env." + environmentVariableName + "}");
      if(StringUtils.hasContent(envValue))
      {
         if("false".equalsIgnoreCase(envValue))
         {
            LOG.info("Read env var [" + environmentVariableName + "] as boolean false.");
            return (false);
         }
         else if("true".equalsIgnoreCase(envValue))
         {
            LOG.info("Read env var [" + environmentVariableName + "] as boolean true.");
            return (true);
         }
         else
         {
            LOG.warn("Unrecognized boolean value [" + envValue + "] for env var [" + environmentVariableName + "].");
         }
      }

      return defaultIfNotSet;
   }



   /*******************************************************************************
    ** First look for an Integer in the specified system property -
    ** Next look for an Integer in the specified env var name -
    ** Finally return the default (null allowed as default!)
    *******************************************************************************/
   public Integer getIntegerFromPropertyOrEnvironment(String systemPropertyName, String environmentVariableName, Integer defaultIfNotSet)
   {
      String propertyValue = System.getProperty(systemPropertyName);
      if(StringUtils.hasContent(propertyValue))
      {
         if(canParseAsInteger(propertyValue))
         {
            LOG.info("Read system property [" + systemPropertyName + "] as integer " + propertyValue);
            return (Integer.parseInt(propertyValue));
         }
         else
         {
            LOG.warn("Unrecognized integer value [" + propertyValue + "] for system property [" + systemPropertyName + "].");
         }
      }

      String envValue = interpret("${env." + environmentVariableName + "}");
      if(StringUtils.hasContent(envValue))
      {
         if(canParseAsInteger(envValue))
         {
            LOG.info("Read env var [" + environmentVariableName + "] as integer " + environmentVariableName);
            return (Integer.parseInt(envValue));
         }
         else
         {
            LOG.warn("Unrecognized integer value [" + envValue + "] for env var [" + environmentVariableName + "].");
         }
      }

      return defaultIfNotSet;
   }



   /*******************************************************************************
    ** we'd use NumberUtils.isDigits, but that doesn't allow negatives, or
    ** numberUtils.isParseable, but that allows decimals, so...
    *******************************************************************************/
   private boolean canParseAsInteger(String value)
   {
      if(value == null)
      {
         return (false);
      }

      return (value.matches("^-?[0-9]+$"));
   }

}
