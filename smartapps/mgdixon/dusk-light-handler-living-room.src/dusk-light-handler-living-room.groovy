/**
 *  Dusk Light Handler - Living Room
 *
 *  Copyright 2017 Martin G. Dixon
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Dusk Light Handler - Living Room",
    namespace: "mgdixon",
    author: "Martin Dixon",
    description: "Turns lights on at dusk and then off an hour afterwards.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Lights") {
        input "switches", "capability.switch", title: "Which lights to turn on?", multiple: true
        input "offset", "number", title: "Turn on this many minutes before sunset"
        input "minutes", "number", required: true, title: "Turn off this many minutes after sunset"
	}
    section("Send Push Notification?") {
        input "sendPush", "bool", required: false,
              title: "Send Push Notification"
    }

}
    
def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
    subscribe(location, "sunsetTime", sunsetTimeHandler)

    //schedule it to run today too
    scheduleTurnOn(location.currentValue("sunsetTime"))
}

def sunsetTimeHandler(evt) {
    //when I find out the sunset time, schedule the lights to turn on with an offset
     if (sendPush) {
        sendPush("Scheduling turn-on at ${evt.value}!")
    }
    scheduleTurnOn(evt.value)
    
}

def scheduleTurnOn(sunsetString) {
    //get the Date value for the string
    def sunsetTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sunsetString)


    //calculate the offset
    def timeBeforeSunset = new Date(sunsetTime.time - (offset * 60 * 1000))

    log.debug "Scheduling for: $timeBeforeSunset (sunset is $sunsetTime)"

     if (sendPush) {
        sendPush("Scheduling on for: $timeBeforeSunset (sunset is $sunsetTime)!")
    }


    //schedule this to run one time - often we get the new sunset time before we turn on / 
    // off so it ends up delaying a day.
    runOnce(timeBeforeSunset, turnOn, [overwrite: false])

    //calculate the timeoff
    def timeAfterSunset = new Date(sunsetTime.time + (minutes * 60 * 1000))

    log.debug "Scheduling off for: $timeAfterSunset (sunset is $sunsetTime)"

	if (sendPush) {
        sendPush("Scheduling off for: $timeAfterSunset (sunset is $sunsetTime)!")
    }

    //schedule this to run one time
    runOnce(timeAfterSunset, turnOff, [overwrite: false])
}

def turnOn() {
    log.debug "turning on lights"
	if (sendPush) {
        sendPush("Lights going on!")
    }
    switches.on()
}

def turnOff() {
	log.debug "turning off lights"
	if (sendPush) {
        sendPush("Lights going off!")
    }
    switches.off()
}