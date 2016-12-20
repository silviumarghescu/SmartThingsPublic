/**
 *  Device type for an XBee garage door opener
 */
metadata {
  definition (name: "XBee Garage Door Opener", namespace: "silviu", author: "Silviu Marghescu") {
    capability "Actuator"
    capability "Refresh"
    capability "Polling"
    capability "Sensor"
    capability "Contact Sensor"
    capability "Door Control"
    capability "Configuration"
    
    command "actuate"
    
    fingerprint endpointId: "1", profileId: "0104", deviceId: "000B", deviceVersion: "00", 
      inClusters: "0000,0006"    
  }

  // simulator metadata
  simulator {
    // status messages
    status "on": "on/off: 1"
    status "off": "on/off: 0"

    // reply messages
    reply "zcl on-off on": "on/off: 1"
    reply "zcl on-off off": "on/off: 0"
  }

  // UI tile definitions
  tiles {
    standardTile("door", "device.door", width: 2, height: 2, canChangeIcon: true, canChangeBackground: true) {
	  //state("unknown", label:'${name}', action:"refresh.refresh", icon:"st.doors.garage.garage-open", backgroundColor:"#ffa81e")
      state("closed", label:'${name}', icon:"st.doors.garage.garage-closed", action: "door control.open", backgroundColor:"#79b821", nextState:"opening")
      state("open", label:'${name}', icon:"st.doors.garage.garage-open", action: "door control.close", backgroundColor:"#ffa81e", nextState:"closing")
      state("opening", label:'${name}', action:"refresh.refresh", icon:"st.doors.garage.garage-opening", backgroundColor:"#ffe71e")
      state("closing", label:'${name}', action:"refresh.refresh", icon:"st.doors.garage.garage-closing", backgroundColor:"#ffe71e")
    }
    standardTile("contact", "device.contact") {
      state("open", label:'${name}', icon:"st.contact.contact.open", backgroundColor:"#ffa81e")
      state("closed", label:'${name}', icon:"st.contact.contact.closed", backgroundColor:"#79b821")
    }
    
    standardTile("toggle", "device.door", inactiveLabel: false, decoration: "flat") {
      state "default", label:'open/close', action:"actuate", icon:"st.doors.garage.garage-closed"
    }
    
    standardTile("refresh", "device.door", inactiveLabel: false, decoration: "flat") {
      state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
    }

    main "status"
    details(["status", "toggle", "contact", "refresh"])
  }
}

// Parse incoming device messages to generate events
def parse(String description) {
  log.debug "Parsing description $description"
  def results = []
  
  if (description?.startsWith("catchall:")) {
    def msg = zigbee.parse(description)
    log.trace msg
    /*
    log.trace "profileId: ${hex(msg?.profileId, 4)}"
    log.trace "clusterId: ${hex(msg?.clusterId, 4)}"
    log.trace "endpointId: ${hex(msg?.sourceEndpoint)}"
    log.trace "command: ${hex(msg?.command)}"
    log.trace "data: ${msg?.data}"
    */
    // TODO: any way to not hardcode the endpoint id or the profile?
    if (msg?.profileId == 0x0104 && msg?.clusterId == 0x0006 && msg?.sourceEndpoint == 1) {
      if (msg?.command == 0x0B) {
        log.debug "Open/Close command response received"
        if (msg?.data[0] == 0) {
          log.debug "Close command"
          results << createEvent(name: "door", value: "closing", isStateChange: true)
        } else if (msg?.data[0] == 1){
          log.debug "Open command"
          results << createEvent(name: "door", value: "opening", isStateChange: true)
        }
      } else if (msg?.command == 0x01) {
        log.debug "Attr query response received"
        if (msg?.data[4] == 0) {
          log.debug "Closed status"
          results << createEvent(name: "door", value: "closed", isStateChange: true)
          results << createEvent(name: "contact", value: "closed", isStateChange: true)
        } else if (msg?.data[4] == 1){
          log.debug "Open status"
          results << createEvent(name: "door", value: "open", isStateChange: true)
          results << createEvent(name: "contact", value: "open", isStateChange: true)
        }
      } else if (msg?.command == 0x07) {
        log.debug "Reporting configure response received"
        if (msg?.data[0] == 0x00) {
          log.debug "Success!"
        }
      }
    }
  } else if (description?.startsWith("on/off: ")) {
    log.debug "Status update received"
    def value = description?.endsWith(" 1") ? "open" : "closed"
    results << createEvent(name: "door", value: value, isStateChange: true)
    results << createEvent(name: "contact", value: value, isStateChange: true)
  }

  log.debug "Parse returned ${results?.descriptionText}"
  return results
}

// Commands to device

def open() {
  log.debug "Open door..."
  "zcl on-off on"
}

def close() {
  log.debug "Close door..."
  "zcl on-off off"
}

def poll(){
  log.debug "Polling door..."
  refresh()
}

def refresh() {
  log.debug "Refreshing door status..."
  "st rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0x0000"
}

def actuate() {
  log.debug "Actuating..."
  if (device.currentValue("contact") == "open") {
    close()
  } else {
    open()
  }
}

def configure() {
  //TODO: binding not supported by XBee
  log.debug "Configuring Reporting."
  //String zigbeeId = swapEndianHex(device.hub.zigbeeId)
  def configCmds = [
    "zcl global send-me-a-report 0x0006 0x0000 0x10 0 3600 {0100}", "delay 500",
    "send 0x${device.deviceNetworkId} 1 1"
    //"zdo bind 0x${device.deviceNetworkId} 1 1 0x0006 {${zigbeeId}} {}", "delay 500",
    //"zdo bind 0x${device.deviceNetworkId} 1 1 0x0006 {${device.zigbeeId}} {}"
  ]
  log.debug "Configuring with ${configCmds}"
  return configCmds
}

private hex(value, width=2) {
  def s = new BigInteger(Math.round(value).toString()).toString(16)
  while (s.size() < width) {
    s = "0" + s
  }
  s
}

private String swapEndianHex(String hex) {
    reverseArray(hex.decodeHex()).encodeHex()
}

private byte[] reverseArray(byte[] array) {
    int i = 0;
    int j = array.length - 1;
    byte tmp;
    while (j > i) {
        tmp = array[j];
        array[j] = array[i];
        array[i] = tmp;
        j--;
        i++;
    }
    return array
}