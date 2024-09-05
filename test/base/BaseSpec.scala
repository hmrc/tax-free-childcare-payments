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

package base

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import models.request.IdentifierRequest
import org.scalactic.Prettifier
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{Assertion, EitherValues, LoneElement, OptionValues}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import play.api.libs.json._

abstract class BaseSpec
    extends AnyWordSpec
    with should.Matchers
    with OptionValues
    with EitherValues
    with ScalaCheckPropertyChecks
    with LoneElement {

  implicit val prettifier: Prettifier = {
    case IdentifierRequest(_, _, underlying) => s"IdentifierRequest( ${underlying.body} )"
    case other                               => Prettifier.default(other)
  }

  protected def checkErrorJson(
      actualJson: => JsValue,
      expectedErrorCode: String,
      expectedErrorDescription: String
    ): Assertion = {
    actualJson \ "errorCode"        shouldBe JsDefined(JsString(expectedErrorCode))
    actualJson \ "errorDescription" shouldBe JsDefined(JsString(expectedErrorDescription))
  }

  protected def checkJsonError[A: Reads](expectedJsonPath: String, expectedMessage: String)(json: JsValue): Assertion =
    checkJsonError { (jsPath, jsError) =>
      jsPath.path.loneElement shouldBe KeyPathNode(expectedJsonPath)
      jsError.message         shouldBe expectedMessage
    }(json)

  protected def checkJsonError[A: Reads](check: (JsPath, JsonValidationError) => Assertion)(json: JsValue): Assertion = {
    val (jsPath, jsErrors) = json.validate[A].asEither.left.value.loneElement

    check(jsPath, jsErrors.loneElement)
  }

  protected def checkLoneLog(expectedLevel: Level, expectedMessage: String)(logs: List[ILoggingEvent]): Unit = {
    val log = logs.loneElement

    log.getLevel   shouldBe expectedLevel
    log.getMessage shouldBe expectedMessage
  }

  protected val EXPECTED_E0024_DESC = "Please check that the epp_reg_reference and epp_unique_customer_id are both correct"
  protected val EXPECTED_E0025_DESC = "Please check that the child_date_of_birth and outbound_child_payment_reference are both correct"
  protected val EXPECTED_E0026_DESC = "Please check the outbound_child_payment_ref supplied"

  protected val EXPECTED_E0027_DESC: String =
    "The Childcare Provider (CCP) you have specified is not linked to the TFC Account. The parent must go into their " +
      "TFC Portal and add the CCP to their account first before attempting payment again later."

  protected val EXPECTED_E0030_DESC: String =
    "The External Payment Provider (EPP) record is inactive on the TFC system. The EPP must complete the sign up " +
      "process on the TFC Portal or contact their HMRC POC for further information."

  protected val EXPECTED_E0031_DESC: String =
    "The CCP is inactive, please check the CCP details and ensure that the CCP is still registered with their " +
      "childcare regulator and that they have also signed up to TFC via the TFC portal to receive TFC funds."

  protected val EXPECTED_E0032_DESC = "The epp_unique_customer_id or epp_reg_reference is not associated with the outbound_child_payment_ref"
  protected val EXPECTED_E0033_DESC = "The TFC account used to request payment contains insufficient funds."
  protected val EXPECTED_E0035_DESC = "There is an issue with this TFC Account, please advise parent / carer to contact TFC customer Services"
  protected val EXPECTED_E0036_DESC = "Error processing payment due to Payee bank details"

  protected val EXPECTED_E0042_DESC =
    "The ccp_reg_reference could not be found in the TFC system or does not correlate with the ccp_postcode. Please check the details and try again."
  protected val EXPECTED_E0043_DESC = "Parent associated with the bearer token does not have a TFC account. The parent must create a TFC account."

  protected val EXPECTED_500_DESC = "We encountered an error on our servers and did not process your request, please try again later."
  protected val EXPECTED_502_DESC = "Bad Gateway"
  protected val EXPECTED_503_DESC = "The service is currently unavailable."
}
