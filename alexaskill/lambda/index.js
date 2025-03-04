/* *
 * This sample demonstrates handling intents from an Alexa skill using the Alexa Skills Kit SDK (v2).
 * Please visit https://alexa.design/cookbook for additional examples on implementing slots, dialog management,
 * session persistence, api calls, and more.
 * */
const Alexa = require('ask-sdk-core');
const got = require('got');
const extApi = "https://somepublicdomain/";
const rAudio = /<audio\s+src=['"]([^'"]+)['"]\s*\/>/;


const LaunchRequestHandler = {
    canHandle(handlerInput) {
        return Alexa.getRequestType(handlerInput.requestEnvelope) === 'LaunchRequest';
    },
    async handle(handlerInput) {
        const res = await got(extApi+"OpenGameIntent");
        const o = res.body;
        return handlerInput.responseBuilder
            .speak(o)
            .reprompt(o)
            .getResponse();
    }
};

const intents=[
    "SetFirstPlayerIntent",
    "SetSecondPlayerIntent",
    "StartGameIntent",
    "TakeDiceNrsIntent",
    "TakeDiceOrdinalsIntent",
    "ReleaseDiceNrsIntent",
    "ReleaseDiceOrdinalsIntent",
    "RollDiceIntent",
    "GetScoreIntent",
    "ScoreDicesIntent",
    "RestartIntent",
    "ListScoresIntent",
    "ListMissingScoresIntent",
    "ListDicesIntent", //also multimodal usage
    "HelpScoreIntent",
    "RerollDiceNrsIntent",
    "RerollDiceOrdinalsIntent",
    "YesIntent","NoIntent","SetPlayerIntent","ToggleVuiOnlyIntent",
    //multimodal usage
    "StartRerollCmdIntent","OrdinalIntent","StartScoreCmdIntent","ScoreNameIntent","StartCloseCmdIntent","EndCloseCmdIntent"];

const GameIntentHandler = {
    canHandle(handlerInput) {
        if(!(Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'))return false;
        return intents.includes(Alexa.getIntentName(handlerInput.requestEnvelope));
    },
    async handle(handlerInput) {
        let o = "";
        const intent = Alexa.getIntentName(handlerInput.requestEnvelope);
        const intentIdx = intents.indexOf(intent);
        //build call
        let extApiCall = extApi+intent;
        let extApiArrParams = undefined;
        const createApiArrParams = function(arrname,arr){
            let nums = [];for(const nr of arr)nums.push([arrname,nr.value]);
            extApiArrParams = new URLSearchParams(nums);
        };
        // inc single params in the url
        if([0,1,19].includes(intentIdx)){
            extApiCall+="/?name="+Alexa.getSlotValue(handlerInput.requestEnvelope, 'name');
        }
        if([8,9,14,24].includes(intentIdx)){
            extApiCall+="/?sname="+handlerInput.requestEnvelope.request.intent.slots.sname.resolutions.resolutionsPerAuthority[0].values[0].value.name;
        }
        if([22].includes(intentIdx)){
            extApiCall+="/?onumber="+Alexa.getSlotValue(handlerInput.requestEnvelope, 'onumber');
        }
        // inc arr params 
        if([3,5,15].includes(intentIdx)){
            //extApiCall+="/?numbers="+handlerInput.requestEnvelope.request.intent.slots.numbers.value;
            const slot = handlerInput.requestEnvelope.request.intent.slots.numbers.slotValue;
            let arr;
            if(slot.values && slot.values.length > 1)arr=slot.values;
            else arr = [slot];
            createApiArrParams('numbers',arr);
        }
        if([4,6,16].includes(intentIdx)){
            //extApiCall+="/?onumber="+handlerInput.requestEnvelope.request.intent.slots.onumber.value;
            const slot = handlerInput.requestEnvelope.request.intent.slots.onumbers.slotValue;
            let arr;
            if(slot.values && slot.values.length > 1)arr=slot.values;
            else arr = [slot];
            createApiArrParams('onumbers',arr);
        }
        
        //call and wait
        let res;
        if(extApiArrParams !== undefined){
            res = await got(extApiCall+"/?"+extApiArrParams.toString());
            //o=extApiCall+"/?"+extApiArrParams.toString().replace(new RegExp('&', 'g'), '_amp_');
        }else {
            res = await got(extApiCall);
            //o=extApiCall;
            
        }
        o=res.body; 
        //remove the audio shit, yes, u contains the last audio src url
        //https://github.com/dabblelab/3-alexa-audio-streaming-example-skill/blob/master/lambda/custom/index.js doesnt want to work (tested: inside diff handler, w/o async, example)
        let u;
        o = o.replace(rAudio, (match, p1) => {
            u = p1;
            return '';  // Replace the <audio> tag with an empty string
        });
        return handlerInput.responseBuilder
            .speak(o)
            .reprompt(o)
            .getResponse();
    }
};

const HelpIntentHandler = {
    canHandle(handlerInput) {
        return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
            && Alexa.getIntentName(handlerInput.requestEnvelope) === 'AMAZON.HelpIntent';
    },
    async handle(handlerInput) {
        //build call
        const extApiCall = extApi+"HelpIntent";
        const res = await got(extApiCall);
        const o = res.body;
        //const o=extApiCall;
        return handlerInput.responseBuilder
            .speak(o)
            .reprompt(o)
            .getResponse();
    }
};

const CancelAndStopIntentHandler = {
    canHandle(handlerInput) {
        return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
            && (Alexa.getIntentName(handlerInput.requestEnvelope) === 'AMAZON.CancelIntent'
                || Alexa.getIntentName(handlerInput.requestEnvelope) === 'AMAZON.StopIntent');
    },
    handle(handlerInput) {
        const speakOutput = 'Goodbye!';

        return handlerInput.responseBuilder
            .speak(speakOutput)
            .getResponse();
    }
};
/* *
 * FallbackIntent triggers when a customer says something that doesn’t map to any intents in your skill
 * It must also be defined in the language model (if the locale supports it)
 * This handler can be safely added but will be ingnored in locales that do not support it yet 
 * */
const FallbackIntentHandler = {
    canHandle(handlerInput) {
        return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
            && Alexa.getIntentName(handlerInput.requestEnvelope) === 'AMAZON.FallbackIntent';
    },
    handle(handlerInput) {
        const speakOutput = 'Sorry, I don\'t know about that. Please try again.';

        return handlerInput.responseBuilder
            .speak(speakOutput)
            .reprompt(speakOutput)
            .getResponse();
    }
};
/* *
 * SessionEndedRequest notifies that a session was ended. This handler will be triggered when a currently open 
 * session is closed for one of the following reasons: 1) The user says "exit" or "quit". 2) The user does not 
 * respond or says something that does not match an intent defined in your voice model. 3) An error occurs 
 * */
const SessionEndedRequestHandler = {
    canHandle(handlerInput) {
        return Alexa.getRequestType(handlerInput.requestEnvelope) === 'SessionEndedRequest';
    },
    handle(handlerInput) {
        console.log(`~~~~ Session ended: ${JSON.stringify(handlerInput.requestEnvelope)}`);
        // Any cleanup logic goes here.
        return handlerInput.responseBuilder.getResponse(); // notice we send an empty response
    }
};
/* *
 * The intent reflector is used for interaction model testing and debugging.
 * It will simply repeat the intent the user said. You can create custom handlers for your intents 
 * by defining them above, then also adding them to the request handler chain below 
 * */
const IntentReflectorHandler = {
    canHandle(handlerInput) {
        return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest';
    },
    handle(handlerInput) {
        const intentName = Alexa.getIntentName(handlerInput.requestEnvelope);
        const speakOutput = `You just triggered ${intentName}`;

        return handlerInput.responseBuilder
            .speak(speakOutput)
            //.reprompt('add a reprompt if you want to keep the session open for the user to respond')
            .getResponse();
    }
};
/**
 * Generic error handling to capture any syntax or routing errors. If you receive an error
 * stating the request handler chain is not found, you have not implemented a handler for
 * the intent being invoked or included it in the skill builder below 
 * */
const ErrorHandler = {
    canHandle() {
        return true;
    },
    handle(handlerInput, error) {
        const speakOutput = 'Sorry, I had trouble doing what you asked. Please try again.';
        console.log(`~~~~ Error handled: ${JSON.stringify(error)}`);

        return handlerInput.responseBuilder
            .speak(speakOutput)
            .reprompt(speakOutput)
            .getResponse();
    }
};

/**
 * This handler acts as the entry point for your skill, routing all request and response
 * payloads to the handlers above. Make sure any new handlers or interceptors you've
 * defined are included below. The order matters - they're processed top to bottom 
 * */
exports.handler = Alexa.SkillBuilders.custom()
    .addRequestHandlers(
        LaunchRequestHandler,
        GameIntentHandler,
        HelpIntentHandler,
        CancelAndStopIntentHandler,
        FallbackIntentHandler,
        SessionEndedRequestHandler,
        IntentReflectorHandler)
    .addErrorHandlers(
        ErrorHandler)
    .withCustomUserAgent('sample/hello-world/v1.2')
    .lambda();
