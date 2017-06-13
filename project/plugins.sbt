/*
resolvers ++= Seq(
  "Sonatype Releases" at "http://oss.sonatype.org/content/repositories/releases",
  "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
  Classpaths.typesafeResolver
)
*/
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")