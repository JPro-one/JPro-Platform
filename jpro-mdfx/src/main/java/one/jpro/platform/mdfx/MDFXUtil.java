package one.jpro.platform.mdfx;

import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for splitting Markdown text into Chapters and Subchapters.
 */
public class MDFXUtil {

    /**
     * Parses the given Markdown string and splits it into a list of Chapter objects.
     * Level 1 headings (#) are treated as Chapters.
     * Level 2 headings (##) are treated as Subchapters.
     *
     * @param markdown the input Markdown string
     * @return a list of Chapter objects, each containing zero or more Subchapters
     */
    public static List<Chapter> getChapters(String markdown) {
        // Build a Flexmark parser
        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdown);

        List<Chapter> chapters = new ArrayList<>();
        Chapter currentChapter = null;
        int chapterIndex = 0;

        for (Node node = document.getFirstChild(); node != null; node = node.getNext()) {
            // We only look at Heading nodes or anything else (Paragraph, etc.)
            if (node instanceof Heading) {
                Heading heading = (Heading) node;
                int level = heading.getLevel();
                String headingText = heading.getText().toString().trim();

                if (level == 1) {
                    // Start a new Chapter
                    chapterIndex++;
                    currentChapter = new Chapter(chapterIndex, headingText);
                    chapters.add(currentChapter);

                } else if (level == 2) {
                    // Start a new Subchapter
                    if (currentChapter == null) {
                        // If there's no Chapter yet, we create one so we don't lose content
                        chapterIndex++;
                        currentChapter = new Chapter(chapterIndex, "Unnamed Chapter");
                        chapters.add(currentChapter);
                    }
                    currentChapter.addSubchapter(headingText);
                } else {
                    // For heading level 3+ we ignore or you can handle differently if needed
                }
            } else {
                // If it's not a heading, it's part of the current chapter/subchapter content
                if (currentChapter != null) {
                    currentChapter.appendContentToCurrentSubchapterOrChapter(node.getChars().toString());
                }
            }
        }

        return chapters;
    }

    // --------------------------------------------------
    // Inner classes: Chapter, Subchapter
    // --------------------------------------------------

    /**
     * Represents a top-level Chapter with an index, a heading, and optional subchapters.
     */
    public static class Chapter {
        private final int index;
        private final String headingText; // The text of the # heading, e.g. "Chapter 1"
        private final List<Subchapter> subchapters = new ArrayList<>();
        private final StringBuilder chapterContent = new StringBuilder(); // Content under # heading
        private int subchapterIndexCounter = 0;
        private Subchapter currentSubchapter = null;

        public Chapter(int index, String headingText) {
            this.index = index;
            this.headingText = headingText;
        }

        /**
         * Call this when encountering a level-2 heading.
         */
        public void addSubchapter(String headingText) {
            // End the current subchapter if one is open
            this.currentSubchapter = null;

            // Create a new subchapter
            subchapterIndexCounter++;
            Subchapter sub = new Subchapter(subchapterIndexCounter, headingText);
            subchapters.add(sub);

            // Mark as current subchapter so further text goes here
            this.currentSubchapter = sub;
        }

        /**
         * Append non-heading text to the current subchapter (if present) or to the chapter content otherwise.
         */
        public void appendContentToCurrentSubchapterOrChapter(String content) {
            if (currentSubchapter == null) {
                chapterContent.append(content).append("\n");
            } else {
                currentSubchapter.appendContent(content);
            }
        }

        /**
         * Returns the integer index for this chapter.
         */
        public int getIndex() {
            return index;
        }

        /**
         * Returns the full Markdown for this chapter, including the # heading line plus all content.
         */
        public String getFullMD() {
            StringBuilder sb = new StringBuilder();
            // The heading line (including the # symbol)
            sb.append("# ").append(headingText).append("\n");
            // The chapter content (before any subchapters)
            if (chapterContent.length() > 0) {
                sb.append(chapterContent);
            }
            // Each subchapter’s full MD
            for (Subchapter sc : subchapters) {
                sb.append(sc.getFullMD());
            }
            return sb.toString();
        }

        /**
         * Returns the content of this chapter (excluding the # heading line, but *including* subchapters).
         * If you want to exclude the subchapters in the content, you can do so by adjusting the logic.
         */
        public String getContent() {
            StringBuilder sb = new StringBuilder();
            if (chapterContent.length() > 0) {
                sb.append(chapterContent);
            }
            for (Subchapter sc : subchapters) {
                // Add subchapter content (excluding the subchapter heading itself).
                // If you do NOT want subchapter content included, remove this loop.
                sb.append(sc.getContent());
            }
            return sb.toString();
        }

        /**
         * Returns the plain text for the chapter’s heading (no # prefix).
         */
        public String getHeadingText() {
            return headingText;
        }

        public List<Subchapter> getSubchapters() {
            return subchapters;
        }
    }

    /**
     * Represents a subchapter with an index, a heading, and content.
     */
    public static class Subchapter {
        private final int index;
        private final String headingText;  // The text of the ## heading
        private final StringBuilder content = new StringBuilder();

        public Subchapter(int index, String headingText) {
            this.index = index;
            this.headingText = headingText;
        }

        /**
         * Append text to this subchapter.
         */
        public void appendContent(String c) {
            content.append(c).append("\n");
        }

        /**
         * The subchapter index (starts from 1 for the first subchapter in a chapter).
         */
        public int getIndex() {
            return index;
        }

        /**
         * Returns the subchapter heading text (excluding ##).
         */
        public String getHeadingText() {
            return headingText;
        }

        /**
         * Returns the full Markdown for this subchapter, including the ## heading plus content.
         */
        public String getFullMD() {
            StringBuilder sb = new StringBuilder();
            sb.append("## ").append(headingText).append("\n");
            if (content.length() > 0) {
                sb.append(content);
            }
            return sb.toString();
        }

        /**
         * Returns only the content of this subchapter, excluding the ## heading line.
         */
        public String getContent() {
            return content.toString();
        }
    }
}
