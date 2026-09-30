// Runs after the build above, which is expected to have failed.

def log = new File(basedir, 'build.log').text

assert log.contains('At least one <transformation> must be configured') : log
assert log.contains('QUARKUS') : 'the error should list the valid flavours'
assert log.contains('SPRING') : 'the error should list the valid flavours'

// Nothing may have been written: refusing early is the point.
def generated = new File(basedir, 'target/generated-sources')

assert !generated.exists() || generated.listFiles().length == 0 :
        "no sources should be written when the configuration is rejected, found ${generated.listFiles()}"

return true
