/**
 *  Lights on unlock
 *
 *  Copyright 2015 Silviu Marghescu
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
    name: "Lights on unlock",
    namespace: "silviu",
    author: "Silviu Marghescu",
    description: "Turn on/off door lights on unlock/lock, but only at night",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("When the door locks/unlocks..."){
		input "lock", "capability.lock", title: "Which lock?"
	}
	section("Turn off/on..."){
		input "light", "capability.switch", title: "Which light?"
	}
	section("After locking, keep lights on for...") {
		input "onTime", "number", title: "Minutes?"
	}
	section ("Zip code...") {
		input "zipCode", "text"
	}
}


def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(lock, "lock.locked", lockHandler);
    subscribe(lock, "lock.unlocked", unlockHandler);
    state.triggered = false
}

def lockHandler(evt) {
    if (state.triggered) {
    	log.debug "Turning light off in $onTime minutes..."
        runIn(onTime * 60, "lightOff")
    }
}

def lightOff() {
    	log.debug "Turning light off..."
	    light.off()
}

def unlockHandler(evt) {
    if (astroCheck()) {
    	log.debug "Nighttime..."
        if (light.currentState("switch").value == "off") {
        	log.debug "Turning light on..."
        	light.on()
            state.triggered = true
        } else {
        	log.debug "Light is already on."
            state.triggered = false
        }
    } else {
    	log.debug "Daylight, nothing to do..."
    }
}

def astroCheck() {
	def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset)

	def now = new Date()
	def riseTime = s.sunrise
	def setTime = s.sunset
    log.debug "now: $now"
	log.debug "riseTime: $riseTime"
	log.debug "setTime: $setTime"
    return now < riseTime || now > setTime
}
