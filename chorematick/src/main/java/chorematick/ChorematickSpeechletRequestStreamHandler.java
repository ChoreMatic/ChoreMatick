package chorematick;

import java.util.HashSet;
import java.util.Set;

import com.amazon.speech.speechlet.lambda.SpeechletRequestStreamHandler;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;

public final class ChorematickSpeechletRequestStreamHandler extends SpeechletRequestStreamHandler {
    private static final Set<String> supportedApplicationIds = new HashSet<String>();
    static {
        supportedApplicationIds.add("amzn1.ask.skill.6ff2efd2-c737-49e4-9dc2-6c33dd33eb9d");
    }

    private static final AmazonDynamoDBClient client = new AmazonDynamoDBClient();
    private static final DynamoDBMapper mapper = new DynamoDBMapper(client);
    private static final Dao dao = new Dao(mapper);

    public ChorematickSpeechletRequestStreamHandler() {
        super(new ChorematickSpeechlet(dao), supportedApplicationIds);

    }
}
