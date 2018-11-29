// See https://github.com/dialogflow/dialogflow-fulfillment-nodejs
// for Dialogflow fulfillment library docs, samples, and to report issues
'use strict';
 
const functions = require('firebase-functions');
const {WebhookClient} = require('dialogflow-fulfillment');
const {Card, Suggestion} = require('dialogflow-fulfillment');
 
process.env.DEBUG = 'dialogflow:debug'; // enables lib debugging statements
 
exports.dialogflowFirebaseFulfillment = functions.https.onRequest((request, response) => {
  const agent = new WebhookClient({ request, response });
  console.log('Dialogflow Request headers: ' + JSON.stringify(request.headers));
  console.log('Dialogflow Request body: ' + JSON.stringify(request.body));
 
  function welcome(agent) {
    agent.add(`Hello, welcome to airport navigation. Your first step will be to checkin, drop off your luggage and get your boarding pass. Have you already checked in online?`);
    
  }
 
  function fallback(agent) {
    agent.add(`I didn't understand`);
    agent.add(`I'm sorry, can you try again?`);
}

function welcomeResponse(agent){
    if(agent.parameters.checkinresponse.toString()=="yes"){
        agent.add("Do you have any baggage that needs to be checked in?");
        //agent.clearContext("checkIfDone2");
        const context = {'name': 'baggage', 'lifespan': 1};
        agent.setContext(context);
    }else{
        agent.add("Proceed to the checkin counter and keep your passport in hand, let me know when ur done");
        //agent.clearContext("baggage");
        const context = {'name': 'checkIfDone2', 'lifespan': 1, 'parameters': {'action': 'Nav_To_Checkin'}}
        agent.setContext(context);
        
    }
    
}
function baggage(agent){
    if(agent.parameters.baggageResponse.toString()=="yes"){
        agent.add("Proceed to checkin counter to drop off your baggage, let me know when your done");
        const context = {'name': 'checkIfDone', 'lifespan': 1, 'parameters': {'action': 'Nav_To_Checkin'}}
        agent.setContext(context);
    }else{
        agent.add("You may go straight to the security check,let me know when your done");
        const context = {'name': 'checkIfDone', 'lifespan': 1, 'parameters': {'action': 'Nav_To_SC'}}
        agent.setContext(context);
    }
}
function SCAssistance(agent){
    if(agent.parameters.user_response.toString()=="yes"){
        agent.add("TIPS,Keep your passport and boarding pass in hand,Keep all electronics in a separate tray and your carry-on bags in another tray,");
        agent.add("Proceed to Security check, let me know once done");
        const context = {'name': 'Nav_To_SC', 'lifespan': 1, 'parameters': {'action': 'Nav_To_SC'}};
        agent.setContext(context);
    }else{
        agent.add("Proceed to Security check, let me know once done");
        agent.clearOutgoingContexts();
        const context = {'name': 'Nav_To_SC', 'lifespan': 1, 'parameters': {'action': 'Nav_To_SC'}};
        agent.setContext(context);
    }
}
function gateAction(agent){
    agent.add("Proceed to the gate, and let us know when you have reached");
    const context = {'name': 'Nav_To_Gate', 'lifespan': 1, 'parameters': {'action': 'Nav_To_Gate'}};
    agent.setContext(context);
}
  // // Uncomment and edit to make your own intent handler
  // // uncomment `intentMap.set('your intent name here', yourFunctionHandler);`
  // // below to get this function to be run when a Dialogflow intent is matched
  // function yourFunctionHandler(agent) {
  //   agent.add(`This message is from Dialogflow's Cloud Functions for Firebase editor!`);
  //   agent.add(new Card({
  //       title: `Title: this is a card title`,
  //       imageUrl: 'https://developers.google.com/actions/images/badges/XPM_BADGING_GoogleAssistant_VER.png',
  //       text: `This is the body text of a card.  You can even use line\n  breaks and emoji! 💁`,
  //       buttonText: 'This is a button',
  //       buttonUrl: 'https://assistant.google.com/'
  //     })
  //   );
  //   agent.add(new Suggestion(`Quick Reply`));
  //   agent.add(new Suggestion(`Suggestion`));
  //   agent.setContext({ name: 'weather', lifespan: 2, parameters: { city: 'Rome' }});
  // }

  // // Uncomment and edit to make your own Google Assistant intent handler
  // // uncomment `intentMap.set('your intent name here', googleAssistantHandler);`
  // // below to get this function to be run when a Dialogflow intent is matched
  // function googleAssistantHandler(agent) {
  //   let conv = agent.conv(); // Get Actions on Google library conv instance
  //   conv.ask('Hello from the Actions on Google client library!') // Use Actions on Google library
  //   agent.add(conv); // Add Actions on Google library responses to your agent's response
  // }
  // // See https://github.com/dialogflow/dialogflow-fulfillment-nodejs/tree/master/samples/actions-on-google
  // // for a complete Dialogflow fulfillment library Actions on Google client library v2 integration sample

  // Run the proper function handler based on the matched Dialogflow intent name
  let intentMap = new Map();
  intentMap.set('Default Welcome Intent', welcome);
  intentMap.set('Default Fallback Intent', fallback);
  intentMap.set('welcome.response', welcomeResponse);
  intentMap.set('baggage',baggage);
  intentMap.set('SCAssistance',SCAssistance);
  intentMap.set('Nav_To_SC',gateAction);
  // intentMap.set('your intent name here', googleAssistantHandler);
  agent.handleRequest(intentMap);
});
