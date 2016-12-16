package chorematick;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.SsmlOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
import com.amazon.speech.ui.StandardCard;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import java.util.Iterator;
import java.util.List;

public class ChorematickSpeechletTest extends BaseTestCase {

  private ChorematickSpeechlet speechlet;

  @Mock private IntentRequest mockedIntentRequest;
  @Mock private LaunchRequest mockedLaunchRequest;
  @Mock private Session mockedSession;
  @Mock private Intent mockedIntent;
  @Mock private SessionStartedRequest mockedSessionStartedRequest;
  @Mock private Slot mockedDateSlot;
  @Mock private Slot mockedChoreSlot;
  @Mock private Slot mockedPasswordSlot;
  @Mock private Task mockedTask;
  @Mock private List<Task> mockedList;
  @Mock private DynamoDBScanExpression mockedExpression;
  @Mock private Iterator mockedIterator;
  @Mock private Dao mockedDao;

  @Before
  public void setup() {
    when(mockedIntentRequest.getIntent()).thenReturn(mockedIntent);
    speechlet = new ChorematickSpeechlet(mockedDao);
    speechlet.onSessionStarted(mockedSessionStartedRequest, mockedSession);
  }

  @Test
  public void WelcomeResponseTest() {
    when(mockedDao.scanDB(eq("Complete"), any(String.class))).thenReturn(mockedList);
    when(mockedList.size()).thenReturn(5);

    SsmlOutputSpeech speech = new SsmlOutputSpeech();

    SpeechletResponse response = speechlet.onLaunch(mockedLaunchRequest, mockedSession);

    assertEquals("<speak> Welcome to, <phoneme alphabet=\"ipa\" ph=\"tʃɔːrmætɪk\">Chorematic</phoneme>! What would you like to do today? </speak>", ((SsmlOutputSpeech) response.getOutputSpeech()).getSsml());
  }

  @Test
  public void testGiftSuggestion(){
    when(mockedDao.scanDB(eq("Complete"), any(String.class))).thenReturn(mockedList);
    when(mockedList.size()).thenReturn(11);

    SsmlOutputSpeech speech = new SsmlOutputSpeech();

    SpeechletResponse response = speechlet.onLaunch(mockedLaunchRequest, mockedSession);

    assertEquals("<speak> Welcome to, <phoneme alphabet=\"ipa\" ph=\"tʃɔːrmætɪk\">Chorematic</phoneme>! What would you like to do today? </speak>", ((SsmlOutputSpeech) response.getOutputSpeech()).getSsml());

    StandardCard card = (StandardCard) response.getCard();

    assertThat(card.getTitle(), equalTo("10 chores complete!! \n Suggested gift:"));
    assertThat(card.getText(), equalTo("Hasbro NERF Rebelle Diamondista Blaster £4.99"));
    assertThat(card.getImage().getSmallImageUrl(), equalTo("https://images-na.ssl-images-amazon.com/images/I/61miKEYpgSL._SL1000_.jpg"));
  }

  @Test
  public void errorResponseTest() {

    when(mockedIntent.getName()).thenReturn("jibberish");

    SpeechletResponse response = speechlet.onIntent(mockedIntentRequest, mockedSession);

    assertEquals("error error error. Danger Will Robinson.", ((PlainTextOutputSpeech) response.getOutputSpeech()).getText());
  }

  @Test
  public void testHelpResponse() {

    when(mockedIntent.getName()).thenReturn("AMAZON.HelpIntent");

    SpeechletResponse response = speechlet.onIntent(mockedIntentRequest, mockedSession);
    SimpleCard card = (SimpleCard) response.getCard();

    assertThat(card.getTitle(), equalTo("ChoreMatick Tips!"));
    assertThat(((PlainTextOutputSpeech) response.getOutputSpeech()).getText(), equalTo("You can tell me to add a chore; you can ask me for today's chore; tell me that you've finished your chore; confirm a password; ask me for a list of chores; or ask me for the number of completed chores"));

  }

  @Test
  public void testgetChoreResponse() {
    when(mockedIntent.getName()).thenReturn("GetChoreIntent");
    when(mockedIntent.getSlot("choreDate")).thenReturn(mockedDateSlot);
    when(mockedDao.scanDB(eq("Due"), any(String.class))).thenReturn(mockedList);
    when(mockedList.size()).thenReturn(1);
    when(mockedList.get(0)).thenReturn(mockedTask);
    when(mockedTask.getChore()).thenReturn("Sweep the chimney. That's right. Sweep the chimney.");


    SpeechletResponse response = speechlet.onIntent(mockedIntentRequest, mockedSession);
    SimpleCard card = (SimpleCard) response.getCard();

    assertThat(((PlainTextOutputSpeech) response.getOutputSpeech()).getText(), equalTo("Your chore is. Sweep the chimney. That's right. Sweep the chimney."));
    assertThat(card.getTitle(), equalTo("Chore requested"));
    assertThat(card.getContent(), equalTo("Your child just asked for today's chore"));
  }

  @Test
  public void testgetChoreResponseOnNullDate() {
    when(mockedIntent.getName()).thenReturn("GetChoreIntent");
    when(mockedIntent.getSlot("choreDate")).thenReturn(mockedDateSlot);
    when(mockedDateSlot.getValue()).thenReturn(null);
    when(mockedDao.scanDB(eq("Due"), any(String.class))).thenReturn(mockedList);
    when(mockedList.size()).thenReturn(1);
    when(mockedList.get(0)).thenReturn(mockedTask);
    when(mockedTask.getChore()).thenReturn("Sweep the chimney. That's right. Sweep the chimney.");


    SpeechletResponse response = speechlet.onIntent(mockedIntentRequest, mockedSession);
    SimpleCard card = (SimpleCard) response.getCard();

    assertThat(((PlainTextOutputSpeech) response.getOutputSpeech()).getText(), equalTo("Your chore is. Sweep the chimney. That's right. Sweep the chimney."));
    assertThat(card.getTitle(), equalTo("Chore requested"));
    assertThat(card.getContent(), equalTo("Your child just asked for today's chore"));
  }

  @Test
  public void testConfirmChoreResponse(){
    when(mockedIntent.getName()).thenReturn("ConfirmChoreIntent");
    when(mockedIntent.getSlot("password")).thenReturn(mockedPasswordSlot);
    when(mockedPasswordSlot.getValue()).thenReturn("1234");
    when(mockedDao.scanDB(eq("password"), any(String.class))).thenReturn(mockedList);
    when(mockedList.size()).thenReturn(1);
    when(mockedList.get(0)).thenReturn(mockedTask);
    when(mockedTask.getDate()).thenReturn("12-12-2016");
    when(mockedTask.getChore()).thenReturn("Shear the sheep");

    SpeechletResponse response = speechlet.onIntent(mockedIntentRequest, mockedSession);
    verify(mockedTask).setIsComplete(true);
    verify(mockedDao).saveToDB(any(Task.class));
    assertThat(((PlainTextOutputSpeech) response.getOutputSpeech()).getText(), equalTo("I've confirmed 12-12-2016 Shear the sheep chore is completed."));
  }

  @Test
  public void testUnableToConfirmChoreResponse(){
    when(mockedIntent.getName()).thenReturn("ConfirmChoreIntent");
    when(mockedIntent.getSlot("password")).thenReturn(mockedPasswordSlot);
    when(mockedPasswordSlot.getValue()).thenReturn("1234");
    when(mockedDao.scanDB(eq("password"), any(String.class))).thenReturn(mockedList);
    when(mockedList.size()).thenReturn(0);

    SpeechletResponse response = speechlet.onIntent(mockedIntentRequest, mockedSession);
    assertThat(((PlainTextOutputSpeech) response.getOutputSpeech()).getText(), equalTo("Unable to confirm password, please try again."));
  }

  @Test
  public void testAddChoreResponse(){
    when(mockedIntent.getName()).thenReturn("AddChoreIntent");
    when(mockedIntent.getSlot("choreDate")).thenReturn(mockedDateSlot);
    when(mockedDateSlot.getValue()).thenReturn("02-03-2016");
    when(mockedIntent.getSlot("chore")).thenReturn(mockedChoreSlot);
    when(mockedChoreSlot.getValue()).thenReturn("Shear the sheep");

    SpeechletResponse response = speechlet.onIntent(mockedIntentRequest, mockedSession);
    SimpleCard card = (SimpleCard) response.getCard();
    verify(mockedDao).saveToDB(any(Task.class));
    assertThat(((PlainTextOutputSpeech) response.getOutputSpeech()).getText(), equalTo("Very well, I have added a Shear the sheep chore for 02-03-2016"));
    assertThat(card.getTitle(), equalTo("02-03-2016 " + "Shear the sheep"));
  }

  @Test
  public void testDoneResponse() {
    when(mockedIntent.getName()).thenReturn("GetDoneIntent");
    when(mockedIntent.getSlot("choreDate")).thenReturn(mockedDateSlot);
    when(mockedDateSlot.getValue()).thenReturn("02-03-2016");
    when(mockedIntent.getSlot("chore")).thenReturn(mockedChoreSlot);
    when(mockedChoreSlot.getValue()).thenReturn("Shear the sheep");
    when(mockedDao.loadFromDB("02-03-2016","Shear the sheep")).thenReturn(mockedTask);
    when(mockedTask.getPassword()).thenReturn("1234");

    SpeechletResponse response = speechlet.onIntent(mockedIntentRequest, mockedSession);
    SimpleCard card = (SimpleCard) response.getCard();

    assertThat(((PlainTextOutputSpeech) response.getOutputSpeech()).getText(), equalTo("Very well, I have informed your appropriate adult."));
    assertThat(card.getTitle(), equalTo("Chore Verification"));
    assertThat(card.getContent(), equalTo("Your child claims to have completed their chore. Here is the password: 1234"));
  }

  @Test
  public void testGetChoreList(){
    when(mockedIntent.getName()).thenReturn("GetChoreListIntent");
    when(mockedDao.getAllChores()).thenReturn(mockedList);
    when(mockedList.iterator()).thenReturn(mockedIterator);
    when(mockedIterator.hasNext()).thenReturn(true).thenReturn(false);
    when(mockedIterator.next()).thenReturn(mockedTask);
    when(mockedTask.getChore()).thenReturn("Clean the gutters");

    SpeechletResponse response = speechlet.onIntent(mockedIntentRequest, mockedSession);

    assertThat(((PlainTextOutputSpeech) response.getOutputSpeech()).getText(), equalTo("Clean the gutters, "));
  }

  @Test
  public void testEasterEggResponse() {
    when(mockedIntent.getName()).thenReturn("ChorematickIntent");

    SpeechletResponse response = speechlet.onIntent(mockedIntentRequest, mockedSession);

    assertThat(((PlainTextOutputSpeech) response.getOutputSpeech()).getText(), equalTo("Go stand in the corner and think about what you've done."));
  }

  @Test
  public void testGetNumberOfCompletedChoresResponse() {
    when(mockedIntent.getName()).thenReturn("NumberOfCompletedChoresIntent");
    when(mockedDao.scanDB(eq("Complete"), any(String.class))).thenReturn(mockedList);
    when(mockedList.size()).thenReturn(5);

    SpeechletResponse response = speechlet.onIntent(mockedIntentRequest, mockedSession);

    verify(mockedList).size();

    assertThat(((PlainTextOutputSpeech) response.getOutputSpeech()).getText(), equalTo("There are 5 completed chores."));
  }
}
