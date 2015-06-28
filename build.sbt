name := "feather-crest"

version := "0.1"

organization := "eu.calavoow"

organizationHomepage := Some(new URL("http://calavoow.eu"))

description := "A library for accessing the EVE CREST API"

homepage := Some(new URL("https://github.com/calavoow/feather-crest"))

startYear := Some(2015)

licenses := Seq("AGPL" -> new URL("http://www.gnu.org/licenses/agpl-3.0.txt"))

scalaVersion := "2.11.6"

scalacOptions ++= Seq("-feature", "-unchecked")

libraryDependencies ++= Seq(
	"com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
	"io.spray" %% "spray-json" % "1.3.1",
	"io.spray" %% "spray-caching" % "1.3.1",
	"net.databinder.dispatch" %% "dispatch-core" % "0.11.3",
	"com.twitter" %% "util-core" % "6.24.0",
	// Explicitly depending on the following modules, because of conflicting dependencies
	"org.slf4j" % "slf4j-api" % "1.7.12",
	"org.scala-lang.modules" %% "scala-xml" % "1.0.3",
	"org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.3",
	"org.scala-lang" % "scala-reflect" % "2.11.6",

	// Testing
	"org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
	"ch.qos.logback" % "logback-classic" % "1.1.2" % "test",
	"org.scala-lang.modules" %% "scala-async" % "0.9.2" % "test"
)
