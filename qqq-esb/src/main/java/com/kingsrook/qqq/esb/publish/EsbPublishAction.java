/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
