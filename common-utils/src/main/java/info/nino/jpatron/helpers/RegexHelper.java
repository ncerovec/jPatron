package info.nino.jpatron.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexHelper
{
    /*
    private boolean wildcardListContains(List<String> wildcards, String key)
    {
        for(String wildcard : wildcards)
        {
            if(key.matches(wildcard)) return true;
        }

        return false;
    }
    */

    public static List<String> compileRegexWildcards(String... wildcardList)
    {
        List<String> regexWildcards = null;

        if(wildcardList != null)
        {
            regexWildcards = new ArrayList<>();

            for(String wildcard : wildcardList)
            {
                regexWildcards.add(RegexHelper.compileRegexWildcard(wildcard));
            }
        }

        return regexWildcards;
    }

    public static String compileRegexWildcard(String wildcard)
    {
        //regex: https://www.rexegg.com/regex-quickstart.html
        //source: https://stackoverflow.com/questions/24337657/wildcard-matching-in-java

        String wildcardRegex = null;

        if(wildcard != null)
        {
            StringBuffer b = new StringBuffer();
            Pattern regex = Pattern.compile("[^.*]+|(\\.)$|(\\.\\*)");
            Matcher m = regex.matcher(wildcard);
            while(m.find())
            {
                //TODO ADD: '!<path>.*' ('!.field', '!.field.*') => exclude path
                if(m.group(1) != null) m.appendReplacement(b, "\\\\.[^.]*");    //'<path>.' ('.', 'field.') => all root level fields under path
                else if(m.group(2) != null) m.appendReplacement(b, ".*");       //'<path>.*' ('.*', 'field.*') => all level fields under path
                else m.appendReplacement(b, "\\\\Q" + m.group(0) + "\\\\E");    //'<path>' ('field', 'field.subfield') => exact field path

                //NOTICE: Java Regex literal string: Pattern.quote("<string>") = "\\Q" + "<string>" + "\\E"
            }
            m.appendTail(b);

            wildcardRegex = b.toString();
            //System.out.println("Compiled wildcardRegex: " + wildcardRegex);
        }

        return wildcardRegex;
    }
}
