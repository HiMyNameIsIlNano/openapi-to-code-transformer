// Runs after "mvn clean compile". The plugin must add its output directory as a
// compile source root, otherwise nothing lands in target/classes.

def generated = new File(basedir, 'target/generated-sources/openapi/quarkus')

assert generated.isDirectory() : "expected generated sources at ${generated}"

def classes = new File(basedir, 'target/classes')

['com/acme/generated/api/PetsApi.class',
 'com/acme/generated/api/OwnersApi.class',
 'com/acme/generated/model/Pet.class',
 'com/acme/generated/model/Owner.class'].each { name ->
    assert new File(classes, name).isFile() : "generated source was not compiled: ${name}"
}

// Compiling proves the transformed output is valid against the real jakarta.ws.rs and MicroProfile
// APIs, which are this project's only dependencies — the plugin itself is not on the compile path.
return true
