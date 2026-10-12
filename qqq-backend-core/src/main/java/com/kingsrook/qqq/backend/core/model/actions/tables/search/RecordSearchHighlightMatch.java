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

package com.kingsrook.qqq.backend.core.model.actions.tables.search;


import java.io.Serializable;


/*******************************************************************************
 ** One matched span in a record search highlight's text: character offsets,
 ** start inclusive and end exclusive.
 *******************************************************************************/
public class RecordSearchHighlightMatch implements Serializable
{
   private static final long serialVersionUID = 1L;

   private Integer start;
   private Integer end;



   /*******************************************************************************
    ** Getter for start
    *******************************************************************************/
   public Integer getStart()
   {
      return (this.start);
   }



   /*******************************************************************************
    ** Setter for start
    *******************************************************************************/
   public void setStart(Integer start)
   {
      this.start = start;
   }



   /*******************************************************************************
    ** Fluent setter for start - offset of the first matched character
    *******************************************************************************/
   public RecordSearchHighlightMatch withStart(Integer start)
   {
      this.start = start;
      return (this);
   }



   /*******************************************************************************
    ** Getter for end
    *******************************************************************************/
   public Integer getEnd()
   {
      return (this.end);
   }



   /*******************************************************************************
    ** Setter for end
    *******************************************************************************/
   public void setEnd(Integer end)
   {
      this.end = end;
   }



   /*******************************************************************************
    ** Fluent setter for end - offset just after the last matched character
    *******************************************************************************/
   public RecordSearchHighlightMatch withEnd(Integer end)
   {
      this.end = end;
      return (this);
   }

}
