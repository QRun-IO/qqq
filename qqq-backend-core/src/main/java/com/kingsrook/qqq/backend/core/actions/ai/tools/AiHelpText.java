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

package com.kingsrook.qqq.backend.core.actions.ai.tools;


import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Plain-text renderings of QQQ help content (HTML, markdown or text), for the
 ** model: tags removed, whitespace collapsed, length limited.
 *******************************************************************************/
public class AiHelpText
{
   public static final int MAX_HELP_CHARS = 300;



   /*******************************************************************************
    ** Plain text of a list of help contents (null if there is none).
    *******************************************************************************/
   public static String plain(Collection<QHelpContent> helpContents)
   {
      StringBuilder rs = new StringBuilder();
      for(QHelpContent helpContent : CollectionUtils.nonNullCollection(helpContents))
      {
         if(helpContent != null && helpContent.getContent() != null)
         {
            rs.append(helpContent.getContent()).append(' ');
         }
      }
      return (clean(rs.toString()));
   }



   /*******************************************************************************
    ** Plain text of a map of help slots to contents (slots in sorted order).
    *******************************************************************************/
   public static String plain(Map<String, List<QHelpContent>> helpContentMap)
   {
      StringBuilder rs = new StringBuilder();
      for(List<QHelpContent> helpContents : new TreeMap<>(CollectionUtils.nonNullMap(helpContentMap)).values())
      {
         String text = plain(helpContents);
         if(text != null)
         {
            rs.append(text).append(' ');
         }
      }
      return (clean(rs.toString()));
   }



   /*******************************************************************************
    ** Remove tags, collapse whitespace, limit length (null for blank).
    *******************************************************************************/
   public static String clean(String text)
   {
      if(text == null)
      {
         return (null);
      }

      String cleaned = text.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
      if(cleaned.isEmpty())
      {
         return (null);
      }

      return (cleaned.length() > MAX_HELP_CHARS ? cleaned.substring(0, MAX_HELP_CHARS) + "..." : cleaned);
   }
}
