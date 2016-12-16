package chorematick;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;


public class Dao {

  private DynamoDBMapper mapper;

  public Dao(DynamoDBMapper mapper) {
    this.mapper = mapper;
  }

  public List<Task> scanDB(String columnName, String value) {
    Map<String, String> attributeNames = new HashMap<String, String>();
    attributeNames.put("#columnName", columnName);

    Map<String, AttributeValue> attributeValues = new HashMap<String, AttributeValue>();
    if(value.equals("1")) {
      attributeValues.put(":Value", new AttributeValue().withN(value));
    } else {
      attributeValues.put(":Value", new AttributeValue().withS(value));
    }
    DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
    .withFilterExpression("#columnName = :Value")
    .withExpressionAttributeNames(attributeNames)
    .withExpressionAttributeValues(attributeValues);

    return mapper.scan(Task.class, scanExpression);
  }

  public Task loadFromDB(String choreDate, String chore){
    return mapper.load(Task.class, choreDate, chore);
  }

  public void saveToDB(Task task){
    mapper.save(task);
  }

  public List<Task> getAllChores(){
    DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
    return  mapper.scan(Task.class, scanExpression);
  }
}
