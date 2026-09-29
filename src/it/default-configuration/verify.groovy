// Runs after "mvn clean generate-sources" on the project above.

def generated = new File(basedir, 'target/generated-sources/openapi')

assert generated.isDirectory() : "expected the default output directory, got nothing at ${generated}"

def sources = []
generated.eachFileRecurse { if (it.name.endsWith('.java')) sources << it }

assert !sources.isEmpty() : "no sources written into ${generated}"

def relative = sources.collect { generated.toPath().relativize(it.toPath()).toString().replace('\\', '/') }

assert relative.contains('com/acme/generated/api/PetsApi.java') : relative
assert relative.contains('com/acme/generated/api/OwnersApi.java') : relative
assert relative.contains('com/acme/generated/model/Pet.java') : relative
assert relative.contains('com/acme/generated/model/Owner.java') : relative

def petsApi = new File(generated, 'com/acme/generated/api/PetsApi.java').text

assert petsApi.contains('package com.acme.generated.api;') : petsApi
assert petsApi.contains('public interface PetsApi') : petsApi
assert petsApi.contains('@ApiInterface') : petsApi
assert petsApi.contains('getPet(') : petsApi

// The goal ran inside generate-sources without the phase being declared in the pom.
def log = new File(basedir, 'build.log').text

assert log.contains('openapi-generator') : 'plugin did not run at all'
assert log.contains('generate-sources') : 'plugin was not bound to generate-sources'
assert log.contains('Generated 2 API interfaces and 2 models') : 'unexpected plugin summary'

return true
