package gcx;

import com.fasterxml.jackson.core.*;
import org.bson.BsonReader;
import org.bson.BsonType;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedList;


/**
 * Created by greg on 29/12/15.
 */

public class CustomBsonParser extends JsonParser {

    private JsonStreamContext context  = new CustomJsonStreamContext();;

    class CustomJsonStreamContext extends JsonStreamContext {

        JsonStreamContext parent;

        @Override
        public JsonStreamContext getParent() {
            return parent;
        }

        @Override
        public String getCurrentName() {
            return valueToken.name;
        }
    }

    static class ValueToken {
        JsonToken jsonToken;
        String name;
        Object value;
        int intValue;
        long longValue;
        double doubleValue;
        String stringValue;



        public String toString(){
            return jsonToken + " " + name;
        }
    }

    public void reset (){
        valueToken.jsonToken = null;
        valueToken.name = null;
        valueToken.value = null;
        valueToken.stringValue = null;
    }

    ValueToken valueToken = new ValueToken();
    //JsonToken typeToken = null;

    private final BsonReader reader;
    private ObjectCodec c;

    public CustomBsonParser(BsonReader reader) {
        this.reader = reader;

        BsonType currentBsonType = reader.readBsonType();
        reader.readStartDocument();
        pushState(State.OBJECT);
/*
        //_id
        currentBsonType = reader.readBsonType();
        String name = reader.readName(); // 1 token
        Object value = reader.readObjectId(); //1 token

        //address
        currentBsonType = reader.readBsonType(); //object
        name = reader.readName(); //1 token
        reader.readStartDocument();

        //address.building
        currentBsonType = reader.readBsonType(); //string
        name = reader.readName(); //1 token
        value = reader.readString(); //1 token

        //address.coord
        currentBsonType = reader.readBsonType(); //array
        name = reader.readName();
        reader.readStartArray();

        //address.coord[0]
        currentBsonType = reader.readBsonType(); //double
        value = reader.readDouble();

        //address.coord[1]
        currentBsonType = reader.readBsonType(); //double
        value = reader.readDouble();

        currentBsonType = reader.readBsonType();//end of array
        reader.readEndArray(); //end array

        //address.street
        currentBsonType = reader.readBsonType(); //string
        name = reader.readName();
        value = reader.readString();

        //address.zipcode
        currentBsonType = reader.readBsonType(); //string
        name = reader.readName();
        value = reader.readString();

        currentBsonType = reader.readBsonType(); //end of document
        reader.readEndDocument();

        //borough
        currentBsonType = reader.readBsonType(); //string
        name = reader.readName();
        value = reader.readString();

        //cuisine
        currentBsonType = reader.readBsonType(); //string
        name = reader.readName();
        value = reader.readString();

        //grades
        currentBsonType = reader.readBsonType(); //begin of array
        name = reader.readName();
        reader.readStartArray();

        //grades[0]
        currentBsonType = reader.readBsonType(); //begin of document
        reader.readStartDocument();

        //grades[0].date
        currentBsonType = reader.readBsonType(); //begin of document
        name = reader.readName();
        value = reader.readDateTime();

        //grades[0].grade
        currentBsonType = reader.readBsonType(); //
        name = reader.readName();
        value = reader.readString();

        //grades[0].score
        currentBsonType = reader.readBsonType(); //
        name = reader.readName();
        value = reader.readInt32();

        currentBsonType = reader.readBsonType(); //end of document
        reader.readEndDocument();

        //grades[1]
        currentBsonType = reader.readBsonType(); //begin of document
        reader.readStartDocument();

        //_id
        name = reader.readName();
   */

    }

    @Override
    public ObjectCodec getCodec() {
        return c;
    }

    @Override
    public void setCodec(ObjectCodec c) {
        this.c = c;

    }

    @Override
    public Version version() {
        return null;
    }

    @Override
    public void close() throws IOException {

    }

    static enum State {
        OBJECT,
        FIELD,
        ARRAY
    }

    LinkedList<State> states = new LinkedList<State>();

    private void pushState(State state) {
        states.push(state);
    }

    private State peek() {
        return states.peek();
    }

    private State poll() {
        return states.poll();
    }

    private boolean isInDocument() {
        return peek() == State.OBJECT;
    }

    private boolean isInField() {
        return peek() == State.FIELD;
    }

    private boolean isInArray() {
        return peek() == State.ARRAY;
    }

    private boolean isInDocumentOrArray() {
        return peek() == State.OBJECT || peek() == State.ARRAY;
    }

    public JsonToken _nextToken() {

        //TODO clean this
        reset();

        if (isInDocument()) {
            BsonType currentBsonType = reader.readBsonType();

            switch (currentBsonType) {
                case END_OF_DOCUMENT:
                    poll();
                    reader.readEndDocument();
                    valueToken.jsonToken = JsonToken.END_OBJECT;
                    break;
                default:
                    valueToken.jsonToken = JsonToken.FIELD_NAME;
                    valueToken.name = reader.readName();
                    pushState(State.FIELD);
            }
            return valueToken.jsonToken;
        }
        if (isInField()) {
            BsonType currentBsonType = reader.getCurrentBsonType();

            handleBsonType(currentBsonType);

            return valueToken.jsonToken;
        }
        if (isInArray()) {
            BsonType currentBsonType = reader.readBsonType();
            switch (currentBsonType) {
                case END_OF_DOCUMENT:
                    poll();
                    reader.readEndArray();
                    valueToken.jsonToken = JsonToken.END_ARRAY;
                    break;
                default:
                    pushState(State.FIELD);
                    handleBsonType(currentBsonType);
            }
            return valueToken.jsonToken;
        }
        return JsonToken.END_OBJECT;//we are done !!
    }

    private void handleBsonType(BsonType currentBsonType) {
        switch (currentBsonType) {
            case ARRAY:
                reader.readStartArray();
                poll();
                pushState(State.ARRAY);
                valueToken.jsonToken = JsonToken.START_ARRAY;
                break;
            case DOCUMENT:
                reader.readStartDocument();
                poll();
                pushState(State.OBJECT);
                valueToken.jsonToken = JsonToken.START_OBJECT;
                break;
            case OBJECT_ID:
                valueToken.jsonToken = JsonToken.VALUE_EMBEDDED_OBJECT;
                valueToken.value = reader.readObjectId();
                poll();
                break;
            case STRING:
                valueToken.jsonToken = JsonToken.VALUE_STRING;
                valueToken.stringValue = reader.readString();
                poll();
                break;
            case DOUBLE:
                valueToken.jsonToken = JsonToken.VALUE_NUMBER_FLOAT;
                valueToken.doubleValue = reader.readDouble();
                poll();
                break;
            case INT32:
                valueToken.jsonToken = JsonToken.VALUE_NUMBER_INT;
                valueToken.intValue = reader.readInt32();
                poll();
                break;
            case INT64:
                valueToken.jsonToken = JsonToken.VALUE_NUMBER_INT;
                valueToken.longValue = reader.readInt64();
                poll();
                break;
            case DATE_TIME:
                valueToken.jsonToken = JsonToken.VALUE_EMBEDDED_OBJECT;
                valueToken.longValue = reader.readDateTime();
                poll();
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public JsonToken nextToken() throws IOException, JsonParseException {

        return _nextToken();
    }

    @Override
    public JsonToken nextValue() throws IOException, JsonParseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public JsonParser skipChildren() throws IOException, JsonParseException {
        return this; //throw new UnsupportedOperationException();
    }

    @Override
    public boolean isClosed() {
        throw new UnsupportedOperationException();
    }

    @Override
    public JsonToken getCurrentToken() {
       return valueToken.jsonToken;
    }

    @Override
    public int getCurrentTokenId() {
        JsonToken jsonToken = getCurrentToken();
        return jsonToken.id();
    }

    @Override
    public boolean hasCurrentToken() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasTokenId(int id) {
        if (id == 5 && valueToken != null && valueToken.name != null)
            return true;
        return getCurrentTokenId() == id;
    }

    @Override
    public boolean hasToken(JsonToken t) {
        return valueToken.jsonToken == t;
    }

    @Override
    public String getCurrentName() throws IOException {
        return valueToken.name;
    }

    @Override
    public JsonStreamContext getParsingContext() {
        return context;
    }

    @Override
    public JsonLocation getTokenLocation() {
        return new JsonLocation(this,1,-1,-1);
    }

    @Override
    public JsonLocation getCurrentLocation() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearCurrentToken() {
        //throw new UnsupportedOperationException();
    }

    @Override
    public JsonToken getLastClearedToken() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void overrideCurrentName(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getText() throws IOException {
        return valueToken.stringValue;
    }

    @Override
    public char[] getTextCharacters() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getTextLength() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getTextOffset() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasTextCharacters() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Number getNumberValue() throws IOException {
        return valueToken.doubleValue;
    }

    @Override
    public NumberType getNumberType() throws IOException {

        switch(reader.getCurrentBsonType()){
            case INT32:
                return NumberType.INT;
            case DOUBLE:
                return NumberType.DOUBLE;
            default:
                return NumberType.INT;
        }
    }

    @Override
    public int getIntValue() throws IOException {
        return valueToken.intValue;
    }

    @Override
    public long getLongValue() throws IOException {
        return valueToken.longValue;
    }

    @Override
    public BigInteger getBigIntegerValue() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public float getFloatValue() throws IOException {
        return (float) valueToken.doubleValue;
    }

    @Override
    public double getDoubleValue() throws IOException {
        return valueToken.doubleValue;
    }

    @Override
    public BigDecimal getDecimalValue() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getEmbeddedObject() throws IOException {
        return valueToken.value;
    }

    @Override
    public byte[] getBinaryValue(Base64Variant bv) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getValueAsString(String def) throws IOException {
        return valueToken.stringValue;
    }
}
