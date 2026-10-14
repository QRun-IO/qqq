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

package com.kingsrook.qqq.backend.core.actions.help;




/*******************************************************************************
    ** HelpCoverageTotal data.
    *******************************************************************************/
public class HelpCoverageTotal
{
   private String kind;
   private int total;
   private int withHelp;



   /*******************************************************************************
    ** Getter for kind
    *******************************************************************************/
   public String getKind()
   {
      return (this.kind);
   }



   /*******************************************************************************
    ** Setter for kind
    *******************************************************************************/
   public void setKind(String kind)
   {
      this.kind = kind;
   }



   /*******************************************************************************
    ** Fluent setter for kind
    *******************************************************************************/
   public HelpCoverageTotal withKind(String kind)
   {
      this.kind = kind;
      return (this);
   }



   /*******************************************************************************
    ** Getter for total
    *******************************************************************************/
   public int getTotal()
   {
      return (this.total);
   }



   /*******************************************************************************
    ** Setter for total
    *******************************************************************************/
   public void setTotal(int total)
   {
      this.total = total;
   }



   /*******************************************************************************
    ** Fluent setter for total
    *******************************************************************************/
   public HelpCoverageTotal withTotal(int total)
   {
      this.total = total;
      return (this);
   }



   /*******************************************************************************
    ** Getter for withHelp
    *******************************************************************************/
   public int getWithHelp()
   {
      return (this.withHelp);
   }



   /*******************************************************************************
    ** Setter for withHelp
    *******************************************************************************/
   public void setWithHelp(int withHelp)
   {
      this.withHelp = withHelp;
   }



   /*******************************************************************************
    ** Fluent setter for withHelp
    *******************************************************************************/
   public HelpCoverageTotal withWithHelp(int withHelp)
   {
      this.withHelp = withHelp;
      return (this);
   }
}
