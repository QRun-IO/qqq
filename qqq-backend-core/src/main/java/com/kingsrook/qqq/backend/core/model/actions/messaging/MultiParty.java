/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.actions.messaging;


import java.util.ArrayList;
import java.util.List;


/*******************************************************************************
 **
 *******************************************************************************/
public class MultiParty extends Party
{
   private List<Party> partyList;



   /*******************************************************************************
    ** Getter for partyList
    *******************************************************************************/
   public List<Party> getPartyList()
   {
      return (this.partyList);
   }



   /*******************************************************************************
    ** Setter for partyList
    *******************************************************************************/
   public void setPartyList(List<Party> partyList)
   {
      this.partyList = partyList;
   }



   /*******************************************************************************
    ** Fluent setter for partyList
    *******************************************************************************/
   public MultiParty withPartyList(List<Party> partyList)
   {
      this.partyList = partyList;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public MultiParty withParty(Party party)
   {
      addParty(party);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void addParty(Party party)
   {
      if(this.partyList == null)
      {
         this.partyList = new ArrayList<>();
      }
      this.partyList.add(party);
   }

}
