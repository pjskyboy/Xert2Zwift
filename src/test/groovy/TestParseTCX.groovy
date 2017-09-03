import groovy.xml.MarkupBuilder
import groovy.xml.Namespace

import java.math.MathContext

final String REPEAT_T = "Repeat_t"

XmlParser parser = new XmlParser()

FileNameFinder fileNameFinder = new FileNameFinder()
fileNameFinder.getFileNames(new File("../resources").getAbsolutePath(), "**/*.tcx").each { filePath ->
    File xertFile = new File(filePath)
    println "Processing [" + xertFile.getAbsolutePath() + "]"
    assert xertFile.exists()
    def trainingCenterDatabase = parser.parse(xertFile)
    NodeList workoutTag = trainingCenterDatabase.Workouts.Workout
    String workoutName = workoutTag.Name.text()
    String xertFilename = workoutName + ".tcx"
    String zwoFilename = workoutName + ".zwo"

    def workoutSteps = workoutTag.Step
    def extensionsSteps = workoutTag.Extentions.Steps.Step

    final ArrayList workoutStepsList = new HashMap<String, NodeList>()
    final HashMap extensionStepsMap = new HashMap<String, NodeList>()

    final def xsi = new Namespace('http://www.w3.org/2001/XMLSchema-instance', 'xsi')
    workoutSteps.each { step -> processStep(step, xsi, workoutStepsList) }
    extensionsSteps.each { step -> processStep(step, xsi, extensionStepsMap) }

    int ftp = 314

    def writer = new StringWriter()
    def xml = new MarkupBuilder(writer)
    xml.workout_file() {
        author('Xert2Zwift')
        name(workoutName)
        description('Conversion from Xert TCX file [' + xertFilename + "]")
        sportType('bike')
        tags {
            tag(name: 'INTERVALS')
            tag(name: 'FTP')
            tag(name: "XERT")
        }
        workout {
            workoutStepsList.each { workoutStep ->
                if (workoutStep.attributes().getAt(xsi.type).equals(REPEAT_T)) {
                    int repeats = Integer.valueOf(workoutStep.Repetitions.text())
                    def onChild = extensionStepsMap.get(workoutStep.Child[0].StepId.text())
                    def offChild = extensionStepsMap.get(workoutStep.Child[1].StepId.text())
                    def onChildPower = midPower(onChild)
                    def onChildDuration = roundedDuration(onChild.Duration.Seconds.text())
                    def offChildPower = midPower(offChild)
                    def offChildDuration = roundedDuration(offChild.Duration.Seconds.text())

                    if (repeats > 1) {
                        // it really is an interval
                        IntervalsT(Repeat: workoutStep.Repetitions.text(),
                                OnDuration: onChildDuration,
                                OffDuration: offChildDuration,
                                OnPower: asPercentage(onChildPower, ftp),
                                OffPower: asPercentage(offChildPower, ftp),
                                pace: "0")
                    } else {
                        // it is probably xert using and "interval" to ramp up
                        SteadyState(
                                Duration: onChildDuration,
                                Power: asPercentage(onChildPower, ftp),
                                pace: "0")
                        SteadyState(
                                Duration: offChildDuration,
                                Power: asPercentage(offChildPower, ftp),
                                pace: "0")
                    }
                } else {
                    def extensionStep = extensionStepsMap.get(workoutStep.StepId.text())
                    SteadyState(Duration: roundedDuration(extensionStep.Duration.Seconds.text()),
                            Power: asPercentage(midPower(extensionStep), ftp),
                            pace: "0")
                }
            }
        }
    }

    String zwoXmlString = writer.toString()
    File zwoFile = new File("../../../build/" + zwoFilename)
    println "Writing ZWO file [" + zwoFile.getAbsolutePath() + "]"
    if (zwoFile.exists()) {
        zwoFile.delete()
    }
    zwoFile.createNewFile()
    FileWriter fileWriter = new FileWriter(zwoFile)
    fileWriter.write(zwoXmlString)
    fileWriter.flush()
    fileWriter.close()
}

static String roundedDuration(final String duration) {
    return (Float.valueOf(duration)).round(0).toString()
}

static String asPercentage(int power, int ftp) {
    return (power / ftp).round(new MathContext("precision=2 roundingMode=UP")).toString()
}

void processStep(final Node step, final xsi, final List list) {
    list.add(step)
}

void processStep(final Node step, final xsi, final Map map) {
    String stepId = step.StepId.text()
    map.put(stepId, step)
}

static int midPower(final Node child) {
    int high = Integer.valueOf(child.Target.PowerZone.High.Value.text())
    int low = Integer.valueOf(child.Target.PowerZone.Low.Value.text())
    return (low + (high.minus(low) / 2)).round(new MathContext("precision=3 roundingMode=UP"))
}