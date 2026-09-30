// Runs after "mvn clean process-sources" on the project above, where the user
// rebound the goal to process-sources, chose a custom output folder and asked for the
// SPRING flavour with an explicit client group.

// The flavour still gets its own subdirectory below the configured output folder.
def generated = new File(basedir, 'target/my-api/spring')

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

// The SPRING flavour ran, not the Quarkus one.
assert storesApi.contains('@HttpExchange') : storesApi
assert storesApi.contains('import org.springframework.web.service.annotation.') : storesApi
assert !storesApi.contains('jakarta.ws.rs') : storesApi
assert !storesApi.contains('@ApiInterface') : storesApi

// The configuration class that registers the clients, using the configured group.
def config = new File(generated, 'com/acme/custom/api/HttpServiceClientConfig.java')

assert config.isFile() : relative
assert config.text.contains('@ImportHttpServices(group = "pet-store"') : config.text
assert config.text.contains('basePackages = "com.acme.custom.api"') : config.text

def store = new File(generated, 'com/acme/custom/model/Store.java').text

assert store.contains('public record Store(') : store
assert store.contains('UUID id') : store

// The custom spec was used, so nothing from the default spec name may appear.
assert !relative.any { it.contains('PetsApi') } : relative

return true
