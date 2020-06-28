load("//tools/bzl:maven_jar.bzl", "maven_jar")

def external_plugin_deps():
  maven_jar(
    name = 'redmineapi',
    artifact = 'com.taskadapter:redmine-java-api:3.1.2',
    sha1 = 'b1725e34bee09f5c3dc7ca3a3c3645cd313c4b45',
  )
