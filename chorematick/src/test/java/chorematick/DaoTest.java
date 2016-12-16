package chorematick;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class DaoTest extends BaseTestCase {

  private Dao dao;

  @Mock private DynamoDBMapper mockedMapper;
  @Mock private Task mockedTask;

  @Before
  public void setup() {
    dao = new Dao(mockedMapper);
  }

  @Test
  public void scanTestWithStringValue() {
    dao.scanDB("Due", "2016-12-15");
    verify(mockedMapper).scan(eq(Task.class),any(DynamoDBScanExpression.class));
  }

  @Test
  public void scanTestWithNumberValue() {
    dao.scanDB("Complete", "1");
    verify(mockedMapper).scan(eq(Task.class),any(DynamoDBScanExpression.class));
  }

  @Test
  public void loadTest(){
    dao.loadFromDB("2016-12-13", "Sweep the chimney");
    verify(mockedMapper).load(eq(Task.class), any(String.class), any(String.class));
  }

  @Test
  public void saveTest(){
    dao.saveToDB(mockedTask);
    verify(mockedMapper).save(any(Task.class));
  }

  @Test
  public void allChoresTest(){
    dao.getAllChores();
    verify(mockedMapper).scan(eq(Task.class),any(DynamoDBScanExpression.class));
  }
}
