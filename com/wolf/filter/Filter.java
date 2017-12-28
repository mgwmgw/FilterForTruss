/**
 * See ReadMeFromTruss.md for a description of the functionality of this class.
 */
package com.wolf.filter;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Filter {
    static String separator = ",";
    // not Thread safe if multiple Threads share a single instance
    List<String> quotedStrings;
    // not Thread safe if multiple Threads share a single instance
    Duration totalDuration;
    /**
     * Replace quoted Strings with indices into the quotedStrings array,
     * so commas in quoted Strings won't break split.
     * This works with multiple quoted Strings, although current specs
     * do not require this ability.
     * @param line the input text
     * @return the text with the quoted Strings replaced by quotedString(index)
     */
    protected String replaceQuotedStrings(String line) {
        String regex = "(\"[^\"]*\")"; 
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            int index = this.quotedStrings.size();
            this.quotedStrings.add(matcher.group());
            line = matcher.replaceFirst("quotedString(" + String.valueOf(index) + ")");
            return this.replaceQuotedStrings(line);
        }
        return line;
    }
    static DateTimeFormatter pacific = DateTimeFormatter.ofPattern("M/d/yy h:mm:ss a").
           withZone(ZoneId.of("US/Pacific"));
    static DateTimeFormatter eastern = DateTimeFormatter.ofPattern("M/d/yy h:mm:ss a").
           withZone(ZoneId.of("US/Eastern"));
    /**
     * Parse the timestamp assuming Pacific time, and format it to eastern time.
     * @param data the input to process
     * @return String representing the processed data (eastern time zone).
     * Java thinks the standard separates dates using - rather than /, which is why
     * a custom pattern is used here instead of a constant that implements the standard.
     */
    protected String fixTimestamp(String data) {
        return ZonedDateTime.parse(data, pacific).format(eastern);
    }
    static DecimalFormat zipDecimalFormat = new DecimalFormat("00000");
    /**
     * Zip code is assumed to be an integer.  Non-digits will cause an Exception to be thrown.
     * Integers with less than 5 digits are padded with zeroes on the left.  
     * This code does not handle 9 digit zip codes.
     * @param data the input to be processed
     * @return a String with 0 padding
     */
    protected String fixZip(String data) {
        return zipDecimalFormat.format(Integer.parseInt(data));
    }
    /**
     * Data from Name columns is converted to all uppercase.
     * @param data the input to be processed
     * @return the uppercase name data
     */
    protected String fixName(String data) {
        return data.toUpperCase();
    }
    /**
     * Converts data to Duration in seconds.  Also adds this duration to totalDuration.
     * @param data the input to be processed
     * @return seconds of duration in String format
     */
    protected String fixDuration(String data) {
        String regex = "(\\d+):(\\d+):(\\d+).(\\d+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(data);
        if (false == matcher.matches()) {
            throw new NumberFormatException("Unrecognized duration format:" + data);
        }
        Duration duration = Duration.ofSeconds(0);
        int index = 1;
        duration = duration.plusHours(Long.parseLong(matcher.group(index++)));
        duration = duration.plusMinutes(Long.parseLong(matcher.group(index++)));
        duration = duration.plusSeconds(Long.parseLong(matcher.group(index++)));
        duration = duration.plusMillis(Long.parseLong(matcher.group(index++)));
        this.totalDuration = this.totalDuration.plus(duration);
        return String.valueOf(duration.getSeconds()) + "." + 
                String.valueOf(duration.getNano() / 1000000);
    }
    static DecimalFormat durationDecimalFormat = new DecimalFormat("000");
    /**
     * @return return the sum of durations calculated in fixDuration calls as seconds
     * and milliseconds.  It is assumed that TotalDuration is to the right of all
     * other Duration columns.
     */
    protected String fixTotalDuration() {
        return String.valueOf(this.totalDuration.getSeconds()) + "." + 
               durationDecimalFormat.format(this.totalDuration.getNano() / 1000000);
    }
    /**
     * Replace the input column content with appropriate output.
     * Delegate to appropriate method according to the column heading.
     * This method returns after it has found the right column.
     * It also reinserts quoted strings previously removed.
     * Restrictions on which columns may contain quoted strings
     * could be added here.  For columns where a specific format
     * is required, that checking would already reject quoted strings
     * because the quotation marks are currently left in place.
     * Making the treatment of quotation marks specific to column
     * could also be added here.
     * @param columnHead column heading
     * @param data content of that column to process
     * @return the result of fixing the data.
     */
    protected String fix(String columnHead, String data) {
        String regex = "quotedString\\((\\d+)\\)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(data);
        if (matcher.matches()) {
            data = this.quotedStrings.get(Integer.parseInt(matcher.group(1)));
        }
        if (columnHead.contains("Timestamp")) {
            return(this.fixTimestamp(data));
        }
        if (columnHead.contains("ZIP")) {
            return(this.fixZip(data));
        }
        if (columnHead.contains("Name")){
            return(this.fixName(data));
        }
        if (columnHead.equals("TotalDuration")) {
            return(this.fixTotalDuration());
        }
        if (columnHead.contains("Duration")) {
            return this.fixDuration(data);
        }
        // By default, no change to the input is made here, because replacement
        // of invalid characters is done earlier.
        return data;
    }
    
    /**
     * Shared code between testing and command-line filter methods, does most of the work.
     * @param inScanner input text
     * @param outWriter where to write processed output
     * @param errWriter where to write error messages
     * @throws EOFException if no column headings found
     */
    protected void filter(Scanner inScanner, PrintWriter outWriter, PrintWriter errWriter) throws EOFException {
        if (false == inScanner.hasNextLine()) {
            throw new EOFException("No column headings found.");
        }
        String columnLine = inScanner.nextLine();
        outWriter.println(columnLine);
        List<String> columnHeadList = Arrays.asList(columnLine.split(separator));
        while(inScanner.hasNextLine()) {
            String lineAsRead = inScanner.nextLine();
            // Where output is stored until we know whether it is valid.
            StringWriter fixedLine = new StringWriter();
            try {
                this.quotedStrings  = new ArrayList<String>();
                this.totalDuration = Duration.ofSeconds(0);
                String withoutQuotes = this.replaceQuotedStrings(lineAsRead);
                List<String>dataList = Arrays.asList(withoutQuotes.split(separator));
                Iterator<String> columnHeadIterator = columnHeadList.iterator();
                Iterator<String> dataIterator = dataList.iterator();
                while(columnHeadIterator.hasNext()) {
                    String columnHead = columnHeadIterator.next();
                    String data;
                    if (dataIterator.hasNext()) {
                        data = dataIterator.next();
                    }
                    else {
                        data = "";
                    }
                    String fixedData = this.fix(columnHead, data);
                    fixedLine.write(fixedData);
                    if (dataIterator.hasNext()) {
                        fixedLine.write(separator);
                    }
                }
                while(dataIterator.hasNext()) {
                    fixedLine.write(dataIterator.next());
                    if (dataIterator.hasNext()) {
                        fixedLine.write(separator);
                    }
                }
                outWriter.println(fixedLine.toString());
            }
            // Throwing an exception skips println to outWriter so bad rows are not in the output.
            catch(Exception e) {
                errWriter.println(e.getMessage());
            }
        }
        outWriter.flush();
        errWriter.flush();
    }
    /**
     * Would be used for testing
     * @param inString input text
     * @param outWriter where to write processed output
     * @param errWriter where to write error messages
     * @throws IOException when no column headings found
     */
    public void filter(String inString, StringWriter outWriter, StringWriter errWriter) 
    throws IOException {
        this.filter(new Scanner(new String(inString.getBytes(), StandardCharsets.UTF_8)), 
            new PrintWriter(outWriter), new PrintWriter(errWriter));
    }
    /**
     * Used in command-line
     * @throws IOException when no column headings found.
     */
    public void filter() throws IOException {
        this.filter(new Scanner(System.in, StandardCharsets.UTF_8.name()), 
             new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8)),
             new PrintWriter(new OutputStreamWriter(System.err, StandardCharsets.UTF_8)));
    }

    public static void main(String[] args) {
        try {
            new Filter().filter();
        }
        catch(Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}
