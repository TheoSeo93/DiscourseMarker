
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DMDetector {
    private static final String ANNOTATORS = "tokenize, ssplit, parse, lemma, ner";
    private static StanfordCoreNLP pipeline = new StanfordCoreNLP(
            new Properties() {
                private static final long serialVersionUID = -4554276601642784806L;
                {
                    put("annotators", ANNOTATORS);
                }
            }
    );

    public static int countPattern(String regex, String treeString){
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(treeString);
        int count = 0 ;

        while(matcher.find())
            count += 1;

        return count;
    }

    public static String findMatchingWord(String regex, String treeString){
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(treeString);

        if(matcher.find())
            return matcher.group();

        else return null;
    }
    public static StringBuilder load(final String file) throws IOException {
        final InputStream stream = DMDetector.class.getClassLoader().getResourceAsStream(file);
        final StringBuilder sb = new StringBuilder();
        final BufferedReader r = new BufferedReader(new InputStreamReader(stream));
        String temp;
        while((temp = r.readLine()) != null) {
            sb.append(temp);
            sb.append(' ');
        }
        r.close();
        return sb;
    }
    public static Set<String> detectDiscourseMarker(String input) {
        Set<String> discourseMarkers = new HashSet<>();
        Annotation doc = new Annotation(input);
        pipeline.annotate(doc);
        List<CoreMap> sentences = doc.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
            String treeString = tree.toString();
            int rootCount = countPattern("ROOT", treeString);
            if (rootCount == 1) {
                int sCount = countPattern("\\(S[\\n ]", treeString);
                if (sCount == 1) {
                    String dm = findMatchingWord("\\(ADVP \\(RB (.*?)\\)", treeString);
                    if (dm != null)
                        discourseMarkers.add(dm);

                    dm = findMatchingWord("\\(CC(.*)\\(PP \\((TO|IN) (.*?)\\)", treeString);
                    if (dm != null)
                        discourseMarkers.add(dm);

                } else if (sCount >= 2) {
                    String dm  = findMatchingWord("\\(CC (.*?)\\)|\\(ADVP \\(RB (.*?)\\)|\\(WHNP \\(WDT (.*?)\\)", treeString);
                    if (dm == null) {
                        // 2S + no ADVP ()
                        dm = findMatchingWord("SBAR", treeString);
                        if (dm == null) {
                            // 2S + no SBAR (())
                            int matchCount = countPattern("\\(CC", treeString);
                            if (matchCount == 0) {
                                matchCount = countPattern("\\(IN", treeString);
                                if (matchCount > 0)
                                    discourseMarkers.add(findMatchingWord("\\(IN (.*?)\\)", treeString));

                            } else
                                discourseMarkers.add(findMatchingWord("\\(CC (.*?)\\)", treeString));

                        }
                    } else
                        discourseMarkers.add(dm);
                }
            }
        }
        return discourseMarkers;
    }
    public static void main(String[] args) throws IOException {
        StringBuilder input = load("Test2");
        Set<String> rs = detectDiscourseMarker(input.toString());
        for(String can : rs){
            System.out.println(can);
        }
    }
}
