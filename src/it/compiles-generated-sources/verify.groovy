// Runs after "mvn clean compile". The plugin must add its output directory as a
// compile source root, otherwise nothing lands in target/classes.

def generated = new File(basedir, 'target/generated-sources/openapi')

assert generated.isDirectory() : "expected generated sources at ${generated}"

def classes = new File(basedir, 'target/classes')

['com/acme/generated/api/PetsApi.class',
 'com/acme/generated/api/OwnersApi.class',
 'com/acme/generated/model/Pet.class',
 'com/acme/generated/model/Owner.class'].each { name ->
    assert new File(classes, name).isFile() : "generated source was not compiled: ${name}"
}

return true
