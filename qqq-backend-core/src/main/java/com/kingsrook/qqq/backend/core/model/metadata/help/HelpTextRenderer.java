/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.help;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.help.bundles.HelpBundles;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;


/*******************************************************************************
 ** Resolve raw references at the server boundary, never in persisted help text.
 *******************************************************************************/
public class HelpTextRenderer
{
   private static final QLogger LOG = QLogger.getLogger(HelpTextRenderer.class);
   private static final Pattern REFERENCE = Pattern.compile("\\{\\{([^{}]*)}}");
   private static final Set<String> warned = ConcurrentHashMap.newKeySet();
   private final QInstance instance;


   /*******************************************************************************
    ** Metadata used for each live lookup.
    *******************************************************************************/
   public HelpTextRenderer(QInstance instance)
   {
      this.instance = instance;
   }



   /*******************************************************************************
    ** Escape substitutions so metadata cannot inject Markdown syntax.
    *******************************************************************************/
   public String render(String text)
   {
      if(text == null)
      {
         return (null);
      }
      Matcher matcher = REFERENCE.matcher(text);
      StringBuilder result = new StringBuilder();
      while(matcher.find())
      {
         String key = matcher.group(1);
         Optional<String> value = resolve(key);
         if(value.isEmpty() && warned.add(key))
         {
            LOG.warn("Unresolved help reference: " + key);
         }
         String escaped = value.orElse("").replaceAll("([\\\\`*_{}\\[\\]()#+.!|>~<\\-])", "\\\\$1");
         matcher.appendReplacement(result, Matcher.quoteReplacement(escaped));
      }
      matcher.appendTail(result);
      return (result.toString());
   }



   /*******************************************************************************
    ** Return every invalid reference for the coverage gate, without logging.
    *******************************************************************************/
   public List<String> unresolved(String text)
   {
      List<String> result = new ArrayList<>();
      if(text != null)
      {
         Matcher matcher = REFERENCE.matcher(text);
         while(matcher.find())
         {
            if(resolve(matcher.group(1)).isEmpty())
            {
               result.add(matcher.group());
            }
         }
         String remainder = REFERENCE.matcher(text).replaceAll("");
         if(remainder.contains("{{") || remainder.contains("}}"))
         {
            result.add("Malformed reference in: " + text);
         }
      }
      return (result);
   }



   /*******************************************************************************
    ** Resolve only metadata and explicitly registered settings, never table data.
    *******************************************************************************/
   public Optional<String> resolve(String reference)
   {
      if(instance == null || !reference.contains(":"))
      {
         return (Optional.empty());
      }
      String[] parts = reference.split(":", 2);
      String name = parts[1];
      String[] path = name.split("\\.", 2);
      var table = instance.getTable(path[0]);
      QFieldMetaData field = table == null || path.length != 2 ? null : table.getFields().get(path[1]);
      Object value = null;
      switch(parts[0])
      {
         case "label" ->
         {
            if(field != null)
            {
               value = field.getLabel();
            }
            else
            {
               if(instance.getTable(name) != null)
               {
                  value = instance.getTable(name).getLabel();
               }
               else if(instance.getProcess(name) != null)
               {
                  value = instance.getProcess(name).getLabel();
               }
               else if(instance.getApp(name) != null)
               {
                  value = instance.getApp(name).getLabel();
               }
            }
         }
         case "default" -> value = field == null ? null : field.getDefaultValue();
         case "maxLength" -> value = field == null ? null : field.getMaxLength();
         case "values" ->
         {
            var source = instance.getPossibleValueSource(name);
            if(source != null && source.getType() == QPossibleValueSourceType.ENUM && source.getEnumValues() != null)
            {
               value = source.getEnumValues().stream().map(v -> v.getLabel()).collect(Collectors.joining(", "));
            }
         }
         case "setting" ->
         {
            HelpBundles bundles = (HelpBundles) instance.getSupplementalMetaData(HelpBundles.NAME);
            if(bundles != null)
            {
               for(HelpValueProvider provider : bundles.getValueProviders())
               {
                  Optional<String> provided = provider.value(name);
                  if(provided.isPresent())
                  {
                     return (provided);
                  }
               }
            }
         }
         default ->
         {
            return (Optional.empty());
         }
      }
      return (Optional.ofNullable(value).map(Object::toString));
   }
}
