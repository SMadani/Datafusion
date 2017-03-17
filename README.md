# Datafusion

Datafusion is an Android application which, in the broadest sense, is a data monitor. It takes as input a URL of a CSV, XML or plain text (TXT) file and, provided it is in the “correct” format (as specified by my application), it will monitor numerical values in the file. The user can then, for each file, select which values they would like to monitor by providing a target value, low threshold and/or high threshold (all three are optional). When the value meets (as in the case of a target), exceeds (as in the case of high threshold) or falls below (as with low threshold) the specified value, the user will receive a notification.

For example data, see the "test/res" directory.

Please note that the resource a given URL points to must be a static text file in one of the pre-defined formats (currently, .txt, .csv or .xml), and this must be accessible from the device without any additional authentication.

Datafusion simply polls a static resource on a period defined by the user, and looks for changes in the values. Since the data from all provided resources are also stored in a local SQLite database, the user can view the values even when the resource is not accessible.

Disclaimer: This is NOT and open-source project, and default copyright laws apply. You may not copy or distribute this work without permission; especially if it's for commercial gain. If you are interested in using this application and/or developing it or have suggestions, please get in touch with me :)
