package com.milaboratory.minnn.correct;

import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import org.junit.*;

import java.util.*;

import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.correct.CorrectionUtils.*;
import static org.junit.Assert.*;

public class CorrectionUtilsTest {
    @Test
    public void multipleSequencesMergedTest() {
        LinkedHashMap<List<NSequenceWithQuality>, NSequenceWithQuality> testData = new LinkedHashMap<>();
        testData.put(Arrays.asList(
                new NSequenceWithQuality("AAA", "###"),
                new NSequenceWithQuality("ATA", "3C3"),
                new NSequenceWithQuality("GTC", "111")),
                new NSequenceWithQuality("ATA", ",[,"));
        testData.put(Arrays.asList(
                new NSequenceWithQuality(new NucleotideSequence("ATTAGACA"), DEFAULT_MAX_QUALITY),
                new NSequenceWithQuality(new NucleotideSequence("ATTAGACA"), DEFAULT_MAX_QUALITY)),
                new NSequenceWithQuality(new NucleotideSequence("ATTAGACA"), DEFAULT_MAX_QUALITY));
        testData.put(Arrays.asList(
                new NSequenceWithQuality("WDNNNGWCCCAGTBAAAAGCA"),
                new NSequenceWithQuality("ATTCCCGKNNNNNNNNNNNNN"),
                new NSequenceWithQuality("NNNNNNAAAABBBGGGGGGTT")),
                new NSequenceWithQuality("ATTCCGAAAAAGTGAAAAGTA", "D9444\"-!\"\"\"999\"\"\"\"[\"\""));
        testData.put(Collections.singletonList(
                new NSequenceWithQuality("ATTAGACA", "12345678")),
                new NSequenceWithQuality("ATTAGACA", "12345678"));
        testData.put(Arrays.asList(
                new NSequenceWithQuality("AHABATYANGNHTRAGNDGRADAGAADDA",
                        "597349AA8FNW54#%^385703583476"),
                NSequenceWithQuality.EMPTY,
                new NSequenceWithQuality("BBABBHHYAANAAAABBBNNNNNA",
                        "1597349A8F4#%^5703583476"),
                new NSequenceWithQuality("RAGCDGRADAGAACDA",
                        "FNW54#%^38570476"),
                new NSequenceWithQuality("AHABATYABGBHTRAGBDGRADAGAABDA",
                        "597349AA8FNW54#%^385703583476"),
                new NSequenceWithQuality("ATA", ",[,")),
                new NSequenceWithQuality("ATACATTAAGGATAAATTGAAAAGAATAA",
                        "S&G&-G![!(!!.E-!!!8!8!.$3)!!/"));
        for (HashMap.Entry<List<NSequenceWithQuality>, NSequenceWithQuality> currentTestData : testData.entrySet())
            assertEquals(currentTestData.getValue(), multipleSequencesMerged(currentTestData.getKey()));
    }
}
