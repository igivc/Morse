package igivc.morse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TextToMorseProcessor class for processing text to morse code.
 */
class TextToMorseProcessor {
    /**
     * The Morse codes map
     */
    private static final String MORSECODES = "morsecodes";
    private Map<Integer, String> morseCodes;

    /**
     * Constructor
     */
    TextToMorseProcessor() {
        try {
            init();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Initialize the Morse codes map
     *
     * @throws IOException if an I/O error occurs
     */
    private void init() throws IOException {
        // get the input stream from the resource MORSECODES
        var inputStream = getClass().getClassLoader().getResourceAsStream(MORSECODES);
        // check input stream is not null
        if (inputStream == null) {
            throw new NullPointerException("Resource " + MORSECODES + " not found");
        }
        // read the lines from the input stream
        try (var bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            // create stream of lines
            Stream<String> lines = bufferedReader.lines();
            Map<Integer, String> map = lines.collect( // collect the lines into a map container
                    // typical pairs look like that:
                    // A.-
                    // B-...
                    // C-.-.
                    // ...
                    // there is no space between the key and the value
                    Collectors.toMap(
                            // key for the map
                            // convert the first character to uppercase and get the equivalent integer value
                            s -> Integer.valueOf(Character.toUpperCase(s.charAt(0))),
                            // value for the map
                            // get the substring skipping the very first character (we count from 0)
                            s -> s.substring(1)));
            this.morseCodes = Collections.unmodifiableMap(map); // create an unmodifiable map
        }
    }

    /**
     * Get the morse codes map
     *
     * @return the morse codes map
     */
    Map<Integer, String> getMorseCodes() {
        return morseCodes;
    }

    /**
     * Convert text to morse code
     *
     * @param text the text to convert
     * @return the morse code string, for example: ".-|...|.-."
     */
    String textToMorse(String text) {
        return text
            // convert the text to uppercase
            .toUpperCase()
            // convert the text to a stream of characters
            .chars()
            // convert the stream of characters to a stream of strings
            .mapToObj((c) -> {
                if (c == ' ') { // if the character is a space
                    return " "; // return a space
                }
                // get the morse code for the character
                String code = morseCodes.get(c);
                // return the morse code or a space if the code is not found (==null)
                return code != null ? code : " ";
            })
            .collect(
                Collector.<String, ArrayList<String>, String>of(
                    // Supplier: Creates a new ArrayList
                    () -> {
                        // reserve space for strings
                        return new ArrayList<String>(text.length());
                    },
                    // Accumulator: Adds strings to the list skipping duplicated spaces
                    (list, s) -> {
                        if (!s.equals(" ") || (!list.isEmpty() && !list.getLast().equals(" "))) {
                            list.addLast(s);
                        }
                    },
                    // Combiner: Combines two list (for parallel streams)
                    (list1, list2) -> {
                        list1.addAll(list2);
                        return list1;
                    },
                    // Finisher: Joins the list elements into a single string
                    acc -> {
                        if (!acc.isEmpty()) {
                            int to = acc.getLast().equals(" ") ? acc.size() - 1 : acc.size();
                            return String.join("|", acc.subList(0, to));
                        }
                        return "";
                    }
                )
            );
    }
}
