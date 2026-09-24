/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.api.actions;


import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrGetInputInterface;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.CaseChangeBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.api.BaseTest;
import com.kingsrook.qqq.backend.module.api.model.AuthorizationType;
import com.kingsrook.qqq.backend.module.api.model.metadata.APIBackendMetaData;
import com.kingsrook.qqq.backend.module.api.model.metadata.APITableBackendDetails;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;


/*******************************************************************************
 ** Native API Get must sanitize the actual customized record that it publishes.
 *******************************************************************************/
class APIGetActionTest extends BaseTest
{
   private static final String PROVIDER_JSON = "{\"provider_id\":7,\"name\":\"provider name\",\"password\":\"provider-secret\",\"hidden\":\"provider-hidden\",\"revealed\":\"visible-secret\"}";



   /*******************************************************************************
    ** Real provider decoding, replacement records, and display generation share privacy rules.
    *******************************************************************************/
   @Test
   void testNativeGetSanitizesReplacementRecord() throws Exception
   {
      assertNativeGetPrivacy(false, false, List.of("password", "hidden"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNativeGetSanitizesReplacementDisplayValues() throws Exception
   {
      assertNativeGetPrivacy(true, false, List.of("password", "hidden"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNativeGetExplicitPrivacyOverride() throws Exception
   {
      assertNativeGetPrivacy(false, true, List.of("password", "hidden"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNativeGetExplicitPrivacyOverrideWithDisplayValues() throws Exception
   {
      assertNativeGetPrivacy(true, true, List.of("password", "hidden"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNativeGetPreservesPublicRecordLabel() throws Exception
   {
      assertNativeGetPrivacy(true, false, List.of("name"));
   }



   /*******************************************************************************
    ** A REVEAL-adorned password may deliberately be used in a visible label.
    *******************************************************************************/
   @Test
   void testNativeGetPreservesRevealedRecordLabel() throws Exception
   {
      assertNativeGetPrivacy(true, false, List.of("revealed"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNativeGetPrivateLabelFallbackCannotReuseOriginalSecret() throws Exception
   {
      assertNativeGetPrivacy(false, false, List.of("password"), "%d");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertNativeGetPrivacy(boolean display, boolean reveal, List<String> labelFields) throws Exception
   {
      assertNativeGetPrivacy(display, reveal, labelFields, String.join(" ", Collections.nCopies(labelFields.size(), "%s")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertNativeGetPrivacy(boolean display, boolean reveal, List<String> labelFields, String labelFormat) throws Exception
   {
      List<String> requests = Collections.synchronizedList(new ArrayList<>());
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext("/records", exchange ->
      {
         requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI());
         byte[] bytes = PROVIDER_JSON.getBytes(StandardCharsets.UTF_8);
         exchange.getResponseHeaders().add("Content-Type", "application/json");
         exchange.sendResponseHeaders(200, bytes.length);
         try(exchange)
         {
            exchange.getResponseBody().write(bytes);
         }
      });
      server.start();
      try
      {
         QContext.getQInstance().addBackend(new APIBackendMetaData().withName("privacyApi")
            .withBaseUrl("http://127.0.0.1:" + server.getAddress().getPort())
            .withAuthorizationType(AuthorizationType.NONE).withContentType("application/json")
            .withActionUtil(new QCodeReference(SingleRecordActionUtil.class)));
         QTableMetaData table = new QTableMetaData().withName("privacyRecord").withBackendName("privacyApi").withPrimaryKeyField("id")
            .withField(new QFieldMetaData("id", QFieldType.INTEGER).withBackendName("provider_id"))
            .withField(new QFieldMetaData("name", QFieldType.STRING).withBehavior(CaseChangeBehavior.TO_UPPER_CASE))
            .withField(new QFieldMetaData("password", QFieldType.PASSWORD))
            .withField(new QFieldMetaData("hidden", QFieldType.STRING).withIsHidden(true))
            .withField(new QFieldMetaData("revealed", QFieldType.PASSWORD).withFieldAdornment(new FieldAdornment(AdornmentType.REVEAL)))
            .withField(new QFieldMetaData("omittedPassword", QFieldType.PASSWORD))
            .withVirtualFields(List.of(new QVirtualFieldMetaData("virtualPassword", QFieldType.PASSWORD).withIsQuerySelectable(true),
               new QVirtualFieldMetaData("virtualHidden", QFieldType.STRING).withIsQuerySelectable(true).withIsHidden(true)))
            .withCustomizer(TableCustomizers.POST_QUERY_RECORD.getRole(), new QCodeReference(CloneRecord.class))
            .withBackendDetails(new APITableBackendDetails().withTablePath("/records"))
            .withRecordLabelFields(labelFields).withRecordLabelFormat(labelFormat);
         QContext.getQInstance().addTable(table);
         GetInput input = new GetInput();
         input.setTableName(table.getName());
         input.setPrimaryKey(7);
         input.setShouldGenerateDisplayValues(display);
         input.setShouldMaskPasswords(!reveal);
         input.setShouldOmitHiddenFields(!reveal);
         QRecord result = new GetAction().execute(input).getRecord();
         String serialized = JsonUtils.toJson(result);
         assertAll(
            () -> assertThat(requests).containsExactly("GET /records/7"),
            () -> assertThat(result).isNotSameAs(CloneRecord.original),
            () ->
            {
               String expectedLabel = "%d".equals(labelFormat) ? "7" : labelFields.contains("password") ? (reveal ? "copy-secret copy-hidden" : "************ ")
                  : labelFields.contains("name") ? "COPY NAME" : "visible-secret";
               assertThat(result.getRecordLabel()).isEqualTo(expectedLabel);
            },
            () -> assertThat(result.getValueString("name")).isEqualTo("COPY NAME"),
            () -> assertThat(CloneRecord.original.getValues()).containsEntry("name", "provider name")
               .containsEntry("password", "provider-secret").containsEntry("hidden", "provider-hidden"),
            () -> assertThat(result.getValueString("password")).isEqualTo(reveal ? "copy-secret" : "************"),
            () -> assertThat(result.getValueString("virtualPassword")).isEqualTo(reveal ? "virtual-secret" : "************"),
            () -> assertThat(result.getValues()).doesNotContainKey("omittedPassword"),
            () -> assertThat(result.getValueString("revealed")).isEqualTo("visible-secret"),
            () -> assertThat(result.getValues().containsKey("hidden")).isEqualTo(reveal),
            () -> assertThat(result.getValues().containsKey("virtualHidden")).isEqualTo(reveal),
            () ->
            {
               if(!reveal)
               {
                  assertThat(result.getDisplayValues()).doesNotContainKeys("hidden", "virtualHidden", "omittedPassword");
               }
            },
            () ->
            {
               if(!reveal)
               {
                  assertThat(result.getDisplayValue("password")).isEqualTo(display ? "************" : null);
               }
            },
            () ->
            {
               if(!reveal)
               {
                  assertThat(result.getDisplayValue("virtualPassword")).isEqualTo(display ? "************" : null);
               }
            },
            () ->
            {
               if(!reveal)
               {
                  assertThat(serialized).doesNotContain("provider-secret", "provider-hidden", "copy-secret", "copy-hidden", "virtual-secret", "virtual-hidden");
               }
            });
      }
      finally
      {
         server.stop(0);
         CloneRecord.original = null;
      }
   }



   /*******************************************************************************
    ** The owned provider addresses a record by its primary key in the URL.
    *******************************************************************************/
   public static class SingleRecordActionUtil extends BaseAPIActionUtil
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public String buildUrlSuffixForSingleRecordGet(Serializable primaryKey)
      {
         return "/" + primaryKey;
      }
   }



   /*******************************************************************************
    ** A provider record is retained only to verify that replacement processing does not mutate it.
    *******************************************************************************/
   public static class CloneRecord implements TableCustomizerInterface
   {
      private static QRecord original;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postQuery(QueryOrGetInputInterface input, List<QRecord> records)
      {
         original = records.get(0);
         QRecord replacement = new QRecord(original).withValue("name", "copy name")
            .withValue("password", "copy-secret").withValue("hidden", "copy-hidden")
            .withValue("virtualPassword", "virtual-secret").withValue("virtualHidden", "virtual-hidden");
         replacement.removeValue("omittedPassword");
         replacement.setDisplayValues(new HashMap<>(Map.of("password", "copy-secret", "hidden", "copy-hidden",
            "virtualPassword", "virtual-secret", "virtualHidden", "virtual-hidden", "omittedPassword", "stale-omitted-secret")));
         replacement.setRecordLabel("copy-secret copy-hidden");
         replacement.setRecordLabel(QValueFormatter.formatRecordLabel(QContext.getQInstance().getTable(input.getTableName()), replacement));
         return List.of(replacement);
      }
   }
}
