/**
 *  Thingshield Garage Door
 *
 *  Author: Silviu Marghescu
 */

metadata {
	definition (name: "Thingshield Garage Door", author: "silviu") {
		capability "Switch"
    	command "push"
	}

	tiles {
		standardTile("door", "device.door", width: 1, height: 1, canChangeIcon: true, canChangeBackground: true) {
			state "closed", label: 'Closed', action: "push", icon: "st.doors.garage.garage-closed", backgroundColor: "#79b821", nextState: "opening"
			state "open", label: 'Open', action: "push", icon: "st.doors.garage.garage-open", backgroundColor: "#ffa81e", nextState: "closing"
			state "opening", label: "Opening", icon: "st.doors.garage.garage-opening", backgroundColor: "89C2E8"
			state "closing", label: "Closing", icon: "st.doors.garage.garage-closing", backgroundColor: "89C2E8"
		 }

		main("door")
		details("door")
	}

	simulator {
		status "on":  "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A6F6E"
		status "off": "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A6F6666"

		// reply messages
		reply "raw 0x0 { 00 00 0a 0a 6f 6e }": "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A6F6E"
		reply "raw 0x0 { 00 00 0a 0a 6f 66 66 }": "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A6F6666"
	}
}

def parse(String description) {
	def name = null
	def value = zigbee.parse(description)?.text
	log.debug "Parsed: ${value}"
	def linkText = getLinkText(device)
	def descriptionText = getDescriptionText(description, linkText, value)
	def handlerName = value
	def isStateChange = value != "ping"
	def displayed = value && isStateChange

	def incoming_cmd = value.split()

	name = incoming_cmd[0]
	if (incoming_cmd.size() > 1) {
		value = incoming_cmd[1]
	}
	
	def result = [
		value: value,
		name: value != "ping" ? name : null,
		handlerName: handlerName,
		linkText: linkText,
		descriptionText: descriptionText,
		isStateChange: isStateChange,
		displayed: displayed
	]
	log.debug result
	
	return result
}

def push() {
	log.debug "Pushing..."
	zigbee.smartShield(text: "push").format()
}
