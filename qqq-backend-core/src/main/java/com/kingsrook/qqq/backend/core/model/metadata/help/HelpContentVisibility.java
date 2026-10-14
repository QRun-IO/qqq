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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Per-session copies of help, with targeting enforced before transmission.
 *******************************************************************************/
public class HelpContentVisibility
{
   /*******************************************************************************
    ** Filter a list without changing shared metadata or exposing targeting fields.
    *******************************************************************************/
   public static List<QHelpContent> filter(List<QHelpContent> contents)
   {
      if(contents == null)
      {
         return (null);
      }
      QSession session = QContext.getQSession();
      List<QHelpContent> result = new ArrayList<>();
      for(QHelpContent content : contents)
      {
         if(content != null
            && (!StringUtils.hasContent(content.getPermissionName()) || (session != null && session.hasPermission(content.getPermissionName())))
            && (!StringUtils.hasContent(content.getClientChannel()) || (session != null && content.getClientChannel().equals(session.getClientChannelName()))))
         {
            result.add(content.clone().withPermissionName(null).withClientChannel(null).withCodeContent(null));
         }
      }
      return (result);
   }



   /*******************************************************************************
    ** Filter each slot, retaining the existing null/empty shape.
    *******************************************************************************/
   public static Map<String, List<QHelpContent>> filter(Map<String, List<QHelpContent>> contents)
   {
      if(contents == null)
      {
         return (null);
      }
      Map<String, List<QHelpContent>> result = new LinkedHashMap<>();
      contents.forEach((slot, entries) -> result.put(slot, filter(entries)));
      return (result);
   }
}
