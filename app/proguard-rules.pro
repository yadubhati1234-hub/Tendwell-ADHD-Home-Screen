# Tendwell has no reflection-based code of its own. Firebase and Play Billing ship
# their own keep rules inside their AARs, so nothing is needed here for them.
# The Crashlytics Gradle plugin uploads the R8 mapping file with each release build,
# so crash reports from Play builds show readable stack traces.
