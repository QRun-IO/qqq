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

package com.kingsrook.qqq.backend.core.model.metadata.fields;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import com.kingsrook.qqq.backend.core.actions.values.ValueBehaviorApplier;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Field behavior: a string value must match a regular expression (the whole
 ** value, as Matcher.matches), or the record is refused on insert and update.
 ** Empty values are left to isRequired.  The pattern and its message are
 ** published to frontends in the field's validation rules (UI API 1.6.0), so a
 ** client can check a value before saving; the server stays the authority.
 **
 ** Write patterns that mean the same in Java and in ECMAScript (no possessive
 ** quantifiers, lookbehind or inline flags), since clients test them in their
 ** own regex engines.
 *******************************************************************************/
public class ValuePatternBehavior implements FieldBehavior<ValuePatternBehavior>
{
   private String  pattern;
   private String  message;
   private Pattern compiled;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public ValuePatternBehavior()
   {
   }



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public ValuePatternBehavior(String pattern, String message)
   {
      setPattern(pattern);
      setMessage(message);
   }



   /*******************************************************************************
    ** No pattern by default.
    *******************************************************************************/
   @Override
   public ValuePatternBehavior getDefault()
   {
      return (null);
   }



   /*******************************************************************************
    ** Add an error to each record whose (non-empty) value does not match.
    *******************************************************************************/
   @Override
   public void apply(ValueBehaviorApplier.Action action, List<QRecord> recordList, QInstance instance, QTableMetaData table, QFieldMetaData field)
   {
      Pattern regex = getCompiled();
      if(regex == null || recordList == null)
      {
         return;
      }

      for(QRecord record : recordList)
      {
         if(!record.getValues().containsKey(field.getName()))
         {
            continue;
         }

         String value = record.getValueString(field.getName());
         if(StringUtils.hasContent(value) && !regex.matcher(value).matches())
         {
            record.addError(new BadInputStatusMessage(getMessageFor(field)));
         }
      }
   }



   /*******************************************************************************
    ** The message for a value that does not match.
    *******************************************************************************/
   public String getMessageFor(QFieldMetaData field)
   {
      return (StringUtils.hasContent(message) ? message : "The value for " + field.getLabel() + " is not in the expected format");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<String> validateBehaviorConfiguration(QTableMetaData tableMetaData, QFieldMetaData fieldMetaData)
   {
      List<String> errors = new ArrayList<>();
      String       prefix = "Field " + fieldMetaData.getName() + " in table " + tableMetaData.getName() + " ValuePatternBehavior: ";
      if(!StringUtils.hasContent(pattern))
      {
         errors.add(prefix + "pattern is required");
      }
      else
      {
         try
         {
            Pattern.compile(pattern);
         }
         catch(PatternSyntaxException e)
         {
            errors.add(prefix + "pattern is not a valid regular expression: " + e.getDescription());
         }
      }

      if(fieldMetaData.getType() != null && !fieldMetaData.getType().isStringLike())
      {
         errors.add(prefix + "applies only to string fields");
      }
      return (errors);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Pattern getCompiled()
   {
      if(compiled == null && StringUtils.hasContent(pattern))
      {
         compiled = Pattern.compile(pattern);
      }
      return (compiled);
   }



   /*******************************************************************************
    ** Getter for pattern
    *******************************************************************************/
   public String getPattern()
   {
      return (this.pattern);
   }



   /*******************************************************************************
    ** Setter for pattern
    *******************************************************************************/
   public void setPattern(String pattern)
   {
      this.pattern = pattern;
      this.compiled = null;
   }



   /*******************************************************************************
    ** Fluent setter for pattern
    *******************************************************************************/
   public ValuePatternBehavior withPattern(String pattern)
   {
      setPattern(pattern);
      return (this);
   }



   /*******************************************************************************
    ** Getter for message
    *******************************************************************************/
   public String getMessage()
   {
      return (this.message);
   }



   /*******************************************************************************
    ** Setter for message
    *******************************************************************************/
   public void setMessage(String message)
   {
      this.message = message;
   }



   /*******************************************************************************
    ** Fluent setter for message
    *******************************************************************************/
   public ValuePatternBehavior withMessage(String message)
   {
      this.message = message;
      return (this);
   }
}
