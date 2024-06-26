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

package config

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import models.response.TfcErrorResponse
import util.FormattedLogging

import play.api.Configuration
import play.api.http.Status
import play.api.mvc.{RequestHeader, Result, Results}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.backend.http.JsonErrorHandler
import uk.gov.hmrc.play.bootstrap.config.HttpAuditEvent

@Singleton
class CustomJsonErrorHandler @Inject() (
    auditConnector: AuditConnector,
    httpAuditEvent: HttpAuditEvent,
    configuration: Configuration
  )(implicit ec: ExecutionContext
  ) extends JsonErrorHandler(auditConnector, httpAuditEvent, configuration) with Results with Status with FormattedLogging {

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] =
    if (message startsWith "Json validation error") Future.successful {
      logger.info(formattedLog(message)(request))

      TfcErrorResponse(BAD_REQUEST, "Request data is invalid or missing").toResult
    }
    else super.onClientError(request, statusCode, message)
}
