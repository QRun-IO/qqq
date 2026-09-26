/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.rdbms.jdbc;


import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.module.rdbms.BaseTest;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Unit test for ConnectionManager
 *******************************************************************************/
@Disabled("This was okay for POC, but shouldn't run in CI")
class ConnectionManagerTest extends BaseTest
{
   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   public void test() throws SQLException
   {
      Connection connection = new ConnectionManager().getConnection(getAuroraBacked());
      assertNotNull(connection);

      String sql = """
         insert into raw_parcel_invoice_line_ups (id, create_date, modify_date, parcel_invoice_line_id, account_country, account_number, account_split_payment_indicator, account_tax_id, alternate_invoice_amount, alternate_invoice_number, alternate_invoicing_currency_code, bol__number_1, bol__number_2, bol__number_3, bol__number_4, bol__number_5, basis_currency_code, basis_value, bill_option_code,
         billed_weight, billed_weight_type, billed_weight_unit_of_measure, cccd_number, cpc_code, carrier_name_clinical_trial_identification_number__sds_id, charge_category_code, charge_category_detail_code, charge_classification_code, charge_description, charge_description_code, charge_source, charged_unit_quantity, class_number, contact_name, container_type,
         corrected_zone, currency_variance_amount, customer_reference_number, customs_number, customs_office_name, cycle_date, cycle_number, declaration_number, declared_freight_class, detail_class, detail_keyed_billed_dimension, detail_keyed_billed_unit_of_measure, detail_keyed_dim, detail_keyed_unit_of_measure, direct_shipment_date, document_number, document_type,
         duty_amount, duty_rate, duty_value, eft_date, eori_number, epu, entered_currency_code, entered_value, entered_weight, entered_weight_unit_of_measure, entry_date, entry_number, entry_port, entry_type, exchange_rate, excise_tax_amount, excise_tax_rate, export_place, foreign_trade_reference_number, freight_sequence_number, gst_amount, gst_rate,
         goods_description, import_tax_id, incentive_amount, invoice_amount, invoice_currency_code, invoice_date, invoice_due_date, invoice_exchange_rate, invoice_level_charge, invoice_number, invoice_remit_amount, invoice_type_code, invoice_type_detail_code, item_quantity, item_quantity_unit_of_measure, job_number, lead_shipment_number, line_item_number,
         master_air_waybill_number, miscellaneous_address_1_address_line_1, miscellaneous_address_1_address_line_2, miscellaneous_address_1_city, miscellaneous_address_1_company_name, miscellaneous_address_1_country, miscellaneous_address_1_name, miscellaneous_address_1_postal, miscellaneous_address_1_state, miscellaneous_address_2_address_line_1,
         miscellaneous_address_2_address_line_2, miscellaneous_address_2_city, miscellaneous_address_2_company_name, miscellaneous_address_2_country, miscellaneous_address_2_name, miscellaneous_address_2_postal, miscellaneous_address_2_state, miscellaneous_address_qual_1, miscellaneous_address_qual_2, miscellaneous_currency_code, miscellaneous_incentive_amount,
         miscellaneous_line_1, miscellaneous_line_10, miscellaneous_line_11, miscellaneous_line_2, miscellaneous_line_3, miscellaneous_line_4, miscellaneous_line_5, miscellaneous_line_7, miscellaneous_line_8, miscellaneous_line_9, miscellaneous_net_amount, nmfc, net_amount, office_number, order_in_council, origin_country, original_service_description,
         original_shipment_package_quantity, original_tracking_number, other_amount, other_basis_amount, other_customs_number, other_customs_number_indicator, other_rate, oversize_quantity, po__number_1, po__number_10, po__number_2, po__number_3, po__number_4, po__number_5, po__number_6, po__number_7, po__number_8, po__number_9, package_dimension_unit_of_measure,
         package_dimensions, package_quantity, package_reference_number_1, package_reference_number_2, package_reference_number_3, package_reference_number_4, package_reference_number_5, payer_role_code, pickup_record_number, place_holder_46, place_holder_47, place_holder_48, place_holder_52, place_holder_53, place_holder_54, place_holder_55, place_holder_56,
         place_holder_57, place_holder_58, place_holder_59, promo_discount_alias, promo_discount_applied_indicator, raw_dimension_unit_of_measure, raw_dimensions, receiver_address_line_1, receiver_address_line_2, receiver_city, receiver_company_name, receiver_country, receiver_name, receiver_postal, receiver_state, recipient_number, scc_scale_weight,
         sds_delivery_date, sds_error_code, sds_match_level_cd, sds_rnr_date, sima_access, scale_weight_unit_of_measure, scale_weight_quantity, sender_address_line_1, sender_address_line_2, sender_city, sender_company_name, sender_country, sender_name, sender_postal, sender_state, shipment_date, shipment_delivery_date, shipment_description, shipment_export_date,
         shipment_import_date, shipment_reference_number_1, shipment_reference_number_2, shipment_release_date, shipment_value_amount, sold_to_address_line_1, sold_to_address_line_2, sold_to_city, sold_to_company_name, sold_to_country, sold_to_name, sold_to_postal, sold_to_state, store_number, tariff_code, tariff_rate, tariff_treatment_number, tax_indicator,
         tax_type, tax_value, tax_variance_amount, tax_law_article_basis_amount, tax_law_article_number, third_party_address_line_1, third_party_address_line_2, third_party_city, third_party_company_name, third_party_country, third_party_name, third_party_postal, third_party_state, total_customs_amount, total_value_for_duty, tracking_number,
         transaction_currency_code, transaction_date, transport_mode, type_code_1, type_code_2, type_detail_code_1, type_detail_code_2, type_detail_value_1, type_detail_value_2, unit_of_measure, vat_amount, vat_basis_amount, vat_rate, validation_date, version, weight, world_ease_number, zone, data_lake_id)
         values
         """;

      String values = """
         (0, '2022-06-13 13:07:52.0', '2022-06-13 13:07:52.0', null, 'US', '00000F2098', null, null, 0, null, null, null, null, null, null, null, null, 0.00, null, 0, null, null, null, null,
            null, 'MIS', 'SVCH', 'FRT', 'Service Charge', null, null, 0, null, null, null, null, 0, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, 0, 0, null, null, null, null, 0,
            0, null, null, null, null, null, 0, 0, 0, null, null, 0, 0, 0, null, null, 0.00, 36, 'USD', '2022-01-01', '2022-01-10', 0, 0, '0000000F2098012', null, 'E', null, 0, null, null, null,
            0, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, null, null, null, null, null, null, null, null, null, null, 0, null, 36.00, null, null, null, null, 0, null, 0,
            0, null, null, 0, 0, null, null, null, null, null, null, null, null, null, null, null, null, 0, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, '00000F2098', 0, null, null, null, null, 0, null, 0, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, null, null, null, null, null, null, null, null, null, null,
            0, null, null, null, 0, 0, 0, null, null, null, null, null, null, null, null, null, 0, 0, null, 'USD', '2022-01-01', null, null, null, null, null, null, null, null, 0, 0, 0, null, 2, null, null, null, 'XXXXXX')
         """;
      sql += String.join(",", Collections.nCopies(1000, values));

      for(int i = 0; i < 10; i++)
      {
         System.out.println("== Cycle " + i);
         QueryManager.executeUpdate(connection, "BEGIN WORK");
         System.out.println("Begin work...");
         Integer insertCount = QueryManager.executeUpdateForRowCount(connection, sql);
         System.out.println("Inserted: " + insertCount);
         Integer deleteCount = QueryManager.executeUpdateForRowCount(connection, "DELETE from raw_parcel_invoice_line_ups WHERE data_lake_id='XXXXXX'");
         System.out.println("Deleted: " + deleteCount);

         boolean commit = true;
         if(commit)
         {
            QueryManager.executeUpdate(connection, "COMMIT WORK");
            System.out.println("Commit.");
         }
         else
         {
            QueryManager.executeUpdate(connection, "ROLLBACK WORK");
            System.out.println("Rollback.");
         }
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private RDBMSBackendMetaData getAuroraBacked()
   {
      QMetaDataVariableInterpreter interpreter  = new QMetaDataVariableInterpreter();
      String                       vendor       = interpreter.interpret("${env.RDBMS_VENDOR}");
      String                       hostname     = interpreter.interpret("${env.RDBMS_HOSTNAME}");
      Integer                      port         = Integer.valueOf(interpreter.interpret("${env.RDBMS_PORT}"));
      String                       databaseName = interpreter.interpret("${env.RDBMS_DATABASE_NAME}");
      String                       username     = interpreter.interpret("${env.RDBMS_USERNAME}");
      String                       password     = interpreter.interpret("${env.RDBMS_PASSWORD}");

      return new RDBMSBackendMetaData()
         .withName("aurora-test")
         .withVendor(vendor)
         .withHostName(hostname)
         .withPort(port)
         .withDatabaseName(databaseName)
         .withUsername(username)
         .withPassword(password);
   }
}
