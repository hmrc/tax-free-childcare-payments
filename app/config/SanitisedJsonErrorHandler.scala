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

import scala.concurrent.{ExecutionContext, Future}

import com.google.inject.{Inject, Singleton}
import utils.ErrorResponseFactory

import play.api.Configuration
import play.api.mvc.{RequestHeader, Result, Results}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.backend.http.JsonErrorHandler
import uk.gov.hmrc.play.bootstrap.config.HttpAuditEvent

/** [[JsonErrorHandler]] exposes a Pekko stack trace in the case of a JSON parse failure. This handler overrides that behaviour, sanitising the error message.
  */
@Singleton
class SanitisedJsonErrorHandler @Inject() (
    auditConnector: AuditConnector,
    httpAuditEvent: HttpAuditEvent,
    configuration: Configuration
  )(implicit ec: ExecutionContext
  ) extends JsonErrorHandler(auditConnector, httpAuditEvent, configuration) with Results {

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    message match {
      case invalidJSON() => Future.successful(BadRequest(ErrorResponseFactory.getJson("E0000", "Invalid JSON")))
      case _             => super.onClientError(request, statusCode, message)
    }
  }

  private val invalidJSON = "^(?s)Invalid Json:.*".r
}
