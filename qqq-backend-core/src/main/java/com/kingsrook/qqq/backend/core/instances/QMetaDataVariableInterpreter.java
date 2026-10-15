/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kingsrook.qqq.backend.core.instances;


import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
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
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** To avoid having secrets (passwords, access keys, etc) committed into meta data
 ** files, as well as to just let some meta data not be hard-coded, this class is
 ** used by the Enricher to "promote" values, such as ${env.ACCESS_KEY}
 ** to be read from the environment (or other secret providers (to be implemented)).
 **
 ** Supported syntax / value sources are:
 ** ${env.VAR} = system environment variables, e.g., export VAR=val
 ** ${prop.VAR} = properties, e.g., -DVAR=val
 ** ${file.PATH} = the trimmed contents of a mounted secret file, e.g., a
 **   Kubernetes secret volume (${file./var/run/secrets/storage/access-key}) or a
 **   Docker secret (${file./run/secrets/db-password}).  Read-only; the path must
 **   be absolute and inside an allowed root (default /var/run/secrets and
 **   /run/secrets; override with the qqq.fileReference.roots system property or
 **   the QQQ_FILE_REFERENCE_ROOTS environment variable, comma-separated), and the
 **   file at most FILE_REFERENCE_MAX_BYTES.  The file is read on every
 **   interpretation (so a rotated secret is picked up), and its contents are
 **   never logged.  Prefer env or a secret manager where they fit; this is for
 **   orchestrator-mounted secret files only, not for application data.
 **   An interpreter with value maps (one that resolves ${input.X} and similar
 **   values from users, e.g., in query filters and report formulas) does not
 **   read files unless setFileReferencesEnabled(true) is called on it.
 ** ${literal.VAR} = get back a literal "VAR" (in case VAR matches some of the other supported syntax in here)
 *******************************************************************************/
public class QMetaDataVariableInterpreter
{
   private static final QLogger LOG = QLogger.getLogger(QMetaDataVariableInterpreter.class);

   public static final int          FILE_REFERENCE_MAX_BYTES      = 1024 * 1024;
   public static final String       FILE_REFERENCE_ROOTS_PROPERTY = "qqq.fileReference.roots";
   public static final String       FILE_REFERENCE_ROOTS_ENV      = "QQQ_FILE_REFERENCE_ROOTS";
   public static final List<String> DEFAULT_FILE_REFERENCE_ROOTS  = List.of("/var/run/secrets", "/run/secrets");

   private Map<String, String>                    environmentOverrides;
   private Map<String, Map<String, Serializable>> valueMaps;
   private Boolean                                fileReferencesEnabled;



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

      String filePrefix = "${file.";
      if(value.startsWith(filePrefix) && value.endsWith("}") && isFileReferencesEnabled())
      {
         String path   = value.substring(filePrefix.length(), value.length() - 1);
         String result = readFileReference(path);
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
      Set<String> allInputMaps             = new HashSet<>(Set.of("env", "prop", "file"));
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
    ** Whether ${file.PATH} references are read: as set, else only when this
    ** interpreter has no value maps (so values from users are never read as files).
    *******************************************************************************/
   public boolean isFileReferencesEnabled()
   {
      return (fileReferencesEnabled != null ? fileReferencesEnabled : valueMaps == null);
   }



   /*******************************************************************************
    ** Explicitly enable or disable ${file.PATH} references for this interpreter.
    *******************************************************************************/
   public void setFileReferencesEnabled(Boolean fileReferencesEnabled)
   {
      this.fileReferencesEnabled = fileReferencesEnabled;
   }



   /*******************************************************************************
    ** Fluent setter for fileReferencesEnabled.
    *******************************************************************************/
   public QMetaDataVariableInterpreter withFileReferencesEnabled(Boolean fileReferencesEnabled)
   {
      this.fileReferencesEnabled = fileReferencesEnabled;
      return (this);
   }



   /*******************************************************************************
    ** Read a ${file.PATH} reference: the trimmed UTF-8 contents of the file, or
    ** null when the path is not absolute, is outside the allowed roots (after
    ** resolving symbolic links, as Kubernetes secret volumes use them), is not a
    ** regular file, is larger than FILE_REFERENCE_MAX_BYTES, or cannot be read.
    ** Log lines name the path, never the contents.
    *******************************************************************************/
   private String readFileReference(String pathString)
   {
      try
      {
         Path path = Path.of(pathString);
         if(!path.isAbsolute())
         {
            LOG.warn("Ignoring a file reference that is not an absolute path", logPair("path", pathString));
            return (null);
         }

         Path realPath = path.toRealPath();
         if(!isUnderAllowedRoot(realPath))
         {
            LOG.warn("Ignoring a file reference outside the allowed roots", logPair("path", pathString), logPair("roots", getFileReferenceRoots()));
            return (null);
         }

         if(!Files.isRegularFile(realPath))
         {
            LOG.warn("Ignoring a file reference that is not a regular file", logPair("path", pathString));
            return (null);
         }

         if(Files.size(realPath) > FILE_REFERENCE_MAX_BYTES)
         {
            LOG.warn("Ignoring a file reference larger than the limit", logPair("path", pathString), logPair("maxBytes", FILE_REFERENCE_MAX_BYTES));
            return (null);
         }

         return (Files.readString(realPath, StandardCharsets.UTF_8).strip());
      }
      catch(NoSuchFileException e)
      {
         LOG.debug("File reference not found", logPair("path", pathString));
         return (null);
      }
      catch(IOException | InvalidPathException | SecurityException e)
      {
         LOG.warn("Could not read a file reference", logPair("path", pathString), logPair("error", e.getClass().getSimpleName()));
         return (null);
      }
   }



   /*******************************************************************************
    ** Whether a (real) path is inside one of the allowed file-reference roots.
    *******************************************************************************/
   private boolean isUnderAllowedRoot(Path realPath)
   {
      for(String root : getFileReferenceRoots())
      {
         try
         {
            Path rootPath = Path.of(root);
            if(!rootPath.isAbsolute() || !Files.isDirectory(rootPath))
            {
               continue;
            }

            if(realPath.startsWith(rootPath.toRealPath()))
            {
               return (true);
            }
         }
         catch(IOException | InvalidPathException e)
         {
            LOG.debug("Skipping an unusable file reference root", logPair("root", root));
         }
      }
      return (false);
   }



   /*******************************************************************************
    ** The allowed roots for ${file.PATH} references: the system property, else the
    ** environment variable (comma-separated), else the defaults.
    *******************************************************************************/
   List<String> getFileReferenceRoots()
   {
      String configured = System.getProperty(FILE_REFERENCE_ROOTS_PROPERTY);
      if(!StringUtils.hasContent(configured))
      {
         configured = getEnvironmentVariable(FILE_REFERENCE_ROOTS_ENV);
      }

      if(!StringUtils.hasContent(configured))
      {
         return (DEFAULT_FILE_REFERENCE_ROOTS);
      }

      List<String> roots = new ArrayList<>();
      for(String root : configured.split(","))
      {
         if(StringUtils.hasContent(root))
         {
            roots.add(root.strip());
         }
      }
      return (roots);
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
