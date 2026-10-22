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

package com.kingsrook.qqq.backend.core.model.metadata.possiblevalues;


import java.util.List;


/*******************************************************************************
 ** Define some standard ways to format the value portion of a PossibleValueSource.
 **
 ** Can be passed to short-cut {set,with}ValueFormatAndFields methods in QPossibleValueSource
 ** class, or the format & field properties can be extracted and passed to regular field-level setters.
 *******************************************************************************/
public enum PVSValueFormatAndFields
{
   LABEL_ONLY("%s", "label"),
   LABEL_PARENS_ID("%s (%s)", "label", "id"),
   ID_COLON_LABEL("%s: %s", "id", "label");


   private final String       format;
   private final List<String> fields;



   /*******************************************************************************
    **
    *******************************************************************************/
   PVSValueFormatAndFields(String format, String... fields)
   {
      this.format = format;
      this.fields = List.of(fields);
   }



   /*******************************************************************************
    ** Getter for format
    **
    *******************************************************************************/
   public String getFormat()
   {
      return format;
   }



   /*******************************************************************************
    ** Getter for fields
    **
    *******************************************************************************/
   public List<String> getFields()
   {
      return fields;
   }
}
