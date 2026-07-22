import sbt._

object AppDependencies {

  private val bootstrapVersion  = "10.7.0"
  private val enumeratumVersion = "1.9.8"

  private val scalacheckVersion = "3.2.18.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"  %% "bootstrap-backend-play-30" % bootstrapVersion,
    "com.beachape" %% "enumeratum"                % enumeratumVersion
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30" % bootstrapVersion,
    "org.scalatestplus" %% "scalacheck-1-17"        % scalacheckVersion
  ).map(_ % "test")

  val it: Seq[ModuleID] = Seq.empty
}
