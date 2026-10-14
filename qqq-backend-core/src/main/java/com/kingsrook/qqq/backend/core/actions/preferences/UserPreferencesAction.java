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

package com.kingsrook.qqq.backend.core.actions.preferences;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.permissions.CorePermissionEnforcer;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataInput;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceChange;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceListMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceProviderInterface;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceRequestContext;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceRow;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceRowChange;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceSectionMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceSectionResult;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceValues;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferencesMetaData;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Session-scoped user preferences: visibility, validation and trusted persistence.
 *******************************************************************************/
public class UserPreferencesAction
{
   private static final String TRUST_REASON = "user preferences: the session user's own settings";



   /*******************************************************************************
    ** List only visible installed sections, ordered by order then label.
    *******************************************************************************/
   public List<UserPreferenceSectionResult> listSections() throws QException
   {
      requireUser();
      UserPreferencesMetaData metadata = UserPreferencesMetaData.of(QContext.getQInstance());
      List<UserPreferenceSectionResult> result = new ArrayList<>();
      if(metadata != null)
      {
         for(UserPreferenceSectionMetaData section : metadata.getSections().stream()
            .sorted(Comparator.comparingInt(UserPreferenceSectionMetaData::getOrder).thenComparing(UserPreferenceSectionMetaData::getLabel)).toList())
         {
            if(isVisible(section))
            {
               result.add(load(section));
            }
         }
      }
      return (result);
   }



   /*******************************************************************************
    ** Load the session user's section, or hide it with not found.
    *******************************************************************************/
   public UserPreferenceSectionResult getSection(String name) throws QException
   {
      return (load(findSection(name)));
   }



   /*******************************************************************************
    ** Validate the complete body without discarding unknown properties.
    *******************************************************************************/
   public UserPreferenceSectionResult patchSection(String name, Map<String, ?> body) throws QException
   {
      UserPreferenceSectionMetaData section = findSection(name);
      List<String> errors = new ArrayList<>();
      UserPreferenceSectionResult loaded = load(section);
      UserPreferenceChange change = parseChange(section, loaded, body, errors);
      return (patch(section, change, errors, loaded));
   }



   /*******************************************************************************
    ** Apply a typed partial change from an in-process caller.
    *******************************************************************************/
   public UserPreferenceSectionResult patchSection(String name, UserPreferenceChange change) throws QException
   {
      UserPreferenceSectionMetaData section = findSection(name);
      return (patch(section, change, new ArrayList<>(), load(section)));
   }



   /*******************************************************************************
    ** Resolve visibility before parsing or loading any preference data.
    *******************************************************************************/
   private UserPreferenceSectionMetaData findSection(String name) throws QNotFoundException
   {
      requireUser();
      UserPreferencesMetaData metadata = UserPreferencesMetaData.of(QContext.getQInstance());
      if(metadata != null)
      {
         for(UserPreferenceSectionMetaData section : metadata.getSections())
         {
            if(section.getName().equals(name) && isVisible(section))
            {
               return (section);
            }
         }
      }
      throw (new QNotFoundException("Preference section not found"));
   }



   /*******************************************************************************
    ** Require a real session user, including for an empty registry.
    *******************************************************************************/
   private void requireUser()
   {
      new UserPreferenceRequestContext(null, QContext.getQSession());
   }



   /*******************************************************************************
    ** Use the same app permission helper as other core entry points, including channel denies.
    *******************************************************************************/
   private boolean isVisible(UserPreferenceSectionMetaData section)
   {
      if(section.getAppName() != null && (QContext.getQInstance().getApp(section.getAppName()) == null
         || !PermissionsHelper.hasAppPermission(new MetaDataInput(), section.getAppName())))
      {
         return (false);
      }
      return (section.getPermissionName() == null || QContext.getQSession().hasPermission(section.getPermissionName()));
   }



   /*******************************************************************************
    ** Resolve a provider through the standard code-reference loader.
    *******************************************************************************/
   private UserPreferenceProviderInterface provider(UserPreferenceSectionMetaData section) throws QException
   {
      if(section.getProvider() == null)
      {
         return (new KeyValueUserPreferenceProvider());
      }
      Object provider = QCodeLoader.getAdHoc(Object.class, section.getProvider());
      if(provider instanceof UserPreferenceProviderInterface preferencesProvider)
      {
         return (preferencesProvider);
      }
      throw (new QException("Could not load preference provider for " + section.getName()));
   }



   /*******************************************************************************
    ** Load without requiring permissions on backing tables; preserve the original session.
    *******************************************************************************/
   private UserPreferenceSectionResult load(UserPreferenceSectionMetaData section) throws QException
   {
      UserPreferenceRequestContext context = new UserPreferenceRequestContext(section, QContext.getQSession());
      UserPreferenceValues values = CorePermissionEnforcer.runTrusted(TRUST_REASON, () -> provider(section).load(context));
      values.setValues(new LinkedHashMap<>(values.getValues()));
      for(QFieldMetaData field : section.getFields())
      {
         if(!values.getValues().containsKey(field.getName()))
         {
            values.getValues().put(field.getName(), ValueUtils.getValueAsFieldType(field.getType(), field.getDefaultValue()));
         }
      }
      for(UserPreferenceListMetaData list : section.getLists())
      {
         for(UserPreferenceRow row : values.getLists().getOrDefault(list.getName(), List.of()))
         {
            row.setValues(new LinkedHashMap<>(row.getValues()));
            for(QFieldMetaData field : list.getFields())
            {
               if(!row.getValues().containsKey(field.getName()))
               {
                  row.getValues().put(field.getName(), ValueUtils.getValueAsFieldType(field.getType(), field.getDefaultValue()));
               }
            }
         }
      }
      return (new UserPreferenceSectionResult().withSection(section).withValues(values));
   }



   /*******************************************************************************
    ** Validate every supplied field and fixed row before any store call.
    *******************************************************************************/
   private UserPreferenceSectionResult patch(UserPreferenceSectionMetaData section, UserPreferenceChange change, List<String> errors, UserPreferenceSectionResult loaded) throws QException
   {
      UserPreferenceChange coerced = new UserPreferenceChange();
      validateValues(section.getFields(), change.getValues(), coerced.getValues(), List.of(), null, "", errors);
      for(Map.Entry<String, List<UserPreferenceRowChange>> entry : change.getLists().entrySet())
      {
         UserPreferenceListMetaData list = section.getLists().stream().filter(l -> l.getName().equals(entry.getKey())).findFirst().orElse(null);
         if(list == null)
         {
            errors.add(entry.getKey() + ": isn't a setting here");
            continue;
         }
         List<UserPreferenceRowChange> rows = new ArrayList<>();
         coerced.getLists().put(entry.getKey(), rows);
         Set<String> seen = new java.util.HashSet<>();
         for(UserPreferenceRowChange rowChange : entry.getValue())
         {
            String path = list.getLabel() + ", " + rowChange.getKey();
            UserPreferenceRow row = loaded.getValues().getLists().getOrDefault(entry.getKey(), List.of()).stream()
               .filter(r -> r.getKey().equals(rowChange.getKey())).findFirst().orElse(null);
            if(row == null)
            {
               errors.add(path + ": isn't a setting here");
               continue;
            }
            path = list.getLabel() + ", " + row.getLabel();
            if(!seen.add(rowChange.getKey()))
            {
               errors.add(path + ": row appears twice");
            }
            UserPreferenceRowChange normalized = new UserPreferenceRowChange().withKey(rowChange.getKey());
            validateValues(list.getFields(), rowChange.getValues(), normalized.getValues(), row.getReadOnlyFieldNames(), row.getReadOnlyReason(), path + ": ", errors);
            rows.add(normalized);
         }
      }
      throwErrors(errors);
      UserPreferenceProviderInterface provider = provider(section);
      UserPreferenceRequestContext context = new UserPreferenceRequestContext(section, QContext.getQSession());
      errors.addAll(provider.validate(context, coerced));
      throwErrors(errors);
      CorePermissionEnforcer.runTrusted(TRUST_REASON, () ->
      {
         provider.store(context, coerced);
         return (null);
      });
      return (load(section));
   }



   /*******************************************************************************
    ** Accumulate editability, required, type, length and enum errors using user-facing labels.
    *******************************************************************************/
   private void validateValues(List<QFieldMetaData> fields, Map<String, Serializable> input, Map<String, Serializable> output, List<String> readOnly, String readOnlyReason, String path, List<String> errors)
   {
      for(Map.Entry<String, Serializable> entry : input.entrySet())
      {
         String name = entry.getKey();
         String fieldPath = path + name;
         QFieldMetaData field = fields.stream().filter(f -> f.getName().equals(name)).findFirst().orElse(null);
         if(field == null || "userId".equals(name))
         {
            errors.add(fieldPath + ": isn't a setting here");
            continue;
         }
         fieldPath = path + (field.getLabel() == null ? QInstanceEnricher.nameToLabel(name) : field.getLabel());
         if(!field.getIsEditable() || readOnly.contains(name))
         {
            errors.add(fieldPath + ": can't be changed" + (readOnlyReason == null || readOnlyReason.isBlank() ? "" : ": " + readOnlyReason));
         }
         Serializable value;
         try
         {
            Object raw = entry.getValue();
            if(raw instanceof Map || raw instanceof List || (field.getType() == QFieldType.BOOLEAN && raw != null
               && !(raw instanceof Boolean) && !(raw instanceof String s && Set.of("true", "false", "yes", "no").contains(s.toLowerCase(java.util.Locale.ROOT)))))
            {
               throw (new IllegalArgumentException("Expected a scalar " + field.getType()));
            }
            value = ValueUtils.getValueAsFieldType(field.getType(), raw);
            if(raw != null && value == null)
            {
               throw (new IllegalArgumentException("Value cannot be converted"));
            }
         }
         catch(Exception e)
         {
            errors.add(fieldPath + ": " + switch(field.getType())
            {
               case INTEGER, LONG -> "must be a whole number";
               case DECIMAL -> "must be a number";
               case BOOLEAN -> "must be true or false";
               case DATE -> "must be a date";
               case DATE_TIME -> "must be a date and time";
               case TIME -> "must be a time";
               default -> "must be text";
            });
            continue;
         }
         if(value == null && field.getIsRequired())
         {
            errors.add(fieldPath + ": is required");
         }
         if(value instanceof String text && field.getMaxLength() != null && text.length() > field.getMaxLength())
         {
            errors.add(fieldPath + ": must be at most " + field.getMaxLength() + " characters");
         }
         QPossibleValueSource source = field.getPossibleValueSourceName() == null ? null : QContext.getQInstance().getPossibleValueSource(field.getPossibleValueSourceName());
         if(value != null && source != null && source.getType() == QPossibleValueSourceType.ENUM
            && source.getEnumValues().stream().noneMatch(v -> String.valueOf(v.getId()).equals(String.valueOf(value))))
         {
            errors.add(fieldPath + ": isn't one of the choices");
         }
         output.put(name, value);
      }
   }



   /*******************************************************************************
    ** Parse the wire shape while preserving and reporting every unexpected key.
    *******************************************************************************/
   private UserPreferenceChange parseChange(UserPreferenceSectionMetaData section, UserPreferenceSectionResult loaded, Map<String, ?> body, List<String> errors)
   {
      UserPreferenceChange change = new UserPreferenceChange();
      for(String key : body.keySet())
      {
         if(!Set.of("values", "lists").contains(key))
         {
            errors.add("Unexpected property: " + key);
         }
      }
      if(body.containsKey("values"))
      {
         change.setValues(parseValues(body.get("values"), "Settings", errors));
      }
      if(body.containsKey("lists"))
      {
         if(body.get("lists") instanceof Map<?, ?> lists)
         {
            for(Map.Entry<?, ?> entry : lists.entrySet())
            {
               String name = String.valueOf(entry.getKey());
               UserPreferenceListMetaData list = section.getLists().stream().filter(l -> l.getName().equals(name)).findFirst().orElse(null);
               String label = list == null ? name : list.getLabel();
               List<UserPreferenceRowChange> rows = new ArrayList<>();
               change.getLists().put(name, rows);
               if(entry.getValue() instanceof List<?> rawRows)
               {
                  for(Object rawRow : rawRows)
                  {
                     if(rawRow instanceof Map<?, ?> row && row.get("key") instanceof String key)
                     {
                        UserPreferenceRow knownRow = loaded.getValues().getLists().getOrDefault(name, List.of()).stream().filter(r -> r.getKey().equals(key)).findFirst().orElse(null);
                        String rowLabel = label + ", " + (knownRow == null ? key : knownRow.getLabel());
                        for(Object property : row.keySet())
                        {
                           if(!Set.of("key", "values").contains(property))
                           {
                              errors.add(rowLabel + ": Unexpected property: " + property);
                           }
                        }
                        rows.add(new UserPreferenceRowChange().withKey(key).withValues(parseValues(row.get("values"), rowLabel, errors)));
                     }
                     else
                     {
                        errors.add(label + ": each row must include a text key and settings");
                     }
                  }
               }
               else
               {
                  errors.add(label + ": must be a list of rows");
               }
            }
         }
         else
         {
            errors.add("Lists: must contain named lists of settings");
         }
      }
      return (change);
   }



   /*******************************************************************************
    ** Parse a values object, retaining invalid structured values for validation.
    *******************************************************************************/
   private Map<String, Serializable> parseValues(Object raw, String path, List<String> errors)
   {
      Map<String, Serializable> values = new LinkedHashMap<>();
      if(raw instanceof Map<?, ?> map)
      {
         for(Map.Entry<?, ?> entry : map.entrySet())
         {
            if(entry.getValue() == null || entry.getValue() instanceof Serializable)
            {
               values.put(String.valueOf(entry.getKey()), (Serializable) entry.getValue());
            }
            else
            {
               errors.add(path + ": " + entry.getKey() + ": must be a supported value");
            }
         }
      }
      else
      {
         errors.add(path + ": must contain named settings");
      }
      return (values);
   }



   /*******************************************************************************
    ** Return every validation message in one bad request.
    *******************************************************************************/
   private void throwErrors(List<String> errors) throws QBadRequestException
   {
      if(!errors.isEmpty())
      {
         throw (new QBadRequestException(String.join("\n", errors)));
      }
   }
}
