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

package com.kingsrook.qqq.backend.core.model.actions.tables;


/*******************************************************************************
 ** interface to define input sources - idea being, so QQQ can have its standard
 ** ones (see QInputSource), but applications can define their own as well.
 **
 ** We might imagine things like a user's session dictating what InputSource
 ** gets passed into all DML actions.  Or perhaps API meta-data, or just a method
 ** on QInstance in the future?
 **
 ** We might imagine, maybe, more methods growing in the future...
 *******************************************************************************/
public interface InputSource
{

   /*******************************************************************************
    **
    *******************************************************************************/
   boolean shouldValidateRequiredFields();

}
