package services

import models._

import com.phidgets._
import com.phidgets.AnalogPhidget
import com.phidgets.event._

trait AnalogSupport {
	def getAnalogInputs() = {
		(for(i <- (0 until 8))
			yield AnalogIO(i, analog.getVoltage(i))
		).toList
	}

	def setAnalogOutput(io: AnalogIO) = {
		if(!analogAttached) initAnalog()
		analog.setVoltage(io.position, io.value)
	}

	println(Phidget.getLibraryVersion())

	val analog = new AnalogPhidget
	var analogAttached = false

	analog.addErrorListener(new ErrorListener() {
		def error(ee: ErrorEvent) = {
			println("error event for " + ee)
		}
	})

	def initAnalog(): Unit = {
		analog.openAny()
		println("waiting for LCD attachment...")
		analog.waitForAttachment()

		println("Phidget Information")
		println("====================================");
		println("Version: " + analog.getDeviceVersion())
		println("Name: " + analog.getDeviceName())
		println("Serial #: " + analog.getSerialNumber())
		println("# Analog Outputs: " + analog.getOutputCount())

		println("Voltage Min: " + analog.getVoltageMin(0))
		println("Voltage Max: " + analog.getVoltageMax(3) + "\n")

		(0 until analog.getOutputCount).foreach(i => {
			analog.setEnabled(i, true)
		})

		// analog.getVoltage

    analogAttached = true
	}

	def disconnectAnalog = {
		analogAttached = false
		analog.close()
	}
}