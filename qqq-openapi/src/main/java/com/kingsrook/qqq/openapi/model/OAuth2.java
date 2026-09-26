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

package com.kingsrook.qqq.openapi.model;


import java.util.Map;


/*******************************************************************************
 **
 *******************************************************************************/
public class OAuth2 extends SecurityScheme
{
   private Map<String, OAuth2Flow> flows;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public OAuth2()
   {
      setType(SecuritySchemeType.OAUTH2);
   }



   /*******************************************************************************
    ** Getter for flows
    *******************************************************************************/
   public Map<String, OAuth2Flow> getFlows()
   {
      return (this.flows);
   }



   /*******************************************************************************
    ** Setter for flows
    *******************************************************************************/
   public void setFlows(Map<String, OAuth2Flow> flows)
   {
      this.flows = flows;
   }



   /*******************************************************************************
    ** Fluent setter for flows
    *******************************************************************************/
   public OAuth2 withFlows(Map<String, OAuth2Flow> flows)
   {
      this.flows = flows;
      return (this);
   }

}
