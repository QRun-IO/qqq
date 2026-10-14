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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.RecordSearchProviderInterface;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchResult;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import io.javalin.http.ContentType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for RecordSearchSpecV1
 *******************************************************************************/
class RecordSearchSpecV1Test extends SpecTestBase
{
   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new RecordSearchSpecV1();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected String getVersion()
   {
      return "v1";
   }



   /***************************************************************************
    ** person is searchable by name and email; pet by name.
    ***************************************************************************/
   @Override
   protected QInstance defineQInstance() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      qInstance.getTable(TestUtils.TABLE_NAME_PERSON).withSearchFields("firstName", "lastName", "email");
      qInstance.getTable(TestUtils.TABLE_NAME_PET).withSearchFields("name");
      return (qInstance);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private HttpResponse<String> post(Map<String, ?> body)
   {
      return Unirest.post(getBaseUrlAndPath() + "/search")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(body))
         .asString();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static List<String> results(HttpResponse<String> response)
   {
      assertEquals(200, response.getStatus(), response.getBody());
      JSONArray    results = JsonUtils.toJSONObject(response.getBody()).getJSONArray("results");
      List<String> list    = new ArrayList<>();
      for(int i = 0; i < results.length(); i++)
      {
         JSONObject result = results.getJSONObject(i);
         list.add(result.getString("tableName") + "|" + result.getString("tableLabel") + "|" + result.getString("recordId") + "|" + result.getString("recordLabel"));
      }
      return (list);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSearchAcrossTablesIgnoringCase()
   {
      assertThat(results(post(Map.of("searchTerm", "KELKHOFF"))))
         .containsExactly("person|Person|1|Darin Kelkhoff", "person|Person|6|Linda Kelkhoff");

      assertThat(results(post(Map.of("searchTerm", "ch")))).containsExactly("person|Person|3|Tim Chamberlain", "person|Person|5|Garret Richardson", "pet|Pet|1|Chester");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableNamesAndLimit()
   {
      assertThat(results(post(Map.of("searchTerm", "ch", "tableNames", List.of("pet"))))).containsExactly("pet|Pet|1|Chester");
      assertThat(results(post(Map.of("searchTerm", "kelkhoff", "limitPerTable", 1)))).containsExactly("person|Person|1|Darin Kelkhoff");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNoMatchesReturnsAnEmptyList()
   {
      HttpResponse<String> response = post(Map.of("searchTerm", "no such text"));
      assertEquals(200, response.getStatus());
      assertEquals(0, JsonUtils.toJSONObject(response.getBody()).getJSONArray("results").length());
   }



   /*******************************************************************************
    ** A table the session may not read is skipped, even when named.
    *******************************************************************************/
   @Test
   void testTableWithoutReadPermissionIsNotSearched()
   {
      serverQInstance.getTable(TestUtils.TABLE_NAME_PET).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));

      assertThat(results(post(Map.of("searchTerm", "ch")))).containsExactly("person|Person|3|Tim Chamberlain", "person|Person|5|Garret Richardson");
      assertThat(results(post(Map.of("searchTerm", "ch", "tableNames", List.of("pet"))))).isEmpty();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBadRequests()
   {
      for(Map<String, ?> body : List.<Map<String, ?>>of(Map.of(), Map.of("searchTerm", "  "), Map.of("searchTerm", "x".repeat(101)), Map.of("searchTerm", "a", "tableNames", "person"), Map.of("searchTerm", "a", "limitPerTable", "x")))
      {
         HttpResponse<String> response = post(body);
         assertEquals(400, response.getStatus(), "for body " + body + ": " + response.getBody());
         assertThat(JsonUtils.toJSONObject(response.getBody()).getString("error")).isNotBlank();
      }
   }



   /*******************************************************************************
    ** With a record search provider registered, the response keeps the same
    ** shape: the provider's tables (here, person - its hits in its order) and
    ** the default search's tables (pet) are merged into one results list.
    *******************************************************************************/
   @Test
   void testProviderResultsUseTheSameContract()
   {
      serverQInstance.withRecordSearchProvider(new QCodeReference(PersonOnlyProvider.class));

      assertThat(results(post(Map.of("searchTerm", "ch"))))
         .containsExactly("person|Person|6|Linda Kelkhoff", "person|Person|1|Darin Kelkhoff", "pet|Pet|1|Chester");
      assertThat(results(post(Map.of("searchTerm", "ch", "limitPerTable", 1))))
         .containsExactly("person|Person|6|Linda Kelkhoff", "pet|Pet|1|Chester");
   }



   /*******************************************************************************
    ** A failing provider does not fail the request: its tables fall back to the
    ** default search.
    *******************************************************************************/
   @Test
   void testFailingProviderFallsBackToDefaultSearch()
   {
      serverQInstance.withRecordSearchProvider(new QCodeReference(FailingProvider.class));

      assertThat(results(post(Map.of("searchTerm", "ch")))).containsExactly("person|Person|3|Tim Chamberlain", "person|Person|5|Garret Richardson", "pet|Pet|1|Chester");
   }



   /*******************************************************************************
    ** Each result carries plain-text highlights of matching non-label fields;
    ** the response summarizes each table and the provider status.
    *******************************************************************************/
   @Test
   void testResponseHasHighlightsTablesAndProvider()
   {
      HttpResponse<String> response = post(Map.of("searchTerm", "KELKHOFF"));
      assertEquals(200, response.getStatus(), response.getBody());
      JSONObject body = JsonUtils.toJSONObject(response.getBody());

      JSONObject darin = body.getJSONArray("results").getJSONObject(0);
      assertEquals("Darin Kelkhoff", darin.getString("recordLabel"));
      assertThat(darin.has("score")).isFalse();
      JSONObject highlight = darin.getJSONArray("highlights").getJSONObject(0);
      assertEquals("email", highlight.getString("fieldName"));
      assertEquals("Email", highlight.getString("fieldLabel"));
      assertEquals("darin.kelkhoff@gmail.com", highlight.getString("text"));
      assertEquals(6, highlight.getJSONArray("matches").getJSONObject(0).getInt("start"));
      assertEquals(14, highlight.getJSONArray("matches").getJSONObject(0).getInt("end"));

      JSONArray tables = body.getJSONArray("tables");
      assertEquals(2, tables.length());
      JSONObject person = tables.getJSONObject(0);
      assertEquals("person", person.getString("tableName"));
      assertEquals("Person", person.getString("tableLabel"));
      assertEquals(2, person.getInt("resultCount"));
      assertThat(person.getBoolean("hasMore")).isFalse();
      assertEquals("DEFAULT", person.getString("source"));
      assertThat(person.has("totalCount")).isFalse();
      assertEquals("pet", tables.getJSONObject(1).getString("tableName"));
      assertEquals(0, tables.getJSONObject(1).getInt("resultCount"));

      assertEquals("NONE", body.getJSONObject("provider").getString("status"));

      ///////////////////////////////////////////////////////////////////
      // a match in the label only: the (non-label) email is a detail //
      ///////////////////////////////////////////////////////////////////
      JSONObject tim = JsonUtils.toJSONObject(post(Map.of("searchTerm", "Tim")).getBody()).getJSONArray("results").getJSONObject(0);
      assertEquals("Tim Chamberlain", tim.getString("recordLabel"));
      assertThat(tim.has("highlights")).isFalse();
      assertEquals("tchamberlain@mmltholdings.com", tim.getJSONArray("details").getJSONObject(0).getString("displayValue"));
   }



   /*******************************************************************************
    ** offset pages through one table; sort is validated.
    *******************************************************************************/
   @Test
   void testOffsetAndSort()
   {
      HttpResponse<String> response = post(Map.of("searchTerm", "mmltholdings", "limitPerTable", 2));
      assertThat(results(response)).containsExactly("person|Person|2|James Maes", "person|Person|3|Tim Chamberlain");
      assertThat(JsonUtils.toJSONObject(response.getBody()).getJSONArray("tables").getJSONObject(0).getBoolean("hasMore")).isTrue();

      response = post(Map.of("searchTerm", "mmltholdings", "limitPerTable", 2, "offset", 2, "sort", "relevance"));
      assertThat(results(response)).containsExactly("person|Person|4|Tyler Samples", "person|Person|5|Garret Richardson");
      assertThat(JsonUtils.toJSONObject(response.getBody()).getJSONArray("tables").getJSONObject(0).getBoolean("hasMore")).isFalse();

      assertThat(results(post(Map.of("searchTerm", "mmltholdings", "sort", "RECENT")))).hasSize(4);

      for(Map<String, ?> body : List.<Map<String, ?>>of(Map.of("searchTerm", "a", "offset", "x"), Map.of("searchTerm", "a", "sort", "bogus"), Map.of("searchTerm", "a", "sort", 1)))
      {
         HttpResponse<String> badResponse = post(body);
         assertEquals(400, badResponse.getStatus(), "for body " + body + ": " + badResponse.getBody());
      }
   }



   /*******************************************************************************
    ** The provider's status and each table's source are reported.
    *******************************************************************************/
   @Test
   void testProviderStatusAndSources()
   {
      serverQInstance.withRecordSearchProvider(new QCodeReference(PersonOnlyProvider.class));
      JSONObject body = JsonUtils.toJSONObject(post(Map.of("searchTerm", "ch")).getBody());
      assertEquals("OK", body.getJSONObject("provider").getString("status"));
      assertEquals("PROVIDER", body.getJSONArray("tables").getJSONObject(0).getString("source"));
      assertEquals("DEFAULT", body.getJSONArray("tables").getJSONObject(1).getString("source"));
      assertEquals(3.5, body.getJSONArray("results").getJSONObject(0).getDouble("score"));

      serverQInstance.withRecordSearchProvider(new QCodeReference(FailingProvider.class));
      body = JsonUtils.toJSONObject(post(Map.of("searchTerm", "ch")).getBody());
      assertEquals("FAILED", body.getJSONObject("provider").getString("status"));
      assertEquals("QException", body.getJSONObject("provider").getString("failureClass"));
      assertThat(body.getJSONObject("provider").has("message")).isFalse();
      assertEquals("FALLBACK", body.getJSONArray("tables").getJSONObject(0).getString("source"));
      assertEquals("FALLBACK", body.getJSONArray("tables").getJSONObject(1).getString("source"));
   }



   /*******************************************************************************
    ** Provider that claims the person table and always finds persons 6 then 1.
    *******************************************************************************/
   public static class PersonOnlyProvider implements RecordSearchProviderInterface
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public boolean claimsTable(QTableMetaData table)
      {
         return (TestUtils.TABLE_NAME_PERSON.equals(table.getName()));
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public RecordSearchOutput search(RecordSearchInput input)
      {
         return (new RecordSearchOutput().withResults(List.of(
            new RecordSearchResult().withTableName(TestUtils.TABLE_NAME_PERSON).withRecordId("6").withScore(3.5),
            new RecordSearchResult().withTableName(TestUtils.TABLE_NAME_PERSON).withRecordId(1))));
      }
   }



   /*******************************************************************************
    ** Provider that claims every table and always fails, like an unreachable
    ** search index.
    *******************************************************************************/
   public static class FailingProvider implements RecordSearchProviderInterface
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public boolean claimsTable(QTableMetaData table)
      {
         return (true);
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public RecordSearchOutput search(RecordSearchInput input) throws QException
      {
         throw (new QException("search index unavailable"));
      }
   }

}
