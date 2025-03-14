package one.jpro.platform.mdfx;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class MDFXUtilTest {

    @Test
    public void testSingleChapterNoSubchapters() {
        String md = "# Chapter 1\n" +
                "Some introduction text.\n" +
                "More text...";

        List<MDFXUtil.Chapter> chapters = MDFXUtil.getChapters(md);
        Assertions.assertEquals(1, chapters.size());

        MDFXUtil.Chapter ch = chapters.get(0);
        Assertions.assertEquals(1, ch.getIndex());
        Assertions.assertEquals("Chapter 1", ch.getHeadingText());

        // Check full MD
        String fullMD = ch.getFullMD();
        Assertions.assertTrue(fullMD.contains("# Chapter 1"));
        Assertions.assertTrue(fullMD.contains("Some introduction text."));

        // No subchapters
        Assertions.assertTrue(ch.getSubchapters().isEmpty());
    }

    @Test
    public void testChapterWithSubchapters() {
        String md = "# Main Chapter\n" +
                "Intro for main chapter\n" +
                "## Sub A\n" +
                "Content for sub A\n" +
                "## Sub B\n" +
                "Content for sub B\n";

        List<MDFXUtil.Chapter> chapters = MDFXUtil.getChapters(md);
        Assertions.assertEquals(1, chapters.size(), "Should have 1 chapter");

        MDFXUtil.Chapter chapter = chapters.get(0);
        Assertions.assertEquals(1, chapter.getIndex());
        Assertions.assertEquals("Main Chapter", chapter.getHeadingText());

        // Check subchapters
        List<MDFXUtil.Subchapter> subs = chapter.getSubchapters();
        Assertions.assertEquals(2, subs.size(), "Should have 2 subchapters");

        MDFXUtil.Subchapter subA = subs.get(0);
        Assertions.assertEquals(1, subA.getIndex());
        Assertions.assertEquals("Sub A", subA.getHeadingText());
        Assertions.assertTrue(subA.getContent().contains("Content for sub A"));

        MDFXUtil.Subchapter subB = subs.get(1);
        Assertions.assertEquals(2, subB.getIndex());
        Assertions.assertEquals("Sub B", subB.getHeadingText());
        Assertions.assertTrue(subB.getContent().contains("Content for sub B"));
    }

    @Test
    public void testMultipleChapters() {
        String md = "# Chapter One\n" +
                "Text for chapter one.\n" +
                "## Sub-1\n" +
                "Text for sub-1.\n" +
                "# Chapter Two\n" +
                "Text for chapter two.\n" +
                "## Sub-2\n" +
                "Text for sub-2.\n";

        List<MDFXUtil.Chapter> chapters = MDFXUtil.getChapters(md);
        Assertions.assertEquals(2, chapters.size(), "Should have 2 chapters");

        // Chapter 1 checks
        MDFXUtil.Chapter ch1 = chapters.get(0);
        Assertions.assertEquals(1, ch1.getIndex());
        Assertions.assertEquals("Chapter One", ch1.getHeadingText());
        Assertions.assertTrue(ch1.getContent().contains("Text for chapter one."));
        Assertions.assertEquals(1, ch1.getSubchapters().size());
        Assertions.assertEquals("Sub-1", ch1.getSubchapters().get(0).getHeadingText());

        // Chapter 2 checks
        MDFXUtil.Chapter ch2 = chapters.get(1);
        Assertions.assertEquals(2, ch2.getIndex());
        Assertions.assertEquals("Chapter Two", ch2.getHeadingText());
        Assertions.assertTrue(ch2.getContent().contains("Text for chapter two."));
        Assertions.assertEquals(1, ch2.getSubchapters().size());
        Assertions.assertEquals("Sub-2", ch2.getSubchapters().get(0).getHeadingText());
    }
}
