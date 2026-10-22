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

package com.kingsrook.qqq.backend.core.actions.collaboration;


import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.FollowEvent;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFollower;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordReference;


/*******************************************************************************
 ** Provide record followers without coupling consumers to a collaboration qbit.
 *******************************************************************************/
public interface RecordFollowersProviderInterface
{
   /*******************************************************************************
    ** Return followers who want this event. Treat ALERT like CHANGE unless the provider
    ** has a finer preference. This does NOT filter by each follower's read access.
    ** Callers must check each recipient's access before sending record data, for example
    ** by passing sourceTableName/sourceRecordId to QAlerts.notify (which re-checks access),
    ** or by using RecordAccessCheckerInterface.
    *******************************************************************************/
   List<RecordFollower> getFollowers(QInstance instance, RecordReference reference, FollowEvent event) throws QException;

}
