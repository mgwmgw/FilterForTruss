To understand what this program does, see ReadMeFromTruss.md.

To understand the sample*csv input files, 
import into a spreadsheet such as the one in LibreOffice is recommended.
This is done one sheet at a time under the Sheet menu.

The main source file is Filter.java.

This was developed in Eclipse Mars on OSX 10.11.6 using Java 8.
It depends on classes not present in Java 7 or earlier,
but not on any external libraries.
The different handling of classpath configuration in Java 9
means changes would be needed to run it there.

It has not been tested on Linux or newer OSX, but should work on both.
Please test it on OSX.

cd src
javac com/wolf/filter/Filter.java 
cat sample-with-broken-utf8.csv | java -cp . com.wolf.filter.Filter >out1 2>err1
cat sample.csv | java -cp . com.wolf.filter.Filter >out2 2>err2

The unit tests are in FilterTest.java.

In order to compile and run the junit tests, 
you need to know where your junit.jar and hamcrest jar files are,
because the tests depend on them.  
This was developed using junit 4 and has not been tested using other
versions.  Nothing specific to Junit 4 was used as far as I know.
It is known that the command-line for junit 3 is different.

You can find hamcrest, which includes version-specific information in the jar name, using
sudo find / -name "*hamcrest*jar" -print

These jars are typically installed as part of Eclipse and IntelliJ on a developer's machine.
Import into your favorite IDE should also work.

javac -cp .:/path/to/junit.jar com/wolf/filter/FilterTest.java
java -cp .:/path/to/junit.jar:/path/to/org.hamcrest...jar org.junit.runner.JUnitCore com.wolf.filter.FilterTest
