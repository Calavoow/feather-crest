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
		"net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
		// Explicitly depending on the following modules, because of conflicting dependencies
		"org.slf4j" % "slf4j-api" % "1.7.12",
		"org.scala-lang.modules" % "scala-xml_2.11" % "1.0.3",
		"org.scala-lang" % "scala-reflect" % "2.11.6"
)

