/**
 *  Device type for an XBee switch
 */
metadata {
  definition (name: "XBee Switch", namespace: "silviu", author: "Silviu Marghescu") {
	capability "Actuator"
    capability "Refresh"
    capability "Polling"
    capability "Sensor"
	capability "Configuration"
    capability "Switch"
      
    fingerprint endpointId: "1", profileId: "0104", deviceId: "0002", deviceVersion: "00", 
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
    standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true, canChangeBackground: true) {
      state "off", label: '${name}', action: "Switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
      state "on", label: '${name}', action: "Switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821"
    }
        
    standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat") {
      state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
    }
        
    main ("switch")
    details (["switch", "refresh"])
  }
}

// Parse incoming device messages to generate events
def parse(String description) {
  log.debug "Parsing description $description"
  def name = null
  def value = null
  
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
        log.debug "On/Off command response received"
        if (msg?.data[0] == 0) {
          log.debug "Off command"
          name = "switch"
          value = "off"
        } else if (msg?.data[0] == 1){
          log.debug "On command"
          name = "switch"
          value = "on"
        }
      } else if (msg?.command == 0x01) {
        log.debug "Attr query response received"
        if (msg?.data[4] == 0) {
          log.debug "Off status"
          name = "switch"
          value = "off"
        } else if (msg?.data[4] == 1){
          log.debug "On status"
          name = "switch"
          value = "on"
        }
      } else if (msg?.command == 0x07) {
        log.debug "Reporting configure response received"
        if (msg?.data[0] == 0x00) {
          log.debug "Success!"
        }
      }
    }
  } else {
    name = description?.startsWith("on/off: ") ? "switch" : null
    value = name == "switch" ? (description?.endsWith(" 1") ? "on" : "off") : null
  }

  // "isStateChange: true" is important here, otherwise state transitions are not triggered
  def result = createEvent(name: name, value: value, isStateChange: true)
  log.debug "Parse returned ${result?.descriptionText}"
  return result
}

// Commands to device

def on() {
  log.debug "Switch on..."
  //"st cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0x1 {}"
  'zcl on-off on'
}

def off() {
  log.debug "Switch off..."
  //"st cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0x0 {}"
  'zcl on-off off'
}

def poll(){
  log.debug "Polling switch..."
  refresh()
}

def refresh() {
  log.debug "Refreshing switch status..."
  "st rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0x0000"
}

def configure() {
  //TODO: binding not supported by XBee
  log.debug "Configuring Reporting."
  //String zigbeeId = swapEndianHex(device.hub.zigbeeId)
  def configCmds = [
    "zcl global send-me-a-report 0x0006 0x0000 0x10 0 3600 {0100}", "delay 500",
    "send 0x${device.deviceNetworkId} 1 1"
    //"zdo bind 0x${device.deviceNetworkId} 1 1 0x0B04 {${device.zigbeeId}} {}", "delay 500",
    //"zdo bind 0x${device.deviceNetworkId} 1 1 0x0006 {${zigbeeId}} {}", "delay 500",
    //"zdo bind 0x${device.deviceNetworkId} 1 1 0x0006 {${device.zigbeeId}} {}"
  ]
  log.debug "Configuring with ${configCmds}"
  return configCmds // execute commands
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

private hex(value, width=2) {
  def s = new BigInteger(Math.round(value).toString()).toString(16)
  while (s.size() < width) {
    s = "0" + s
  }
  s
}