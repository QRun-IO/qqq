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

package com.kingsrook.qqq.esb.metadata;


import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;


/*******************************************************************************
 * The ESB app: one overview widget, for users with esbView.hasAccess (the
 * ESB's service-level permission).  Also names the ESB's other permissions:
 * esbOperate (pause, resume, restart, replay, move) and esbDelete (purge and
 * delete messages).
 *******************************************************************************/
public class EsbAppMetaDataProducer extends MetaDataProducer<QAppMetaData>
{
   public static final String NAME = "esb";

   public static final String VIEW_PERMISSION_BASE_NAME    = "esbView";
   public static final String OPERATE_PERMISSION_BASE_NAME = "esbOperate";
   public static final String DELETE_PERMISSION_BASE_NAME  = "esbDelete";



   /*******************************************************************************
    ** The has-access permission rules on esbView - for the app, its widget, and
    ** the overview endpoint (which checks access to the app).
    *******************************************************************************/
   public static QPermissionRules viewPermissionRules()
   {
      return (new QPermissionRules()
         .withLevel(PermissionLevel.HAS_ACCESS_PERMISSION)
         .withPermissionBaseName(VIEW_PERMISSION_BASE_NAME));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QAppMetaData produce(QInstance qInstance) throws QException
   {
      return (new QAppMetaData()
         .withName(NAME)
         .withLabel("ESB")
         .withPermissionRules(viewPermissionRules())
         .withWidgets(List.of(EsbOverviewWidgetMetaDataProducer.NAME)));
   }

}
