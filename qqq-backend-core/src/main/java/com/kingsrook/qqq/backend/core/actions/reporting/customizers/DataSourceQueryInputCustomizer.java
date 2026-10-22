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

package com.kingsrook.qqq.backend.core.actions.reporting.customizers;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;


/*******************************************************************************
 ** Interface for customizer on a QReportDataSource's query.
 **
 ** Useful, for example, to look at what input field values were given, and change
 ** the query filter (e.g., conditional criteria), or issue an error based on the
 ** combination of input fields given.
 *******************************************************************************/
public interface DataSourceQueryInputCustomizer
{

   /*******************************************************************************
    **
    *******************************************************************************/
   QueryInput run(ReportInput reportInput, QueryInput queryInput) throws QException;

}
