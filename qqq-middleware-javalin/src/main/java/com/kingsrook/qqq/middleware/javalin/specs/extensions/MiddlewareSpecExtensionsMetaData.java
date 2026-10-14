/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.specs.extensions;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.CompleteOperation;


/*******************************************************************************
 ** The instance's registry of UI API extensions: endpoint specs that modules
 ** and qbits add to the versioned UI API (`/qqq/v1`).  Validated with the
 ** QInstance; see docs/UI-API-EXTENSIONS.md.
 *******************************************************************************/
public class MiddlewareSpecExtensionsMetaData implements QSupplementalInstanceMetaData
{
   public static final String NAME = "middlewareSpecExtensions";

   /***************************************************************************
    ** Path prefixes that shipped to clients before the extension point existed,
    ** and so keep their URLs rather than moving under `/qbit/{name}`.
    ** Keys are prefixes; values are the only extension name allowed to use them.
    ***************************************************************************/
   public static final Map<String, String> ESTABLISHED_PREFIXES = Map.of(
      "/esb", "esb",
      "/alerts", "alerts",
      "/assistant", "assistant",
      "/sql-workbench", "sql-workbench");

   /***************************************************************************
    ** First path segments that belong to core and are never available to an
    ** extension.
    ***************************************************************************/
   public static final Set<String> CORE_RESERVED_ROOTS = Set.of(
      "metaData", "table", "processes", "possibleValues", "widget", "reports", "download",
      "manageSession", "logout", "oidc", "search", "help", "version", "openapi.yaml", "openapi.json", "openapi.html");

   private static final Pattern NAME_PATTERN    = Pattern.compile("[a-z0-9][a-z0-9-]*");
   private static final Pattern SEMVER_PATTERN  = Pattern.compile("(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)");
   private static final Pattern PATH_PARAMETERS = Pattern.compile("\\{[^}/]+}");

   private Map<String, MiddlewareSpecExtension> extensions = new LinkedHashMap<>();



   /*******************************************************************************
    ** Get the instance's registry, or null when nothing registered an extension.
    *******************************************************************************/
   public static MiddlewareSpecExtensionsMetaData of(QInstance qInstance)
   {
      return (QSupplementalInstanceMetaData.of(qInstance, NAME));
   }



   /*******************************************************************************
    ** Get the instance's registry, adding an empty one if it has none.
    *******************************************************************************/
   public static MiddlewareSpecExtensionsMetaData ofOrWithNew(QInstance qInstance)
   {
      return (QSupplementalInstanceMetaData.ofOrWithNew(qInstance, NAME, MiddlewareSpecExtensionsMetaData::new));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getName()
   {
      return (NAME);
   }



   /*******************************************************************************
    ** Register an extension.  Registering one with the same name again replaces
    ** it, so producers may register idempotently.
    *******************************************************************************/
   public MiddlewareSpecExtensionsMetaData withExtension(MiddlewareSpecExtension extension)
   {
      if(extension != null)
      {
         extensions.put(extension.getName(), extension);
      }
      return (this);
   }



   /*******************************************************************************
    ** Getter for extensions
    *******************************************************************************/
   public Map<String, MiddlewareSpecExtension> getExtensions()
   {
      return (this.extensions);
   }



   /*******************************************************************************
    ** Setter for extensions
    *******************************************************************************/
   public void setExtensions(Map<String, MiddlewareSpecExtension> extensions)
   {
      this.extensions = extensions == null ? new LinkedHashMap<>() : new LinkedHashMap<>(extensions);
   }



   /*******************************************************************************
    ** Instantiate one spec reference.
    *******************************************************************************/
   public static AbstractEndpointSpec<?, ?, ?> instantiateSpec(QCodeReference reference) throws ReflectiveOperationException
   {
      Class<?> specClass = Class.forName(reference.getName());
      if(!AbstractEndpointSpec.class.isAssignableFrom(specClass))
      {
         throw (new ClassCastException(reference.getName() + " is not an AbstractEndpointSpec"));
      }
      return ((AbstractEndpointSpec<?, ?, ?>) specClass.getConstructor().newInstance());
   }



   /*******************************************************************************
    ** Normalize a path template for comparison: path parameters become `{}` and
    ** duplicate / trailing slashes are removed.
    *******************************************************************************/
   public static String normalizePathTemplate(String path)
   {
      String normalized = PATH_PARAMETERS.matcher(path == null ? "" : path).replaceAll("{}");
      normalized = normalized.replaceAll("<[^>/]+>", "{}");
      normalized = ("/" + normalized).replaceAll("/+", "/");
      return (normalized.length() > 1 ? normalized.replaceFirst("/+$", "") : normalized);
   }



   /*******************************************************************************
    ** Validate every extension: name, semver, prefix and specs.
    *******************************************************************************/
   @Override
   public void validate(QInstance qInstance, QInstanceValidator validator)
   {
      Map<String, String> prefixOwners  = new HashMap<>();
      Map<String, String> operationKeys = new HashMap<>();

      for(Map.Entry<String, MiddlewareSpecExtension> entry : extensions.entrySet())
      {
         MiddlewareSpecExtension extension = entry.getValue();
         String                  name      = extension.getName();
         String                  label     = "UI API extension [" + name + "]: ";

         if(!validator.assertCondition(name != null && NAME_PATTERN.matcher(name).matches(), label + "name must match " + NAME_PATTERN.pattern()))
         {
            continue;
         }

         validator.assertCondition(extension.getSemanticVersion() != null && SEMVER_PATTERN.matcher(extension.getSemanticVersion()).matches(), label + "semanticVersion [" + extension.getSemanticVersion() + "] must be MAJOR.MINOR.PATCH");

         String prefix = extension.getEffectivePathPrefix();
         if(StringUtils.hasContent(extension.getPathPrefix()))
         {
            String allowedName = ESTABLISHED_PREFIXES.get(prefix);
            validator.assertCondition(prefix.equals("/qbit/" + name) || name.equals(allowedName), label + "pathPrefix [" + prefix + "] is not allowed; use the default /qbit/" + name + " (only the established prefixes " + ESTABLISHED_PREFIXES.keySet() + " are kept for their modules)");
         }

         String firstSegment = prefix.replaceFirst("^/", "").split("/")[0];
         validator.assertCondition(!CORE_RESERVED_ROOTS.contains(firstSegment), label + "pathPrefix [" + prefix + "] uses a path reserved by core");

         for(Map.Entry<String, String> other : prefixOwners.entrySet())
         {
            boolean overlaps = (prefix + "/").startsWith(other.getKey() + "/") || (other.getKey() + "/").startsWith(prefix + "/");
            validator.assertCondition(!overlaps, label + "pathPrefix [" + prefix + "] overlaps the prefix of extension [" + other.getValue() + "]");
         }
         prefixOwners.put(prefix, name);

         validator.assertCondition(!extension.getSpecs().isEmpty(), label + "has no specs");
         for(QCodeReference reference : extension.getSpecs())
         {
            validateSpec(validator, label, prefix, reference, operationKeys);
         }
      }
   }



   /*******************************************************************************
    ** Validate one spec of an extension.
    *******************************************************************************/
   private void validateSpec(QInstanceValidator validator, String label, String prefix, QCodeReference reference, Map<String, String> operationKeys)
   {
      if(!validator.assertCondition(reference != null && StringUtils.hasContent(reference.getName()), label + "has a null spec reference"))
      {
         return;
      }

      AbstractEndpointSpec<?, ?, ?> spec;
      try
      {
         spec = instantiateSpec(reference);
      }
      catch(Exception | LinkageError e)
      {
         validator.assertCondition(false, label + "spec [" + reference.getName() + "] could not be instantiated as an AbstractEndpointSpec with a public no-arg constructor: " + e.getMessage());
         return;
      }

      CompleteOperation operation;
      try
      {
         operation = spec.defineCompleteOperation();
      }
      catch(Exception e)
      {
         validator.assertCondition(false, label + "spec [" + reference.getName() + "] failed to define its operation: " + e.getMessage());
         return;
      }

      String path = normalizePathTemplate(operation.getPath());
      validator.assertCondition(path.equals(prefix) || path.startsWith(prefix + "/"), label + "spec [" + reference.getName() + "] path [" + operation.getPath() + "] must start with " + prefix);

      String key      = operation.getHttpMethod() + " " + path;
      String previous = operationKeys.put(key, reference.getName());
      validator.assertCondition(previous == null, label + "spec [" + reference.getName() + "] defines " + key + ", which [" + previous + "] also defines");
   }



   /*******************************************************************************
    ** The specs of every extension, instantiated, in registration order.
    *******************************************************************************/
   public List<AbstractEndpointSpec<?, ?, ?>> instantiateAllSpecs() throws ReflectiveOperationException
   {
      List<AbstractEndpointSpec<?, ?, ?>> rs = new ArrayList<>();
      for(MiddlewareSpecExtension extension : extensions.values())
      {
         for(QCodeReference reference : extension.getSpecs())
         {
            rs.add(instantiateSpec(reference));
         }
      }
      return (rs);
   }
}
