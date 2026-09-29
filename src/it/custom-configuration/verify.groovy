// Runs after "mvn clean process-sources" on the project above, where the user
// rebound the goal to process-sources and chose a custom output folder.

def generated = new File(basedir, 'target/my-api')

assert generated.isDirectory() : "expected the configured output directory at ${generated}"

def defaultLocation = new File(basedir, 'target/generated-sources/pet-store')

assert !defaultLocation.exists() : "the default location must not be used when outputFolder is set"

def sources = []
generated.eachFileRecurse { if (it.name.endsWith('.java')) sources << it }

def relative = sources.collect { generated.toPath().relativize(it.toPath()).toString().replace('\\', '/') }

assert relative.contains('com/acme/custom/api/StoresApi.java') : relative
assert relative.contains('com/acme/custom/model/Store.java') : relative

def storesApi = new File(generated, 'com/acme/custom/api/StoresApi.java').text

assert storesApi.contains('package com.acme.custom.api;') : storesApi
assert storesApi.contains('public interface StoresApi') : storesApi
assert storesApi.contains('listStores(') : storesApi
assert storesApi.contains('void deleteStore(') : storesApi

def store = new File(generated, 'com/acme/custom/model/Store.java').text

assert store.contains('public record Store(') : store
assert store.contains('UUID id') : store

// The custom spec was used, so nothing from the default spec name may appear.
assert !relative.any { it.contains('PetsApi') } : relative

return true
