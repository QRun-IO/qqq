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

package com.kingsrook.qqq.backend.core.model.actions.tables.query;


/*******************************************************************************
 ** Possible query criteria field operators
 **
 *******************************************************************************/
public enum QCriteriaOperator
{
   EQUALS,
   NOT_EQUALS,
   NOT_EQUALS_OR_IS_NULL,
   IN,
   NOT_IN,
   IS_NULL_OR_IN,
   LIKE,
   NOT_LIKE,
   STARTS_WITH,
   ENDS_WITH,
   CONTAINS,
   NOT_STARTS_WITH,
   NOT_ENDS_WITH,
   NOT_CONTAINS,
   LESS_THAN,
   LESS_THAN_OR_EQUALS,
   GREATER_THAN,
   GREATER_THAN_OR_EQUALS,
   IS_BLANK,
   IS_NOT_BLANK,
   BETWEEN,
   NOT_BETWEEN,
   TRUE,
   FALSE
}
