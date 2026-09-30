/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models.response.error

import helpers.BaseSpec
import helpers.generators.request.external.{
  ExternalBalanceRequestGenerators,
  ExternalLinkRequestGenerators,
  ExternalPaymentRequestGenerators
}
import models.request.external.{ExternalBalanceRequest, ExternalLinkRequest, ExternalPaymentRequest}
import models.response.error.ServiceErrorResponse.*
import org.scalacheck.Gen
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.TableFor3
import play.api.http.Status
import play.api.libs.json.{JsError, JsObject, JsPath, JsonValidationError}
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing

class ServiceErrorResponseSpec
    extends BaseSpec
    with EitherValues
    with LogCapturing
    with Status
    with ScalaFutures
    with ExternalLinkRequestGenerators
    with ExternalBalanceRequestGenerators
    with ExternalPaymentRequestGenerators {

  private val linkRequestJsonErrorScenarios: TableFor3[Gen[JsObject], ServiceErrorResponse, String] = Table(
    ("Invalid Payloads", "Expected Error Code", "Expected Error Description"),
    (
      genExternalLinkRequestJsObjectsWithoutOutboundChildPaymentRef,
      E0001,
      "outbound_child_payment_ref is in invalid format or missing"
    ),
    (
      genExternalLinkRequestJsObjectsWithInvalidOutboundChildPaymentRef,
      E0001,
      "outbound_child_payment_ref is in invalid format or missing"
    ),
    (genExternalLinkRequestJsObjectsWithoutEppRegReference, E0002, "epp_reg_reference is in invalid format or missing"),
    (
      genExternalLinkRequestJsObjectsWithInvalidEppRegReference,
      E0002,
      "epp_reg_reference is in invalid format or missing"
    ),
    (
      genExternalLinkRequestJsObjectsWithoutEppUniqueCustomerId,
      E0004,
      "epp_unique_customer_id is in invalid format or missing"
    ),
    (
      genExternalLinkRequestJsObjectsWithInvalidEppUniqueCustomerId,
      E0004,
      "epp_unique_customer_id is in invalid format or missing"
    ),
    (
      genExternalLinkRequestJsObjectsWithoutChildDateOfBirth,
      E0006,
      "child_date_of_birth is in invalid format or missing"
    ),
    (
      genExternalLinkRequestJsObjectsWithNonStringChildDateOfBirth,
      E0006,
      "child_date_of_birth is in invalid format or missing"
    ),
    (
      genExternalLinkRequestJsObjectsWithNonIso8601ChildDateOfBirth,
      E0006,
      "child_date_of_birth is in invalid format or missing"
    )
  )

  private val balanceRequestJsonErrorScenarios = Table(
    ("Invalid Payloads", "Expected Error Code", "Expected Error Description"),
    (
      genExternalBalanceRequestJsObjectsWithInvalidOutboundChildPaymentRef,
      E0001,
      "outbound_child_payment_ref is in invalid format or missing"
    ),
    (
      genExternalBalanceRequestJsObjectsWithoutOutboundChildPaymentRef,
      E0001,
      "outbound_child_payment_ref is in invalid format or missing"
    ),
    (
      genExternalBalanceRequestJsObjectsWithoutEppRegReference,
      E0002,
      "epp_reg_reference is in invalid format or missing"
    ),
    (
      genExternalBalanceRequestJsObjectsWithInvalidEppRegReference,
      E0002,
      "epp_reg_reference is in invalid format or missing"
    ),
    (
      genExternalBalanceRequestJsObjectsWithoutEppUniqueCustomerId,
      E0004,
      "epp_unique_customer_id is in invalid format or missing"
    ),
    (
      genExternalBalanceRequestJsObjectsWithInvalidEppUniqueCustomerId,
      E0004,
      "epp_unique_customer_id is in invalid format or missing"
    )
  )

  private val paymentRequestJsonErrorScenarios = Table(
    ("Invalid Payloads", "Expected Error Code", "Expected Error Description"),
    (
      genExternalPaymentRequestJsObjectsWithInvalidOutboundChildPaymentRef,
      E0001,
      "outbound_child_payment_ref is in invalid format or missing"
    ),
    (
      genExternalPaymentRequestJsObjectsWithoutOutboundChildPaymentRef,
      E0001,
      "outbound_child_payment_ref is in invalid format or missing"
    ),
    (
      genExternalPaymentRequestJsObjectsWithoutEppRegReference,
      E0002,
      "epp_reg_reference is in invalid format or missing"
    ),
    (
      genExternalPaymentRequestJsObjectsWithInvalidEppRegReference,
      E0002,
      "epp_reg_reference is in invalid format or missing"
    ),
    (
      genExternalPaymentRequestJsObjectsWithoutEppUniqueCustomerId,
      E0004,
      "epp_unique_customer_id is in invalid format or missing"
    ),
    (
      genExternalPaymentRequestJsObjectsWithInvalidEppUniqueCustomerId,
      E0004,
      "epp_unique_customer_id is in invalid format or missing"
    ),
    (genExternalPaymentRequestJsObjectsWithoutPayeeType, E0007, "payee_type is in invalid format or missing"),
    (genExternalPaymentRequestJsObjectsWithInvalidPayeeType, E0007, "payee_type is in invalid format or missing"),
    (
      genExternalPaymentRequestJsObjectsWithoutCcpRegReference,
      E0003,
      "ccp_reg_reference is in invalid format or missing"
    ),
    (
      genExternalPaymentRequestJsObjectsWithInvalidCcpRegReference,
      E0003,
      "ccp_reg_reference is in invalid format or missing"
    ),
    (genExternalPaymentRequestJsObjectsWithoutCcpPostcode, E0009, "ccp_postcode is in invalid format or missing"),
    (genExternalPaymentRequestJsObjectsWithInvalidCcpPostcode, E0009, "ccp_postcode is in invalid format or missing"),
    (genExternalPaymentRequestJsObjectsWithoutPaymentAmount, E0008, "payment_amount is in invalid format or missing"),
    (
      genExternalPaymentRequestJsObjectsWithFractionalPaymentAmount,
      E0008,
      "payment_amount is in invalid format or missing"
    ),
    (
      genExternalPaymentRequestJsObjectsWithStringPaymentAmount,
      E0008,
      "payment_amount is in invalid format or missing"
    ),
    (
      genExternalPaymentRequestJsObjectsWithNegativePaymentAmount,
      E0008,
      "payment_amount is in invalid format or missing"
    )
  )

  "fromValidationError" should {
    "return correct error code" when {
      "ExternalLinkRequest JSON is invalid" in
        forAll(linkRequestJsonErrorScenarios) { (invalidPayloads, expectedErrorCode, _) =>
          forAll(invalidPayloads) { payload =>
            val jsError = payload.validate[ExternalLinkRequest].asInstanceOf[JsError]

            val serviceErrorResponse = ServiceErrorResponse.fromValidationError(jsError)

            serviceErrorResponse shouldBe expectedErrorCode
          }
        }

      "ExternalBalanceRequest JSON is invalid" in
        forAll(balanceRequestJsonErrorScenarios) { (invalidPayloads, expectedErrorCode, _) =>
          forAll(invalidPayloads) { payload =>
            val jsError = payload.validate[ExternalBalanceRequest].asInstanceOf[JsError]

            val serviceErrorResponse = ServiceErrorResponse.fromValidationError(jsError)

            serviceErrorResponse shouldBe expectedErrorCode
          }
        }

      "ExternalPaymentRequest JSON is invalid" in
        forAll(paymentRequestJsonErrorScenarios) { (invalidPayloads, expectedErrorCode, _) =>
          forAll(invalidPayloads) { payload =>
            val jsError = payload.validate[ExternalPaymentRequest].asInstanceOf[JsError]

            val serviceErrorResponse = ServiceErrorResponse.fromValidationError(jsError)

            serviceErrorResponse shouldBe expectedErrorCode
          }
        }
    }

    "return ServiceErrorResponse.E0000" when {
      "provided with an unknown Json validation exception" in {
        val jsError = JsError(JsPath \ "some_unknown_path", JsonValidationError("error.unknown"))

        ServiceErrorResponse.fromValidationError(jsError) shouldBe ServiceErrorResponse.E0000
      }
    }
  }

}
