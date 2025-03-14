/**
 * Module descriptor for the JPro Platform Media Test module.
 *
 * @author Besmir Beqiri
 */
open module one.jpro.platform.media.test {
    requires one.jpro.platform.media;
    requires org.slf4j;

    requires org.junit.jupiter;
    requires org.testfx.core;
    requires org.testfx.junit5;
    requires org.assertj.core;

    exports one.jpro.platform.media.test;
}