# Xert2Zwift
Simple converter from Xert TCX to Zwift ZWO - some training resolution will be lost such as MPA related durations

Not all the Xert training features can be supported in TCX and/or ZWO such as %MPA related durations.

Xert appear to approximate MPA durations as a set time period in the TXC export.

## How to use

Export a Xert workout from your account in TCX format.
Put the file in the test/resources/tcx folder
Run the TestParseTCX.groovy - all files will be processed into the build folder.

Use this link to learn how to make the created ZWO files available to your Zwift application:

[how to add custom workouts in Zwift](https://support.zwift.com/hc/en-us/articles/214574383-I-have-a-ZWO-file-What-do-I-do-with-it-)

