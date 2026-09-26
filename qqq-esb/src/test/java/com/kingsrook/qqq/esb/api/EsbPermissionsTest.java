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

package com.kingsrook.qqq.esb.api;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.dashboard.RenderWidgetAction;
import com.kingsrook.qqq.backend.core.actions.metadata.MetaDataAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataOutput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.esb.metadata.EsbAppMetaDataProducer;
import com.kingsrook.qqq.esb.metadata.EsbOverviewWidgetMetaDataProducer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 * Permission tests for the ESB endpoints and the ESB app: table READ, process
 * access, and esbView (the ESB app) gate the endpoints; subscribers on hidden
 * processes are omitted; esbOperate and esbDelete show in the permissions.
 *******************************************************************************/
class EsbPermissionsTest extends EsbApiTestBase
{
   private static final String ORDER_READ           = "order.read";
   private static final String SYNC_ORDER_ACCESS    = "syncOrder.hasAccess";
   private static final String FULFILL_ORDER_ACCESS = "fulfillOrder.hasAccess";
   private static final String ESB_VIEW             = "esbView.hasAccess";
   private static final String ESB_OPERATE          = "esbOperate.hasAccess";
   private static final String ESB_DELETE           = "esbDelete.hasAccess";



   /*******************************************************************************
    ** Protect the order table (read/insert/edit/delete) and the processes
    ** (has-access).
    *******************************************************************************/
   @BeforeEach
   void beforeEach()
   {
      qInstance.getTable(TABLE_NAME_ORDER).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      for(String processName : List.of(PROCESS_NAME_SYNC_ORDER, PROCESS_NAME_FULFILL_ORDER))
      {
         qInstance.getProcess(processName).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
      }
   }



   /*******************************************************************************
    ** Every endpoint is a 403 without its permission.
    *******************************************************************************/
   @Test
   void testForbiddenWithoutPermission() throws Exception
   {
      setPermissions(ESB_OPERATE, ESB_DELETE);
      for(String path : List.of(
         "/qqq/v1/esb/table/" + TABLE_NAME_ORDER,
         "/qqq/v1/esb/process/" + PROCESS_NAME_SYNC_ORDER,
         "/qqq/v1/esb/overview",
         "/qqq/v1/esb/deadLetters/" + TRIGGER_SYNC_ORDER,
         "/qqq/v1/esb/messages/" + DESTINATION_FULFILLMENT))
      {
         assertEquals(403, get(path).statusCode(), path);
      }
   }



   /*******************************************************************************
    ** Table READ opens the table endpoint; its subscribers on processes the user
    ** can't access are left out.
    *******************************************************************************/
   @Test
   void testHiddenSubscribersAreOmitted() throws Exception
   {
      setPermissions(ORDER_READ);
      assertEquals(0, getJson("/qqq/v1/esb/table/" + TABLE_NAME_ORDER).getJSONArray("subscribers").length());

      setPermissions(ORDER_READ, FULFILL_ORDER_ACCESS);
      JSONArray subscribers = getJson("/qqq/v1/esb/table/" + TABLE_NAME_ORDER).getJSONArray("subscribers");
      assertEquals(1, subscribers.length());
      assertEquals(TRIGGER_FULFILL_ORDER, subscribers.getJSONObject(0).getString("name"));

      setPermissions(ESB_VIEW);
      JSONArray destinations = getJson("/qqq/v1/esb/overview").getJSONArray("destinations");
      assertEquals(0, destinations.getJSONObject(0).getJSONArray("triggers").length());
      assertEquals(0, destinations.getJSONObject(1).getJSONArray("triggers").length());

      setPermissions(ESB_VIEW, SYNC_ORDER_ACCESS);
      destinations = getJson("/qqq/v1/esb/overview").getJSONArray("destinations");
      assertEquals(TRIGGER_SYNC_ORDER, destinations.getJSONObject(0).getJSONArray("triggers").getJSONObject(0).getString("name"));
      assertEquals(0, destinations.getJSONObject(1).getJSONArray("triggers").length());
   }



   /*******************************************************************************
    ** Process access opens the process and dead-letter endpoints.
    *******************************************************************************/
   @Test
   void testProcessAccessOpensProcessAndDeadLetters() throws Exception
   {
      setPermissions(SYNC_ORDER_ACCESS);
      assertEquals(PROCESS_NAME_SYNC_ORDER, getJson("/qqq/v1/esb/process/" + PROCESS_NAME_SYNC_ORDER).getString("process"));
      assertEquals(403, get("/qqq/v1/esb/process/" + PROCESS_NAME_FULFILL_ORDER).statusCode());

      sendToQueue(DEAD_LETTERS_FULFILL_ORDER, Map.of(), "dead");
      assertEquals(403, get("/qqq/v1/esb/deadLetters/" + TRIGGER_FULFILL_ORDER).statusCode());

      setPermissions(FULFILL_ORDER_ACCESS);
      assertEquals(1, getJson("/qqq/v1/esb/deadLetters/" + TRIGGER_FULFILL_ORDER).getJSONArray("messages").length());
   }



   /*******************************************************************************
    ** canOperate and canDelete reflect esbOperate and esbDelete.
    *******************************************************************************/
   @Test
   void testPermissionsReflectOperateAndDelete() throws Exception
   {
      setPermissions(ORDER_READ);
      EsbRouteProviderTest.assertPermissions(getJson("/qqq/v1/esb/table/" + TABLE_NAME_ORDER).getJSONObject("permissions"), false, false);

      setPermissions(ORDER_READ, ESB_OPERATE);
      EsbRouteProviderTest.assertPermissions(getJson("/qqq/v1/esb/table/" + TABLE_NAME_ORDER).getJSONObject("permissions"), true, false);

      setPermissions(ORDER_READ, ESB_OPERATE, ESB_DELETE);
      EsbRouteProviderTest.assertPermissions(getJson("/qqq/v1/esb/table/" + TABLE_NAME_ORDER).getJSONObject("permissions"), true, true);

      setPermissions(SYNC_ORDER_ACCESS, ESB_DELETE);
      EsbRouteProviderTest.assertPermissions(getJson("/qqq/v1/esb/process/" + PROCESS_NAME_SYNC_ORDER).getJSONObject("permissions"), false, true);

      setPermissions(ESB_VIEW, ESB_OPERATE);
      EsbRouteProviderTest.assertPermissions(getJson("/qqq/v1/esb/overview").getJSONObject("permissions"), true, false);
   }



   /*******************************************************************************
    ** A destination's messages need esbView, or READ on a table publishing to
    ** it, or access to a process it triggers.
    *******************************************************************************/
   @Test
   void testMessagesPermission() throws Exception
   {
      sendToQueue(BROKER_NAME_FULFILLMENT, Map.of(), "one");
      String path = "/qqq/v1/esb/messages/" + DESTINATION_FULFILLMENT;

      for(String permission : List.of(ESB_VIEW, ORDER_READ, FULFILL_ORDER_ACCESS))
      {
         setPermissions(permission);
         assertEquals(1, getJson(path).getJSONArray("messages").length(), permission);
      }

      setPermissions(SYNC_ORDER_ACCESS);
      assertEquals(403, get(path).statusCode());
   }



   /*******************************************************************************
    ** Reading a table that publishes to a topic must not expose a hidden
    ** process's subscription messages.
    *******************************************************************************/
   @Test
   void testTopicMessagesRequireSelectedTriggerProcessAccess() throws Exception
   {
      String path = "/qqq/v1/esb/messages/" + DESTINATION_ORDER_EVENTS + "?trigger=" + TRIGGER_SYNC_ORDER;

      setPermissions(ORDER_READ);
      assertEquals(403, get(path).statusCode());

      setPermissions(ORDER_READ, SYNC_ORDER_ACCESS);
      assertEquals(200, get(path).statusCode());
   }



   /*******************************************************************************
    ** The esb app (and its overview widget) are in the meta-data only for a
    ** session with esbView.hasAccess; the instance with them is valid.
    *******************************************************************************/
   @Test
   void testEsbAppOnlyWithEsbView() throws Exception
   {
      new QInstanceValidator().validate(qInstance);

      QContext.init(qInstance, new QSession());
      MetaDataOutput withoutView = new MetaDataAction().execute(new MetaDataInput());
      assertThat(withoutView.getApps()).doesNotContainKey(EsbAppMetaDataProducer.NAME);
      assertThat(withoutView.getWidgets()).doesNotContainKey(EsbOverviewWidgetMetaDataProducer.NAME);

      QContext.init(qInstance, new QSession().withPermission(ESB_VIEW));
      MetaDataOutput withView = new MetaDataAction().execute(new MetaDataInput());
      assertThat(withView.getApps()).containsKey(EsbAppMetaDataProducer.NAME);
      assertEquals("ESB", withView.getApps().get(EsbAppMetaDataProducer.NAME).getLabel());
      assertThat(withView.getApps().get(EsbAppMetaDataProducer.NAME).getWidgets()).containsExactly(EsbOverviewWidgetMetaDataProducer.NAME);
      assertEquals(EsbOverviewWidgetMetaDataProducer.TYPE, withView.getWidgets().get(EsbOverviewWidgetMetaDataProducer.NAME).getType());
   }



   /*******************************************************************************
    ** The overview widget renders: its data is the widget type (Next's renderer
    ** reads the overview endpoint).
    *******************************************************************************/
   @Test
   void testOverviewWidgetRenders() throws Exception
   {
      RenderWidgetInput input = new RenderWidgetInput().withWidgetMetaData(qInstance.getWidget(EsbOverviewWidgetMetaDataProducer.NAME));

      QContext.init(qInstance, new QSession().withPermission(ESB_VIEW));
      RenderWidgetOutput output = new RenderWidgetAction().execute(input);
      assertEquals(EsbOverviewWidgetMetaDataProducer.TYPE, output.getWidgetData().getType());
   }



   /*******************************************************************************
    ** The overview JSON with esbView and nothing else (no table or process
    ** access) still lists every destination and its publishers.
    *******************************************************************************/
   @Test
   void testOverviewWithEsbViewOnly() throws Exception
   {
      setPermissions(ESB_VIEW);
      JSONObject overview = getJson("/qqq/v1/esb/overview");
      assertEquals(2, overview.getJSONArray("destinations").length());
      assertEquals(2, overview.getJSONArray("destinations").getJSONObject(0).getJSONArray("publishers").length());
   }

}
