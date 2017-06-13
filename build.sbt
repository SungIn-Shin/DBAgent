name := "DBAgent"

version := "1.0"

scalaVersion := "2.11.8"

// fullClasspath in Runtime += baseDirectory.value / "cfg"
scalacOptions ++= Seq("-unchecked", "-deprecation"/*, "-Xlint", "-feature"*/)

javacOptions ++= Seq("-encoding", "UTF-8", "-deprecation")


mainClass in assembly := Some("com.hopper.dbagent.App")

assemblyMergeStrategy in assembly := {
 case PathList("META-INF", xs @ _*) => MergeStrategy.discard
 case x => MergeStrategy.first
}


libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.12"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.3"

libraryDependencies += "io.netty" % "netty-all" % "4.1.6.Final"

// libraryDependencies += "org.mybatis.scala" % "z" % "1.0.3"

libraryDependencies += "org.mybatis" % "mybatis" % "3.3.0"

//libraryDependencies += "com.typesafe" % "config" % "1.2.1"  // 1.3.1 for java8

// https://mvnrepository.com/artifact/com.typesafe.play/play-json_2.11
libraryDependencies += "com.typesafe.play" % "play-json_2.11" % "2.3.10"

// https://mvnrepository.com/artifact/commons-codec/commons-codec
libraryDependencies += "commons-codec" % "commons-codec" % "1.5"

// // https://mvnrepository.com/artifact/org.hsqldb/hsqldb
libraryDependencies += "org.hsqldb" % "hsqldb" % "2.3.4"

// https://mvnrepository.com/artifact/mysql/mysql-connector-java
libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.6"

// // https://mvnrepository.com/artifact/org.apache.poi/poi
libraryDependencies += "org.apache.poi" % "poi" % "3.14"


// https://mvnrepository.com/artifact/org.apache.poi/poi-ooxml-schemas
 libraryDependencies += "org.apache.poi" % "poi-ooxml" % "3.15"

// // // https://mvnrepository.com/artifact/com.monitorjbl/xlsx-streamer
 libraryDependencies += "com.monitorjbl" % "xlsx-streamer" % "1.1.0"


// assemblyMergeStrategy in assembly := {
//   case PathList("org", "apache", "poi", xs @ _*)   => MergeStrategy.first
//   case x =>
//     val oldStrategy = (assemblyMergeStrategy in assembly).value
//     oldStrategy(x)
// }

