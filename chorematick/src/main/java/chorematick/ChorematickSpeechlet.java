package chorematick;

import com.amazon.speech.speechlet.*;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.SsmlOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
import com.amazon.speech.ui.StandardCard;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedList;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.time.*;
import java.text.SimpleDateFormat;

public class ChorematickSpeechlet implements Speechlet {

  private final static Logger log = Logger.getLogger(ChorematickSpeechlet.class.getName());

  private  AmazonDynamoDBClient client;

  private DynamoDBMapper mapper;
  private DynamoDB dynamoDB;

  private CardHandler cardHandler;
  private RepromptHandler repromptHandler;

  public ChorematickSpeechlet(DynamoDBMapper mapper) {
    super();
    this.mapper = mapper;
    this.cardHandler = new CardHandler();
    this.repromptHandler = new RepromptHandler();
  }

  public void onSessionStarted(final SessionStartedRequest request, final Session session) {
  }

  @Override
  public SpeechletResponse onLaunch(final LaunchRequest request, final Session session) {
    return getWelcomeResponse();
  }

  @Override
  public SpeechletResponse onIntent(final IntentRequest request, final Session session) {

    Intent intent = request.getIntent();
    String intentName = (intent != null) ? intent.getName() : null;

    if ("GetChoreIntent".equals(intentName)) {
      return getChoreResponse(intent);
    } else if ("GetDoneIntent".equals(intentName)){
      return getDoneResponse(intent);
    } else if ("ConfirmChoreIntent".equals(intentName)){
      return getConfirmChoreResponse(intent);
    } else if ("ChorematickIntent".equals(intentName)) {
      return getEasterEggResponse();
    } else if ("AddChoreIntent".equals(intentName)) {
      return getAddChoreResponse(intent);
    } else if ("GetChoreListIntent".equals(intentName)) {
      return getChoreList();
    } else if ("NumberOfCompletedChoresIntent".equals(intentName)) {
      return getNumberOfCompletedChoresResponse();
    } else if ("AMAZON.HelpIntent".equals(intentName)) {
      return getHelpResponse();
    } else {
      return getErrorResponse();
    }
  }

  public void onSessionEnded(final SessionEndedRequest request, final Session session) {
  }

  private SpeechletResponse getWelcomeResponse() {
    String speechText = "<speak> Welcome to, <phoneme alphabet=\"ipa\" ph=\"tʃɔːrmætɪk\">Chorematic</phoneme>! What would you like to do today? </speak>";

    SsmlOutputSpeech speech = new SsmlOutputSpeech();
    speech.setSsml(speechText);

    Reprompt reprompt = repromptHandler.getReprompt("You can say Help for a full list of options");

    if(this.countChoresCompleted() >= 10){
      StandardCard card = cardHandler.getStandardCard("10 chores complete!! \n Suggested gift:","Hasbro NERF Rebelle Diamondista Blaster £4.99","https://images-na.ssl-images-amazon.com/images/I/61miKEYpgSL._SL1000_.jpg");
      return SpeechletResponse.newAskResponse(speech, reprompt, card);
    } else {
      return SpeechletResponse.newAskResponse(speech, reprompt);
    }
  }

  private SpeechletResponse getChoreResponse(Intent intent) {

    String day;

    if (intent.getSlot("choreDate").getValue() == null) {
      Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("EST"));
      SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");

      day = format1.format(cal.getTime()).toString();

    } else {
      day = intent.getSlot("choreDate").getValue();
    }

    Map<String, String> attributeNames = new HashMap<String, String>();
    attributeNames.put("#due", "Due");

    Map<String, AttributeValue> attributeValues = new HashMap<String, AttributeValue>();
    attributeValues.put(":Due", new AttributeValue().withS(day));

    DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
            .withFilterExpression("#due = :Due")
            .withExpressionAttributeNames(attributeNames)
            .withExpressionAttributeValues(attributeValues);

    PaginatedList<Task> chores =  mapper.scan(Task.class, scanExpression);

    PlainTextOutputSpeech speech = new PlainTextOutputSpeech();

    if (chores.size() != 0) {
        Task task = chores.get(0);
        speech.setText("Your chore is. " + task.getChore());
    } else {
        speech.setText("It's your lucky day! you have no assigned chores.");
    }

    SimpleCard card = cardHandler.getSimpleCard("Chore requested", "Your child just asked for today's chore");

    return SpeechletResponse.newTellResponse(speech, card);
  }

  private SpeechletResponse getDoneResponse(Intent intent) {
    String speechText = "Very well, I have informed your appropriate adult.";

    // Should we refactor this to have default values and so forth?
    String day = intent.getSlot("choreDate").getValue();
    String chore = intent.getSlot("chore").getValue();

    Task task = this.mapper.load(Task.class, day, chore);

    SimpleCard card = cardHandler.getSimpleCard("Chore Verification",("Your child claims to have completed their chore. Here is the password: " + task.getPassword()));

    PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
    speech.setText(speechText);

    return SpeechletResponse.newTellResponse(speech, card);
  }

  public SpeechletResponse getChoreList() {

    DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();

    List<Task> chores =  mapper.scan(Task.class, scanExpression);

    String result = "";

    for(Task task : chores) {
      result = result + task.getChore() + ", ";
    }

    PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
    System.out.println("result " + result);
    speech.setText(result);
    return SpeechletResponse.newTellResponse(speech);
  }

  private SpeechletResponse getAddChoreResponse(Intent intent){

    Random random = new Random();

    PlainTextOutputSpeech speech = new PlainTextOutputSpeech();

    String day = intent.getSlot("choreDate").getValue();
    String chore = intent.getSlot("chore").getValue();
    String password = String.format("%04d", random.nextInt(10000));

    Task task = new Task();
    task.setDate(day);
    task.setChore(chore);
    task.setPassword(password);
    this.mapper.save(task);

    speech.setText("Very well, I have added a " + chore + " chore for " + day);

    SimpleCard card = cardHandler.getSimpleCard("New chore added",(day + " " + chore + "\nPassword: " + password));

    return SpeechletResponse.newTellResponse(speech, card);
  }

  private SpeechletResponse getHelpResponse() {

    PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
    speech.setText("You can tell me to add a chore; you can ask me for today's chore; tell me that you've finished your chore; confirm a password; ask me for a list of chores; or ask me for the number of completed chores");

    Reprompt reprompt = repromptHandler.getReprompt("Tell me what you would like to do!");

    SimpleCard card = cardHandler.getSimpleCard("ChoreMatick Tips!", "You can tell me to add a chore by saying, for example, 'Add mow the lawn for Tuesday'. \n  You can ask me 'What is my chore for today?'. \n You can tell me that you have finished the chore by saying 'I am done with mow the lawn for today'. \n You can confirm that your child has completed their chore by providing the given password e.g 'Confirm 1234'. \n  You can also ask me for a 'Full list of chores', and 'The total number of chores that are completed'." );

    return SpeechletResponse.newAskResponse(speech, reprompt, card);
  }

  private SpeechletResponse getErrorResponse() {
    String speechText = "error error error. Danger Will Robinson.";
    PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
    speech.setText(speechText);
    return SpeechletResponse.newTellResponse(speech);
  }

  private SpeechletResponse getConfirmChoreResponse(Intent intent) {

    PlainTextOutputSpeech speech = new PlainTextOutputSpeech();

    String password = intent.getSlot("password").getValue();

    Map<String, String> attributeNames = new HashMap<String, String>();
    attributeNames.put("#password", "password");

    Map<String, AttributeValue> attributeValues = new HashMap<String, AttributeValue>();
    attributeValues.put(":pass", new AttributeValue().withS(password));

    DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
    .withFilterExpression("#password = :pass")
    .withExpressionAttributeNames(attributeNames)
    .withExpressionAttributeValues(attributeValues);

    PaginatedList<Task> chores =  mapper.scan(Task.class, scanExpression);

    if (chores.size() > 0) {
      Task task = chores.get(0);
      task.setIsComplete(true);
      this.mapper.save(task);
      speech.setText("I've confirmed "+ task.getDate() + " " + task.getChore() +" chore is completed.");
    } else {
      speech.setText("Is there anything else I can help you with today?");
    }

    Reprompt reprompt = repromptHandler.getReprompt("Please state the password for the chore you wish to confirm");

    SimpleCard card = cardHandler.getSimpleCard("Chore Completed!","Thank you for confiming your child has completed their chore; the list has been updated");

    return SpeechletResponse.newAskResponse(speech, reprompt, card);
  }

  private SpeechletResponse getNumberOfCompletedChoresResponse(){

    int number = countChoresCompleted();

    PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
    speech.setText("There are " + number + " completed chores.");
    return SpeechletResponse.newTellResponse(speech);
  }

  private SpeechletResponse getEasterEggResponse() {
    PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
    speech.setText("Go stand in the corner and think about what you've done.");
    return SpeechletResponse.newTellResponse(speech);
  }

  private int countChoresCompleted(){
    Map<String, String> attributeNames = new HashMap<String, String>();
    attributeNames.put("#complete", "Complete");

    Map<String, AttributeValue> attributeValues = new HashMap<String, AttributeValue>();
    attributeValues.put(":yes", new AttributeValue().withN("1"));

    DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
    .withFilterExpression("#complete = :yes")
    .withExpressionAttributeNames(attributeNames)
    .withExpressionAttributeValues(attributeValues);

    PaginatedList<Task> completedChores =  mapper.scan(Task.class, scanExpression);

    return completedChores.size();
  }
}
