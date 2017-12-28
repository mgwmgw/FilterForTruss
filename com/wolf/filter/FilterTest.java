/**
 * 
 */
package com.wolf.filter;

import static org.junit.Assert.*;

import java.io.StringWriter;

import org.junit.Test;

/**
 * @author admin
 *
 */
public class FilterTest {

    @Test
    public void testQuotedStringsAndCapitalizeNames() {
        String input = "1,2Name,3,Name4\n1x,2y,\"a,b,c\",\"d,e,f\"\n";
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        try {
            new Filter().filter(input, out, err);
            assertEquals(out.toString(), "1,2Name,3,Name4\n1x,2Y,\"a,b,c\",\"D,E,F\"\n");
            assertEquals(err.toString(), "");
        }
        catch(Exception e) {
            fail(e.getMessage());
        } 
    }
    
    @Test
    public void testTimestamp() {
        String input = "1,2Timestamp,3,Timestamp4\n1,4/1/11 11:00:00 AM,3,12/31/16 11:59:59 PM\n1,notTimestamp,2,3,4\n";
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        try {
            new Filter().filter(input, out, err);
            assertEquals(out.toString(), "1,2Timestamp,3,Timestamp4\n1,4/1/11 2:00:00 PM,3,1/1/17 2:59:59 AM\n");
            assertEquals(err.toString(), "Text 'notTimestamp' could not be parsed at index 0\n");
        }
        catch(Exception e) {
            fail(e.getMessage());
        } 
    }

    @Test
    public void testZIP() {
        String input = "1,2ZIP,3ZIP,ZIP4\n1,42,123456,12345\n1,notZIP,2,3,4\n";
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        try {
            new Filter().filter(input, out, err);
            assertEquals(out.toString(), "1,2ZIP,3ZIP,ZIP4\n1,00042,123456,12345\n");
            assertEquals(err.toString(), "For input string: \"notZIP\"\n");
        }
        catch(Exception e) {
            fail(e.getMessage());
        } 
    }
    
    @Test
    public void testDuration() {
        String input = "1,2Duration,Duration3,TotalDuration\n1,1:23:32.123,1:32:33.123,notADuration\n1,notADuration,2,3,4\n";
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        try {
            new Filter().filter(input, out, err);
            assertEquals(out.toString(), "1,2Duration,Duration3,TotalDuration\n1,5012.123,5553.123,10565.246\n");
            assertEquals(err.toString(), "Unrecognized duration format:notADuration\n");
        }
        catch(Exception e) {
            fail(e.getMessage());
        } 
    }
    
    @Test
    public void testCharacters() {
        // Character.toString((char) 65533)
        String input = "1,2,3\n\u00e8,\u263a,\u2211";
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        try {
            new Filter().filter(input, out, err);
            assertEquals(out.toString(), "1,2,3\n\u00e8,\u263a,\u2211\n");
            assertEquals(err.toString(), "");
        }
        catch(Exception e) {
            fail(e.getMessage());
        } 
    }
}
