package services

import data._
import apis._
import config._

import akka.actor._
import akka.util.Duration
import akka.util.duration._

import java.util.concurrent.TimeUnit

object HydronicSupport {
  val system = ActorSystem("HydronicScheduler")
	var saveCancellable: Option[Cancellable] = None
	var lcdCancellable: Option[Cancellable] = None
	var aggregationCancellable: Option[Cancellable] = None

	def startUpdate = {
		Configurator.hasConfig("saveData") match {
			case true => {
				saveCancellable = Some(system.scheduler.schedule(30 seconds, Duration.create(30, TimeUnit.SECONDS), new Runnable {
					def run() = {
						val inputs = PhidgetApiService.getAnalogInputs()
						inputs.foreach(analog => {
							if(analog.value > 0.1) AnalogDao.save(analog)
						})
					}
				}))
			}
			case _ => println("saving data disabled")
		}

		Configurator.hasConfig("updateLcd") match {
			case true => {
				lcdCancellable = Some(system.scheduler.schedule(5 second, Duration.create(20, TimeUnit.SECONDS), new Runnable {
					def run() = {
						val inputs = PhidgetApiService.getAnalogInputs()
						inputs.foreach(analog => {
							val msg = "Channel %d is %.1f F" format(analog.position, analog.value)
							PhidgetApiService.setLcd(msg, 0)
							Thread.sleep(2000)
						})
					}
				}))
			}
			case _ => println("updating LCD disabled")
		}
		Configurator.hasConfig("aggregate") match {
			case true => {
				aggregationCancellable = Some(system.scheduler.schedule(5 second, Duration.create(5, TimeUnit.MINUTES), new Runnable {
					def run() = {
						AnalogDao.computeAverages
					}
				}))
			}
			case _ => println("aggregation disabled 	")
		}
	}
}

trait HydronicSupport {
	def getZones(limit:Option[Int] = Some(5), resolution: String) = {
    resolution match {
    	case "all" => AnalogDao.findAll(resolutionToMs(resolution), limit.getOrElse(5))
    	case _ =>	AnalogDao.findAggregates(resolutionToMs(resolution), limit.getOrElse(5))
    }
	}

	def getZone(channel: Int, limit:Option[Int] = Some(5), resolution: String) = {
		AnalogDao.findByChannel(channel, resolutionToMs(resolution), limit.getOrElse(5))
	}

	def resolutionToMs(str: String): Long = {
		str match {
			case "hour" => 1000 * 60 * 60
			case "day" => 24 * 60* 1000 * 60
			case "week" => 7 * 24 * 60* 1000 * 60
			case "all" => 30 * 1000
			case _ => 15 * 1000 * 60
		}
	}
}

