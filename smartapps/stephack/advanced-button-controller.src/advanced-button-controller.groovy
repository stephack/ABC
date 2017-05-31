/*
 *	Advanced Button Controller
 *
 *	Author: SmartThings, modified by Bruce Ravenel, Dale Coffing, Stephan Hackett
 *
 */
def version(){"v0.1.170531"}

definition(
    name: "Advanced Button Controller",
    namespace: "stephack",
    author: "Bruce Ravenel, Dale Coffing, Stephan Hackett",
    description: "Configure devices with buttons like the Aeon Labs Minimote and Lutron Pico Remotes.",
    category: "Convenience",
    iconUrl: "https://cdn.rawgit.com/stephack/ABC/master/resources/images/abcNew.png",
    iconX2Url: "https://cdn.rawgit.com/stephack/ABC/master/resources/images/abcNew.png",
    iconX3Url: "https://cdn.rawgit.com/stephack/ABC/master/resources/images/abcNew.png"
)

preferences {
	page(name: "startPage")
    page(name: "parentPage")      
	page(name: "mainPage", nextPage: confirmPage)
	page(name: "configButtonsPage")
    page(name: "confirmPage")
    page(name: "aboutPage")
	page(name: "timeIntervalInput", title: "Only during a certain time") {
		section {
			input "starting", "time", title: "Starting", required: false
			input "ending", "time", title: "Ending", required: false
		}
	}
}

def startPage() {
    if (parent) {
        mainPage()
    } else {
        parentPage()
    }
}

def parentPage() {
	return dynamicPage(name: "parentPage", title: "", install: true, uninstall: true) {
        section("Create a new button mapping.") {
            app(name: "childApps", appName: appName(), namespace: "stephack", title: "New Button Mapping", multiple: true)
        }
        section("Version Info, User's Guide") {
       	href (name: "aboutPage", title: "Advanced Button Controller \n"+version(), 
       		description: "Tap to get Smartapp Info and User's Guide.",
       		image: "https://cdn.rawgit.com/stephack/ABC/master/resources/images/abcNew.png", required: false,
       		page: "aboutPage"
		)		
   		}
    }
}

private def appName() { return "${parent ? "Button Map Config" : "Advanced Button Controller"}" }

def mainPage() {
	dynamicPage(name: "mainPage", uninstall: true) {
		section("Step 1: Select Your Button Device") {
			input "buttonDevice", "capability.button", title: "Button Controller", multiple: false, required: true, submitOnChange: true
		}
        if(buttonDevice){
            state.buttonCount = manualCount?: buttonDevice.currentValue('numberOfButtons')
            if(state.buttonCount==null) state.buttonCount = buttonDevice.currentValue('numButtons')	//added for kyse minimote(hopefully will be updated to correct attribute name)
       		section("Step 2: Configure Your Buttons"){
            	if(state.buttonCount<1) {
                	paragraph "The selected button device did not report the number of buttons it has. Please specify in the Advanced Config section below."
                }
                else {
                	for(i in 1..state.buttonCount){
                		href "configButtonsPage", title: "Button ${i}", description: getDescription(i), params: [pbutton: i]
                    }
            	}                
            }            
		}		        
        section("Set Custom Name (Optional)") {
        	label title: "Assign a name:", required: false
        }
        section("Advanced Config:", hideable: true, hidden: hideOptionsSection()) { 
            	input "manualCount", "number", title: "How many Buttons on remote?", required: false, description: "Only set if DTH does not specify", submitOnChange: true
                input "collapseAll", "bool", title: "Only show sections that have been configured?", defaultValue: "true"
                input "hwSpecifics", "enum", title: "Include H/W specific details?", options: ["None", "Lutron Pico", "HomeSeer"], defaultValue: "None", submitOnChange: true
			}
        section(title: "Only Execute When:", hideable: true, hidden: hideOptionsSection()) {
			def timeLabel = timeIntervalLabel()
			href "timeIntervalInput", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : null
			input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
					options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
			input "modes", "mode", title: "Only when mode is", multiple: true, required: false
		}        
	}
}

def confirmPage() {
	dynamicPage(name: "confirmPage", title: "Confirm all button settings are as desired below.", uninstall: true, install: true) {
    	for(i in 1..state.buttonCount){
        	section("BUTTON ${i} PUSHED:\n"+getConfirmPage("${i}_pushed")+"\n"+"BUTTON ${i} HELD:\n"+getConfirmPage("${i}_held")) {
        	}
        }    
    }
}

def configButtonsPage(params) {
	if (params.pbutton != null) state.currentButton = params.pbutton.toInteger()
	dynamicPage(name: "configButtonsPage", title: "Configure Button ${state.currentButton} below", getButtonSections(state.currentButton))
}

def aboutPage() {
	dynamicPage(name: "aboutPage", title: none){
     	section("User's Guide - Advanced Button Controller") {
        	paragraph textHelp()
 		}
	}
}

def getButtonSections(buttonNumber) {
	return {
    	getButtonSpecifics(buttonNumber)
		section("Switches (Toggle On/Off)", hideable: true, hidden: !shallHide("lights_${buttonNumber}")) {        
			input "lights_${buttonNumber}_pushed", "capability.switch", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
			input "lights_${buttonNumber}_held", "capability.switch", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
		}
        section("Switches (Turn On)", hideable: true, hidden: !shallHide("lightOn_${buttonNumber}")) {
			input "lightOn_${buttonNumber}_pushed", "capability.switch", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
			input "lightOn_${buttonNumber}_held", "capability.switch", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
		}
		section("Switches (Turn Off)", hideable: true, hidden: !shallHide("lightOff_${buttonNumber}")) {
			input "lightOff_${buttonNumber}_pushed", "capability.switch", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
			input "lightOff_${buttonNumber}_held", "capability.switch", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
		}
		section(" "){}
		section("Dimmers (On to Level - Group 1)", hideable: true, hidden: !(shallHide("lightDim_${buttonNumber}") || shallHide("valLight${buttonNumber}"))) {
			input "lightDim_${buttonNumber}_pushed", "capability.switchLevel", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
			input "valLight${buttonNumber}_pushed", "number", title: "Bright Level", multiple: false, required: false, description: "0 to 100%"
			input "lightDim_${buttonNumber}_held", "capability.switchLevel", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
			input "valLight${buttonNumber}_held", "number", title: "Bright Level", multiple: false, required: false, description: "0 to 100%"
		}
		section("Dimmers (On to Level - Group 2)", hideable: true, hidden: !(shallHide("lightD2m_${buttonNumber}") || shallHide("valLight2${buttonNumber}"))) {
			input "lightD2m_${buttonNumber}_pushed", "capability.switchLevel", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
			input "valLight2${buttonNumber}_pushed", "number", title: "Bright Level", multiple: false, required: false, description: "0 to 100%"
			input "lightD2m_${buttonNumber}_held", "capability.switchLevel", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
			input "valLight2${buttonNumber}_held", "number", title: "Bright Level", multiple: false, required: false, description: "0 to 100%"
		}
        section("Dimmers (Toggle OnToLevel/Off)", hideable: true, hidden: !(shallHide("lightsDT_${buttonNumber}") || shallHide("valDT${buttonNumber}"))) {
			input "lightsDT_${buttonNumber}_pushed", "capability.switchLevel", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
			input "valDT${buttonNumber}_pushed", "number", title: "Bright Level", required: false, description: "0 to 100%"
			input "lightsDT_${buttonNumber}_held", "capability.switchLevel", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
			input "valDT${buttonNumber}_held", "number", title: "Bright Level", required: false, description: "0 to 100%"
		}
        section("Dimmers (Increase Level By)", hideable: true, hidden: !(shallHide("dimPlus_${buttonNumber}") || shallHide("valDimP${buttonNumber}"))) {
			input "dimPlus_${buttonNumber}_pushed", "capability.switchLevel", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
            input "valDimP${buttonNumber}_pushed", "number", title: "When Pushed Increase by", multiple: false, required: false, description: "0 to 15"
			input "dimPlus_${buttonNumber}_held", "capability.switchLevel", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
            input "valDimP${buttonNumber}_held", "number", title: "When Held Increase by", multiple: false, required: false, description: "0 to 15"
		}        
          	section("Dimmers (Decrease Level By)", hideable: true, hidden: !(shallHide("dimMinus_${buttonNumber}") || shallHide("valDimM${buttonNumber}"))) {
			input "dimMinus_${buttonNumber}_pushed", "capability.switchLevel", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
            input "valDimM${buttonNumber}_pushed", "number", title: "When Pushed Decrease by", multiple: false, required: false, description: "0 to 15"
			input "dimMinus_${buttonNumber}_held", "capability.switchLevel", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
            input "valDimM${buttonNumber}_held", "number", title: "When Held Decrease by", multiple: false, required: false, description: "0 to 15"
		}
        section(" "){}
		section("Fans (Adjust - Low, Medium, High, Off)", hideable: true, hidden: !shallHide("fanAdjust_${buttonNumber}")) {
			input "fanAdjust_${buttonNumber}_pushed", "capability.switchLevel", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
			input "fanAdjust_${buttonNumber}_held", "capability.switchLevel", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
		}
		section("Shades (Adjust - Up, Down, or Stop)", hideable: true, hidden: !shallHide("shadeAdjust_${buttonNumber}")) {
			input "shadeAdjust_${buttonNumber}_pushed", "capability.doorControl", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
			input "shadeAdjust_${buttonNumber}_held", "capability.doorControl", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
		}
		section("Locks (Lock Only)", hideable: true, hidden: !shallHide("locks_${buttonNumber}")) {
			input "locks_${buttonNumber}_pushed", "capability.lock", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
			input "locks_${buttonNumber}_held", "capability.lock", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
		}
        section("Sirens (Toggle)", hideable: true, hidden: !shallHide("sirens_${buttonNumber}")) {
			input "sirens_${buttonNumber}_pushed","capability.alarm" ,title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
			input "sirens_${buttonNumber}_held", "capability.alarm", title: "When Held", multiple: true, required: false, submitOnChange: true
		}
        section(" "){}
		section("Speakers (Toggle Play/Pause)", hideable: true, hidden: !shallHide("speakerpp_${buttonNumber}")) {
			input "speakerpp_${buttonNumber}_pushed", "capability.musicPlayer", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
			input "speakerpp_${buttonNumber}_held", "capability.musicPlayer", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
		}        
        section("Speakers (Go to Next Track)", hideable: true, hidden: !shallHide("speakernt_${buttonNumber}")) {
			input "speakernt_${buttonNumber}_pushed", "capability.musicPlayer", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
			input "speakernt_${buttonNumber}_held", "capability.musicPlayer", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
		}        
        section("Speakers (Toggle Mute/Unmute)", hideable: true, hidden: !shallHide("speakermu_${buttonNumber}")) {
			input "speakermu_${buttonNumber}_pushed", "capability.musicPlayer", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
			input "speakermu_${buttonNumber}_held", "capability.musicPlayer", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
		}
        section("Speakers (Increase Vol By)", hideable: true, hidden: !(shallHide("speakervu_${buttonNumber}") || shallHide("valSpeakU${buttonNumber}"))) {
			input "speakervu_${buttonNumber}_pushed", "capability.musicPlayer", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
            input "valSpeakU${buttonNumber}_pushed", "number", title: "When Pushed Increase by", multiple: false, required: false, description: "0 to 15"
			input "speakervu_${buttonNumber}_held", "capability.musicPlayer", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
            input "valSpeakU${buttonNumber}_held", "number", title: "When Held Increase by", multiple: false, required: false, description: "0 to 15"
		}        
        section("Speakers (Decrease Vol By)", hideable: true, hidden: !(shallHide("speakervd_${buttonNumber}") || shallHide("valSpeakD${buttonNumber}"))) {
			input "speakervd_${buttonNumber}_pushed", "capability.musicPlayer", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
            input "valSpeakD${buttonNumber}_pushed", "number", title: "When Pushed Decrease by", multiple: false, required: false, description: "0 to 15"
			input "speakervd_${buttonNumber}_held", "capability.musicPlayer", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
            input "valSpeakD${buttonNumber}_held", "number", title: "When Held Decrease by", multiple: false, required: false, description: "0 to 15"
		}
        section(" "){}
		section("Set Mode", hideable: true, hidden: !shallHide("mode_${buttonNumber}")) {
			input "mode_${buttonNumber}_pushed", "mode", title: "When Pushed", required: false, submitOnChange: collapseAll
			input "mode_${buttonNumber}_held", "mode", title: "When Held", required: false, submitOnChange: collapseAll
		}
		def phrases = location.helloHome?.getPhrases()*.label
		if (phrases) {		
        	section("Run Routine", hideable: true, hidden: !shallHide("phrase_${buttonNumber}")) {
				//log.trace phrases
				input "phrase_${buttonNumber}_pushed", "enum", title: "When Pushed", required: false, options: phrases, submitOnChange: collapseAll
				input "phrase_${buttonNumber}_held", "enum", title: "When Held", required: false, options: phrases, submitOnChange: collapseAll
			}
		}
        section("Notifications:\nSMS, In App or Both", hideable: true, hidden: !shallHide("notifications_${buttonNumber}")) {
        paragraph "****************\nWHEN PUSHED\n****************"
			input "notifications_${buttonNumber}_pushed", "text", title: "Message", description: "Enter message to send", required: false, submitOnChange: collapseAll
            input "phone_${buttonNumber}_pushed","phone" ,title: "Send Text To", description: "Enter phone number", required: false, submitOnChange: collapseAll
            input "valNotify${buttonNumber}_pushed","bool" ,title: "Notify In App?", required: false, defaultValue: false, submitOnChange: collapseAll
            paragraph "*************\nWHEN HELD\n*************"
			input "notifications_${buttonNumber}_held", "text", title: "Message", description: "Enter message to send", required: false, submitOnChange: collapseAll
		//}
       // section("Type of Notification to Send", hideable: true, hidden: !shallHide("notifications_${buttonNumber}")) {	//!(shallHide("valNotify${buttonNumber}" || "phone_${buttonNumber}"))) {
			input "phone_${buttonNumber}_held", "phone", title: "Send Text To", description: "Enter phone number", required: false, submitOnChange: collapseAll
			input "valNotify${buttonNumber}_held", "bool", title: "Notify In App?", required: false, defaultValue: false, submitOnChange: collapseAll
			
			
		}


/*		section("Push Notification", hideable: true, hidden: !shallHide("notifications_${buttonNumber}")) {
			input "notifications_${buttonNumber}_pushed","bool" ,title: "When Pushed", required: false, defaultValue: false, submitOnChange: collapseAll
			input "notifications_${buttonNumber}_held", "bool", title: "When Held", required: false, defaultValue: false, submitOnChange: collapseAll
		}
		section("SMS Notification", hideable: true, hidden: !shallHide("phone_${buttonNumber}")) {
			input "phone_${buttonNumber}_pushed","phone" ,title: "When Pushed", description: "Enter phone number", required: false, submitOnChange: collapseAll
			input "phone_${buttonNumber}_held", "phone", title: "When Held", description: "Enter phone number", required: false, submitOnChange: collapseAll
		}
        section("Custom Message Notifications", hideable: true, hidden: !shallHide("valText${buttonNumber}")) {
			input "valText${buttonNumber}_pushed", "text", title: "When Pushed", description: "Enter message to send", required: false, submitOnChange: collapseAll
			input "valText${buttonNumber}_held", "text", title: "When Held", description: "Enter message to send", required: false, submitOnChange: collapseAll
		}
*/        
	}
}

def shallHide(myFeature) {
	if(collapseAll) return (settings["${myFeature}_pushed"]||settings["${myFeature}_held"]||settings["${myFeature}"])
	return true
}

def getDescription(dNumber) {	
    def descript = "Tap to Configure"
    def anySettings = settings.find{it.key.contains("_${dNumber}_")}
    if(anySettings) descript = "CONFIGURED : Tap to edit"
	return descript
}

def getConfirmPage(numType){
	def preferenceNames = settings.findAll{it.key.contains("_${numType}")}.sort()		//get all configured settings that: match button# and type, AND are not false
    if(!preferenceNames){
    	return "  *Not Configured*\n"
    }
    else {
    	def formattedPage ="   "
    	preferenceNames.each {eachPref->		
        	def prefDetail = getPreferenceDetails().find{eachPref.key.contains(it.id)}	//gets decription of action being performed(eg Turn On)
        	def prefValue = eachPref.value												//name of device the action is being performed on (eg Bedroom Fan)
            if(prefDetail.sub) {														//if a sub value is found (eg dimVal) prefix to prefValue
        		def PrefSubValue = settings[prefDetail.sub + numType]?:"!Missing!"		//value stored in val setting (eg 100)
                //log.error eachPref.value
        		if(PrefSubValue==true) prefValue = "(${prefValue})" else prefValue = "(${PrefSubValue}): ${prefValue}"
            }              	
        	formattedPage += prefDetail.desc+" "+prefValue+"\n   "
    	}
		return formattedPage
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
    if(parent) { 
    	initChild() 
    } else {
    	initParent() 
    }  
}

def initParent() {
	log.debug "Parent Initialized"
}

def initChild() {
	log.debug "def INITIALIZE with settings: ${settings}"
	app.label==app.name?app.updateLabel(defaultLabel()):app.updateLabel(app.label)
    
	subscribe(buttonDevice, "button", buttonEvent)
    state.lastshadesUp = true  
}

def defaultLabel() {
	return "${buttonDevice} Config"
}

def getPreferenceDetails(){
	def detailMappings =
    	[[id:'lights_',desc:'Toggle On/Off:',comm:toggle],
         [id:'lightsDT_', desc:'Toggle Off/Dim to', comm:dimToggle, sub:"valDT"],
     	 [id:'lightOn_',desc:'Turn On:',comm:turnOn],
     	 [id:"lightOff_",desc:'Turn Off:',comm:turnOff],
     	 [id:"lightDim_",desc:'Dim to',comm:turnDim, sub:"valLight"],
     	 [id:"lightD2m_",desc:'Dim to',comm:turnDim, sub:"valLight2"],
         [id:'dimPlus_',desc:'Brightness +',comm:levelUp, sub:"valDimP"],
     	 [id:'dimMinus_',desc:'Brightness -',comm:levelDown, sub:"valDimM"],
     	 [id:"fanAdjust_",desc:'Adjust:',comm:adjustFan],
     	 [id:"shadeAdjust_",desc:'Adjust:',comm:adjustShade],
     	 [id:"locks_",desc:'UnLock:',comm:setUnlock],
         [id:"speakerpp_",desc:'Toggle Play/Pause:',comm:speakerplaystate],
         [id:'speakernt_',desc:'Next Track:',comm:speakernexttrack],
    	 [id:'speakermu_',desc:'Mute:',comm:speakermute],
     	 [id:'speakervu_',desc:'Volume +',comm:levelUp, sub:"valSpeakU"],
     	 [id:"speakervd_",desc:'Volume -',comm:levelDown, sub:"valSpeakD"],
     	 [id:"mode_",desc:'Set Mode:',comm:changeMode],
         [id:"notifications_",desc:'InApp Msg',comm:messageHandle, sub: "valNotify"],
         //[id:"notifications_",desc:'InApp Notify',comm:messageHandle, sub: "valText"],
         [id:'sirens_',desc:'Toggle:',comm:toggle],
     	 [id:"phone_",desc:'SMS Msg',comm:smsHandle, sub:"notifications_"],
         //[id:"phone_",desc:'SMS Notify',comm:messageHandle, sub:"valText"],
     	 [id:"phrase_",desc:'Run Routine:',comm:runRout],
        ]         
    return detailMappings
}

def buttonEvent(evt) {
	if(allOk) {
    	def buttonNumber = evt.jsonData.buttonNumber
		def pressType = evt.value
		log.debug "$buttonDevice: Button $buttonNumber was $pressType"
    
    	def preferenceNames = settings.findAll{it.key.contains("_${buttonNumber}_${pressType}")}
    	preferenceNames.each{eachPref->
        	def prefDetail = getPreferenceDetails()?.find{eachPref.key.contains(it.id)}		//returns the detail map of id,desc,comm,sub
        	def PrefSubValue = settings["${prefDetail.sub}${buttonNumber}_${pressType}"]	//value of subsetting (eg 100)
        	if(prefDetail.sub) "$prefDetail.comm"(eachPref.value,PrefSubValue)
        	else "$prefDetail.comm"(eachPref.value)
    	}
	}
}

def turnOn(devices) {
	log.debug "Turning On: $devices"
	devices.on()
}

def turnOff(devices) {
	log.debug "Turning Off: $devices"
	devices.off()
}

def turnDim(devices, level) {
	log.debug "Dimming (to $level): $devices"
	devices.setLevel(level)
}

def adjustFan(device) {
	log.debug "Adjusting: $device"    
	def currentLevel = device.currentLevel
	if(device.currentSwitch == 'off') device.setLevel(15)
	else if (currentLevel < 34) device.setLevel(50)
  	else if (currentLevel < 67) device.setLevel(90)
	else device.off()
}

def adjustShade(device) {
	log.debug "shades: $device = ${device.currentMotor} state.lastUP = $state.lastshadesUp"
	if(device.currentMotor in ["up","down"]) {
    	state.lastshadesUp = device.currentMotor == "up"
    	device.stop()
    } else {
    	state.lastshadesUp ? device.down() : device.up()
//    	if(state.lastshadesUp) device.down()
//        else device.up()
        state.lastshadesUp = !state.lastshadesUp
    }
}

def speakerplaystate(device) {
	log.debug "Toggling Play/Pause: $device"
	device.currentValue('status').contains('playing')? device.pause() : device.play()
}
   
def speakernexttrack(device) {
	log.debug "Next Track Sent to: $device"
	device.nextTrack()
}   

def speakermute(device) {
	log.debug "Toggling Mute/Unmute: $device"
	device.currentValue('mute').contains('unmuted')? device.mute() : device.unmute()
} 

def levelUp(device, inclevel) {
	log.debug "Incrementing Level (by +$inclevel: $device"
	def currentVol = device.currentValue('level')[0]	//currentlevel return a list...[0] is first item in list ie volume level
    def newVol = currentVol + inclevel
  	device.setLevel(newVol)
    log.debug "Level increased by $inclevel to $newVol"
} 

def levelDown(device, declevel) {
	log.debug "Decrementing Level (by -declevel: $device"
	def currentVol = device.currentValue('level')[0]
    def newVol = currentVol.toInteger()-declevel
  	device.setLevel(newVol)
    log.debug "Level decreased by $declevel to $newVol"
} 

def setUnlock(devices) {
	log.debug "Unlocking: $devices"
	devices.unlock()
}

def toggle(devices) {
	log.debug "Toggling: $devices"
	if (devices*.currentValue('switch').contains('on')) {
		devices.off()
	}
	else if (devices*.currentValue('switch').contains('off')) {
		devices.on()
	}
	else if (devices*.currentValue('alarm').contains('off')) {
        devices.siren()
    }
	else {
		devices.on()
	}
}

def dimToggle(devices, dimLevel) {
	log.debug "Toggling On/Off | Dimming (to $dimLevel): $devices"
	if (devices*.currentValue('switch').contains('on')) devices.off()
	else devices.setLevel(dimLevel)
}

def runRout(rout){
	log.debug "Running: $rout"
	location.helloHome.execute(rout)
}

def messageHandle(msg, inApp) {
	if(inApp==true) {
    	log.debug "Push notification sent"
    	sendPush(msg)
	}
}

def smsHandle(phone, msg){
    log.debug "SMS sent"
    sendSms(phone, msg ?:"No custom text entered on: $app.label")
}

def changeMode(mode) {
	log.debug "Changing Mode to: $mode"
	if (location.mode != mode && location.modes?.find { it.name == mode }) setLocationMode(mode)
}

// execution filter methods
private getAllOk() {
	modeOk && daysOk && timeOk
}

private getModeOk() {
	def result = !modes || modes.contains(location.mode)
	log.trace "modeOk = $result"
	result
}

private getDaysOk() {
	def result = true
	if (days) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) {
			df.setTimeZone(location.timeZone)
		}
		else {
			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
		}
		def day = df.format(new Date())
		result = days.contains(day)
	}
	log.trace "daysOk = $result"
	result
}

private getTimeOk() {
	def result = true
	if (starting && ending) {
		def currTime = now()
		def start = timeToday(starting).time
		def stop = timeToday(ending).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	log.trace "timeOk = $result"
	result
}

private hhmm(time, fmt = "h:mm a") {
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
	f.setTimeZone(location.timeZone ?: timeZone(time))
	f.format(t)
}

private hideOptionsSection() {
	(starting || ending || days || modes || manualCount) ? false : true
}

private timeIntervalLabel() {
	(starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""
}

private def textHelp() {
	def text =
	"This smartapp allows you to use a device with buttons including, but not limited to:\n\n  Aeon Labs Minimotes\n"+
    "  HomeSeer HS-WD100+ switches**\n  HomeSeer HS-WS100+ switches\n  And now Lutron Picos***\n\n"+
	"It is a modified version of @dalec's Button Controller Plus SmartApp which is in turn"+
        " a version of @bravenel's Button Controller+ SmartApp.\n\n"+
        "The original apps were hardcoded to allow configuring 4 or 6 button devices."+
        " This app will automatically detect the number of buttons on your device or allow you to manually"+
        " specify (only needed if device does not report on its own)\n\n."+
	"This SmartApp also allows you to give your buton device full speaker control including: Play/Pause, NextTrack, Mute, VolumeUp/Down."+
    "(***Standard Pico remotes can be converted to Audio Picos)\n\n"+
        "The control options available are: \n"+
        "	Switches - Toggle \n"+
        "	Switches - Turn On \n"+
        "	Switches - Turn Off \n"+
        "	Dimmers - Toggle \n"+
        "	Dimmers - Set Level (Group 1) \n"+
        "	Dimmers - Set Level (Group 2) \n"+
        "	Dimmers - Inc Level \n"+
        "	Dimmers - Dec Level \n"+
        "	Fan to Adjust - Low, Medium, High, Off \n"+
        "	Shade to Adjust - Up, Down, or Stop \n"+
        "	Locks - Unlock Only \n"+
        "	Speaker - Play/Pause \n"+
        "	Speaker - Next Track \n"+
        "	Speaker - Mute/Unmute \n"+
        "	Speaker - Volume Up \n"+
        "	Speaker - Volume Down \n"+
        "	Set Modes \n"+
        "	Run Routines \n"+
        "	Sirens - Toggle \n"+
        "	Push Notifications \n"+
        "	SMS Notifications \n\n"+
	    "** Quirk for HS-WD100+ on 5/6 buttons:\n"+
        "Because a dimmer switch already uses press&hold to manually set the dimming level"+
        " please be aware of this operational behavior. If you only want to manually change"+
        " the dim level to the lights that are wired to the switch you will automatically"+
        " trigger the 5/6 button event as well. And the same is true in reverse, if you"+ 
        " only want to trigger a 5/6 button event action with press&hold you will manually"+
        " be changing the dim level of the switch simultaneously as well.\n"+
        "This quirk doesn't exist of course with the HS-HS100+ since it is not a dimmer.\n\n"+
        "*** Please Note: A Lutron SmartBridge, a device running @njschwartz's python script and the Lutron Caseta Service Manager"+
    	" SmartApp are also required for this functionality!\nSearch the forums for details."
    
  }
  


def getButtonSpecifics(buttonNumber) {

	if(hwSpecifics=="Lutron Pico") getLutronSpec(buttonNumber)
    if(hwSpecifics=="HomeSeer") getHomeSeerSpec(buttonNumber)
	if(hwSpecifics=="None") return
}

def getLutronSpec(buttonNumber) {
	switch (buttonNumber) {
   	    case 1:
  	       	section("Hardware specific info on button selection:") { 
           	paragraph image: "https://cdn.rawgit.com/stephack/ABC/master/resources/images/on3.png",
           	title: "",
            "Lutron Picos - Top Button"
            }
		break
       	case 2:
           	section("Hardware specific info on button selection:") {  
       		paragraph image: "https://cdn.rawgit.com/stephack/MyLutron/master/resources/images/off3.png",
           	title: "",
            "Lutron Picos - Bottom Button"
            }
		break
      	case 3:
          	section("Hardware specific info on button selection:") {  
       		paragraph image: "https://cdn.rawgit.com/stephack/MyLutron/master/resources/images/middle.png",
           	title: "",
            "Lutron Picos - Middle Button"
            }
		break
       	case 4:
           	section("Hardware specific info on button selection:") {  
       		paragraph image: "https://cdn.rawgit.com/stephack/MyLutron/master/resources/images/up.png",
           	title: "",
            "Lutron Picos - Up Button"
            }
		break
       	case 5:
          	section("Hardware specific info on button selection:") {  
      		paragraph image: "https://cdn.rawgit.com/stephack/MyLutron/master/resources/images/down.png",
           	title: "",
            "Lutron Picos - Down Button"
            }
		break
        default:
           	section("Hardware specific info on button selection:") {  
            paragraph image: "",
        	title: "",
            "Lutron Picos - NOT USED"
        	}
		break
	}
}

def getHomeSeerSpec(buttonNumber){
	switch (buttonNumber) {
   	    case 1:
  	        section("Hardware specific info on button selection:") {  
           	paragraph "WD100+ or WS100+ devices; this FIRST Button action occurs with a double-tap on upper paddle.\n"+
			"*Select 'Pushed' (not 'Held') options.\n\nAeon Minimote; FIRST button is upper left when operating in hand.\n"+
			"*Select 'Pushed' and/or 'Held' options."             
            }
     	break
        case 2:
           	section("Hardware specific info on button selection:") {  
        	paragraph "WD100+ or WS100+ devices; this SECOND Button action occurs with a double-tap on lower paddle.\n"+
			" *Select 'Pushed' (not 'Held') options.\n\nAeon Minimote; SECOND button is upper right when operating in hand.)\n"+
			"*Select 'Pushed' and/or 'Held' options." 
            }
  		break
        case 3:
           	section("Hardware specific info on button selection:") {  
        	paragraph "WD100+ or WS100+ devices; this THIRD Button action occurs with a triple-tap on upper paddle.\n"+
			" *Select 'Pushed' (not 'Held') options.\n\nAeon Minimote; THIRD button is lower left when operating in hand.)\n"+
			"*Select 'Pushed' and/or 'Held' options."
            }
		break
       	case 4:
           	section("Hardware specific info on button selection:") {  
       		paragraph "WD100+ or WS100+ devices; this FOURTH Button action occurs with a triple-tap on lower paddle.\n"+
			" *Select 'Pushed' (not 'Held') options.\n\nAeon Minimote; FOURTH button is lower right when operating in hand.\n"+
			"*Select 'Pushed' and/or 'Held' options." 
            }
       	break
        case 5:
           	section("Hardware specific info on button selection:") {  
        	paragraph "(See user guide on quirk for WD100+) For WS100+ devices; this FIFTH Button action occurs" +
			" with a press & hold on upper paddle.\n*Select 'Pushed' (not 'Held') options." 
         	}
		break
       	case 6:
           	section("Hardware specific info on button selection:") {  
       		paragraph "(See user guide on quirk for WD100+) For WS100+ devices; SIXTH Button action occurs" +
			" with a press & hold on lower paddle.\n*Select 'Pushed' (not 'Held') options." 
       		}
		break
        default:
           	section("Hardware specific info on button selection:") {  
            paragraph "HomeSeer - NOT USED"
        	}
		break
   	}        	
}
  
//*/ ALTERNATIVE executeHandler code

/*    
    getButtonDetails().each{ //needs to be renamed to getPreff
		def confSettings = settings["${it.id}${buttonNumber}_${value}"]		//return value of device to act on (eg bedroom lamp)
        if(confSettings) {        
        	if(it.sub) {	//if configuration found and has sub setting then do
            	def confVal = settings["${it.sub}${buttonNumber}_${value}"]	//returns the value of the subsetting if it exists (eg. 100)
    			"$it.comm"(confSettings,confVal)
        	}
        	else {
            "$it.comm"(confSettings)	//if configuration exists with no sub value
        	}
		}
	}
*/

/*////////////////////OLD BUTTON EVENT CODE THAT WAS REMOVED WHEN EXECUTEHANDLERS WAS MERGED WITH IT
def buttonEvent(evt){
	if(allOk) {
    	def buttonNumber = evt.jsonData.buttonNumber
		def pressType = evt.value
		log.debug "$buttonDevice: Button $buttonNumber was $pressType"
		//def recentEvents = buttonDevice.eventsSince(new Date(now() - 2000)).findAll{it.value == evt.value && it.data == evt.data}
		//log.debug "Found ${recentEvents.size()?:0} events in past 2 seconds"	
        //if(recentEvents.size <= 1){
        	executeHandlers(buttonNumber, pressType)            
		//} else {
		//	log.debug "Found recent button press events for $buttonNumber with value $pressType"
		//}
	}
}
*/