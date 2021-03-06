name := "feather-crest"

version := "0.2"

organization := "eu.calavoow"

organizationHomepage := Some(new URL("http://calavoow.eu"))

description := "A library for accessing the EVE CREST API"

homepage := Some(new URL("https://github.com/calavoow/feather-crest"))

startYear := Some(2015)

licenses := Seq("LGPLv3" -> new URL("http://www.gnu.org/licenses/lgpl.txt"))

scalaVersion := "2.11.7"

scalacOptions ++= Seq("-feature", "-unchecked")

libraryDependencies ++= Seq(
	"com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
	"io.spray" %% "spray-json" % "1.3.2",
	"io.spray" %% "spray-caching" % "1.3.3",
	"net.databinder.dispatch" %% "dispatch-core" % "0.11.3",
	// Explicitly depending on the following modules, because of conflicting dependencies
	"org.slf4j" % "slf4j-api" % "1.7.12",
	"org.scala-lang.modules" %% "scala-xml" % "1.0.3",
	"org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.3",
	"org.scala-lang" % "scala-reflect" % "2.11.7",

	// Testing
	"org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
	"ch.qos.logback" % "logback-classic" % "1.1.2" % "test",
	"org.scala-lang.modules" %% "scala-async" % "0.9.2" % "test"
)
