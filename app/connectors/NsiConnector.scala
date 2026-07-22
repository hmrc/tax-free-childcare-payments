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

import config.AppConfig
import models.request._
import models.response.NsiErrorResponse.{ETFC3, NsiResponse}
import models.response._
import play.api.http.Status
import play.api.libs.json._
import play.api.mvc.RequestHeader
import sttp.model.HeaderNames
import uk.gov.hmrc.http.HttpReads
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider
import utils.FormattedLogging

import java.net.{URI, URL, URLEncoder}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NsiConnector @Inject() (
    httpClient: HttpClientV2,
    appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends BackendHeaderCarrierProvider
    with FormattedLogging
    with HeaderNames {
  import NsiConnector._

  def linkAccounts(implicit req: IdentifierRequest[LinkRequest]): Future[NsiResponse[LinkResponse]] = httpClient
    .get(linkAccountsUrl)
    .setHeader(appConfig.nsiCorrelationIdHeader -> req.correlation_id.toString)
    .setHeader(Authorization -> s"Basic ${appConfig.nsiAuthorisationToken}")
    .withProxy
    .execute[NsiResponse[LinkResponse]]

  private def linkAccountsUrl(implicit req: IdentifierRequest[LinkRequest]): URL = {
    val queryString = Map(
      "eppURN"     -> req.body.sharedRequestData.epp_reg_reference,
      "eppAccount" -> req.body.sharedRequestData.epp_unique_customer_id,
      "parentNino" -> req.nino,
      "childDoB"   -> req.body.child_date_of_birth
    ).map { case (k, v) => s"$k=$v" }.mkString("?", "&", "")

    val childPaymentRef = encodeParam(req.body.sharedRequestData.outbound_child_payment_ref)

    val url = s"${appConfig.nsiLinkAccountsUrl}/$childPaymentRef$queryString"

    new URI(url).toURL
  }

  def checkBalance(implicit req: IdentifierRequest[SharedRequestData]): Future[NsiResponse[BalanceResponse]] =
    httpClient
      .get(checkBalanceUrl)
      .setHeader(appConfig.nsiCorrelationIdHeader -> req.correlation_id.toString)
      .setHeader(Authorization -> s"Basic ${appConfig.nsiAuthorisationToken}")
      .withProxy
      .execute[NsiResponse[BalanceResponse]]

  private def checkBalanceUrl(implicit req: IdentifierRequest[SharedRequestData]): URL = {
    val queryString = Map(
      "eppURN"     -> req.body.epp_reg_reference,
      "eppAccount" -> req.body.epp_unique_customer_id,
      "parentNino" -> req.nino
    ).map { case (k, v) => s"$k=$v" }.mkString("?", "&", "")

    val childPaymentRef = encodeParam(req.body.outbound_child_payment_ref)

    val url = s"${appConfig.nsiCheckBalanceUrl}/$childPaymentRef$queryString"

    new URI(url).toURL
  }

  def makePayment(implicit req: IdentifierRequest[PaymentRequest]): Future[NsiResponse[PaymentResponse]] =
    httpClient
      .post(new URI(appConfig.nsiMakePaymentUrl).toURL)
      .setHeader(appConfig.nsiCorrelationIdHeader -> req.correlation_id.toString)
      .setHeader(Authorization -> s"Basic ${appConfig.nsiAuthorisationToken}")
      .withBody(enrichedWithNino[PaymentRequest])
      .withProxy
      .execute[NsiResponse[PaymentResponse]]

}

object NsiConnector extends FormattedLogging with Status {

  private def enrichedWithNino[R: OWrites](implicit req: IdentifierRequest[R]): JsObject =
    Json.toJsObject(req.body) + ("parentNino" -> JsString(req.nino))

  private def encodeParam(outboundPaymentRef: String): String =
    URLEncoder.encode(outboundPaymentRef, "UTF-8").replaceAll("\\+", "%20")

  private implicit def httpReadsNsiResponse[A: Reads](implicit rh: RequestHeader): HttpReads[NsiResponse[A]] =
    (_, _, response) =>
      if (response.status / 100 == 2) {
        response.json.validate[A] match {
          case JsSuccess(result, _) =>
            logger.info(
              formattedInfoLog(
                s"NSI responded ${response.status}"
              )
            )
            Right(result)
          case JsError(jsonErrors) =>
            logger.warn(
              formattedErrorLog(
                s"NSI responded ${response.status}. Resulting in JSON validation errors - $jsonErrors - triggering ETFC3"
              )
            )
            Left(ETFC3)
        }
      } else {
        response.json.validate[NsiErrorResponse] match {
          case JsSuccess(nsiErrorResponse, _) =>
            val message = formattedErrorLog(
              s"NSI responded ${response.status} with body ${response.body} - triggering $nsiErrorResponse"
            )
            if (nsiErrorResponse.reportAs < INTERNAL_SERVER_ERROR) {
              logger.info(message)
            } else {
              logger.warn(message)
            }
            Left(nsiErrorResponse)
          case JsError(jsonErrors) =>
            logger.warn(
              formattedErrorLog(
                s"NSI responded ${response.status}. Resulting in JSON validation errors - $jsonErrors - triggering ETFC3"
              )
            )
            Left(ETFC3)
        }
      }

}
