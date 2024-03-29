module one.jpro.platform.mdfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires flexmark;
    requires flexmark.ext.attributes;
    requires flexmark.ext.tables;
    requires flexmark.ext.gfm.strikethrough;
    requires flexmark.ext.gfm.tasklist;
    requires flexmark.util;
    requires flexmark.util.ast;
    requires flexmark.util.builder;
    requires flexmark.util.misc;
    requires flexmark.util.sequence;
    requires flexmark.util.data;
    requires flexmark.util.collection;
    requires one.jpro.platform.youtube;

    opens one.jpro.platform.mdfx;
    exports one.jpro.platform.mdfx;
    exports one.jpro.platform.mdfx.extensions;
    opens one.jpro.platform.mdfx.extensions;
}