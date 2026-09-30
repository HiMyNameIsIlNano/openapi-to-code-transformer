// Runs after "mvn clean generate-sources" on the project above.

// One QUARKUS transformation, so the output lands in a "quarkus" subdirectory of the default folder.
def generated = new File(basedir, 'target/generated-sources/openapi/quarkus')

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
assert petsApi.contains('getPet(') : petsApi

// The transformation ran: JAX-RS annotations replaced the markers.
assert petsApi.contains('@RegisterRestClient') : petsApi
assert petsApi.contains('import jakarta.ws.rs.') : petsApi

// The markers must not survive: they have SOURCE retention, so keeping them would put the plugin on
// the consuming project's compile path for nothing.
assert !petsApi.contains('@ApiInterface') : petsApi
assert !petsApi.contains('@ApiAnnotation') : petsApi
assert !petsApi.contains('hi.mynameis.ilnano') : petsApi

// The goal ran inside generate-sources without the phase being declared in the pom.
def log = new File(basedir, 'build.log').text

assert log.contains('openapi-generator') : 'plugin did not run at all'
assert log.contains('generate-sources') : 'plugin was not bound to generate-sources'
assert log.contains('Applied transformation[flavour=QUARKUS]') : 'unexpected plugin summary'

return true
