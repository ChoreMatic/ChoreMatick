package chorematick;

import com.amazon.speech.speechlet.*;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.SsmlOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
import com.amazon.speech.ui.StandardCard;
import com.amazon.speech.ui.Image;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

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

  private Dao dao;

  public ChorematickSpeechlet(Dao dao) {
    super();
    this.dao = dao;
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
    }else if ("ChorematickIntent".equals(intentName)) {
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

    Reprompt reprompt = new Reprompt();
    PlainTextOutputSpeech repromptSpeech = new PlainTextOutputSpeech();
    repromptSpeech.setText("You can ask Help for a full list of options");
    reprompt.setOutputSpeech(repromptSpeech);

    if(this.countChoresCompleted() >= 10){
      StandardCard card = new StandardCard();
      Image image = new Image();
      image.setSmallImageUrl("https://images-na.ssl-images-amazon.com/images/I/61miKEYpgSL._SL1000_.jpg");
      card.setTitle("10 chores complete!! \n Suggested gift:");
      card.setText("Hasbro NERF Rebelle Diamondista Blaster £4.99");
      card.setImage(image);
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

    List<Task> chores = dao.scanDB("Due", day);

    PlainTextOutputSpeech speech = new PlainTextOutputSpeech();

    if (chores.size() != 0) {
        Task task = chores.get(0);
        speech.setText("Your chore is. " + task.getChore());
    } else {
        speech.setText("It's your lucky day! you have no assigned chores.");
    }

    SimpleCard card = new SimpleCard();
    card.setTitle("Chore requested");
    card.setContent("Your child just asked for today's chore");

    return SpeechletResponse.newTellResponse(speech, card);
  }

  private SpeechletResponse getDoneResponse(Intent intent) {
    String speechText = "Very well, I have informed your appropriate adult.";

    // Should we refactor this to have default values and so forth?
    String day = intent.getSlot("choreDate").getValue();
    String chore = intent.getSlot("chore").getValue();

    Task task = this.dao.loadFromDB(day, chore);

    SimpleCard card = new SimpleCard();
    card.setTitle("Chore Verification");
    card.setContent("Your child claims to have completed their chore. Here is the password: " + task.getPassword());

    PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
    speech.setText(speechText);

    return SpeechletResponse.newTellResponse(speech, card);
  }

  public SpeechletResponse getChoreList() {

    List<Task> chores =  this.dao.getAllChores();

    String result = "";

    for(Task task : chores) {
      result = result + task.getChore() + ", ";
    }

    PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
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
    this.dao.saveToDB(task);

    speech.setText("Very well, I have added a " + chore + " chore for " + day);

    SimpleCard card = new SimpleCard();
    card.setTitle(day + " " + chore);
    card.setContent("Password: " + password);

    return SpeechletResponse.newTellResponse(speech, card);
  }

  private SpeechletResponse getHelpResponse() {

    PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
    speech.setText("You can tell me to add a chore; you can ask me for today's chore; tell me that you've finished your chore; confirm a password; ask me for a list of chores; or ask me for the number of completed chores");

    Reprompt reprompt = new Reprompt();
    PlainTextOutputSpeech repromptSpeech = new PlainTextOutputSpeech();
    repromptSpeech.setText("Tell me what you would like to do!");
    reprompt.setOutputSpeech(repromptSpeech);

    SimpleCard card = new SimpleCard();
    card.setTitle("ChoreMatick Tips!");
    card.setContent("You can tell me to add a chore by saying, for example, 'Add mow the lawn for Tuesday'. \n  You can ask me 'What is my chore for today?'. \n You can tell me that you have finished the chore by saying 'I am done with mow the lawn for today'. \n You can confirm that your child has completed their chore by providing the given password e.g 'Confirm 1234'. \n  You can also ask me for a 'Full list of chores', and 'The total number of chores that are completed'.");

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

    List<Task> chores = this.dao.scanDB("password", password);

    if (chores.size() > 0) {
      Task task = chores.get(0);
      task.setIsComplete(true);
      this.dao.saveToDB(task);
      speech.setText("I've confirmed "+ task.getDate() + " " + task.getChore() +" chore is completed.");
    } else {
      speech.setText("Unable to confirm password, please try again.");
    }

    Reprompt reprompt = new Reprompt();
    PlainTextOutputSpeech repromptSpeech = new PlainTextOutputSpeech();
    repromptSpeech.setText("Please try confirming the password again");
    reprompt.setOutputSpeech(repromptSpeech);

    SimpleCard card = new SimpleCard();
    card.setTitle("Chore Completed!");
    card.setContent("Thank you for confiming your child has completed their chore; the list has been updated");

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

    List<Task> completedChores =  dao.scanDB("Complete", "1");

    return completedChores.size();
  }

}
