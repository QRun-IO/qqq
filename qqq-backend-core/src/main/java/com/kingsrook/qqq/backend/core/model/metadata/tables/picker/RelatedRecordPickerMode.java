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

package com.kingsrook.qqq.backend.core.model.metadata.tables.picker;


/*******************************************************************************
 ** The relationship a {@link QRelatedRecordPicker} picks for.  See
 ** docs/RELATED-RECORD-PICKER.md.
 *******************************************************************************/
public enum RelatedRecordPickerMode
{
   /*******************************************************************************
    ** One target record, for a foreign-key field of the table (e.g. an order's
    ** customer).  Confirming sets the field's value.
    *******************************************************************************/
   SINGLE,

   /*******************************************************************************
    ** Many target records, each added as a new child row through an association
    ** (e.g. SKUs added to an order as order lines), after a review step.
    *******************************************************************************/
   MULTI_CHILD,

   /*******************************************************************************
    ** Many target records, linked and unlinked through an association to a join
    ** table (many-to-many).
    *******************************************************************************/
   LINK
}
