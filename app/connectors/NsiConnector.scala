/*
 * Copyright 2023 HM Revenue & Customs
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

package connectors

import java.net.URL
import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import models.requests.Payee.{ChildCareProvider, ExternalPaymentProvider}
import models.requests.{IdentifierRequest, LinkRequest, Payee, PaymentRequest, SharedRequestData}
import models.response.NsiErrorResponse.Maybe
import models.response.{AccountStatus, BalanceResponse, LinkResponse, PaymentResponse}
import sttp.model.HeaderNames
import utils.FormattedLogging

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class NsiConnector @Inject() (
    httpClient: HttpClientV2,
    servicesConfig: ServicesConfig
  )(implicit ec: ExecutionContext
  ) extends BackendHeaderCarrierProvider with FormattedLogging with HeaderNames {
  import NsiConnector._
  import uk.gov.hmrc.http.HttpReads.Implicits._

  private implicit def httpReadsEither[A: Reads, B: Reads]: HttpReads[Either[B, A]] =
    HttpReads[HttpResponse].map { response =>
      response.json.validate[A].asOpt toRight response.json.as[B]
    }

  def linkAccounts(implicit req: IdentifierRequest[LinkRequest]): Future[Maybe[LinkResponse]] = {
    val queryString = Map(
      "eppURN"     -> req.body.sharedRequestData.epp_reg_reference,
      "eppAccount" -> req.body.sharedRequestData.epp_unique_customer_id,
      "parentNino" -> req.nino,
      "childDoB"   -> req.body.child_date_of_birth
    ).map { case (k, v) => s"$k=$v" }.mkString("?", "&", "")

    val url = new URL(resource("linkAccounts", req.body.sharedRequestData.outbound_child_payment_ref) + queryString)

    httpClient
      .get(url)
      .setHeader(CORRELATION_ID -> req.correlation_id.toString)
      .setHeader(Authorization -> s"Basic $NSI_HEADER_TOKEN")
      .withProxy
      .execute[Maybe[LinkResponse]]
  }

  def checkBalance(implicit req: IdentifierRequest[SharedRequestData]): Future[Maybe[BalanceResponse]] = {
    val queryString = Map(
      "eppURN"     -> req.body.epp_reg_reference,
      "eppAccount" -> req.body.epp_unique_customer_id,
      "parentNino" -> req.nino
    ).map { case (k, v) => s"$k=$v" }.mkString("?", "&", "")

    val url = new URL(resource("checkBalance", req.body.outbound_child_payment_ref) + queryString)

    httpClient
      .get(url)
      .setHeader(CORRELATION_ID -> req.correlation_id.toString)
      .setHeader(Authorization -> s"Basic $NSI_HEADER_TOKEN")
      .withProxy
      .execute[Maybe[BalanceResponse]]
      .map { res =>
        logger.info(formattedLog(s">>>>> response: $res"))
        res
      }
  }

  def makePayment(implicit req: IdentifierRequest[PaymentRequest]): Future[Maybe[PaymentResponse]] =
    httpClient
      .post(new URL(resource("makePayment")))
      .setHeader(CORRELATION_ID -> req.correlation_id.toString)
      .setHeader(Authorization -> s"Basic $NSI_HEADER_TOKEN")
      .withBody(enrichedWithNino[PaymentRequest])
      .withProxy
      .execute[Maybe[PaymentResponse]]

  private def resource(endpoint: String, params: String*) = {
    val domain       = servicesConfig.baseUrl(serviceName)
    val rootPath     = servicesConfig.getString(s"microservice.services.$serviceName.rootPath")
    val resourcePath = servicesConfig.getString(s"microservice.services.$serviceName.$endpoint")
    val pathParams   = params.map("/" + _).mkString

    s"$domain$rootPath$resourcePath$pathParams"
  }

  private val CORRELATION_ID   = servicesConfig.getString(s"microservice.services.$serviceName.correlationIdHeader")
  private val NSI_HEADER_TOKEN = servicesConfig.getString(s"microservice.services.$serviceName.token")
}

object NsiConnector {

  private val serviceName = "nsi"

  private def enrichedWithNino[R: OWrites](implicit req: IdentifierRequest[R]) =
    Json.toJsObject(req.body) + ("parentNino" -> JsString(req.nino))

  private implicit val writesPaymentReq: OWrites[PaymentRequest] = pr =>
    Json.toJsObject(pr.sharedRequestData) ++
      Json.toJsObject(pr.payee) ++
      Json.obj(
        "amount"                 -> pr.payment_amount,
        "childAccountPaymentRef" -> pr.sharedRequestData.outbound_child_payment_ref
      )

  private implicit val writesPayee: OWrites[Payee] = {
    case ExternalPaymentProvider          => Json.obj("payeeType" -> "EPP")
    case ChildCareProvider(urn, postcode) => Json.obj(
        "payeeType"   -> "CCP",
        "ccpURN"      -> urn,
        "ccpPostcode" -> postcode
      )
  }

  private implicit val writesSharedReqData: OWrites[SharedRequestData] = srd =>
    Json.obj(
      "eppAccount" -> srd.epp_unique_customer_id,
      "eppURN"     -> srd.epp_reg_reference
    )

  private implicit val readsLinkResponse: Reads[LinkResponse] =
    (__ \ "childFullName").read[String] map LinkResponse.apply

  private implicit val readsBalanceResponse: Reads[BalanceResponse] = (
    (__ \ "accountStatus").read[AccountStatus.Value] ~
      (__ \ "topUpAvailable").read[Int] ~
      (__ \ "topUpRemaining").read[Int] ~
      (__ \ "paidIn").read[Int] ~
      (__ \ "totalBalance").read[Int] ~
      (__ \ "clearedFunds").read[Int]
  )(BalanceResponse.apply _)

  private implicit val readsPaymentResponse: Reads[PaymentResponse] = (
    (__ \ "paymentReference").read[String] ~
      (__ \ "paymentDate").read[LocalDate]
  )(PaymentResponse.apply _)
}
