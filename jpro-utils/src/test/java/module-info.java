/**
 * Module descriptor for the JPro Utils Test module.
 *
 * @author Besmir Beqiri
 */
module one.jpro.platform.utils.test {
    requires one.jpro.platform.utils;
    requires one.jpro.jmemorybuddy;
    requires org.slf4j;

    requires org.junit.jupiter;
    requires org.testfx.core;
    requires org.testfx.junit5;
    requires org.assertj.core;
    requires org.mockito;
    requires org.mockito.junit.jupiter;
    requires jdk.attach; // Required by Mockito, for Java 21 and later seems to not be required anymore since transitive

    exports one.jpro.platform.utils.test;
    opens one.jpro.platform.utils.test;
}