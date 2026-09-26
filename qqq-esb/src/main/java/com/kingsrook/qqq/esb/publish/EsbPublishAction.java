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

package com.kingsrook.qqq.esb.publish;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.AbstractQActionFunction;
import com.kingsrook.qqq.esb.envelope.EsbEvent;
import com.kingsrook.qqq.esb.envelope.EsbEventFactory;


/*******************************************************************************
 * Explicitly publish one event, with a caller-supplied type and data, to an ESB
 * destination (spec section 5) - callable from any process step or other code.
 *
 * The event's source is qqq://instanceName/sourcePath (see EsbPublishInput),
 * and its causation id is the current EsbCausation id, as for every event QQQ
 * publishes.  It is sent right away (not after any transaction's commit).
 *
 * Never throws: a missing type, an unknown destination, a broker that is down,
 * and so on, are logged, counted as a publish failure, and returned in the
 * output (see EsbPublisher) - so a failed publish never fails its caller.
 *******************************************************************************/
public class EsbPublishAction extends AbstractQActionFunction<EsbPublishInput, EsbPublishOutput>
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public EsbPublishOutput execute(EsbPublishInput input)
   {
      EsbEvent event;
      try
      {
         event = EsbEventFactory.custom(EsbPublisher.getInstanceNameFromContext(), input.getSourcePath(), input.getType(), input.getData());
      }
      catch(IllegalArgumentException e)
      {
         return (EsbPublisher.getInstance().failed(input.getDestinationName(), 1, e));
      }

      return (EsbPublisher.getInstance().publish(input.getDestinationName(), List.of(event)));
   }

}
