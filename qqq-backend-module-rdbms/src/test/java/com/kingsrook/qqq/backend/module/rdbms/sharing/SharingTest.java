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

package com.kingsrook.qqq.backend.module.rdbms.sharing;


import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.QueryManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.sharing.model.Asset;
import com.kingsrook.qqq.backend.module.rdbms.sharing.model.Group;
import com.kingsrook.qqq.backend.module.rdbms.sharing.model.SharedAsset;
import com.kingsrook.qqq.backend.module.rdbms.sharing.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.backend.module.rdbms.sharing.SharingMetaDataProvider.GROUP_ID_ALL_ACCESS_KEY_TYPE;
import static com.kingsrook.qqq.backend.module.rdbms.sharing.SharingMetaDataProvider.GROUP_ID_KEY_TYPE;
import static com.kingsrook.qqq.backend.module.rdbms.sharing.SharingMetaDataProvider.USER_ID_ALL_ACCESS_KEY_TYPE;
import static com.kingsrook.qqq.backend.module.rdbms.sharing.SharingMetaDataProvider.USER_ID_KEY_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 **
 *******************************************************************************/
public class SharingTest
{
   //////////////
   // user ids //
   //////////////
   public static final int HOMER_ID = 1;
   public static final int MARGE_ID = 2;
   public static final int BART_ID  = 3;
   public static final int LISA_ID  = 4;
   public static final int BURNS_ID = 5;

   ///////////////
   // group ids //
   ///////////////
   public static final int SIMPSONS_ID    = 1;
   public static final int POWER_PLANT_ID = 2;
   public static final int BOGUS_GROUP_ID = Integer.MAX_VALUE;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws Exception
   {
      TestUtils.primeTestDatabase("prime-test-database-sharing-test.sql");

      QInstance qInstance = TestUtils.defineInstance();
      SharingMetaDataProvider.defineAll(qInstance);

      QContext.init(qInstance, new QSession());

      loadData();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void loadData() throws QException
   {
      QContext.getQSession().withSecurityKeyValue(SharingMetaDataProvider.USER_ID_ALL_ACCESS_KEY_TYPE, true);

      List<QRecordEntity> userList = List.of(
         new User().withId(HOMER_ID).withUsername("homer"),
         new User().withId(MARGE_ID).withUsername("marge"),
         new User().withId(BART_ID).withUsername("bart"),
         new User().withId(LISA_ID).withUsername("lisa"),
         new User().withId(BURNS_ID).withUsername("burns"));
      new InsertAction().execute(new InsertInput(User.TABLE_NAME).withRecordEntities(userList));

      List<QRecordEntity> groupList = List.of(
         new Group().withId(SIMPSONS_ID).withName("simpsons"),
         new Group().withId(POWER_PLANT_ID).withName("powerplant"));
      new InsertAction().execute(new InsertInput(Group.TABLE_NAME).withRecordEntities(groupList));

      List<QRecordEntity> assetList = List.of(
         new Asset().withId(1).withName("742evergreen").withUserId(HOMER_ID),
         new Asset().withId(2).withName("beer").withUserId(HOMER_ID),
         new Asset().withId(3).withName("car").withUserId(MARGE_ID),
         new Asset().withId(4).withName("skateboard").withUserId(BART_ID),
         new Asset().withId(5).withName("santaslittlehelper").withUserId(BART_ID),
         new Asset().withId(6).withName("saxamaphone").withUserId(LISA_ID),
         new Asset().withId(7).withName("radiation").withUserId(BURNS_ID));
      new InsertAction().execute(new InsertInput(Asset.TABLE_NAME).withRecordEntities(assetList));

      List<QRecordEntity> sharedAssetList = List.of(
         new SharedAsset().withAssetId(1).withGroupId(SIMPSONS_ID), // homer shares his house with the simpson family (group)
         new SharedAsset().withAssetId(3).withUserId(HOMER_ID), // marge shares a car with homer
         new SharedAsset().withAssetId(5).withGroupId(SIMPSONS_ID), // bart shares santa's little helper with the whole family
         new SharedAsset().withAssetId(7).withGroupId(POWER_PLANT_ID) // burns shares radiation with the power plant
      );
      new InsertAction().execute(new InsertInput(SharedAsset.TABLE_NAME).withRecordEntities(sharedAssetList));

      QContext.getQSession().withSecurityKeyValues(new HashMap<>());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQueryAssetWithUserIdOnlySecurityKey() throws QException
   {
      ////////////////////////////////////////////////////////////////////
      // update the asset table to change its lock to only be on userId //
      ////////////////////////////////////////////////////////////////////
      QContext.getQInstance().getTable(Asset.TABLE_NAME)
         .withRecordSecurityLocks(List.of(new RecordSecurityLock()
            .withSecurityKeyType(USER_ID_KEY_TYPE)
            .withFieldName("userId")));

      ////////////////////////////////////////////////////////
      // with nothing in session, make sure we find nothing //
      ////////////////////////////////////////////////////////
      assertEquals(0, new QueryAction().execute(new QueryInput(Asset.TABLE_NAME)).getRecords().size());

      ////////////////////////////////////
      // marge direct owner only of car //
      ////////////////////////////////////
      QContext.getQSession().withSecurityKeyValues(new HashMap<>());
      QContext.getQSession().withSecurityKeyValue(USER_ID_KEY_TYPE, MARGE_ID);
      assertEquals(1, new QueryAction().execute(new QueryInput(Asset.TABLE_NAME)).getRecords().size());

      /////////////////////////////////////////////////
      // homer direct owner of 742evergreen and beer //
      /////////////////////////////////////////////////
      QContext.getQSession().withSecurityKeyValues(new HashMap<>());
      QContext.getQSession().withSecurityKeyValue(USER_ID_KEY_TYPE, HOMER_ID);
      assertEquals(2, new QueryAction().execute(new QueryInput(Asset.TABLE_NAME)).getRecords().size());

      /////////////////////////////////////////////////////
      // marge & homer - own car, 742evergreen, and beer //
      /////////////////////////////////////////////////////
      QContext.getQSession().withSecurityKeyValues(new HashMap<>());
      QContext.getQSession().withSecurityKeyValue(USER_ID_KEY_TYPE, HOMER_ID);
      QContext.getQSession().withSecurityKeyValue(USER_ID_KEY_TYPE, MARGE_ID);
      assertEquals(3, new QueryAction().execute(new QueryInput(Asset.TABLE_NAME)).getRecords().size());
   }



   /*******************************************************************************
    ** normally (?) maybe we wouldn't query sharedAsset directly (we'd instead query
    ** for asset, and understand that there's a security lock coming from sharedAsset),
    ** but this test is here as we build up making a more complex lock like that.
    *******************************************************************************/
   @Test
   void testQuerySharedAssetDirectly() throws QException
   {
      ////////////////////////////////////////////////////////
      // with nothing in session, make sure we find nothing //
      ////////////////////////////////////////////////////////
      assertEquals(0, new QueryAction().execute(new QueryInput(SharedAsset.TABLE_NAME)).getRecords().size());

      /////////////////////////////////////
      // homer has a car shared with him //
      /////////////////////////////////////
      QContext.getQSession().withSecurityKeyValues(new HashMap<>());
      QContext.getQSession().withSecurityKeyValue(USER_ID_KEY_TYPE, HOMER_ID);
      assertEquals(1, new QueryAction().execute(new QueryInput(SharedAsset.TABLE_NAME)).getRecords().size());

      /////////////////////////////////////////////////////////////////////////////////////////
      // now put homer's groups in the session as well - and we should find 742evergreen too //
      /////////////////////////////////////////////////////////////////////////////////////////
      QContext.getQSession().withSecurityKeyValues(new HashMap<>());
      QContext.getQSession().withSecurityKeyValue(USER_ID_KEY_TYPE, HOMER_ID);
      QContext.getQSession().withSecurityKeyValue(GROUP_ID_KEY_TYPE, SIMPSONS_ID);
      QContext.getQSession().withSecurityKeyValue(GROUP_ID_KEY_TYPE, POWER_PLANT_ID);
      List<QRecord> records = new QueryAction().execute(new QueryInput(SharedAsset.TABLE_NAME)).getRecords();
      assertEquals(4, records.size());
   }



   /*******************************************************************************
    ** real-world use-case (e.g., why sharing concept exists) - query the asset table
    **
    *******************************************************************************/
   @Test
   void testQueryAssetsWithLockThroughSharing() throws QException, SQLException
   {
      ////////////////////////////////////////////////////////
      // with nothing in session, make sure we find nothing //
      ////////////////////////////////////////////////////////
      assertEquals(0, new QueryAction().execute(new QueryInput(Asset.TABLE_NAME)).getRecords().size());

      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // homer has a car shared with him and 2 things he owns himself - so w/ only his userId in session (and no groups), should find those 3 //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      QContext.getQSession().withSecurityKeyValues(new HashMap<>());
      QContext.getQSession().withSecurityKeyValue(USER_ID_KEY_TYPE, HOMER_ID);
      assertEquals(3, new QueryAction().execute(new QueryInput(Asset.TABLE_NAME)).getRecords().size());

      //////////////////////////////////////////////////////////////////////
      // add a group that matches nothing now, just to ensure same result //
      //////////////////////////////////////////////////////////////////////
      QContext.getQSession().withSecurityKeyValue(GROUP_ID_KEY_TYPE, BOGUS_GROUP_ID);
      assertEquals(3, new QueryAction().execute(new QueryInput(Asset.TABLE_NAME)).getRecords().size());

      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // now put homer's groups in the session as well - and we should find the 3 from above, plus a shared family asset and shared power-plant asset //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      QContext.getQSession().withSecurityKeyValues(new HashMap<>());
      QContext.getQSession().withSecurityKeyValue(USER_ID_KEY_TYPE, HOMER_ID);
      QContext.getQSession().withSecurityKeyValue(GROUP_ID_KEY_TYPE, SIMPSONS_ID);
      QContext.getQSession().withSecurityKeyValue(GROUP_ID_KEY_TYPE, POWER_PLANT_ID);
      assertEquals(5, new QueryAction().execute(new QueryInput(Asset.TABLE_NAME)).getRecords().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQueryAllAccessKeys() throws QException
   {
      ///////////////////////////////////////////////////////////////
      // with user-id all access key, should get all asset records //
      ///////////////////////////////////////////////////////////////
      QContext.getQSession().withSecurityKeyValues(new HashMap<>());
      QContext.getQSession().withSecurityKeyValue(USER_ID_ALL_ACCESS_KEY_TYPE, true);
      assertEquals(7, new QueryAction().execute(new QueryInput(Asset.TABLE_NAME)).getRecords().size());

      //////////////////////////////////////////////////////////////////////////////////////////////
      // with group-id all access key...                                                          //
      // the original thought was, that we should get all assets which are shared to any group    //
      // but the code that we first wrote generates SQL w/ an OR (1=1) clause, meaning we get all //
      // assets, which makes some sense too, so we'll go with that for now...                     //
      //////////////////////////////////////////////////////////////////////////////////////////////
      QContext.getQSession().withSecurityKeyValues(new HashMap<>());
      QContext.getQSession().withSecurityKeyValue(GROUP_ID_ALL_ACCESS_KEY_TYPE, true);
      assertEquals(7, new QueryAction().execute(new QueryInput(Asset.TABLE_NAME)).getRecords().size());
   }



   /*******************************************************************************
    ** if I'm only able to access user 1 and 2, I shouldn't be able to share to user 3
    *******************************************************************************/
   @Test
   void testInsertUpdateDeleteShareUserIdKey() throws QException, SQLException
   {
      SharedAsset recordToInsert = new SharedAsset().withUserId(3).withAssetId(6);

      /////////////////////////////////////////
      // empty set of keys should give error //
      /////////////////////////////////////////
      QContext.getQSession().withSecurityKeyValues(new HashMap<>());
      InsertOutput insertOutput = new InsertAction().execute(new InsertInput(SharedAsset.TABLE_NAME).withRecordEntity(recordToInsert));
      assertThat(insertOutput.getRecords().get(0).getErrors()).isNotEmpty();

      /////////////////////////////////////////////
      // mis-matched keys should give same error //
      /////////////////////////////////////////////
      QContext.getQSession().withSecurityKeyValue(USER_ID_KEY_TYPE, 1);
      QContext.getQSession().withSecurityKeyValue(USER_ID_KEY_TYPE, 2);
      insertOutput = new InsertAction().execute(new InsertInput(SharedAsset.TABLE_NAME).withRecordEntity(recordToInsert));
      assertThat(insertOutput.getRecords().get(0).getErrors()).isNotEmpty();

      /////////////////////////////////////////////////////////
      // then if I get user 3, I can insert the share for it //
      /////////////////////////////////////////////////////////
      QContext.getQSession().withSecurityKeyValue(USER_ID_KEY_TYPE, 3);
      insertOutput = new InsertAction().execute(new InsertInput(SharedAsset.TABLE_NAME).withRecordEntity(recordToInsert));
      assertThat(insertOutput.getRecords().get(0).getErrors()).isEmpty();

      /////////////////////////////////////////
      // get ready for a sequence of updates //
      /////////////////////////////////////////
      Integer           shareId            = insertOutput.getRecords().get(0).getValueInteger("id");
      Supplier<QRecord> makeRecordToUpdate = () -> new QRecord().withValue("id", shareId).withValue("modifyDate", Instant.now());

      ///////////////////////////////////////////////////////////////////////////////
      // now w/o user 3 in my session, I shouldn't be allowed to update that share //
      // start w/ empty security keys                                              //
      ///////////////////////////////////////////////////////////////////////////////
      QContext.getQSession().withSecurityKeyValues(new HashMap<>());
      UpdateOutput updateOutput = new UpdateAction().execute(new UpdateInput(SharedAsset.TABLE_NAME).withRecord(makeRecordToUpdate.get()));
      assertThat(updateOutput.getRecords().get(0).getErrors())
         .anyMatch(e -> e.getMessage().contains("No record was found")); // because w/o the key, you can't even see it.

      /////////////////////////////////////////////
      // mis-matched keys should give same error //
      /////////////////////////////////////////////
      QContext.getQSession().withSecurityKeyValue(USER_ID_KEY_TYPE, 1);
      updateOutput = new UpdateAction().execute(new UpdateInput(SharedAsset.TABLE_NAME).withRecord(makeRecordToUpdate.get()));
      assertThat(updateOutput.getRecords().get(0).getErrors())
         .anyMatch(e -> e.getMessage().contains("No record was found")); // because w/o the key, you can't even see it.

      //////////////////////////////////////////////////
      // now with user id 3, should be able to update //
      //////////////////////////////////////////////////
      QContext.getQSession().withSecurityKeyValue(USER_ID_KEY_TYPE, 3);
      updateOutput = new UpdateAction().execute(new UpdateInput(SharedAsset.TABLE_NAME).withRecord(makeRecordToUpdate.get()));
      assertThat(updateOutput.getRecords().get(0).getErrors()).isEmpty();

      //////////////////////////////////////////////////////////////////////////
      // now see if you can update to a user that you don't have (you can't!) //
      //////////////////////////////////////////////////////////////////////////
      /* todo - here's where the logic in ValidateRecordSecurityLockHelper fails us...
      updateOutput = new UpdateAction().execute(new UpdateInput(SharedAsset.TABLE_NAME).withRecord(makeRecordToUpdate.get().withValue("userId", 2)));
      assertThat(updateOutput.getRecords().get(0).getErrors()).isNotEmpty();
       */

      ///////////////////////////////////////////////////////////////////////
      // Add that user (2) to the session - then the update should succeed //
      ///////////////////////////////////////////////////////////////////////
      QContext.getQSession().withSecurityKeyValue(USER_ID_KEY_TYPE, 2);
      updateOutput = new UpdateAction().execute(new UpdateInput(SharedAsset.TABLE_NAME).withRecord(makeRecordToUpdate.get().withValue("userId", 2)));
      assertThat(updateOutput.getRecords().get(0).getErrors()).isEmpty();

      ///////////////////////////////////////////////
      // now move on to deletes - first empty keys //
      ///////////////////////////////////////////////
      QContext.getQSession().withSecurityKeyValues(new HashMap<>());
      DeleteOutput deleteOutput = new DeleteAction().execute(new DeleteInput(SharedAsset.TABLE_NAME).withPrimaryKey(shareId));
      assertEquals(0, deleteOutput.getDeletedRecordCount()); // can't even find it, so no error to be reported.

      ///////////////////////
      // next mismatch key //
      ///////////////////////
      QContext.getQSession().withSecurityKeyValue(USER_ID_KEY_TYPE, 1);
      deleteOutput = new DeleteAction().execute(new DeleteInput(SharedAsset.TABLE_NAME).withPrimaryKey(shareId));
      assertEquals(0, deleteOutput.getDeletedRecordCount()); // can't even find it, so no error to be reported.

      ///////////////////
      // next success! //
      ///////////////////
      QContext.getQSession().withSecurityKeyValue(USER_ID_KEY_TYPE, 2);
      deleteOutput = new DeleteAction().execute(new DeleteInput(SharedAsset.TABLE_NAME).withPrimaryKey(shareId));
      assertEquals(1, deleteOutput.getDeletedRecordCount());
   }



   /*******************************************************************************
    ** useful to debug (e.g., to see inside h2). add calls as needed.
    *******************************************************************************/
   private void printSQL(String sql) throws SQLException
   {
      Connection                connection = new ConnectionManager().getConnection((RDBMSBackendMetaData) QContext.getQInstance().getBackend(TestUtils.DEFAULT_BACKEND_NAME));
      List<Map<String, Object>> maps       = QueryManager.executeStatementForRows(connection, sql);
      System.out.println(sql);
      maps.forEach(System.out::println);
   }



   /*******************************************************************************
    ** if I only have access to group 1, make sure I can't share to group 2
    *******************************************************************************/
   @Test
   void testInsertShareGroupIdKey() throws QException
   {
      QContext.getQSession().withSecurityKeyValues(new HashMap<>());
      QContext.getQSession().withSecurityKeyValue(GROUP_ID_KEY_TYPE, 1);
      InsertOutput insertOutput = new InsertAction().execute(new InsertInput(SharedAsset.TABLE_NAME).withRecordEntity(new SharedAsset().withGroupId(2).withAssetId(6)));
      assertThat(insertOutput.getRecords().get(0).getErrors()).isNotEmpty();

      //////////////////////////////////////////
      // add group 2, then we can share to it //
      //////////////////////////////////////////
      QContext.getQSession().withSecurityKeyValue(GROUP_ID_KEY_TYPE, 2);
      insertOutput = new InsertAction().execute(new InsertInput(SharedAsset.TABLE_NAME).withRecordEntity(new SharedAsset().withGroupId(2).withAssetId(6)));
      assertThat(insertOutput.getRecords().get(0).getErrors()).isEmpty();
   }



   /*******************************************************************************
    ** w/ user-all-access key, can insert shares for any user
    *******************************************************************************/
   @Test
   void testInsertUpdateDeleteShareUserAllAccessKey() throws QException
   {
      QContext.getQSession().withSecurityKeyValues(new HashMap<>());
      QContext.getQSession().withSecurityKeyValue(USER_ID_ALL_ACCESS_KEY_TYPE, true);
      InsertOutput insertOutput = new InsertAction().execute(new InsertInput(SharedAsset.TABLE_NAME).withRecordEntity(new SharedAsset().withUserId(1).withAssetId(4)));
      assertThat(insertOutput.getRecords().get(0).getErrors()).isEmpty();

      /////////////////////////////////////////
      // get ready for a sequence of updates //
      /////////////////////////////////////////
      Integer           shareId            = insertOutput.getRecords().get(0).getValueInteger("id");
      Supplier<QRecord> makeRecordToUpdate = () -> new QRecord().withValue("id", shareId).withValue("modifyDate", Instant.now());

      //////////////////////////////////
      // now w/o all-access key, fail //
      //////////////////////////////////
      QContext.getQSession().withSecurityKeyValues(new HashMap<>());
      UpdateOutput updateOutput = new UpdateAction().execute(new UpdateInput(SharedAsset.TABLE_NAME).withRecord(makeRecordToUpdate.get()));
      assertThat(updateOutput.getRecords().get(0).getErrors())
         .anyMatch(e -> e.getMessage().contains("No record was found")); // because w/o the key, you can't even see it.

      ///////////////////////////////////////////////////////
      // now with all-access key, should be able to update //
      ///////////////////////////////////////////////////////
      QContext.getQSession().withSecurityKeyValue(USER_ID_ALL_ACCESS_KEY_TYPE, true);
      updateOutput = new UpdateAction().execute(new UpdateInput(SharedAsset.TABLE_NAME).withRecord(makeRecordToUpdate.get().withValue("userId", 2)));
      assertThat(updateOutput.getRecords().get(0).getErrors()).isEmpty();

      ///////////////////////////////////////////////
      // now move on to deletes - first empty keys //
      ///////////////////////////////////////////////
      QContext.getQSession().withSecurityKeyValues(new HashMap<>());
      DeleteOutput deleteOutput = new DeleteAction().execute(new DeleteInput(SharedAsset.TABLE_NAME).withPrimaryKey(shareId));
      assertEquals(0, deleteOutput.getDeletedRecordCount()); // can't even find it, so no error to be reported.

      ///////////////////
      // next success! //
      ///////////////////
      QContext.getQSession().withSecurityKeyValue(USER_ID_ALL_ACCESS_KEY_TYPE, true);
      deleteOutput = new DeleteAction().execute(new DeleteInput(SharedAsset.TABLE_NAME).withPrimaryKey(shareId));
      assertEquals(1, deleteOutput.getDeletedRecordCount());
   }



   /*******************************************************************************
    ** w/ group-all-access key, can insert shares for any group
    *******************************************************************************/
   @Test
   void testInsertShareGroupAllAccessKey() throws QException
   {
      QContext.getQSession().withSecurityKeyValues(new HashMap<>());
      QContext.getQSession().withSecurityKeyValue(GROUP_ID_ALL_ACCESS_KEY_TYPE, true);
      InsertOutput insertOutput = new InsertAction().execute(new InsertInput(SharedAsset.TABLE_NAME).withRecordEntity(new SharedAsset().withGroupId(1).withAssetId(4)));
      assertThat(insertOutput.getRecords().get(0).getErrors()).isEmpty();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   @Disabled("This needs fixed, but we're committing as-we are to move forwards")
   void testUpdateAsset() throws QException
   {
      ////////////////////////////////////////////////////////////////////////////////////////
      // make sure we can't update an Asset if we don't have a key that would let us see it //
      ////////////////////////////////////////////////////////////////////////////////////////
      {
         QContext.getQSession().withSecurityKeyValues(new HashMap<>());
         UpdateOutput updateOutput = new UpdateAction().execute(new UpdateInput(Asset.TABLE_NAME).withRecord(new QRecord().withValue("id", 1).withValue("modifyDate", Instant.now())));
         assertThat(updateOutput.getRecords().get(0).getErrors()).isNotEmpty();
      }

      ///////////////////////////////////////////////
      // and if we do have a key, we can update it //
      ///////////////////////////////////////////////
      {
         QContext.getQSession().withSecurityKeyValues(new HashMap<>());
         QContext.getQSession().withSecurityKeyValue(USER_ID_KEY_TYPE, HOMER_ID);
         UpdateOutput updateOutput = new UpdateAction().execute(new UpdateInput(Asset.TABLE_NAME).withRecord(new QRecord().withValue("id", 1).withValue("modifyDate", Instant.now())));
         assertThat(updateOutput.getRecords().get(0).getErrors()).isEmpty();
      }
   }

}
