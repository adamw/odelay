import sbt.settingKey
import sbtrelease.ReleaseStateTransformations._

lazy val is2_11_or_2_12 = settingKey[Boolean]("Is the scala version 2.11 or 2.12.")

val only2_11_and_2_12_settings = Seq(
  publishArtifact := is2_11_or_2_12.value,
  skip in compile := !is2_11_or_2_12.value,
  skip in publish := !is2_11_or_2_12.value,
  libraryDependencies := (if (is2_11_or_2_12.value) libraryDependencies.value else Nil)
)

val commonSettings = Seq(
  organization := "com.softwaremill.odelay",
  crossScalaVersions := Seq("2.11.12", "2.13.1", "2.12.10"),
  scalaVersion := crossScalaVersions.value.last,
  scalacOptions ++= Seq(Opts.compile.deprecation) ++
    Seq("-Ywarn-unused-import", "-Ywarn-unused", "-Xlint", "-feature").filter(
      Function.const(scalaVersion.value.startsWith("2.11"))),
  licenses := Seq(
    ("MIT", url(s"https://github.com/softprops/odelay/blob/${version.value}/LICENSE"))),
  // publishing
  publishTo := Some(
    if (isSnapshot.value)
      Opts.resolver.sonatypeSnapshots
    else
      Opts.resolver.sonatypeStaging
  ),
  publishArtifact in Test := false,
  publishMavenStyle := true,
  scmInfo := Some(
    ScmInfo(url("https://github.com/softwaremill/odelay"),
      "scm:git:git@github.com/softwaremill/odelay.git")),
  developers := List(
    Developer("adamw", "Adam Warski", "", url("https://softwaremill.com")),
    Developer("softprops", "Doug Tangren", "", url("https://github.com/softprops"))),
  homepage := Some(url("http://softwaremill.com/open-source")),
  sonatypeProfileName := "com.softwaremill",
  // sbt-release
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseProcess := Seq(
    checkSnapshotDependencies,
    inquireVersions,
    // publishing locally so that the pgp password prompt is displayed early
    // in the process
    releaseStepCommand("publishLocalSigned"),
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    setNextVersion,
    commitNextVersion,
    releaseStepCommand("sonatypeReleaseAll"),
    pushChanges
  ),
  is2_11_or_2_12 := scalaVersion.value.startsWith("2.11.") || scalaVersion.value.startsWith("2.12.")
)

val unpublished = Seq(publish := {}, publishLocal := {})

commonSettings

lazy val `odelay-core` = (crossProject in file("odelay-core")).
  settings(commonSettings:_*)

lazy val `odelay-core-js` = `odelay-core`.js
lazy val `odelay-core-jvm` = `odelay-core`.jvm

lazy val odelaytesting = (crossProject in file("odelay-testing"))
  .settings(commonSettings:_*)
  .settings(unpublished:_*)

lazy val `odelay-testing-js` =
  odelaytesting.js
    .dependsOn(`odelay-core-js`)
    .settings(commonSettings:_*)
    .settings(libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.8" % "test")

lazy val `odelay-testing` =
  odelaytesting.jvm
    .dependsOn(`odelay-core-jvm`)
    .settings(commonSettings:_*)
    .settings(libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % "test")

lazy val `odelay-core-tests` =
  project.dependsOn(`odelay-testing` % "test->test")
    .settings(commonSettings:_*)
    .settings(unpublished:_*)

lazy val `odelay-netty3` =
  project.dependsOn(`odelay-core-jvm`, `odelay-testing` % "test->test")
    .settings(commonSettings:_*)

lazy val `odelay-netty` =
  project.dependsOn(`odelay-core-jvm`, `odelay-testing` % "test->test")
    .settings(commonSettings:_*)

lazy val `odelay-twitter` =
  project.dependsOn(`odelay-core-jvm`, `odelay-testing` % "test->test")
    .settings(commonSettings:_*)
    .settings(only2_11_and_2_12_settings)

