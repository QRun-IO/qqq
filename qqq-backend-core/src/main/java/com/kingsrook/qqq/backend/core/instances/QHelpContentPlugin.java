/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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


import java.util.Map;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;


/*******************************************************************************
 * interface that can be added to a QSupplementalInstanceMetaData, to receive
 * QHelpContent records during instance boot or upon updates in the help content
 * table.
 *******************************************************************************/
public interface QHelpContentPlugin
{
   /***************************************************************************
    * accept a single helpContent record, and apply its data to some data in the
    * qInstance
    *
    * @param qInstance the active qInstance, that the content should be applied to
    * @param helpContent entity with values from HelpContent table
    * @param nameValuePairs parsed string -> string map from the help content key.
    ***************************************************************************/
   void acceptHelpContent(QInstance qInstance, QHelpContent helpContent, Map<String, String> nameValuePairs);
}
