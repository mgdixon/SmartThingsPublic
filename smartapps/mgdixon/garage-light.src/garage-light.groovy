/**
 *  Garage Light
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
    name: "Garage Light",
    namespace: "mgdixon",
    author: "Martin G. Dixon",
    description: "Activate the garage light based on sensor",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	log.debug "In preferences"
    section("Turn on when motion detected:") {
        input "themotion", "capability.motionSensor", required: true, title: "Where?"
    }
    section("Turn off when there's been no movement for") {
        input "minutes", "number", required: true, title: "Minutes?"
    }
    section("Turn on this light") {
        input "theswitch", "capability.switch", required: true
    }
    section("Send Push Notification?") {
        input "sendPush", "bool", required: false,
              title: "Send Push Notification?"
    }
}

def installed() {
	log.debug "In installed"
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
	log.debug "Initializing"
    subscribe(themotion, "motion.active", motionDetectedHandler)
    subscribe(themotion, "motion.inactive", motionStoppedHandler)
}

def motionDetectedHandler(evt) {
	if(sendPush) {
		sendPush("${themotion.displayName} motion! Turning light on")
    }
    log.debug "motionDetectedHandler called: $evt"
    theswitch.on()
}

def motionStoppedHandler(evt) {
    log.debug "motionStoppedHandler called: $evt"
    if(sendPush) {
    	sendPush("Scheduling for ${minutes} away")
    }
    runIn(60 * minutes, checkMotion)
}

def checkMotion() {
    log.debug "In checkMotion scheduled method"
    def motionState = themotion.currentState("motion")

    if (motionState.value == "inactive") {
    	if(sendPush) {
        	sendPush("in checkMotion - inactive!")
        }
        // get the time elapsed between now and when the motion reported inactive
        def elapsed = now() - motionState.date.time

        // elapsed time is in milliseconds, so the threshold must be converted to milliseconds too
        def threshold = 1000 * 60 * minutes

        if (elapsed >= threshold) {
            log.debug "Motion has stayed inactive long enough since last check ($elapsed ms):  turning switch off"
            theswitch.off()
            if(sendPush) {
            	sendPush("Turning off")
            }
        } else {
            log.debug "Motion has not stayed inactive long enough since last check ($elapsed ms):  doing nothing"
            if(sendPush) {
        		sendPush("Turning off")
            }
        }
    } else {
        // Motion active; just log it and do nothing
        log.debug "Motion is active, do nothing and wait for inactive"
		if(sendPush) {
        	sendPush("Still active")
        }
    }
}