/**
 * Module descriptor for the JPro Platform Freeze Detector Test module.
 *
 * @author Besmir Beqiri
 */
module one.jpro.platform.freezedetector.test {
    requires one.jpro.platform.freezedetector;
    requires org.slf4j;

    requires org.junit.jupiter;
    requires org.testfx.core;
    requires org.testfx.junit5;
    requires org.assertj.core;

    opens one.jpro.platform.freezedetector.test;
}