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

package com.kingsrook.qqq.backend.core.actions.ai;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchResult;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** How the AI tool kernel's data tools read data.  Every read the model can
 ** cause goes through here - query, count, get, search, render a widget.
 **
 ** - CoreAiDataGateway (the default) runs the core actions in-process, with the
 **   session in QContext - for kernels whose caller is already a trusted
 **   server-side entry point (e.g. MCP with core permission enforcement).
 ** - The UI API gateway (qqq-middleware-javalin, UiApiAiDataGateway) makes each
 **   read a /qqq/v1 operation through UiApiInvoker, with the human user's own
 **   credentials and the assistant client channel - so the assistant is held to
 **   exactly the UI's security: permissions, record locks, field access, the
 **   assistant wall, credential scopes.  The assistant uses this one.
 *******************************************************************************/
public interface AiDataGateway
{

   /*******************************************************************************
    ** Query records (with display values).
    *******************************************************************************/
   List<QRecord> query(QueryInput input) throws QException;

   /*******************************************************************************
    ** Count records.
    *******************************************************************************/
   Integer count(CountInput input) throws QException;

   /*******************************************************************************
    ** Get one record (with display values) - null when missing or not readable.
    *******************************************************************************/
   QRecord get(GetInput input) throws QException;

   /*******************************************************************************
    ** Free-text record search.
    *******************************************************************************/
   List<RecordSearchResult> search(RecordSearchInput input) throws QException;

   /*******************************************************************************
    ** Render a widget's data, as its JSON tree (null when there is none).
    *******************************************************************************/
   Map<String, Object> renderWidget(RenderWidgetInput input) throws QException;
}
