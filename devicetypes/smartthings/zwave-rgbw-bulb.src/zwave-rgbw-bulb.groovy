/**
 *  Copyright 2020 SmartThings
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
 *  Z-Wave RGBW Bulb, adapted from Aeon LED Bulb 6 Multi-Color
 *
 *  Author: SmartThings
 *  Date: 2020-01-27
 */

metadata {
	definition (name: "Z-Wave RGBW Bulb", namespace: "smartthings", author: "SmartThings", ocfDeviceType: "oic.d.light", mnmn: "SmartThings", vid: "generic-rgbw-color-bulb") {
		capability "Switch Level"
		capability "Color Control"
		capability "Color Temperature"
		capability "Switch"
		capability "Refresh"
		capability "Actuator"
		capability "Sensor"
		capability "Health Check"
		capability "Configuration"

		/*
		 * Relevant device types:
		 *
		 * * 0x11 GENERIC_TYPE_SWITCH_MULTILEVEL
		 * * 0x01 SPECIFIC_TYPE_POWER_SWITCH_MULTILEVEL
		 * * 0x02 SPECIFIC_TYPE_COLOR_TUNABLE_MULTILEVEL
		 *
		 * Plausible command classes we might see in a color light bulb:
		 *
		 * 0x98 COMMAND_CLASS_SECURITY
		 * 0x5E COMMAND_CLASS_ZWAVEPLUS_INFO_V2
		 * 0x20 COMMAND_CLASS_BASIC
		 * 0x26 COMMAND_CLASS_SWITCH_MULTILEVEL
		 * 0X27 COMMAND_CLASS_SWITCH_ALL
		 * 0x33 COMMAND_CLASS_SWITCH_COLOR
		 * 0x70 COMMAND_CLASS_CONFIGURATION
		 * 0x73 COMMAND_CLASS_POWERLEVEL
		 *
		 * Here are the command classes used by this driver that we can fingerprint against:
		 *
		 * * 0x26 COMMAND_CLASS_SWITCH_MULTILEVEL -> yes, it is dimmable
		 * * 0x33 COMMAND_CLASS_SWITCH_COLOR -> yes, it has color control
		 */

		// GENERIC_TYPE_SWITCH_MULTILEVEL:SPECIFIC_TYPE_POWER_SWITCH_MULTILEVEL
		// dimmable, color control
		fingerprint deviceId: "0x1101", inClusters: "0x26,0x33", deviceJoinName: "Z-Wave RGBW Bulb"

		// GENERIC_TYPE_SWITCH_MULTILEVEL:SPECIFIC_TYPE_COLOR_TUNABLE_MULTILEVEL
		// dimmable, color control
		fingerprint deviceId: "0x1102", inClusters: "0x26,0x33", deviceJoinName: "Z-Wave RGBW Bulb"

		// GENERIC_TYPE_SWITCH_MULTILEVEL:SPECIFIC_TYPE_POWER_SWITCH_MULTILEVEL
		// dimmable, color control; supports security command class.
		//
		// Note: explicitly delimiting application command classes as secure with 0xF1,0x00
		fingerprint deviceId: "0x1101", inClusters: "0x98,0xF1,0x00,0x26,0x33", deviceJoinName: "Z-Wave RGBW Bulb"

		// GENERIC_TYPE_SWITCH_MULTILEVEL:SPECIFIC_TYPE_COLOR_TUNABLE_MULTILEVEL
		// dimmable, color control; supports security command class.
		//
		// Note: explicitly delimiting application command classes as secure with 0xF1,0x00
		fingerprint deviceId: "0x1102", inClusters: "0x98,0xF1,0x00,0x26,0x33", deviceJoinName: "Z-Wave RGBW Bulb"

		// Illumin LZW42 RGBW Bulb.
		//
		// GENERIC_TYPE_SWITCH_MULTILEVEL:SPECIFIC_TYPE_POWER_SWITCH_MULTILEVEL
		fingerprint mfr: "031E", prod: "0005", model: "0001", deviceId: "0x1101", deviceJoinName: "Illumin RGBW Bulb"
	}

	simulator {
	}

	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 1, height: 1, canChangeIcon: true) {
			tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
				attributeState("on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00a0dc", nextState:"turningOff")
				attributeState("off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn")
				attributeState("turningOn", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00a0dc", nextState:"turningOff")
				attributeState("turningOff", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn")
			}

			tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"switch level.setLevel"
			}

			tileAttribute ("device.color", key: "COLOR_CONTROL") {
				attributeState "color", action:"color control.setColor"
			}
		}
	}

	controlTile("colorTempSliderControl", "device.colorTemperature", "slider", width: 4, height: 2, inactiveLabel: false, range:"(2700..6500)") {
		state "colorTemperature", action:"color temperature.setColorTemperature"
	}

	main(["switch"])
	details(["switch", "levelSliderControl", "rgbSelector", "colorTempSliderControl"])
}

private getCOLOR_TEMP_MIN() { 2700 }
private getCOLOR_TEMP_MAX() { 6500 }
private getWHITE_MIN() { 0 } // min for Z-Wave coldWhite and warmWhite paramaeters
private getWHITE_MAX() { 255 } // max for Z-Wave coldWhite and warmWhite paramaeters
private getCOLOR_TEMP_DIFF() { COLOR_TEMP_MAX - COLOR_TEMP_MIN }
private getRED() { "red" }
private getGREEN() { "green" }
private getBLUE() { "blue" }
private getWARM_WHITE() { "warmWhite" }
private getCOLD_WHITE() { "coldWhite" }
private getRGB_NAMES() { [RED, GREEN, BLUE] }
private getWHITE_NAMES() { [WARM_WHITE, COLD_WHITE] }
private getCOLOR_NAMES() { RGB_NAMES + WHITE_NAMES }

private def rgbToHSV(red, green, blue) {
	def hex = colorUtil.rgbToHex(red as int, green as int, blue as int)
	def hsv = colorUtil.hexToHsv(hex)
	return [hue: hsv[0], saturation: hsv[1], value: hsv[2]]
}

private def huesatToRGB(hue, sat) {
	def color = colorUtil.hsvToHex(Math.round(hue) as int, Math.round(sat) as int)
	return colorUtil.hexToRgb(color)
}

private zwaveWhiteToTemp(warmWhite, coldWhite) {
	warmWhite = warmWhite < WHITE_MIN ? WHITE_MIN : warmWhite > WHITE_MAX ? WHITE_MAX : warmWhite
	coldWhite = coldWhite < WHITE_MIN ? WHITE_MIN : coldWhite > WHITE_MAX ? WHITE_MAX : coldWhite
	// Compute temp as concensus between warm white and cold white.
	def warmTemp = (WHITE_MAX - warmWhite ) * COLOR_TEMP_DIFF / WHITE_MAX + COLOR_TEMP_MIN
	def coldTemp = coldWhite * COLOR_TEMP_DIFF / WHITE_MAX + COLOR_TEMP_MIN
	def temp = ((warmTemp + coldTemp) * 0.5) as Integer
}

private zwaveTempToWarmWhite(temp) {
	temp = temp < COLOR_TEMP_MIN ? COLOR_TEMP_MIN : temp > COLOR_TEMP_MAX ? COLOR_TEMP_MAX : temp
	def warmValue = ((COLOR_TEMP_MAX - temp) / COLOR_TEMP_DIFF * WHITE_MAX) as Integer
}

private zwaveTempToColdWhite(temp) {
	(WHITE_MAX - zwaveTempToWarmWhite(temp))
}

private def zwToStColor(red, green, blue, warmWhite, coldWhite) {
	def stColor = [:]
	def colors = [red, green, blue]
	def hexColor = "#" + colors.collect { Integer.toHexString(it).padLeft(2, "0") }.join("")
	stColor["color"] = hexColor
	def hsv = rgbToHSV(*colors)
	stColor["hue"] = hsv.hue
	stColor["saturation"] = hsv.saturation
	stColor["colorTemperature"] = zwaveWhiteToTemp(warmWhite, coldWhite);
	log.debug(stColor)
	stColor
}

def updated() {
	response(refresh())
}

def installed() {
	log.debug "installed()..."
	sendEvent(name: "checkInterval", value: 1860, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "0"])
	sendEvent(name: "level", value: 100, unit: "%", displayed: false)
	sendEvent(name: "colorTemperature", value: COLOR_TEMP_MIN, displayed: false)
	sendEvent(name: "color", value: "#000000", displayed: false)
	sendEvent(name: "hue", value: 0, dispalyed: false)
	sendEvent(name: "saturation", value: 0, displayed: false)
}

def parse(description) {
	def result = null
	if (description != "updated") {
		def cmd = zwave.parse(description)
		if (cmd) {
			result = zwaveEvent(cmd)
			log.debug("'$description' parsed to $result")
		} else {
			log.debug("Couldn't zwave.parse '$description'")
		}
	}
	result
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
	dimmerEvents(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
	dimmerEvents(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd) {
	unschedule(offlinePing)
	dimmerEvents(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.switchcolorv3.SwitchColorReport cmd) {
	log.debug "got SwitchColorReport: $cmd"
	def result = []
	if (state.staged != null) {
		state.staged.each{ k, v -> result << createEvent(name: k, value: v) }
	}
	result
}

private dimmerEvents(physicalgraph.zwave.Command cmd) {
	def value = (cmd.value ? "on" : "off")
	def result = [createEvent(name: "switch", value: value, descriptionText: "$device.displayName was turned $value")]
	if (cmd.value) {
		result << createEvent(name: "level", value: cmd.value == 99 ? 100 : cmd.value , unit: "%")
	}
	return result
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	def encapsulatedCommand = cmd.encapsulatedCommand()
	if (encapsulatedCommand) {
		zwaveEvent(encapsulatedCommand)
	} else {
		log.warn "Unable to extract encapsulated cmd from $cmd"
		createEvent(descriptionText: cmd.toString())
	}
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	def linkText = device.label ?: device.name
	[linkText: linkText, descriptionText: "$linkText: $cmd", displayed: false]
}

def buildOffOnEvent(cmd){
	[zwave.basicV1.basicSet(value: cmd), zwave.switchMultilevelV3.switchMultilevelGet()]
}

def on() {
	commands(buildOffOnEvent(0xFF), 5000)
}

def off() {
	commands(buildOffOnEvent(0x00), 5000)
}

def refresh() {
	commands([zwave.switchMultilevelV3.switchMultilevelGet()] + queryAllColors())
}

def ping() {
	log.debug "ping().."
	unschedule(offlinePing)
	runEvery30Minutes(offlinePing)
	command(zwave.switchMultilevelV3.switchMultilevelGet())
}

def offlinePing() {
	log.debug "offlinePing()..."
	sendHubCommand(new physicalgraph.device.HubAction(command(zwave.switchMultilevelV3.switchMultilevelGet())))
}

def setLevel(level) {
	setLevel(level, 1)
}

def setLevel(level, duration) {
	log.debug "setLevel($level, $duration)"
	if(level > 99) level = 99
	commands([
		zwave.switchMultilevelV3.switchMultilevelSet(value: level, dimmingDuration: duration),
		zwave.switchMultilevelV3.switchMultilevelGet(),
	], 5000)
}

def setSaturation(percent) {
	log.debug "setSaturation($percent)"
	setColor(saturation: percent)
}

def setHue(value) {
	log.debug "setHue($value)"
	setColor(hue: value)
}

def setColor(value) {
	log.debug "setColor($value)"
	def rgb
	if (value.hex) {
		rgb = value.hex.findAll(/[0-9a-fA-F]{2}/).collect { Integer.parseInt(it, 16) }
	} else {
		rgb = huesatToRGB(value.hue, value.saturation)
	}
	if (state.staged == null) {
		state.staged = [:]
	}
	state.staged << zwToStColor(rgb[0], rgb[1], rgb[2], 0, 0)
	def cmds = [zwave.switchColorV3.switchColorSet(red: rgb[0], green: rgb[1], blue: rgb[2], warmWhite: 0, coldWhite: 0),
		        zwave.switchColorV3.switchColorGet(colorComponent: COLOR_NAMES[0])]
	commands(cmds)
}

def setColorTemperature(temp) {
	log.debug "setColorTemperature($temp)"
	def warmValue = zwaveTempToWarmWhite(temp)
	def coldValue = zwaveTempToColdWhite(temp)
	if (state.staged == null) {
		state.staged = [:]
	}
	state.staged << zwToStColor(0, 0, 0, warmValue, coldValue)
	def cmds = [zwave.switchColorV3.switchColorSet(red: 0, green: 0, blue: 0, warmWhite: warmValue, coldWhite: coldValue),
		        zwave.switchColorV3.switchColorGet(colorComponent: COLOR_NAMES[0])]
	commands(cmds)
}

private queryAllColors() {
	COLOR_NAMES.collect { zwave.switchColorV3.switchColorGet(colorComponent: it) }
}

private secEncap(physicalgraph.zwave.Command cmd) {
	zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
}

private crcEncap(physicalgraph.zwave.Command cmd) {
	zwave.crc16EncapV1.crc16Encap().encapsulate(cmd).format()
}

private command(physicalgraph.zwave.Command cmd) {
	if (zwaveInfo.zw.contains("s")) {
		secEncap(cmd)
	} else if (zwaveInfo.cc.contains("56")){
		crcEncap(cmd)
	} else {
		cmd.format()
	}
}

private commands(commands, delay=200) {
	delayBetween(commands.collect{ command(it) }, delay)
}
